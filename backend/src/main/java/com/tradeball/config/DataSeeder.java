package com.tradeball.config;

import com.tradeball.domain.Role;
import com.tradeball.entity.UserEntity;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.UserRepository;
import com.tradeball.service.NbaDataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an admin user and initial player/stats data for local development.
 */
@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final NbaDataSyncService nbaDataSyncService;

    public DataSeeder(UserRepository userRepository,
                      PlayerRepository playerRepository,
                      PasswordEncoder passwordEncoder,
                      NbaDataSyncService nbaDataSyncService) {
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.nbaDataSyncService = nbaDataSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmailIgnoreCase("admin@tradeball.local")) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@tradeball.local");
            admin.setDisplayName("Admin");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            userRepository.save(admin);
            log.info("Seeded admin user admin@tradeball.local");
        }
        if (!userRepository.existsByEmailIgnoreCase("demo@tradeball.local")) {
            UserEntity demo = new UserEntity();
            demo.setEmail("demo@tradeball.local");
            demo.setDisplayName("Demo User");
            demo.setRole(Role.USER);
            demo.setPasswordHash(passwordEncoder.encode("Demo1234!"));
            userRepository.save(demo);
            log.info("Seeded demo user demo@tradeball.local");
        }
        if (playerRepository.count() == 0) {
            log.info("No players found — running initial stats sync");
        } else {
            log.info("Refreshing player stats from the NBA provider");
        }
        nbaDataSyncService.syncStats();
    }
}
