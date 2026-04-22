package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long COOLDOWN_MINS = 15L;
    private final StringRedisTemplate redis;

    public void handleBotInteractionNotification(Long userId, Long botId,
                                                  String botName, String action) {
        String cooldownKey = notifCooldownKey(userId);
        String message     = "Bot " + botName + " " + action;

        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            redis.opsForList().rightPush(pendingNotifsKey(userId), message);
            log.info("Notification queued for user {}: {}", userId, message);
        } else {
            log.info("Push Notification Sent to User {}: {}", userId, message);
            redis.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(COOLDOWN_MINS));
        }
    }
    public void flushPendingNotifications(Long userId) {
        String       key     = pendingNotifsKey(userId);
        List<String> pending = redis.opsForList().range(key, 0, -1);
        if (pending == null || pending.isEmpty()) return;

        int    total   = pending.size();
        String first   = pending.get(0);
        String botName = (first.startsWith("Bot ") && first.split(" ").length > 1)
                       ? first.split(" ")[1] : "Unknown";

        // Log summarized notification
        if (total == 1) {
            log.info("Summarized Push Notification to user {}: {}", userId, first);
        } else {
            log.info("Summarized Push Notification to user {}: Bot {} and [{}] others interacted with your posts.",
                    userId, botName, total - 1);
        }
        redis.opsForList().trim(key, total, -1);
        redis.delete(notifCooldownKey(userId));
    }

    private String pendingNotifsKey(Long userId) {
        return "user:" + userId + ":pending_notifs";
    }

    private String notifCooldownKey(Long userId) {
        return "notif:cooldown:user_" + userId;
    }
}
