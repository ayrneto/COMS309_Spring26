package com.example.androidexample;

public class ApiConstants {
    private ApiConstants() {}

    // ─── REAL SERVER ───────────────────────────────────────────
//    public static final String BASE_URL     = "http://REDACTED:8080";
//    public static final String WS_BASE_URL  = "ws://REDACTED:8080";
//
//    public static final String SIGNUP        = BASE_URL + "/signup";
//    public static final String LOGIN         = BASE_URL + "/login";
//    public static final String USERS         = BASE_URL + "/users";
//    public static final String DELETE        = BASE_URL + "/";
//    public static final String EDIT          = BASE_URL + "/api/admin/update/";
//    public static final String ADMIN_DELETE  = BASE_URL + "/api/admin/";
//    public static final String COUNSELLORS   = BASE_URL + "/api/counsellors";
//    public static final String APPOINTMENTS  = BASE_URL + "/api/appointments";
//    public static final String CHAT_HISTORY  = BASE_URL + "/api/chat/history";
//    public static final String WS_CHAT_BASE  = WS_BASE_URL + "/ws/chat";
//
//    public static String counselorProfile(long userId) {
//        return COUNSELLORS + "/" + userId + "/profile";
//    }
//    public static String wsChat(long senderId, long receiverId) {
//        return WS_CHAT_BASE + "/" + senderId + "/" + receiverId;
//    }
//    public static String chatHistory(long userA, long userB) {
//        return CHAT_HISTORY + "?userA=" + userA + "&userB=" + userB;
//    }

    // ─── MOCKOON LOCAL SERVER ──────────────────────────────────
    public static final String BASE_URL     = "http://REDACTED";
    public static final String WS_BASE_URL  = "ws://REDACTED";
    public static final String SIGNUP       = BASE_URL + "/signup";
    public static final String LOGIN        = BASE_URL + "/login";
    public static final String USERS        = BASE_URL + "/users";
    public static final String DELETE       = BASE_URL + "/";
    public static final String EDIT         = BASE_URL + "/api/admin/update/";
    public static final String ADMIN_DELETE = BASE_URL + "/api/admin/";
    public static final String COUNSELLORS  = BASE_URL + "/api/counsellors";
    public static final String APPOINTMENTS = BASE_URL + "/api/appointments";
    public static final String CHAT_HISTORY = BASE_URL + "/api/chat/history";
    public static final String WS_CHAT_BASE = WS_BASE_URL + "/ws/chat";

    public static String counselorProfile(long userId) {
        return COUNSELLORS + "/" + userId + "/profile";
    }
    public static String wsChat(long senderId, long receiverId) {
        return WS_CHAT_BASE + "/" + senderId + "/" + receiverId;
    }
    public static String chatHistory(long userA, long userB) {
        return CHAT_HISTORY + "?userA=" + userA + "&userB=" + userB;
    }
}