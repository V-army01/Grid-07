package com.grid07.service;

import com.grid07.dto.CreateBotRequest;
import com.grid07.entity.Bot;
import com.grid07.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BotService {

    private final BotRepository botRepository;

    @Transactional
    public Bot createBot(CreateBotRequest req) {
        return botRepository.save(Bot.builder()
                .name(req.getName())
                .personaDescription(req.getPersonaDescription())
                .build());
    }

    public Bot getBot(Long id) {
        return botRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Bot not found: " + id));
    }
}