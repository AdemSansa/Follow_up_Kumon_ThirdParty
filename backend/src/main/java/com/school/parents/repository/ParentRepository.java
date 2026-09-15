package com.school.parents.repository;

import com.school.parents.model.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ParentRepository extends JpaRepository<Parent, UUID> {

    @Query("select p from Parent p where :search is null or "
            + "lower(p.firstName) like lower(concat('%', :search, '%')) or "
            + "lower(p.lastName) like lower(concat('%', :search, '%')) or "
            + "p.phone like concat('%', :search, '%') or "
            + "lower(p.email) like lower(concat('%', :search, '%'))")
    Page<Parent> search(@Param("search") String search, Pageable pageable);
}
