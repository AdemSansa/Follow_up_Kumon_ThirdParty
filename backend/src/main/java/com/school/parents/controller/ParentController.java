package com.school.parents.controller;

import com.school.common.dto.PageResponse;
import com.school.parents.dto.ParentRequest;
import com.school.parents.dto.ParentResponse;
import com.school.parents.model.Parent;
import com.school.parents.service.ParentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping
    public PageResponse<ParentResponse> list(@RequestParam(required = false) String search,
                                              @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return PageResponse.of(parentService.list(search, pageable).map(ParentResponse::from));
    }

    @GetMapping("/{id}")
    public ParentResponse get(@PathVariable UUID id) {
        return ParentResponse.from(parentService.get(id));
    }

    @PostMapping
    public ResponseEntity<ParentResponse> create(@Valid @RequestBody ParentRequest request) {
        Parent created = parentService.create(toEntity(request));
        return ResponseEntity.status(201).body(ParentResponse.from(created));
    }

    @PutMapping("/{id}")
    public ParentResponse update(@PathVariable UUID id, @Valid @RequestBody ParentRequest request) {
        return ParentResponse.from(parentService.update(id, toEntity(request)));
    }

    private Parent toEntity(ParentRequest request) {
        return new Parent(request.firstName(), request.lastName(), request.phone(), request.email(),
                request.relationship(), request.preferredContactMethod(), request.notes());
    }
}
