package com.school.students.service;

import com.school.auth.repository.UserRepository;
import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.parents.model.Parent;
import com.school.parents.repository.ParentRepository;
import com.school.students.dto.StudentRequest;
import com.school.students.model.Student;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import com.school.students.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.school.students.repository.StudentSpecifications.hasAssignedTeacher;
import static com.school.students.repository.StudentSpecifications.hasStatus;
import static com.school.students.repository.StudentSpecifications.hasSubject;
import static com.school.students.repository.StudentSpecifications.matchesSearch;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, ParentRepository parentRepository,
                           UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
    }

    public Page<Student> list(StudentStatus status, UUID assignedTeacherId, Subject subject, String search,
                               Pageable pageable) {
        Specification<Student> spec = Specification.allOf(
                hasStatus(status),
                hasAssignedTeacher(assignedTeacherId),
                hasSubject(subject),
                matchesSearch(search));
        return studentRepository.findAll(spec, pageable);
    }

    public Student get(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No student with id " + id));
    }

    @Transactional
    public Student create(StudentRequest request) {
        Student student = new Student(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.gender(),
                request.phone(), request.email(), request.address(), request.enrollmentDate(),
                request.status(), request.level(), request.subjects(),
                validatedTeacherId(request.assignedTeacherId()), request.notes());
        student.setParents(resolveParents(request.parentIds()));
        return studentRepository.save(student);
    }

    @Transactional
    public Student update(UUID id, StudentRequest request) {
        Student student = get(id);
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setPhone(request.phone());
        student.setEmail(request.email());
        student.setAddress(request.address());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setStatus(request.status());
        student.setLevel(request.level());
        student.setSubjects(request.subjects() != null ? new HashSet<>(request.subjects()) : new HashSet<>());
        student.setAssignedTeacherId(validatedTeacherId(request.assignedTeacherId()));
        student.setParents(resolveParents(request.parentIds()));
        student.setNotes(request.notes());
        return student;
    }

    @Transactional
    public void delete(UUID id) {
        Student student = get(id);
        studentRepository.delete(student);
    }

    private UUID validatedTeacherId(UUID teacherId) {
        if (teacherId == null) {
            return null;
        }
        if (!userRepository.existsById(teacherId)) {
            throw new BadRequestException("No staff member with id " + teacherId);
        }
        return teacherId;
    }

    private Set<Parent> resolveParents(Set<UUID> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Parent> parents = new HashSet<>(parentRepository.findAllById(parentIds));
        if (parents.size() != parentIds.size()) {
            throw new BadRequestException("One or more parentIds do not exist");
        }
        return parents;
    }
}
