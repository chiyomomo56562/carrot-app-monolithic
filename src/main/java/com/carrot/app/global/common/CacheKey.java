package com.carrot.app.global.common;

public enum CacheKey {

    PRODUCTS("products", 600),
    PRODUCT_DETAIL("product_detail", 600),
    CATEGORIES("categories", 86400),
    CHAT_ROOMS("chat_rooms", 3600),
    POPULAR_KEYWORDS("popular_keywords", 28800),
    ACTIVE_USERS("active_users", 60);

    private final String key;
    private final int ttl;

    CacheKey(String key, int ttl) {
        this.key = key;
        this.ttl = ttl;
    }

    public String getKey() {
        return key;
    }

    public int getTtl() {
        return ttl;
    }

    public static String getKey(CacheKey cacheKey, String identifier, String type) {
        return cacheKey.getKey() + ":" + identifier + ":" + type;
    }
}