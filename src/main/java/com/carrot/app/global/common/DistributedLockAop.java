package com.carrot.app.global.common;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @DistributedLock 애노테이션이 선언된 메서드에 대해 lock을 수행하는 AOP.
 *                  트랜잭션(@Transactional, 순서: Ordered.LOWEST_PRECEDENCE)보다 먼저
 *                  실행되어야 하므로
 *                  Ordered.HIGHEST_PRECEDENCE로 설정
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_PREFIX = "LOCK:";

    @Around("@annotation(com.carrot.app.common.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(),
                joinPoint.getArgs(), distributedLock.key());

        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();

        long waitTimeMillis = timeUnit.toMillis(waitTime);
        long leaseTimeMillis = timeUnit.toMillis(leaseTime);
        long startTime = System.currentTimeMillis();

        String lockValue = java.util.UUID.randomUUID().toString(); // 락 소유자 식별값
        boolean available = false;

        try {
            while (!available) {
                // 락 획득 시도 (SETNX)
                // 키: key, 값: lockValue (UUID), TTL: leaseTimeMillis
                available = Boolean.TRUE.equals(redisTemplate.opsForValue()
                        .setIfAbsent(key, lockValue, leaseTimeMillis, TimeUnit.MILLISECONDS));

                if (available) {
                    break;
                }

                // 대기 시간 초과 확인
                if (System.currentTimeMillis() - startTime >= waitTimeMillis) {
                    throw new RuntimeException("Lock acquisition timed out for key: " + key);
                }

                // 재시도 텀 (예: 50ms)
                Thread.sleep(50);
            }

            log.info("Acquired lock for key: {}, value: {}", key, lockValue);
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for lock", e);
        } finally {
            if (available) {
                try {
                    // 락 해제 시 본인이 소유한 락인지 확인
                    String currentValue = (String) redisTemplate.opsForValue().get(key);
                    if (lockValue.equals(currentValue)) {
                        redisTemplate.delete(key);
                        log.info("Released lock for key: {}, value: {}", key, lockValue);
                    } else {
                        log.warn(
                                "Lock release skipped. Current lock value mismatch or expired. key: {}, myValue: {}, dbValue: {}",
                                key, lockValue, currentValue);
                    }
                } catch (Exception e) {
                    log.error("Failed to release lock for key: {}", key, e);
                }
            }
        }
    }
}
