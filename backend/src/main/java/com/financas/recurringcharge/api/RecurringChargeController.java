package com.financas.recurringcharge.api;

import com.financas.fund.api.FundResponse;
import com.financas.fund.domain.FundService;
import com.financas.recurringcharge.domain.RecurringCharge;
import com.financas.recurringcharge.domain.RecurringChargeService;
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
@RequestMapping("/api/recurring-charges")
public class RecurringChargeController {

    private final RecurringChargeService service;
    private final FundService fundService;

    public RecurringChargeController(RecurringChargeService service, FundService fundService) {
        this.service = service;
        this.fundService = fundService;
    }

    @GetMapping
    public List<RecurringChargeResponse> findAll() {
        return service.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RecurringChargeResponse findById(@PathVariable Long id) {
        return toResponse(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<RecurringChargeResponse> create(@Valid @RequestBody RecurringChargeRequest request) {
        RecurringCharge recurringCharge = service.create(
                request.type(),
                request.amount(),
                request.dueDay(),
                request.description(),
                request.fundId(),
                request.partyId(),
                request.observations());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(recurringCharge));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<RecurringChargeResponse>> createBulk(
            @Valid @RequestBody RecurringChargeBulkRequest request) {
        List<RecurringChargeResponse> created = service
                .createForGroup(
                        request.type(),
                        request.amount(),
                        request.dueDay(),
                        request.description(),
                        request.fundId(),
                        request.groupId(),
                        request.observations())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public RecurringChargeResponse update(@PathVariable Long id, @Valid @RequestBody RecurringChargeRequest request) {
        RecurringCharge recurringCharge = service.update(
                id,
                request.type(),
                request.amount(),
                request.dueDay(),
                request.description(),
                request.fundId(),
                request.partyId(),
                request.observations());
        return toResponse(recurringCharge);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private RecurringChargeResponse toResponse(RecurringCharge recurringCharge) {
        FundResponse fundResponse = FundResponse.from(
                recurringCharge.getFund(), fundService.calculateRealBalance(recurringCharge.getFund()));
        return RecurringChargeResponse.from(recurringCharge, fundResponse);
    }
}
