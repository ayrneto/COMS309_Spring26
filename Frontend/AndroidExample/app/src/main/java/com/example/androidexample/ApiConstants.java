package com.example.androidexample;

public class ApiConstants {
    private ApiConstants() {}

    // ─── REAL SERVER ───────────────────────────────────────────
    public static final String BASE_URL   = "http://REDACTED:8080";
    public static final String SIGNUP     = BASE_URL + "/signup";
    public static final String LOGIN      = BASE_URL + "/login";
    public static final String USERS      = BASE_URL + "/users";
    public static final String DELETE     = BASE_URL + "/";
    public static final String EDIT       = BASE_URL + "/api/admin/update/"; // + id
    public static final String COUNSELLORS = BASE_URL + "/api/counsellors";

    public static String counselorProfile(long userId) {
        return COUNSELLORS + "/" + userId + "/profile";
    }

    // ─── MOCKOON LOCAL SERVER ──────────────────────────────────
//    public static final String BASE_URL    = "http://REDACTED";
//    public static final String SIGNUP      = BASE_URL + "/signup";
//    public static final String LOGIN       = BASE_URL + "/login";
//    public static final String USERS       = BASE_URL + "/users";
//    public static final String DELETE      = BASE_URL + "/";
//    public static final String EDIT        = BASE_URL + "/api/admin/update/";
//    public static final String COUNSELLORS = BASE_URL + "/api/counsellors";
//
//    public static String counselorProfile(long userId) {
//        return COUNSELLORS + "/" + userId + "/profile";
//    }
}