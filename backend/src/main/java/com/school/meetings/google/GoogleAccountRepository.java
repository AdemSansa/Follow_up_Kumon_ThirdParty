package com.school.meetings.google;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, UUID> {

    Optional<GoogleAccount> findByUserId(UUID userId);

    Optional<GoogleAccount> findFirstByCenterTrue();
}
