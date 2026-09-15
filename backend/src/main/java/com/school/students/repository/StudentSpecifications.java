package com.school.students.repository;

import com.school.parents.model.Parent;
import com.school.students.model.Student;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Filters for GET /api/students — see docs/API.md and docs/PROJECT_SCOPE.md
 * §27 (search by name/parent/phone/level/subject/teacher/status).
 */
public final class StudentSpecifications {

    private StudentSpecifications() {
    }

    public static Specification<Student> hasStatus(StudentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Student> hasAssignedTeacher(UUID teacherId) {
        return (root, query, cb) -> teacherId == null ? null : cb.equal(root.get("assignedTeacherId"), teacherId);
    }

    public static Specification<Student> hasSubject(Subject subject) {
        return (root, query, cb) -> subject == null ? null : cb.isMember(subject, root.get("subjects"));
    }

    public static Specification<Student> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            if (query != null) {
                query.distinct(true);
            }
            String like = "%" + search.toLowerCase() + "%";
            Join<Student, Parent> parents = root.join("parents", JoinType.LEFT);
            Predicate byStudent = cb.or(
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like),
                    cb.like(root.get("phone"), "%" + search + "%"));
            Predicate byParent = cb.or(
                    cb.like(cb.lower(parents.get("firstName")), like),
                    cb.like(cb.lower(parents.get("lastName")), like),
                    cb.like(parents.get("phone"), "%" + search + "%"));
            return cb.or(byStudent, byParent);
        };
    }
}
