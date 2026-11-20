package com.school.students.controller;

import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.students.model.StudentPhoto;
import com.school.students.repository.StudentPhotoRepository;
import com.school.students.repository.StudentRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Student profile picture. Requires login like every student endpoint
 * (children's photos are never public). The type is decided from the file's
 * own bytes, not the name or browser-declared type.
 */
@RestController
@RequestMapping("/api/students/{id}/photo")
public class StudentPhotoController {

    static final int MAX_BYTES = 2 * 1024 * 1024;

    private final StudentRepository students;
    private final StudentPhotoRepository photos;

    public StudentPhotoController(StudentRepository students, StudentPhotoRepository photos) {
        this.students = students;
        this.photos = photos;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        StudentPhoto photo = photos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("This student has no photo"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.getData());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Void> upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file)
            throws IOException {
        if (!students.existsById(id)) {
            throw new ResourceNotFoundException("No student with id " + id);
        }
        if (file.isEmpty()) {
            throw new BadRequestException("The photo file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("The photo is too large — maximum 2 MB");
        }
        byte[] data = file.getBytes();
        String type = detectImageType(data);
        if (type == null) {
            throw new BadRequestException("Please upload a JPEG, PNG or WebP image");
        }
        StudentPhoto photo = photos.findById(id).orElseGet(() -> new StudentPhoto(id));
        photo.replace(type, data);
        photos.save(photo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        photos.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** JPEG / PNG / WebP by magic bytes; null for anything else (incl. SVG, which can carry scripts). */
    static String detectImageType(byte[] d) {
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (d.length > 8 && Arrays.equals(Arrays.copyOf(d, 8),
                new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
