package com.grid07.controller;

import com.grid07.dto.CreateBotRequest;
import com.grid07.entity.Bot;
import com.grid07.service.BotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotController {
    private final BotService botService;

    @PostMapping
    public ResponseEntity<Bot> createBot(@Valid @RequestBody CreateBotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(botService.createBot(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bot> getBot(@PathVariable Long id) {
        return ResponseEntity.ok(botService.getBot(id));
    }
}