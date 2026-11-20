package com.school.students.repository;

import com.school.students.model.StudentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface StudentPhotoRepository extends JpaRepository<StudentPhoto, UUID> {

    /** Which of these students have a photo, and when it last changed — without reading the image bytes. */
    interface PhotoStamp {
        UUID getStudentId();

        Instant getUpdatedAt();
    }

    @Query("select p.studentId as studentId, p.updatedAt as updatedAt from StudentPhoto p where p.studentId in :ids")
    List<PhotoStamp> findStamps(@Param("ids") Collection<UUID> studentIds);

    default Map<UUID, Instant> photoTimes(Collection<UUID> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return findStamps(studentIds).stream()
                .collect(Collectors.toMap(PhotoStamp::getStudentId, PhotoStamp::getUpdatedAt));
    }
}
