package com.grid07.config;

import com.grid07.entity.Bot;
import com.grid07.entity.User;
import com.grid07.repository.BotRepository;
import com.grid07.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds 2 users and 2 bots on first boot so you can test immediately.
 * user id=1 (alice), id=2 (bob)
 * bot  id=1 (BotAlpha), id=2 (BotBeta)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BotRepository  botRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder().username("alice").premium(true).build());
            userRepository.save(User.builder().username("bob").premium(false).build());
            log.info("Seeded users: alice (id=1), bob (id=2)");
        }
        if (botRepository.count() == 0) {
            botRepository.save(Bot.builder().name("BotAlpha").personaDescription("Friendly engagement bot").build());
            botRepository.save(Bot.builder().name("BotBeta").personaDescription("Content recommender bot").build());
            log.info("Seeded bots: BotAlpha (id=1), BotBeta (id=2)");
        }
    }
}