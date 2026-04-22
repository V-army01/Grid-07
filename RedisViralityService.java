package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisViralityService {

    private static final int  BOT_REPLY_PTS     = 1;
    private static final int  HUMAN_LIKE_PTS    = 20;
    private static final int  HUMAN_COMMENT_PTS = 50;
    private static final long MAX_BOT_REPLIES   = 100L;
    private static final int  MAX_DEPTH         = 20;
    private static final long COOLDOWN_MINS     = 10L;

    private final StringRedisTemplate redis;

    // ── Virality score
    public void incrementOnBotReply(Long postId) {
        Long result = redis.opsForValue().increment(viralityKey(postId), BOT_REPLY_PTS);
        if (result != null && result == BOT_REPLY_PTS) {
            redis.expire(viralityKey(postId), Duration.ofDays(30));
        }
    }

    public void incrementOnHumanLike(Long postId) {
        redis.opsForValue().increment(viralityKey(postId), HUMAN_LIKE_PTS);
    }

    public void incrementOnHumanComment(Long postId) {
        redis.opsForValue().increment(viralityKey(postId), HUMAN_COMMENT_PTS);
    }

    public long getViralityScore(Long postId) {
        String v = redis.opsForValue().get(viralityKey(postId));
        try {
            return v == null ? 0L : Long.parseLong(v);
        } catch (NumberFormatException e) {
            log.error("Corrupt Redis value for key, resetting: {}", v);
            return 0L;
        }
    }

    // ── Horizontal Cap 
    public boolean tryIncrementBotCount(Long postId) {
        Long count = redis.opsForValue().increment(botCountKey(postId));
        if (count == null || count > MAX_BOT_REPLIES) {
            redis.opsForValue().decrement(botCountKey(postId));
            log.warn("Horizontal cap reached for post {}. Rejected.", postId);
            return false;
        }
        return true;
    }

    public void decrementBotCount(Long postId) {
        redis.opsForValue().decrement(botCountKey(postId));
    }

    public long getBotCount(Long postId) {
        String v = redis.opsForValue().get(botCountKey(postId));
        try {
            return v == null ? 0L : Long.parseLong(v);
        } catch (NumberFormatException e) {
            log.error("Corrupt Redis value for key, resetting: {}", v);
            return 0L;
        }
    }

    // ── Vertical Cap 
    public boolean isDepthAllowed(int depth) {
        return depth <= MAX_DEPTH;
    }

    // ── Cooldown Cap 
    public boolean tryAcquireCooldown(Long botId, Long humanId) {
        Boolean set = redis.opsForValue()
                .setIfAbsent(cooldownKey(botId, humanId), "1",
                        Duration.ofMinutes(COOLDOWN_MINS));
        if (Boolean.FALSE.equals(set)) {
            log.warn("Cooldown: bot {} blocked from human {} for 10 min.", botId, humanId);
            return false;
        }
        return true;
    }

    // ── Key helpers 
    private String viralityKey(Long postId)  { return "post:" + postId + ":virality_score"; }
    private String botCountKey(Long postId)  { return "post:" + postId + ":bot_count"; }
    private String cooldownKey(Long bId, Long hId){ return "cooldown:bot_" + bId + ":human_" + hId; }
}