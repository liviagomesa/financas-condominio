package com.financas.resident.api;

import com.financas.resident.domain.Resident;
import com.financas.resident.domain.ResidentService;
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
@RequestMapping("/api/residents")
public class ResidentController {

    private final ResidentService service;

    public ResidentController(ResidentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResidentResponse> findAll() {
        return service.findAll().stream().map(ResidentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResidentResponse findById(@PathVariable Long id) {
        return ResidentResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResidentResponse> create(@Valid @RequestBody ResidentRequest request) {
        Resident resident = service.create(request.name(), request.unitId(), request.email(), request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResidentResponse.from(resident));
    }

    @PutMapping("/{id}")
    public ResidentResponse update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        Resident resident =
                service.update(id, request.name(), request.unitId(), request.email(), request.phone());
        return ResidentResponse.from(resident);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
