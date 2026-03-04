package com.example.androidexample;


public class ApiConstants {
    private ApiConstants() {}
//    public static final String BASE_URL= "http://REDACTED:8080/";

    public static final String BASE_URL = "http://REDACTED:8080";
    public static final String SIGNUP = BASE_URL + "/signup";
    public static final String LOGIN  = BASE_URL + "/login";
    public static final String USERS  = BASE_URL + "/users";
    public static final String DELETE = BASE_URL + "/";

     //Admin
    public static final String EDIT = BASE_URL + "/api/admin/update/"; // + id
    // Mockoon local server
//        public static final String BASE_URL = "http://REDACTED";
//
//        // Signup (creates a new user)
//        public static final String USERS = BASE_URL + "/users";
//
//        // Login (if you made a login route)
//        public static final String LOGIN = BASE_URL + "/login";
//
//        // Delete profile
//        public static final String DELETE = BASE_URL + "/";
//
//        // Counsellor endpoints
//        public static final String COUNSELLORS = BASE_URL + "/api/counsellors";
//
//        // Get or update counsellor profile
//        public static String counsellorProfile(long userId) {
//            return COUNSELLORS + "/" + userId + "/profile";
//        }
    }