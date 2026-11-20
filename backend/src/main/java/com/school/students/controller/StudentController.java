package com.school.students.controller;

import com.school.common.dto.PageResponse;
import com.school.students.dto.StudentRequest;
import com.school.students.dto.StudentResponse;
import com.school.students.model.Student;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import com.school.students.repository.StudentPhotoRepository;
import com.school.students.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentPhotoRepository photos;

    public StudentController(StudentService studentService, StudentPhotoRepository photos) {
        this.studentService = studentService;
        this.photos = photos;
    }

    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(required = false) StudentStatus status,
                                               @RequestParam(required = false) UUID assignedTeacherId,
                                               @RequestParam(required = false) Subject subject,
                                               @RequestParam(required = false) UUID parentId,
                                               @RequestParam(required = false) String search,
                                               @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        Page<Student> page = studentService.list(status, assignedTeacherId, subject, parentId, search, pageable);
        // One small query for the whole page (ids + timestamps only, never image bytes).
        Map<UUID, Instant> photoTimes = photos.photoTimes(page.map(Student::getId).getContent());
        return PageResponse.of(page.map(s -> StudentResponse.from(s, photoTimes.get(s.getId()))));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable UUID id) {
        return withPhotoTime(studentService.get(id));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = withPhotoTime(studentService.create(request));
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return withPhotoTime(studentService.update(id, request));
    }

    private StudentResponse withPhotoTime(Student student) {
        return StudentResponse.from(student, photos.photoTimes(List.of(student.getId())).get(student.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
