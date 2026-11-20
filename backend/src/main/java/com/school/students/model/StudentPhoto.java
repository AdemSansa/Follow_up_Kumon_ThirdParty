package com.school.students.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A student's profile picture (JPEG/PNG/WebP, small). */
@Entity
@Table(name = "student_photos")
public class StudentPhoto {

    @Id
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentPhoto() {
        // JPA
    }

    public StudentPhoto(UUID studentId) {
        this.studentId = studentId;
    }

    public void replace(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
