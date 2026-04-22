package com.grid07;

import com.grid07.service.RedisViralityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
class ConcurrencyTest {

    @Autowired RedisViralityService viralityService;
    @Autowired StringRedisTemplate  redis;

    private static final Long TEST_POST_ID = Long.MAX_VALUE;

    @BeforeEach
    void clean() {
        redis.delete("post:" + TEST_POST_ID + ":bot_count");
    }

    @Test
    @Timeout(30)
    void horizontalCap_allows_exactly_100_under_200_concurrent_requests()throws InterruptedException {
        int threads   = 200;
        AtomicInteger allowed  = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);
        CountDownLatch start   = new CountDownLatch(1);
        CountDownLatch done    = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await(); 
                    if (viralityService.tryIncrementBotCount(TEST_POST_ID)) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown(); 
        done.await();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("Allowed: " + allowed.get() + " | Rejected: " + rejected.get());

        assertThat(allowed.get()).isEqualTo(100);
        assertThat(rejected.get()).isEqualTo(100);
        assertThat(viralityService.getBotCount(TEST_POST_ID)).isEqualTo(100L);
    }
}