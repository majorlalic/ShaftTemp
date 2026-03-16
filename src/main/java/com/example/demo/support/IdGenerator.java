package com.example.demo.support;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    private static final int MAX_SEQUENCE = 999;
    private long lastMillis = -1L;
    private final AtomicInteger sequence = new AtomicInteger(0);

    public synchronized long nextId() {
        long currentMillis = Instant.now().toEpochMilli();
        if (currentMillis == lastMillis) {
            int next = sequence.incrementAndGet();
            if (next > MAX_SEQUENCE) {
                while (currentMillis <= lastMillis) {
                    currentMillis = Instant.now().toEpochMilli();
                }
                sequence.set(0);
            }
        } else {
            sequence.set(0);
        }
        lastMillis = currentMillis;
        return currentMillis * 1000 + sequence.get();
    }
}
