package com.school.students.controller;

import com.school.common.dto.PageResponse;
import com.school.students.dto.StudentRequest;
import com.school.students.dto.StudentResponse;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import com.school.students.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(required = false) StudentStatus status,
                                               @RequestParam(required = false) UUID assignedTeacherId,
                                               @RequestParam(required = false) Subject subject,
                                               @RequestParam(required = false) String search,
                                               @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return PageResponse.of(studentService.list(status, assignedTeacherId, subject, search, pageable)
                .map(StudentResponse::from));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable UUID id) {
        return StudentResponse.from(studentService.get(id));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = StudentResponse.from(studentService.create(request));
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return StudentResponse.from(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
