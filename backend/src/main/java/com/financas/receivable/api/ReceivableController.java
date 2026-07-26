package com.financas.receivable.api;

import com.financas.receivable.domain.Receivable;
import com.financas.receivable.domain.ReceivableService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receivables")
public class ReceivableController {

    private final ReceivableService service;

    public ReceivableController(ReceivableService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReceivableResponse> findAll(@RequestParam(required = false) Long unitId) {
        return service.findAll(unitId).stream().map(ReceivableResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReceivableResponse findById(@PathVariable Long id) {
        return ReceivableResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody ReceivableRequest request) {
        Receivable receivable = service.create(
                request.amount(),
                request.dueDate(),
                request.description(),
                request.targetAccount(),
                request.recurring(),
                request.unitId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReceivableResponse.from(receivable));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ReceivableResponse>> createBulk(
            @Valid @RequestBody ReceivableBulkRequest request) {
        List<ReceivableResponse> created = service
                .createForAllUnits(
                        request.amount(),
                        request.dueDate(),
                        request.description(),
                        request.targetAccount(),
                        request.recurring())
                .stream()
                .map(ReceivableResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ReceivableResponse update(@PathVariable Long id, @Valid @RequestBody ReceivableRequest request) {
        Receivable receivable = service.update(
                id,
                request.amount(),
                request.dueDate(),
                request.description(),
                request.targetAccount(),
                request.recurring(),
                request.unitId());
        return ReceivableResponse.from(receivable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
