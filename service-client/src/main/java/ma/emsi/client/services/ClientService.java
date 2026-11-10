package ma.emsi.client.services;

import java.util.List;
import ma.emsi.client.entities.Client;
import ma.emsi.client.repositories.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ClientService {
    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Client findById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Client non trouvé"));
    }

    public Client create(Client client) {
        client.setId(null);
        return repository.save(client);
    }

    public Client update(Long id, Client input) {
        Client client = findById(id);
        client.setNom(input.getNom());
        client.setAge(input.getAge());
        return repository.save(client);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
