package com.example.androidexample;

public class ApiConstants {
    private ApiConstants() {}

    // ─── REAL SERVER ───────────────────────────────────────────
    public static final String BASE_URL     = "http://REDACTED:8080";
    public static final String WS_BASE_URL  = "ws://REDACTED:8080";

    public static final String SIGNUP       = BASE_URL + "/users/signup";
    public static final String LOGIN        = BASE_URL + "/users/login";
    public static final String USERS        = BASE_URL + "/api/admin/users";
    public static final String DELETE       = BASE_URL + "/users/";
    public static final String EDIT         = BASE_URL + "/api/admin/update/";
    public static final String ADMIN_DELETE = BASE_URL + "/api/admin/";
    public static final String COUNSELLORS  = BASE_URL + "/api/counsellors";
    public static final String APPOINTMENTS = BASE_URL + "/api/appointments";
    public static final String CHAT_HISTORY = BASE_URL + "/api/chat/history";
    public static final String WS_CHAT_BASE = WS_BASE_URL + "/ws/chat";

    // ─── NOTES ────────────────────────────────────────────────
    public static String userNotes(long userId) {
        return BASE_URL + "/api/users/" + userId + "/notes";
    }
    public static String note(long noteId) {
        return BASE_URL + "/api/notes/" + noteId;
    }

    // ─── COUNSELLOR ───────────────────────────────────────────
    public static String counselorProfile(long userId) {
        return COUNSELLORS + "/" + userId + "/profile";
    }

    // ─── CHAT ─────────────────────────────────────────────────
    public static String wsChat(long senderId, long receiverId) {
        return WS_CHAT_BASE + "/" + senderId + "/" + receiverId;
    }
    public static String chatHistory(long userA, long userB) {
        return CHAT_HISTORY + "?userA=" + userA + "&userB=" + userB;
    }

    // ─── APPOINTMENTS ─────────────────────────────────────────
    public static String appointmentAccept(long appointmentId) {
        return APPOINTMENTS + "/" + appointmentId + "/accept";
    }
    public static String appointmentDecline(long appointmentId) {
        return APPOINTMENTS + "/" + appointmentId + "/decline";
    }
    public static String appointmentsByCounsellor(long counsellorId) {
        return APPOINTMENTS + "/counsellor/" + counsellorId;
    }
    public static String acceptedByUser(long userId) {
        return APPOINTMENTS + "/user/" + userId + "/accepted";
    }
    public static String acceptedByCounsellor(long counsellorId) {
        return APPOINTMENTS + "/counsellor/" + counsellorId + "/accepted";
    }

    // ─── PRESCRIPTIONS ────────────────────────────────────────
    public static String userPrescriptions(long userId) {
        return BASE_URL + "/prescriptions/users/" + userId;
    }

    // ─── MOCKOON LOCAL SERVER ──────────────────────────────────
//    public static final String BASE_URL     = "http://REDACTED";
//    public static final String WS_BASE_URL  = "ws://REDACTED";
//    public static final String SIGNUP       = BASE_URL + "/users/signup";
//    public static final String LOGIN        = BASE_URL + "/users/login";
//    public static final String USERS        = BASE_URL + "/api/admin/users";
//    public static final String DELETE       = BASE_URL + "/users/";
//    public static final String EDIT         = BASE_URL + "/api/admin/update/";
//    public static final String ADMIN_DELETE = BASE_URL + "/api/admin/";
//    public static final String COUNSELLORS  = BASE_URL + "/api/counsellors";
//    public static final String APPOINTMENTS = BASE_URL + "/api/appointments";
//    public static final String CHAT_HISTORY = BASE_URL + "/api/chat/history";
//    public static final String WS_CHAT_BASE = WS_BASE_URL + "/ws/chat";
//
//    // ─── NOTES ────────────────────────────────────────────────
//    public static String userNotes(long userId) {
//        return BASE_URL + "/api/users/" + userId + "/notes";
//    }
//    public static String note(long noteId) {
//        return BASE_URL + "/api/notes/" + noteId;
//    }
//
//    // ─── COUNSELLOR ───────────────────────────────────────────
//    public static String counselorProfile(long userId) {
//        return COUNSELLORS + "/" + userId + "/profile";
//    }
//
//    // ─── CHAT ─────────────────────────────────────────────────
//    public static String wsChat(long senderId, long receiverId) {
//        return WS_CHAT_BASE + "/" + senderId + "/" + receiverId;
//    }
//    public static String chatHistory(long userA, long userB) {
//        return CHAT_HISTORY + "?userA=" + userA + "&userB=" + userB;
//    }
//
//    // ─── APPOINTMENTS ─────────────────────────────────────────
//    public static String appointmentAccept(long appointmentId) {
//        return APPOINTMENTS + "/" + appointmentId + "/accept";
//    }
//    public static String appointmentDecline(long appointmentId) {
//        return APPOINTMENTS + "/" + appointmentId + "/decline";
//    }
//    public static String appointmentsByCounsellor(long counsellorId) {
//        return APPOINTMENTS + "/counsellor/" + counsellorId;
//    }
//    public static String acceptedByUser(long userId) {
//        return APPOINTMENTS + "/user/" + userId + "/accepted";
//    }
//    public static String acceptedByCounsellor(long counsellorId) {
//        return APPOINTMENTS + "/counsellor/" + counsellorId + "/accepted";
//    }
}