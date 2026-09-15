package com.school.auth;

import com.school.auth.model.Role;
import com.school.auth.model.User;
import com.school.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator account on startup if the users table is
 * empty, so there's always a way to log in on a fresh environment. Set
 * ADMIN_EMAIL / ADMIN_PASSWORD to control the bootstrap account; change the
 * password after first login.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap-admin.email:admin@example.com}") String adminEmail,
                           @Value("${app.bootstrap-admin.password:changeme}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User(adminEmail, passwordEncoder.encode(adminPassword), "Admin", "User", Role.ADMIN);
        userRepository.save(admin);
        log.warn("No users found — created bootstrap admin account ({}). Change its password immediately.",
                adminEmail);
    }
}
