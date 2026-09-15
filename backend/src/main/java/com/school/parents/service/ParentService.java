package com.school.parents.service;

import com.school.common.exception.ResourceNotFoundException;
import com.school.parents.model.Parent;
import com.school.parents.repository.ParentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;

    public ParentService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public Page<Parent> list(String search, Pageable pageable) {
        return parentRepository.search(search, pageable);
    }

    public Parent get(UUID id) {
        return parentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No parent with id " + id));
    }

    @Transactional
    public Parent create(Parent parent) {
        return parentRepository.save(parent);
    }

    @Transactional
    public Parent update(UUID id, Parent changes) {
        Parent existing = get(id);
        existing.setFirstName(changes.getFirstName());
        existing.setLastName(changes.getLastName());
        existing.setPhone(changes.getPhone());
        existing.setEmail(changes.getEmail());
        existing.setRelationship(changes.getRelationship());
        existing.setPreferredContactMethod(changes.getPreferredContactMethod());
        existing.setNotes(changes.getNotes());
        return existing;
    }
}
