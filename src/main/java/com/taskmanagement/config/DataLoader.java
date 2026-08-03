package com.taskmanagement.config;

import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Role;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("📝 LOADING DEFAULT USERS");
        log.info("========================================");

        if (!userRepository.findByUsername("admin").isPresent()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@tms.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Admin User")
                    .phone("+251911111111")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Created ADMIN: admin / admin123");
        }

        if (!userRepository.findByUsername("manager").isPresent()) {
            User manager = User.builder()
                    .username("manager")
                    .email("manager@tms.com")
                    .password(passwordEncoder.encode("manager123"))
                    .name("Manager User")
                    .phone("+251912222222")
                    .role(Role.MANAGER)
                    .isActive(true)
                    .build();
            userRepository.save(manager);
            log.info("✅ Created MANAGER: manager / manager123");
        }

        if (!userRepository.findByUsername("worker").isPresent()) {
            User worker = User.builder()
                    .username("worker")
                    .email("worker@tms.com")
                    .password(passwordEncoder.encode("worker123"))
                    .name("Worker User")
                    .phone("+251913333333")
                    .role(Role.WORKER)
                    .isActive(true)
                    .build();
            userRepository.save(worker);
            log.info("✅ Created WORKER: worker / worker123");
        }

        log.info("========================================");
        log.info("📋 Default users loaded successfully!");
        log.info("========================================");
    }
}