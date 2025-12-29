package com.carrot.app.global.common;

public class CacheKey {

    public static final String PRODUCTS = "products";
    public static final String PRODUCT_DETAIL = "product_detail";
    public static final String CATEGORIES = "categories";
    public static final String CHAT_ROOMS = "chat_rooms";
    public static final String POPULAR_KEYWORDS = "popular_keywords";
    public static final String ACTIVE_USERS = "active_users";

    // TTL (seconds)
    public static final int PRODUCTS_TTL = 600; // 10 min
    public static final int PRODUCT_DETAIL_TTL = 600; // 10 min
    public static final int CATEGORIES_TTL = 86400; // 1 day
    public static final int CHAT_ROOMS_TTL = 60 * 60; // 1 hour
    public static final int POPULAR_KEYWORDS_TTL = 28800; // 8 hours

    public static String getKey(String domain, String identifier, String type) {
        return domain + ":" + identifier + ":" + type;
    }
}
