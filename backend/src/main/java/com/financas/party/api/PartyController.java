package com.financas.party.api;

import com.financas.party.domain.Party;
import com.financas.party.domain.PartyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService service;

    public PartyController(PartyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PartyResponse> findAll() {
        return service.findAll().stream().map(PartyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PartyResponse findById(@PathVariable Long id) {
        return PartyResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PartyResponse> create(@Valid @RequestBody PartyRequest request) {
        Party party = service.create(request.name(), request.pixKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(PartyResponse.from(party));
    }

    @PutMapping("/{id}")
    public PartyResponse update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        return PartyResponse.from(service.update(id, request.name(), request.pixKey()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
