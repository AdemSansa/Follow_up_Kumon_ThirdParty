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
 * Creates a first ADMIN and a first STAFF account on startup if the users
 * table is empty, so there's always a way to log in — and to test
 * role-specific behavior — on a fresh environment. Set
 * ADMIN_EMAIL/ADMIN_PASSWORD and STAFF_EMAIL/STAFF_PASSWORD to control
 * them; see docs/TEST_ACCOUNTS.md. Change these passwords before any
 * non-local deployment.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String staffEmail;
    private final String staffPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap-admin.email:admin@example.com}") String adminEmail,
                           @Value("${app.bootstrap-admin.password:changeme}") String adminPassword,
                           @Value("${app.bootstrap-staff.email:staff@example.com}") String staffEmail,
                           @Value("${app.bootstrap-staff.password:changeme}") String staffPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.staffEmail = staffEmail;
        this.staffPassword = staffPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User(adminEmail, passwordEncoder.encode(adminPassword), "Admin", "User", Role.ADMIN);
        userRepository.save(admin);
        User staff = new User(staffEmail, passwordEncoder.encode(staffPassword), "Staff", "User", Role.STAFF);
        userRepository.save(staff);
        log.warn("No users found — created bootstrap accounts ({}, {}). See docs/TEST_ACCOUNTS.md; change their "
                + "passwords immediately outside local dev.", adminEmail, staffEmail);
    }
}
