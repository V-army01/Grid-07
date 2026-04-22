package com.grid07.scheduler;

import com.grid07.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final StringRedisTemplate redis;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 5 * 60 * 1000L)  
    public void sweepPendingNotifications() {
        log.info("CRON Sweeper: scanning pending notifications...");

        Set<String> keys = redis.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) {
            log.info("CRON Sweeper: nothing to process.");
            return;
        }

        for (String key : keys) {
            try {
                
                String[] parts = key.split(":");
                if (parts.length < 2) {
                    log.warn("Unexpected key format: {}", key);
                    continue;
                }
                long userId = Long.parseLong(parts[1]);
                notificationService.flushPendingNotifications(userId);
            } catch (Exception e) {
                log.error("CRON Sweeper: error on key {}: {}", key, e.getMessage());
            }
        }

        log.info("CRON Sweeper: sweep complete ({} users processed).", keys.size());
    }
}