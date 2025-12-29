package com.carrot.app.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRESENCE_KEY_PREFIX = "user:";
    private static final String PRESENCE_KEY_SUFFIX = ":presence";
    // session:{sessionId} -> userId (Reverse Lookup for Disconnect stability)
    private static final String SESSION_KEY_PREFIX = "session:";

    /**
     * 사용자 접속 처리
     */
    public void connect(Long userId, String sessionId) {
        String presenceKey = getPresenceKey(userId);
        String sessionKey = getSessionKey(sessionId);

        // Pipeline or Multi implementation not strictly needed for just 2 keys but good
        // practice.
        // Using simple ops for clarity.
        redisTemplate.opsForSet().add(presenceKey, sessionId);
        redisTemplate.expire(presenceKey, 24, TimeUnit.HOURS);

        // Reverse Mapping for robust disconnect handling
        redisTemplate.opsForValue().set(sessionKey, userId.toString(), 24, TimeUnit.HOURS);

        log.info("User Connected: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 사용자 접속 해제 처리
     */
    public void disconnect(String sessionId) {
        String sessionKey = getSessionKey(sessionId);

        // 1. Find userId from sessionId (Robustness)
        String userIdStr = (String) redisTemplate.opsForValue().get(sessionKey);

        if (userIdStr != null) {
            Long userId = Long.parseLong(userIdStr);
            String presenceKey = getPresenceKey(userId);

            // Remove session from presence set
            redisTemplate.opsForSet().remove(presenceKey, sessionId);

            // Clean up session key
            redisTemplate.delete(sessionKey);

            log.info("User Disconnected: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.warn("Disconnect event for unknown session: {}", sessionId);
        }
    }

    /**
     * 다중 사용자 온라인 상태 조회 (Redis Pipelining)
     */
    public Map<Long, Boolean> getUsersOnlineStatus(List<Long> userIds) {
        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                for (Long userId : userIds) {
                    String key = getPresenceKey(userId);
                    operations.opsForSet().size((K) key); // SCARD -> returns Long
                }
                return null;
            }
        });

        Map<Long, Boolean> statusMap = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            Long count = 0L;
            if (results.get(i) instanceof Long) {
                count = (Long) results.get(i);
            }
            statusMap.put(userIds.get(i), count > 0);
        }
        return statusMap;
    }

    private String getPresenceKey(Long userId) {
        return PRESENCE_KEY_PREFIX + userId + PRESENCE_KEY_SUFFIX;
    }

    private String getSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}
