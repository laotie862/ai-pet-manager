package com.example.demo.admin;

import com.example.demo.common.security.SecurityProperties;
import com.example.demo.user.UserCreateCommand;
import com.example.demo.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            SecurityProperties securityProperties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isAdminBootstrapEnabled()) {
            return;
        }
        if (userRepository.findByAccount(securityProperties.getAdminEmail()).isPresent()) {
            return;
        }

        Long adminId = userRepository.create(new UserCreateCommand(
                securityProperties.getAdminEmail(),
                null,
                passwordEncoder.encode(securityProperties.getAdminPassword()),
                securityProperties.getAdminNickname(),
                "ADMIN",
                "ACTIVE"
        ));
        log.info("Default admin user created, id={}, email={}", adminId, securityProperties.getAdminEmail());
    }
}
