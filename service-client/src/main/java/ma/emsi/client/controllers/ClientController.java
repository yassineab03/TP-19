package ma.emsi.client.controllers;

import java.net.URI;
import java.util.List;
import ma.emsi.client.entities.Client;
import ma.emsi.client.services.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping("/clients")
    public List<Client> findAll() {
        return service.findAll();
    }

    @GetMapping({"/client/{id}", "/clients/{id}"})
    public Client findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/clients")
    public ResponseEntity<Client> create(@RequestBody Client client) {
        Client created = service.create(client);
        return ResponseEntity.created(URI.create("/client/" + created.getId())).body(created);
    }

    @PutMapping("/clients/{id}")
    public Client update(@PathVariable Long id, @RequestBody Client client) {
        return service.update(id, client);
    }

    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
