package ma.emsi.client;

import ma.emsi.client.entities.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class ClientApiTest {
    @Autowired
    private TestRestTemplate http;

    @Test
    void seededClientsAndCrud() {
        Client[] initial = http.getForObject("/clients", Client[].class);
        assertThat(initial).hasSize(3);
        assertThat(http.getForObject("/client/1", Client.class).getNom()).isEqualTo("Rabab SELIMANI");

        ResponseEntity<Client> created = http.postForEntity("/clients",
                new Client(999L, "Test", 30f), Client.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Client client = created.getBody();
        assertThat(client).isNotNull();
        assertThat(client.getId()).isNotEqualTo(999L);
        String path = "/clients/" + client.getId();
        assertThat(created.getHeaders().getLocation()).hasToString("/client/" + client.getId());

        ResponseEntity<Client> updated = http.exchange(path, HttpMethod.PUT,
                new HttpEntity<>(new Client(null, "Modifié", 31f)), Client.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getForObject(path, Client.class).getNom()).isEqualTo("Modifié");
        assertThat(http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.exchange(path, HttpMethod.PUT,
                new HttpEntity<>(new Client(null, "Absent", 20f)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/actuator/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
