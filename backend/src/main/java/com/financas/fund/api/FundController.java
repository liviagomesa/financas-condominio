package com.financas.fund.api;

import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundService;
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
@RequestMapping("/api/funds")
public class FundController {

    private final FundService service;

    public FundController(FundService service) {
        this.service = service;
    }

    @GetMapping
    public List<FundResponse> findAll() {
        return service.findAll().stream()
                .map(fund -> FundResponse.from(fund, service.calculateRealBalance(fund)))
                .toList();
    }

    @GetMapping("/{id}")
    public FundResponse findById(@PathVariable Long id) {
        Fund fund = service.findById(id);
        return FundResponse.from(fund, service.calculateRealBalance(fund));
    }

    @PostMapping
    public ResponseEntity<FundResponse> create(@Valid @RequestBody FundRequest request) {
        Fund fund = service.create(request.name(), request.initialBalance());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FundResponse.from(fund, service.calculateRealBalance(fund)));
    }

    @PutMapping("/{id}")
    public FundResponse update(@PathVariable Long id, @Valid @RequestBody FundRequest request) {
        Fund fund = service.update(id, request.name(), request.initialBalance());
        return FundResponse.from(fund, service.calculateRealBalance(fund));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
