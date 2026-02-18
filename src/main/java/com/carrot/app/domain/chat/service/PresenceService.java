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
    private static final String SESSION_KEY_PREFIX = "session:";

    public void connect(Long userId, String sessionId) {
        String presenceKey = getPresenceKey(userId);
        String sessionKey = getSessionKey(sessionId);

        redisTemplate.opsForSet().add(presenceKey, sessionId);
        redisTemplate.expire(presenceKey, 1, TimeUnit.MINUTES);

        redisTemplate.opsForValue().set(sessionKey, userId.toString(), 1, TimeUnit.MINUTES);

        log.info("User Connected: userId={}, sessionId={}", userId, sessionId);
    }

    public void disconnect(String sessionId) {
        String sessionKey = getSessionKey(sessionId);

        String userIdStr = (String) redisTemplate.opsForValue().get(sessionKey);

        if (userIdStr != null) {
            Long userId = Long.parseLong(userIdStr);
            String presenceKey = getPresenceKey(userId);

            redisTemplate.opsForSet().remove(presenceKey, sessionId);

            redisTemplate.delete(sessionKey);

            log.info("User Disconnected: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.warn("Disconnect event for unknown session: {}", sessionId);
        }
    }

    public void receiveHeartbeat(Long userId, String sessionId) {
        String presenceKey = getPresenceKey(userId);
        String sessionKey = getSessionKey(sessionId);

        redisTemplate.expire(presenceKey, 1, TimeUnit.MINUTES);
        redisTemplate.expire(sessionKey, 1, TimeUnit.MINUTES);

        log.info("User Heartbeat: userId={}, sessionId={}", userId, sessionId);
    }

    public Map<Long, Boolean> getUsersOnlineStatus(List<Long> userIds) {
        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                for (Long userId : userIds) {
                    String key = getPresenceKey(userId);
                    operations.opsForSet().size((K) key);
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
