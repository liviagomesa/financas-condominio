package com.financas.group.api;

import com.financas.group.domain.Group;
import com.financas.group.domain.GroupService;
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
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<GroupResponse> findAll() {
        return service.findAll().stream().map(GroupResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GroupResponse findById(@PathVariable Long id) {
        return GroupResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
        Group group = service.create(request.name(), request.partyIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(GroupResponse.from(group));
    }

    @PutMapping("/{id}")
    public GroupResponse update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return GroupResponse.from(service.update(id, request.name(), request.partyIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
