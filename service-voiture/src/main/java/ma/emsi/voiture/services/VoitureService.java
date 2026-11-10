package ma.emsi.voiture.services;

import feign.FeignException;
import java.util.List;
import ma.emsi.voiture.clients.ClientService;
import ma.emsi.voiture.entities.Voiture;
import ma.emsi.voiture.repositories.VoitureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoitureService {
    private final VoitureRepository repository;
    private final ClientService clientService;

    public VoitureService(VoitureRepository repository, ClientService clientService) {
        this.repository = repository;
        this.clientService = clientService;
    }

    public List<Voiture> findAll() {
        return repository.findAll().stream().map(this::withClient).toList();
    }

    public Voiture findById(Long id) {
        return withClient(repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Voiture non trouvée")));
    }

    public List<Voiture> findByClientId(Long clientId) {
        return repository.findByClientId(clientId).stream().map(this::withClient).toList();
    }

    private Voiture withClient(Voiture voiture) {
        try {
            voiture.setClient(clientService.clientById(voiture.getClientId()));
            return voiture;
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Le client associé à la voiture n'existe plus", exception);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE-CLIENT indisponible", exception);
        }
    }
}
