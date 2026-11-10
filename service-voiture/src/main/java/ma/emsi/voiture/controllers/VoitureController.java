package ma.emsi.voiture.controllers;

import java.util.List;
import ma.emsi.voiture.entities.Voiture;
import ma.emsi.voiture.services.VoitureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoitureController {
    private final VoitureService service;

    public VoitureController(VoitureService service) {
        this.service = service;
    }

    @GetMapping("/voitures")
    public List<Voiture> findAll() {
        return service.findAll();
    }

    @GetMapping({"/voiture/{id}", "/voitures/{id}"})
    public Voiture findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/voitures/client/{clientId}")
    public List<Voiture> findByClientId(@PathVariable Long clientId) {
        return service.findByClientId(clientId);
    }
}
