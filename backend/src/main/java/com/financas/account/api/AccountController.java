package com.financas.account.api;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountService;
import com.financas.account.domain.AccountType;
import com.financas.fund.api.FundSummaryResponse;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountResponse> findAll(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long fundId,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String dueYearMonth,
            @RequestParam(required = false) String paymentYearMonth) {
        return service.findAll(partyId, fundId, type, paid, overdue, dueYearMonth, paymentYearMonth).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        return toResponse(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        Account account = service.create(
                request.type(),
                request.amount(),
                request.dueDate(),
                request.description(),
                request.fundId(),
                request.partyId(),
                request.paymentDate(),
                request.observations());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AccountResponse>> createBulk(@Valid @RequestBody AccountBulkRequest request) {
        List<AccountResponse> created = service
                .createForGroup(
                        request.type(),
                        request.amount(),
                        request.dueDate(),
                        request.description(),
                        request.fundId(),
                        request.groupId(),
                        request.paymentDate(),
                        request.observations())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        Account account = service.update(
                id,
                request.type(),
                request.amount(),
                request.dueDate(),
                request.description(),
                request.fundId(),
                request.partyId(),
                request.paymentDate(),
                request.observations());
        return toResponse(account);
    }

    @PostMapping("/{id}/pay")
    public AccountResponse pay(@PathVariable Long id, @Valid @RequestBody AccountPaymentRequest request) {
        Account account = service.registerPayment(id, request.paymentDate(), request.paidAmount());
        return toResponse(account);
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<AccountResponse> duplicate(
            @PathVariable Long id, @Valid @RequestBody AccountDuplicateRequest request) {
        Account account = service.duplicate(id, request.zeroAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.from(account, FundSummaryResponse.from(account.getFund()));
    }
}
