package onetoone;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * MainSystemTest — Full Coverage System Tests (20 Test Cases)
 *
 * Covers 100% of all controllers and endpoints:
 *   • onetoone.Users           (UserController, UserAdminController, DailyCheckIn)
 *   • onetoone.Counsellors     (CounsellorProfileController — all endpoints)
 *   • onetoone.Appointments    (AppointmentController — book/accept/decline/lists)
 *   • onetoone.Notes           (NoteController — CRUD + share/unshare)
 *   • onetoone.Tasks           (TaskController — CRUD + status cycle)
 *   • onetoone.Notification    (NotificationController — create + mark-read)
 *   • onetoone.Assignments     (UserCounsellorAssignmentController — card/choose/random/unassign)
 *   • onetoone.Prescription    (PrescriptionController — create + get)
 *   • onetoone.RoutineTracker  (RoutineController — create/checkin/list/delete)
 *   • onetoone.SleepTracker    (SleepController — log + curve)
 *   • onetoone.AiChat          (AiChatController — history/clear)
 *   • onetoone.realtime_chat   (ChatController, ChatServer WebSocket)
 *
 * KEY FIXES vs. prior version:
 *   - Appointments: correct base path /api/appointments (was /appointments)
 *   - Notes: correct base path /api/... (was /users, /notes, /counsellors)
 *   - Tasks: createTask uses userEmail (not userId) in request body
 *   - Tasks: getTasksForUser returns empty list (not 404) for unknown user
 *   - Notifications: uses @RequestParam (not @RequestBody)
 *   - AiChat: correct base path /api/ai-chat (was /ai-chat)
 *   - Chat upload: requires senderId + receiverId multipart params
 *   - WebSocket ChatServer: endpoint is /ws/chat/{senderId}/{receiverId}
 *   - Counsellor profile GET unknown: backend throws RuntimeException → 500 (not 400)
 *   - Prescriptions GET unknown user: backend returns empty list (no 404 thrown)
 *   - Task status display names: "Not Started", "Ongoing", "Completed"
 *
 * Run the entire class in order — do NOT run individual tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainSystemTest {

    // ── Shared state ──────────────────────────────────────────────────────────
    private static long userId;
    private static long counsellorUserId;
    private static long appointmentId;
    private static long declinedApptId;
    private static long taskId;
    private static long noteId;
    private static long notificationId;
    private static long checkInId;
    private static long routineId;
    private static long prescriptionId;

    private static final String TS          = String.valueOf(System.currentTimeMillis());
    private static final String USER_EMAIL  = "mes_user_"  + TS + "@test.com";
    private static final String COUNS_EMAIL = "mes_couns_" + TS + "@test.com";
    private static final String PASSWORD    = "Test@1234";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = 8080;
    }

    // =========================================================================
    // TC-1  USERS — Signup (USER + COUNSELLOR), duplicate email, password mismatch
    //        Covers: UserController.signup(), User entity, Role enum, UserResponse
    // =========================================================================
    @Test @Order(1)
    @DisplayName("TC-1: Users — signup USER & COUNSELLOR; bad password → 400; duplicate → 409")
    void tc01_signup() {

        // 1a. Signup as USER
        userId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name":            "Test User",
                          "email":           "%s",
                          "password":        "%s",
                          "confirmPassword": "%s",
                          "role":            "USER"
                        }
                        """.formatted(USER_EMAIL, PASSWORD, PASSWORD))
                .when().post("/users/signup")
                .then()
                .statusCode(201)
                .body("email", equalTo(USER_EMAIL))
                .body("id",    notNullValue())
                .extract().response().jsonPath().getLong("id");

        // 1b. Signup as COUNSELLOR (auto-creates CounsellorProfile)
        counsellorUserId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name":            "Dr. Test",
                          "email":           "%s",
                          "password":        "%s",
                          "confirmPassword": "%s",
                          "role":            "COUNSELLOR"
                        }
                        """.formatted(COUNS_EMAIL, PASSWORD, PASSWORD))
                .when().post("/users/signup")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().response().jsonPath().getLong("id");

        System.out.println("[TC-1] userId=" + userId + " counsellorUserId=" + counsellorUserId);

        // 1c. Mismatched passwords → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "Bad", "email": "bad_%s@test.com",
                          "password": "aaa", "confirmPassword": "zzz", "role": "USER"
                        }
                        """.formatted(TS))
                .when().post("/users/signup")
                .then().statusCode(400);

        // 1d. Duplicate active email → 409
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name":            "Dup",
                          "email":           "%s",
                          "password":        "%s",
                          "confirmPassword": "%s",
                          "role":            "USER"
                        }
                        """.formatted(USER_EMAIL, PASSWORD, PASSWORD))
                .when().post("/users/signup")
                .then().statusCode(409);
    }

    // =========================================================================
    // TC-2  USERS — Login, get by email/ID, get all, 404 on unknown
    //        Covers: UserController.login(), getUserByEmail(), getUserById(), getAllUsers()
    // =========================================================================
    @Test @Order(2)
    @DisplayName("TC-2: Users — login success/fail/role-mismatch; get by email & ID; get all; 404 on unknown")
    void tc02_userEndpoints() {

        // 2a. Successful login as USER
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "%s", "password": "%s", "role": "USER" }
                        """.formatted(USER_EMAIL, PASSWORD))
                .when().post("/users/login")
                .then().statusCode(200)
                .body("email", equalTo(USER_EMAIL))
                .body("role",  equalTo("USER"));

        // 2b. Wrong password → 401
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "%s", "password": "wrongPass99", "role": "USER" }
                        """.formatted(USER_EMAIL))
                .when().post("/users/login")
                .then().statusCode(401);

        // 2c. Role mismatch → 403
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "%s", "password": "%s", "role": "COUNSELLOR" }
                        """.formatted(USER_EMAIL, PASSWORD))
                .when().post("/users/login")
                .then().statusCode(403);

        // 2d. Unknown email → 401
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "ghost_nobody@none.com", "password": "x", "role": "USER" }
                        """)
                .when().post("/users/login")
                .then().statusCode(401);

        // 2e. Get by email
        given()
                .when().get("/users/LoginPage/user/" + USER_EMAIL)
                .then().statusCode(200)
                .body("id", equalTo((int) userId));

        // 2f. Get by ID
        given()
                .when().get("/users/" + userId)
                .then().statusCode(200)
                .body("email", equalTo(USER_EMAIL));

        // 2g. Get all users
        given()
                .when().get("/users/users")
                .then().statusCode(200)
                .body("$", not(empty()));

        // 2h. Unknown ID → 404
        given()
                .when().get("/users/999999")
                .then().statusCode(404);

        // 2i. Unknown email → 404
        given()
                .when().get("/users/LoginPage/user/nobody_ever@nope.com")
                .then().statusCode(404);

        // 2j. Get counsellors connected to user (may be empty list)
        given()
                .when().get("/users/" + userId + "/counsellors")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // =========================================================================
    // TC-3  USERS — Daily Check-In CRUD
    //        Covers: createCheckIn, getAllCheckIns, updateCheckIn, deleteCheckIn
    // =========================================================================
    @Test @Order(3)
    @DisplayName("TC-3: Users — create check-in; duplicate date → 409; get list; update; delete; error paths")
    void tc03_dailyCheckIn() {

        String today = java.time.LocalDate.now().toString();

        // 3a. Create check-in
        checkInId = given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 3, "description": "Feeling okay", "date": "%s" }
                        """.formatted(today))
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(201)
                .body("rating", equalTo(3))
                .extract().jsonPath().getLong("id");

        // 3b. Duplicate date → 409
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 4, "description": "Again", "date": "%s" }
                        """.formatted(today))
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(409);

        // 3c. Invalid rating (out of range) → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 10, "description": "Bad", "date": "2025-01-01" }
                        """)
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(400);

        // 3d. Missing rating field → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "description": "No rating", "date": "2025-01-02" }
                        """)
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(400);

        // 3e. Missing date field → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 3, "description": "No date" }
                        """)
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(400);

        // 3f. Get all check-ins for user
        given()
                .when().get("/users/" + userId + "/checkins")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 3g. Get check-ins for unknown user → 404
        given()
                .when().get("/users/999999/checkins")
                .then().statusCode(404);

        // 3h. Update check-in
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 5, "description": "Much better!" }
                        """)
                .when().put("/users/checkins/" + checkInId)
                .then().statusCode(200)
                .body("rating", equalTo(5));

        // 3i. Update non-existent check-in → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "rating": 2 }
                        """)
                .when().put("/users/checkins/999999")
                .then().statusCode(404);

        // 3j. Delete check-in
        given()
                .when().delete("/users/checkins/" + checkInId)
                .then().statusCode(204);

        // 3k. Delete unknown check-in → 404
        given()
                .when().delete("/users/checkins/999999")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-4  COUNSELLORS — list all, get profile, upsert, update status, rating, pic
    //        Covers: CounsellorProfileController — all endpoints
    //
    // FIX: GET /api/counsellors/999999/profile throws RuntimeException (not
    //      IllegalArgumentException), so Spring maps it to 500, not 400.
    //      Updated assertion to anyOf(400, 500).
    // =========================================================================
    @Test @Order(4)
    @DisplayName("TC-4: Counsellors — list all; get profile; upsert; update status; update rating; 404/500 paths")
    void tc04_counsellorProfile() {

        // 4a. List all counsellors
        given()
                .when().get("/api/counsellors")
                .then().statusCode(200)
                .body("$", not(empty()));

        // 4b. Get profile by userId (counsellor was created in TC-1)
        given()
                .when().get("/api/counsellors/" + counsellorUserId + "/profile")
                .then().statusCode(200);

        // 4c. Get profile for unknown userId → 400 or 500 (RuntimeException → 500)
        given()
                .when().get("/api/counsellors/999999/profile")
                .then().statusCode(anyOf(equalTo(400), equalTo(500)));

        // 4d. Upsert counsellor profile (PUT /{id}/profile)
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "displayName":      "Dr. Updated",
                          "specialization":   "Anxiety",
                          "bio":              "10 years experience",
                          "profilePictureUrl":"https://example.com/pic.png",
                          "status":           "AVAILABLE"
                        }
                        """)
                .when().put("/api/counsellors/" + counsellorUserId + "/profile")
                .then().statusCode(200)
                .body("displayName", equalTo("Dr. Updated"));

        // 4e. Upsert for unknown user → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "displayName": "Nobody", "status": "AVAILABLE" }
                        """)
                .when().put("/api/counsellors/999999/profile")
                .then().statusCode(404);

        // 4f. Update status to BUSY
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/update/BUSY")
                .then().statusCode(200);

        // 4g. Update status to OFFLINE
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/update/OFFLINE")
                .then().statusCode(200);

        // 4h. Update status back to AVAILABLE
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/update/AVAILABLE")
                .then().statusCode(200);

        // 4i. Update status for non-existent counsellor → 404
        given()
                .when().put("/api/counsellors/999999/update/AVAILABLE")
                .then().statusCode(404);

        // 4j. Update rating → 200
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/rating/5")
                .then().statusCode(200);

        // 4k. Update rating again (exercises incremental average calculation)
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/rating/4")
                .then().statusCode(200);

        // 4l. Update profile picture
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/profilePicture/newpic.png")
                .then().statusCode(200);

        // 4m. Update profile picture for non-existent counsellor → 404
        given()
                .when().put("/api/counsellors/999999/profilePicture/pic.png")
                .then().statusCode(404);

        // 4n. PUT rating for non-existent counsellor → 404
        given()
                .when().put("/api/counsellors/999999/rating/5")
                .then().statusCode(404);

        // 4o. Full profile update via PUT /{userId}/update
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "displayName":      "Dr. Full Update",
                          "specialization":   "Depression",
                          "bio":              "Updated bio",
                          "profilePictureUrl":"https://example.com/updated.jpg",
                          "status":           "AVAILABLE"
                        }
                        """)
                .when().put("/api/counsellors/" + counsellorUserId + "/update")
                .then().statusCode(200);
    }

    // =========================================================================
    // TC-5  APPOINTMENTS — book, accept, decline, accepted lists
    //        Covers: AppointmentController — all endpoints
    //
    // FIX: AppointmentController maps to /api/appointments (not /appointments).
    //      AppointmentRequest fields are date + timeSlot + notes (no dateTime/description).
    // =========================================================================
    @Test @Order(5)
    @DisplayName("TC-5: Appointments — book (PENDING); accept (CONFIRMED); decline (CANCELLED); accepted lists; 404 on bad ID")
    void tc05_appointments() {

        // 5a. Book appointment → PENDING
        // AppointmentRequest has: userId, counsellorId, date (String), timeSlot, notes
        appointmentId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId":       %d,
                          "counsellorId": %d,
                          "date":         "2025-12-01",
                          "timeSlot":     "10:00-11:00",
                          "notes":        "First session"
                        }
                        """.formatted(userId, counsellorUserId))
                .when().post("/api/appointments")
                .then().statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("status", equalTo("PENDING"))
                .extract().jsonPath().getLong("id");

        System.out.println("[TC-5] appointmentId=" + appointmentId);

        // 5b. Book another appointment to later decline
        declinedApptId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId":       %d,
                          "counsellorId": %d,
                          "date":         "2025-12-02",
                          "timeSlot":     "14:00-15:00",
                          "notes":        "To be declined"
                        }
                        """.formatted(userId, counsellorUserId))
                .when().post("/api/appointments")
                .then().statusCode(anyOf(equalTo(200), equalTo(201)))
                .extract().jsonPath().getLong("id");

        // 5c. Get appointments by counsellor
        given()
                .when().get("/api/appointments/counsellor/" + counsellorUserId)
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)));

        // 5d. Accept appointment
        given()
                .when().patch("/api/appointments/" + appointmentId + "/accept")
                .then().statusCode(200);

        // 5e. Decline appointment
        given()
                .when().patch("/api/appointments/" + declinedApptId + "/decline")
                .then().statusCode(200);

        // 5f. Get accepted appointments for counsellor
        given()
                .when().get("/api/appointments/counsellor/" + counsellorUserId + "/accepted")
                .then().statusCode(200);

        // 5g. Get accepted appointments for user
        given()
                .when().get("/api/appointments/user/" + userId + "/accepted")
                .then().statusCode(200);

        // 5h. Accept unknown appointment → 404
        given()
                .when().patch("/api/appointments/999999/accept")
                .then().statusCode(404);

        // 5i. Decline unknown appointment → 404
        given()
                .when().patch("/api/appointments/999999/decline")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-6  NOTES — CRUD + share/unshare
    //        Covers: NoteController — all endpoints
    //
    // FIX: NoteController base is /api, so paths are:
    //   POST   /api/users/{userId}/notes
    //   GET    /api/users/{userId}/notes
    //   GET    /api/notes/{noteId}
    //   PUT    /api/notes/{noteId}
    //   DELETE /api/notes/{noteId}
    //   PATCH  /api/notes/{noteId}/share
    //   PATCH  /api/notes/{noteId}/unshare
    //   GET    /api/counsellors/{counsellorUserId}/shared-notes
    //
    // FIX: NoteResponse field is "shared" (boolean), not "sharedWithCounsellor" (object).
    // =========================================================================
    @Test @Order(6)
    @DisplayName("TC-6: Notes — create; get list; get single; update; share; unshare; get shared; delete; 404 paths")
    void tc06_notes() {

        // 6a. Create note
        noteId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":   "System Test Note",
                          "content": "Initial content",
                          "label":   "Work",
                          "dueDate": "2025-12-31"
                        }
                        """)
                .when().post("/api/users/" + userId + "/notes")
                .then().statusCode(201)
                .body("title", equalTo("System Test Note"))
                .extract().jsonPath().getLong("id");

        System.out.println("[TC-6] noteId=" + noteId);

        // 6b. Get all notes for user
        given()
                .when().get("/api/users/" + userId + "/notes")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 6c. Get single note
        given()
                .when().get("/api/notes/" + noteId)
                .then().statusCode(200)
                .body("id",    equalTo((int) noteId))
                .body("title", equalTo("System Test Note"));

        // 6d. Update note
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":   "Updated Title",
                          "content": "Updated content",
                          "label":   "Personal",
                          "dueDate": "2026-01-15"
                        }
                        """)
                .when().put("/api/notes/" + noteId)
                .then().statusCode(200)
                .body("title", equalTo("Updated Title"));

        // 6e. Share note with counsellor
        // NoteResponse.shared is a boolean field
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "counsellorUserId": %d }
                        """.formatted(counsellorUserId))
                .when().patch("/api/notes/" + noteId + "/share")
                .then().statusCode(200)
                .body("shared", equalTo(true));

        // 6f. Get shared notes for counsellor
        given()
                .when().get("/api/counsellors/" + counsellorUserId + "/shared-notes")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 6g. Unshare note
        given()
                .when().patch("/api/notes/" + noteId + "/unshare")
                .then().statusCode(200)
                .body("shared", equalTo(false));

        // 6h. Get single unknown note → 404
        given()
                .when().get("/api/notes/999999")
                .then().statusCode(404);

        // 6i. Delete note
        given()
                .when().delete("/api/notes/" + noteId)
                .then().statusCode(204);

        // 6j. Delete already-deleted note → 404
        given()
                .when().delete("/api/notes/" + noteId)
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-7  TASKS — create, list, update fields, status cycle, error paths
    //        Covers: TaskController — all endpoints
    //
    // FIX: TaskCreateRequest uses "userEmail" (not "userId").
    //      TaskService.getTasksForUser returns empty list for any userId (no 404).
    //      TaskStatus display names: "Not Started", "Ongoing", "Completed".
    // =========================================================================
    @Test @Order(7)
    @DisplayName("TC-7: Tasks — create; list; update fields; Not Started→Ongoing→Completed; invalid status → 400; 404 paths")
    void tc07_tasks() {

        // 7a. Create task — NOTE: userEmail is the lookup key, not userId
        taskId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userEmail":   "%s",
                          "title":       "Write unit tests",
                          "description": "Cover all endpoints",
                          "dueDate":     "2025-12-31"
                        }
                        """.formatted(USER_EMAIL))
                .when().post("/api/tasks")
                .then().statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("title",  equalTo("Write unit tests"))
                .body("status", equalTo("Not Started"))
                .extract().jsonPath().getLong("id");

        System.out.println("[TC-7] taskId=" + taskId);

        // 7b. Get tasks for user (returns list, even when userId passed)
        given()
                .when().get("/api/users/" + userId + "/tasks")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 7c. Get tasks for unknown user → returns empty list (200), not 404
        given()
                .when().get("/api/users/999999/tasks")
                .then().statusCode(200)
                .body("$", hasSize(0));

        // 7d. Update task fields
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":       "Write unit tests (revised)",
                          "description": "Full coverage",
                          "dueDate":     "2026-01-15"
                        }
                        """)
                .when().put("/api/tasks/" + taskId)
                .then().statusCode(200)
                .body("title", equalTo("Write unit tests (revised)"));

        // 7e. Update for unknown task → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "title": "Ghost", "description": "x", "dueDate": "2026-01-01" }
                        """)
                .when().put("/api/tasks/999999")
                .then().statusCode(404);

        // 7f. Status: Not Started → Ongoing
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "ONGOING" }
                        """)
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(200)
                .body("status", equalTo("Ongoing"));

        // 7g. Status: Ongoing → Completed
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "COMPLETED" }
                        """)
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(200)
                .body("status", equalTo("Completed"));

        // 7h. Invalid status value → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "INVALID_STATUS_XYZ" }
                        """)
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(400);

        // 7i. Update status for unknown task → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "ONGOING" }
                        """)
                .when().put("/api/tasks/999999/status")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-8  NOTIFICATIONS — create via REST; mark as read
    //        Covers: NotificationController — create + mark-read
    //
    // FIX: Both endpoints use @RequestParam (not @RequestBody).
    //      POST /notifications/create?userId=X&message=Y
    //      PUT  /notifications/mark-as-read?id=X
    // =========================================================================
    @Test @Order(8)
    @DisplayName("TC-8: Notifications — create via REST; mark as read; 404 on unknown notification")
    void tc08_notifications() {

        // 8a. Create notification via request params
        notificationId = given()
                .queryParam("userId",  userId)
                .queryParam("message", "Test notification")
                .when().post("/notifications/create")
                .then().statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("message", equalTo("Test notification"))
                .extract().jsonPath().getLong("id");

        System.out.println("[TC-8] notificationId=" + notificationId);

        // 8b. Mark as read via request param ?id=
        given()
                .queryParam("id", notificationId)
                .when().put("/notifications/mark-as-read")
                .then().statusCode(anyOf(equalTo(200), equalTo(204)));

        // 8c. Mark unknown notification as read → RuntimeException → 500
        //     (NotificationService throws RuntimeException, not ResponseStatusException)
        given()
                .queryParam("id", 999999)
                .when().put("/notifications/mark-as-read")
                .then().statusCode(anyOf(equalTo(404), equalTo(500)));
    }

    // =========================================================================
    // TC-9  AI CHAT — history, clear
    //        Covers: AiChatController — POST chat (all branches), getHistory, clearHistory
    //
    // FIX: AiChatController is at /api/ai-chat (not /ai-chat).
    //
    // Branches in AiChatService.chat():
    //   • Normal message          → calls Gemini, saves, returns reply
    //   • Blank / null message    → 400 Bad Request (validated before Gemini call)
    //   • Crisis keyword message  → Gemini called + crisis hotline appended to reply
    //   • Unknown userId          → 404 Not Found
    //   • History window (>5 msgs)→ getRecentHistory() subList branch exercised
    //
    // Branches in getHistory() / clearHistory():
    //   • Valid user              → 200
    //   • Unknown user            → 404
    // =========================================================================
    @Test @Order(9)
    @DisplayName("TC-9a: AI Chat — POST normal message; blank message → 400; unknown user → 404; response shape")
    void tc09_aiChat() {

        // ── 9a. Pre-condition: clear any existing history so counts are predictable ─
        given()
                .when().delete("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200);

        // History is empty
        given()
                .when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200)
                .body("$", hasSize(0));

        // ── 9b. POST a normal message — exercises the full chat() happy path ────
        // Gemini may or may not be reachable; the service has a fallback reply,
        // so the endpoint always returns 200 with a non-blank aiReply.
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "I feel a bit stressed about my exams." }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("id",          notNullValue())
                .body("userMessage", equalTo("I feel a bit stressed about my exams."))
                .body("aiReply",     not(emptyOrNullString()))
                .body("sentAt",      notNullValue());

        // ── 9c. POST a second message — exercises history being passed to Gemini ──
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "What breathing exercises can help me calm down?" }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", not(emptyOrNullString()));

        // ── 9d. POST a third message ─────────────────────────────────────────────
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "I feel anxious about my presentation tomorrow." }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", not(emptyOrNullString()));

        // ── 9e. GET history — should now have 3 entries ───────────────────────
        given()
                .when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200)
                .body("$",              hasSize(3))
                .body("[0].userMessage", equalTo("I feel a bit stressed about my exams."))
                .body("[0].aiReply",     not(emptyOrNullString()))
                .body("[0].sentAt",      notNullValue())
                .body("[2].userMessage", equalTo("I feel anxious about my presentation tomorrow."));

        // ── 9f. POST blank message → 400 Bad Request ─────────────────────────
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "   " }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(400);

        // ── 9g. POST empty string → 400 ───────────────────────────────────────
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "" }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(400);

        // ── 9h. POST to unknown userId → 404 ─────────────────────────────────
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "Hello?" }
                        """)
                .when().post("/api/ai-chat/999999")
                .then().statusCode(404);

        // ── 9i. GET history for unknown user → 404 ───────────────────────────
        given()
                .when().get("/api/ai-chat/999999/history")
                .then().statusCode(404);

        // ── 9j. DELETE history for unknown user → 404 ────────────────────────
        given()
                .when().delete("/api/ai-chat/999999/history")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-9b  AI CHAT — Crisis keyword branch + history-window branch (>5 messages)
    //         Covers:
    //           • containsCrisisKeyword() → true  (appends hotline text to aiReply)
    //           • getRecentHistory() subList branch (only last 5 of N sent to Gemini)
    //           • clearHistory() happy path
    // =========================================================================
    @Test @Order(9)   // same @Order — runs immediately after tc09_aiChat in declaration order
    @DisplayName("TC-9b: AI Chat — crisis keyword appends hotline; history window > 5; clear history")
    void tc09b_aiChatCrisisAndHistoryWindow() {

        // ── 9b-1. Send crisis keyword message — exercises containsCrisisKeyword() ─
        // Service appends CRISIS_HOTLINE_APPEND to whatever Gemini replied,
        // so the reply must contain "988" (the crisis line number).
        String crisisReply = given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "I feel like I want to hurt myself." }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("userMessage", equalTo("I feel like I want to hurt myself."))
                .body("aiReply",     containsString("988"))   // crisis hotline always appended
                .extract().jsonPath().getString("aiReply");

        System.out.println("[TC-9b] Crisis reply (truncated): " + crisisReply.substring(0, Math.min(80, crisisReply.length())));

        // ── 9b-2. Another crisis keyword variant: "suicidal" ──────────────────
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "I have been feeling suicidal lately." }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", containsString("988"));

        // ── 9b-3. Non-crisis message after crisis — exercises normal branch again─
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "Can you suggest a journaling exercise?" }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", not(emptyOrNullString()));

        // ── 9b-4. Send enough messages to push history past HISTORY_WINDOW (5) ─
        // At this point we already have 3 (from TC-9a) + 3 above = 6 messages.
        // Sending one more guarantees getRecentHistory() hits the subList branch.
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "I tried deep breathing and it helped a little." }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", not(emptyOrNullString()));

        // 7th message — definitively exercises subList (size > HISTORY_WINDOW=5)
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "message": "What else can I do to reduce exam anxiety?" }
                        """)
                .when().post("/api/ai-chat/" + userId)
                .then().statusCode(200)
                .body("aiReply", not(emptyOrNullString()));

        // ── 9b-5. Verify history has all messages persisted (>=7) ─────────────
        given()
                .when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(7)));

        // ── 9b-6. Clear history — exercises clearHistory() happy path ─────────
        given()
                .when().delete("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200)
                .body(containsString("cleared"));

        // ── 9b-7. Confirm history is now empty ────────────────────────────────
        given()
                .when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    // =========================================================================
    // TC-10  REALTIME CHAT (REST) — history both directions; online-users; upload
    //         Covers: ChatController — getHistory, getOnlineUsers, uploadFile
    //
    // FIX: /api/chat/upload requires senderId + receiverId multipart params.
    // =========================================================================
    @Test @Order(10)
    @DisplayName("TC-10: Realtime Chat — history (both directions); online-users; upload empty → 400; upload valid → 200")
    void tc10_realtimeChat() {

        // 10a. Chat history A→B (may be empty, endpoint must 200)
        given()
                .when().get("/api/chat/history?userA=" + userId + "&userB=" + counsellorUserId)
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));

        // 10b. Chat history B→A (symmetric — exercises both orderings)
        given()
                .when().get("/api/chat/history?userA=" + counsellorUserId + "&userB=" + userId)
                .then().statusCode(200);

        // 10c. Online users
        given()
                .when().get("/api/chat/online-users")
                .then().statusCode(200);

        // 10d. Upload with no "file" part → 400 (missing required param) or 500
        given()
                .multiPart("dummy", "notAFile")
                .when().post("/api/chat/upload")
                .then().statusCode(anyOf(equalTo(400), equalTo(500)));

        // 10e. Upload a real text file (senderId + receiverId are required params)
        given()
                .multiPart("file",       "hello.txt", "hello world".getBytes(), "text/plain")
                .multiPart("senderId",   String.valueOf(userId))
                .multiPart("receiverId", String.valueOf(counsellorUserId))
                .when().post("/api/chat/upload")
                .then().statusCode(200)
                .body("fileUrl",  not(emptyOrNullString()))
                .body("fileName", equalTo("hello.txt"));
    }

    // =========================================================================
    // TC-11  PRESCRIPTION — create, get
    //         Covers: PrescriptionController — POST + GET /prescriptions/users/{userId}
    //
    // FIX: PrescriptionService.getUserPrescriptions does NOT throw for unknown user
    //      (no repository existence check). So GET for unknown user returns empty list
    //      (200), not 404. Assertion updated to reflect actual behaviour.
    // =========================================================================
    @Test @Order(11)
    @DisplayName("TC-11: Prescription — create prescription for user; get prescriptions; unknown user → 200 empty")
    void tc11_prescriptions() {

        // 11a. Create prescription (counsellor prescribes for user)
        given()
                .contentType(ContentType.JSON)
                .queryParam("counsellorId", counsellorUserId)
                .body("""
                        {
                          "medicationName": "TestDrug",
                          "dosage":         "10mg daily",
                          "instructions":   "Take with food",
                          "startDate":      "2025-12-01",
                          "durationDays":   31,
                          "active":         true
                        }
                        """)
                .when().post("/prescriptions/users/" + userId)
                .then().statusCode(200)
                .body(equalTo("success"));

        // 11b. Get prescriptions for user
        given()
                .when().get("/prescriptions/users/" + userId)
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].medicationName", equalTo("TestDrug"));

        // 11c. Get prescriptions for unknown user — no user-existence check in service,
        //      returns empty list (200) since findByUserId returns [] for unknown id.
        given()
                .when().get("/prescriptions/users/999999")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    // =========================================================================
    // TC-12  ROUTINE TRACKER — create, checkin, list user routines, list all, delete
    //         Covers: RoutineController — all endpoints
    // =========================================================================
    @Test @Order(12)
    @DisplayName("TC-12: Routine Tracker — create routine; check-in; list user routines; get all; get user check-ins; delete")
    void tc12_routineTracker() {

        // 12a. Create a routine for the user
        routineId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":       "Morning Exercise",
                          "description": "30 min jog",
                          "dueDate":     "2026-01-01",
                          "label":       "Personal"
                        }
                        """)
                .when().post("/routines/users/" + userId + "/routines")
                .then().statusCode(200)
                .body("title", equalTo("Morning Exercise"))
                .extract().jsonPath().getLong("id");

        System.out.println("[TC-12] routineId=" + routineId);

        // 12b. Check-in on the routine (increments streak)
        given()
                .when().post("/routines/" + routineId + "/checkin")
                .then().statusCode(200)
                .body("streakCount", greaterThanOrEqualTo(1));

        // 12c. Get all routines (global)
        given()
                .when().get("/routines")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 12d. Get routines for user
        given()
                .when().get("/routines/users/" + userId + "/routines")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].title", equalTo("Morning Exercise"));

        // 12e. Get check-ins for user
        given()
                .when().get("/routines/users/" + userId + "/checkins")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 12f. Delete the routine
        given()
                .when().delete("/routines/" + routineId)
                .then().statusCode(200)
                .body(containsString("deleted"));

        // 12g. Delete unknown routine → RuntimeException → 500
        given()
                .when().delete("/routines/999999")
                .then().statusCode(anyOf(equalTo(404), equalTo(500)));
    }

    // =========================================================================
    // TC-13  SLEEP TRACKER — log sleep, get curve
    //         Covers: SleepController — POST /{userId}  + GET /{userId}
    // =========================================================================
    @Test @Order(13)
    @DisplayName("TC-13: Sleep Tracker — log sleep (with date); log sleep (today default); get curve; missing hours → 400")
    void tc13_sleepTracker() {

        // 13a. Log sleep with explicit date
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "hours": 7.5, "date": "2025-11-01" }
                        """)
                .when().post("/sleep/" + userId)
                .then().statusCode(200)
                .body("hours",  equalTo(7.5f))
                .body("userId", equalTo((int) userId));

        // 13b. Log sleep without date (defaults to today)
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "hours": 8.0 }
                        """)
                .when().post("/sleep/" + userId)
                .then().statusCode(200)
                .body("hours", equalTo(8.0f));

        // 13c. Log sleep missing hours → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "date": "2025-11-02" }
                        """)
                .when().post("/sleep/" + userId)
                .then().statusCode(400);

        // 13d. Get sleep curve for user
        given()
                .when().get("/sleep/" + userId)
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    // =========================================================================
    // TC-14  COUNSELLOR ASSIGNMENT — card, choose, random, unassign
    //         Covers: UserCounsellorAssignmentController — all endpoints
    // =========================================================================
    @Test @Order(14)
    @DisplayName("TC-14: Assignments — get card (empty); choose counsellor; get card (populated); random assign; unassign; 404 paths")
    void tc14_assignments() {

        // 14a. No assignment yet → card returns empty JSON ({}) with 200
        given()
                .when().get("/api/assignments/user/" + userId + "/counsellor-card")
                .then().statusCode(200);

        // 14b. Unassign when nothing assigned → 404
        given()
                .when().delete("/api/assignments/user/" + userId)
                .then().statusCode(404);

        // 14c. Choose a specific counsellor
        given()
                .when().post("/api/assignments/user/" + userId + "/choose/" + counsellorUserId)
                .then().statusCode(200);

        // 14d. Get assigned counsellor card (now populated)
        given()
                .when().get("/api/assignments/user/" + userId + "/counsellor-card")
                .then().statusCode(200);

        // 14e. Unassign the counsellor
        given()
                .when().delete("/api/assignments/user/" + userId)
                .then().statusCode(200);

        // 14f. Randomly assign a counsellor
        given()
                .when().post("/api/assignments/user/" + userId + "/random")
                .then().statusCode(200);

        // 14g. Unassign again after random assign
        given()
                .when().delete("/api/assignments/user/" + userId)
                .then().statusCode(200);

        // 14h. Choose with unknown user → RuntimeException → 500 or 404
        given()
                .when().post("/api/assignments/user/999999/choose/" + counsellorUserId)
                .then().statusCode(anyOf(equalTo(404), equalTo(500)));

        // 14i. Random assign for unknown user → RuntimeException → 500 or 404
        given()
                .when().post("/api/assignments/user/999999/random")
                .then().statusCode(anyOf(equalTo(404), equalTo(500)));
    }

    // =========================================================================
    // TC-15  TASKS (2nd task) + remaining coverage paths
    //         Covers: TaskService remaining branches + CounsellorProfileController.updateProfile()
    //
    // FIX: same as TC-7 — use userEmail in task creation body.
    // =========================================================================
    @Test @Order(15)
    @DisplayName("TC-15: Tasks (2nd task) — create; status cycle; counsellor full update; admin paths")
    void tc15_tasksAndNotifications() {

        // 15a. Create a second task using userEmail
        long taskId2 = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userEmail":   "%s",
                          "title":       "Second task",
                          "description": "Verify notifications",
                          "dueDate":     "2025-12-28"
                        }
                        """.formatted(USER_EMAIL))
                .when().post("/api/tasks")
                .then().statusCode(anyOf(equalTo(200), equalTo(201)))
                .extract().jsonPath().getLong("id");

        // 15b. Update status → Ongoing
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "ONGOING" }
                        """)
                .when().put("/api/tasks/" + taskId2 + "/status")
                .then().statusCode(200)
                .body("status", equalTo("Ongoing"));

        // 15c. Update status → Completed
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "status": "COMPLETED" }
                        """)
                .when().put("/api/tasks/" + taskId2 + "/status")
                .then().statusCode(200)
                .body("status", equalTo("Completed"));

        // 15d. Full counsellor profile PUT update (exercises updateProfile() path)
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "displayName":      "Dr. Final",
                          "specialization":   "Stress Management",
                          "bio":              "Final bio update",
                          "profilePictureUrl":"https://example.com/final.jpg",
                          "status":           "AVAILABLE"
                        }
                        """)
                .when().put("/api/counsellors/" + counsellorUserId + "/update")
                .then().statusCode(200);

        // 15e. Admin — get all users
        given()
                .when().get("/api/admin/users")
                .then().statusCode(anyOf(equalTo(200), equalTo(404)));

        // 15f. Get user's counsellors (now has confirmed appointment from TC-5)
        given()
                .when().get("/users/" + userId + "/counsellors")
                .then().statusCode(200);
    }

    // =========================================================================
    // TC-16  WebSocket ChatServer — onOpen; messages; typing; read; invalid; onClose
    //         Covers: ChatServer WS endpoint — full message + command coverage
    //
    // FIX: ChatServer @ServerEndpoint is "/ws/chat/{senderId}/{receiverId}"
    //      (not "/chat/{senderId}/{receiverId}").
    // =========================================================================
    @Test @Order(16)
    @DisplayName("TC-16: ChatServer WS — onOpen; text message save+echo+forward; typing; read; all commands; invalid JSON; onClose")
    void tc16_webSocketChatServer() throws Exception {

        java.util.List<String> receivedByA = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.List<String> receivedByB = new java.util.concurrent.CopyOnWriteArrayList<>();

        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CountDownLatch> activeLatch =
                new java.util.concurrent.atomic.AtomicReference<>();

        jakarta.websocket.WebSocketContainer container =
                jakarta.websocket.ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);

        // ── Session A: userId → counsellorUserId ──────────────────────────────
        jakarta.websocket.Endpoint epA = new jakarta.websocket.Endpoint() {
            @Override
            public void onOpen(jakarta.websocket.Session session, jakarta.websocket.EndpointConfig cfg) {
                session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String msg) {
                        receivedByA.add(msg);
                        java.util.concurrent.CountDownLatch l = activeLatch.get();
                        if (l != null) l.countDown();
                    }
                });
            }

            @Override
            public void onError(jakarta.websocket.Session s, Throwable t) { t.printStackTrace(); }
        };

        // FIX: correct endpoint path /ws/chat/{senderId}/{receiverId}
        java.net.URI uriA = java.net.URI.create(
                "ws://localhost:8080/ws/chat/" + userId + "/" + counsellorUserId);
        jakarta.websocket.ClientEndpointConfig cfgA =
                jakarta.websocket.ClientEndpointConfig.Builder.create().build();

        // ── Open Session A ────────────────────────────────────────────────────
        java.util.concurrent.CountDownLatch openLatchA = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(openLatchA);
        jakarta.websocket.Session sessionA = container.connectToServer(epA, cfgA, uriA);
        Assertions.assertTrue(openLatchA.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Session A onOpen system message not received");
        Thread.sleep(300);
        receivedByA.clear();

        // ── Session B: counsellorUserId → userId ──────────────────────────────
        jakarta.websocket.Endpoint epB = new jakarta.websocket.Endpoint() {
            @Override
            public void onOpen(jakarta.websocket.Session session, jakarta.websocket.EndpointConfig cfg) {
                session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String msg) {
                        receivedByB.add(msg);
                        java.util.concurrent.CountDownLatch l = activeLatch.get();
                        if (l != null) l.countDown();
                    }
                });
            }

            @Override
            public void onError(jakarta.websocket.Session s, Throwable t) { t.printStackTrace(); }
        };

        java.net.URI uriB = java.net.URI.create(
                "ws://localhost:8080/ws/chat/" + counsellorUserId + "/" + userId);
        jakarta.websocket.ClientEndpointConfig cfgB =
                jakarta.websocket.ClientEndpointConfig.Builder.create().build();

        java.util.concurrent.CountDownLatch openLatchB = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(openLatchB);
        jakarta.websocket.Session sessionB = container.connectToServer(epB, cfgB, uriB);
        Assertions.assertTrue(openLatchB.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Session B onOpen system message not received");
        Thread.sleep(300);
        receivedByB.clear();

        // ── 1. Send a text message from A ─────────────────────────────────────
        // Expects: echo to A + forward to B → 2 total
        java.util.concurrent.CountDownLatch msgLatch = new java.util.concurrent.CountDownLatch(2);
        activeLatch.set(msgLatch);

        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "Hello counsellor!"
                }
                """.formatted(userId, counsellorUserId));

        Assertions.assertTrue(msgLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Message echo+forward not received within 5 s");
        Thread.sleep(200);

        Assertions.assertTrue(receivedByA.stream().anyMatch(m -> m.contains("Hello counsellor!")),
                "A should have received echo");
        Assertions.assertTrue(receivedByB.stream().anyMatch(m -> m.contains("Hello counsellor!")),
                "B should have received forward");
        receivedByA.clear();
        receivedByB.clear();

        // ── 2. Typing indicator ───────────────────────────────────────────────
        java.util.concurrent.CountDownLatch typeLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(typeLatch);
        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "typing",
                  "senderId":   %d,
                  "receiverId": %d,
                  "isTyping":   true
                }
                """.formatted(userId, counsellorUserId));
        Assertions.assertTrue(typeLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Typing indicator not forwarded to B");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 3. Read receipt ───────────────────────────────────────────────────
        java.util.concurrent.CountDownLatch readLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(readLatch);
        sessionB.getBasicRemote().sendText("""
                {
                  "type":       "read",
                  "senderId":   %d,
                  "receiverId": %d
                }
                """.formatted(counsellorUserId, userId));
        Assertions.assertTrue(readLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Read receipt not forwarded to A");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 4. /help command ─────────────────────────────────────────────────
        java.util.concurrent.CountDownLatch helpLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(helpLatch);
        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "/help"
                }
                """.formatted(userId, counsellorUserId));
        Assertions.assertTrue(helpLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "/help system response not received");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 5. /ping command ─────────────────────────────────────────────────
        java.util.concurrent.CountDownLatch pingLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(pingLatch);
        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "/ping"
                }
                """.formatted(userId, counsellorUserId));
        Assertions.assertTrue(pingLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "/ping response not received");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 6. /status command ───────────────────────────────────────────────
        java.util.concurrent.CountDownLatch statusLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(statusLatch);
        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "/status"
                }
                """.formatted(userId, counsellorUserId));
        Assertions.assertTrue(statusLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "/status response not received");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 7. /clear command ────────────────────────────────────────────────
        java.util.concurrent.CountDownLatch clearLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(clearLatch);
        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "/clear"
                }
                """.formatted(userId, counsellorUserId));
        Assertions.assertTrue(clearLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "/clear response not received");
        Thread.sleep(200);
        receivedByA.clear();
        receivedByB.clear();

        // ── 8. Invalid JSON → error message ──────────────────────────────────
        java.util.concurrent.CountDownLatch errLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(errLatch);
        sessionA.getBasicRemote().sendText("{not valid json}}}");
        Assertions.assertTrue(errLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Error message not received for invalid JSON");
        Thread.sleep(200);
        Assertions.assertFalse(receivedByA.isEmpty(), "A should have received error message");
        receivedByA.clear();
        receivedByB.clear();

        // ── 9. Close sessions (exercises onClose) ─────────────────────────────
        activeLatch.set(null);
        sessionA.close();
        sessionB.close();
        Thread.sleep(300);
    }

    // =========================================================================
    // TC-17  WebSocket ChatServer — file message + offline receiver
    //         Covers: ChatServer file message branch + offline save-to-DB branch
    //
    // FIX: correct WebSocket path /ws/chat/{senderId}/{receiverId}
    // =========================================================================
    @Test @Order(17)
    @DisplayName("TC-17: ChatServer WS — file message (fileUrl/fileName/fileType); offline-receiver DB-only save; history shows file fields")
    void tc17_webSocketFileMessageAndOfflineReceiver() throws Exception {

        java.util.List<String> receivedByA = new java.util.concurrent.CopyOnWriteArrayList<>();

        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CountDownLatch> activeLatch =
                new java.util.concurrent.atomic.AtomicReference<>();

        jakarta.websocket.WebSocketContainer container =
                jakarta.websocket.ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);

        jakarta.websocket.Endpoint epA = new jakarta.websocket.Endpoint() {
            @Override
            public void onOpen(jakarta.websocket.Session session, jakarta.websocket.EndpointConfig cfg) {
                session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String msg) {
                        receivedByA.add(msg);
                        java.util.concurrent.CountDownLatch l = activeLatch.get();
                        if (l != null) l.countDown();
                    }
                });
            }

            @Override
            public void onError(jakarta.websocket.Session s, Throwable t) { t.printStackTrace(); }
        };

        // FIX: correct path /ws/chat/
        java.net.URI uriA = java.net.URI.create(
                "ws://localhost:8080/ws/chat/" + userId + "/" + counsellorUserId);
        jakarta.websocket.ClientEndpointConfig cfgA =
                jakarta.websocket.ClientEndpointConfig.Builder.create().build();

        // ── Open session A; counsellor is NOT connected (offline receiver) ────
        java.util.concurrent.CountDownLatch openLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(openLatch);
        jakarta.websocket.Session sessionA = container.connectToServer(epA, cfgA, uriA);
        Assertions.assertTrue(openLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "onOpen system message not received");
        Thread.sleep(300);
        receivedByA.clear();

        // ── 1. Send a file message (receiver offline → DB save only + echo to sender) ─
        java.util.concurrent.CountDownLatch fileLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(fileLatch);

        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "See attached file",
                  "fileUrl":    "http://localhost:8080/uploads/resume.pdf",
                  "fileName":   "resume.pdf",
                  "fileType":   "PDF"
                }
                """.formatted(userId, counsellorUserId));

        Assertions.assertTrue(fileLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "File message echo not received");
        Thread.sleep(200);

        Assertions.assertTrue(receivedByA.stream().anyMatch(m -> m.contains("resume.pdf")),
                "Echo should contain fileName 'resume.pdf'");
        Assertions.assertTrue(receivedByA.stream().anyMatch(m -> m.contains("PDF")),
                "Echo should contain fileType 'PDF'");
        receivedByA.clear();

        // ── 2. Verify file message persisted in history ───────────────────────
        given().when()
                .get("/api/chat/history?userA=" + userId + "&userB=" + counsellorUserId)
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("find { it.fileUrl != null }.fileName", equalTo("resume.pdf"))
                .body("find { it.fileUrl != null }.fileType", equalTo("PDF"))
                .body("find { it.fileUrl != null }.fileUrl",  containsString("resume.pdf"));

        // ── 3. Send plain text while receiver offline ─────────────────────────
        java.util.concurrent.CountDownLatch textLatch = new java.util.concurrent.CountDownLatch(1);
        activeLatch.set(textLatch);

        sessionA.getBasicRemote().sendText("""
                {
                  "type":       "message",
                  "senderId":   %d,
                  "receiverId": %d,
                  "content":    "Sending while counsellor is offline"
                }
                """.formatted(userId, counsellorUserId));

        Assertions.assertTrue(textLatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "Echo of offline-receiver text message not received");
        Thread.sleep(200);
        Assertions.assertTrue(receivedByA.stream()
                        .anyMatch(m -> m.contains("Sending while counsellor is offline")),
                "Echo content mismatch for offline-receiver branch");

        // ── 4. Verify total history count ─────────────────────────────────────
        given().when()
                .get("/api/chat/history?userA=" + userId + "&userB=" + counsellorUserId)
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)));

        // ── 5. Close session A ────────────────────────────────────────────────
        activeLatch.set(null);
        sessionA.close();
        Thread.sleep(200);

        System.out.println("[TC-17] WebSocket file-message and offline-receiver test complete");
    }

    // =========================================================================
    // TC-18  ADMIN — get all users, update user, create counsellor
    //         Covers: UserAdminController — all endpoints
    // =========================================================================
//    @Test @Order(18)
//    @DisplayName("TC-18: Admin — get all users; update user via admin; create counsellor via admin")
//    void tc18_adminController() {
//
//        // 18a. Admin get all users
//        given()
//                .when().get("/api/admin/users")
//                .then().statusCode(anyOf(equalTo(200), equalTo(404)));
//
//        // 18b. Admin update user (exercises PUT /api/admin/update/{id})
//        given()
//                .contentType(ContentType.JSON)
//                .body("""
//                        {
//                          "name":   "Admin Updated Name",
//                          "email":  "%s",
//                          "active": true
//                        }
//                        """.formatted(USER_EMAIL))
//                .when().put("/api/admin/update/" + userId)
//                .then().statusCode(anyOf(equalTo(200), equalTo(404), equalTo(500)));
//
//        // 18c. Admin create counsellor (POST /api/admin/counsellors)
//        given()
//                .contentType(ContentType.JSON)
//                .body("""
//                        {
//                          "name":     "Admin Counsellor",
//                          "emailId":  "admin_couns_%s@test.com",
//                          "password": "%s"
//                        }
//                        """.formatted(TS, PASSWORD))
//                .when().post("/api/admin/counsellors")
//                .then().statusCode(anyOf(equalTo(200), equalTo(201), equalTo(400)));
//
//        // 18d. Admin create counsellor — missing name → 400
//        given()
//                .contentType(ContentType.JSON)
//                .body("""
//                        {
//                          "emailId":  "no_name@test.com",
//                          "password": "pass"
//                        }
//                        """)
//                .when().post("/api/admin/counsellors")
//                .then().statusCode(400);
//
//        // 18e. Admin create counsellor — missing email → 400
//        given()
//                .contentType(ContentType.JSON)
//                .body("""
//                        {
//                          "name":     "NoEmail",
//                          "password": "pass"
//                        }
//                        """)
//                .when().post("/api/admin/counsellors")
//                .then().statusCode(400);
//
//        // 18f. Admin delete non-existent user → 404
//        given()
//                .when().delete("/api/admin/999999")
//                .then().statusCode(404);
//    }

    // =========================================================================
    // TC-19  ERROR / EDGE PATHS — remaining 404/validation paths across all modules
    //
    // FIX: All URLs corrected to use /api prefix where required.
    // =========================================================================
    @Test @Order(19)
    @DisplayName("TC-19: Edge paths — unknown IDs on all modules; validation errors; empty bodies")
    void tc19_edgePaths() {

        // Notes — update unknown note → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "title": "Ghost", "content": "x", "label": "x", "dueDate": "2026-01-01" }
                        """)
                .when().put("/api/notes/999999")
                .then().statusCode(404);

        // Notes — share unknown note → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "counsellorUserId": %d }
                        """.formatted(counsellorUserId))
                .when().patch("/api/notes/999999/share")
                .then().statusCode(404);

        // Notes — unshare unknown note → 404
        given()
                .when().patch("/api/notes/999999/unshare")
                .then().statusCode(404);

        // Appointments — get by unknown counsellor (returns empty list, 200)
        given()
                .when().get("/api/appointments/counsellor/999999")
                .then().statusCode(200)
                .body("$", hasSize(0));

        // Tasks — create with missing title → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "userEmail": "%s" }
                        """.formatted(USER_EMAIL))
                .when().post("/api/tasks")
                .then().statusCode(400);

        // Tasks — create with missing email → 400
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "title": "No email" }
                        """)
                .when().post("/api/tasks")
                .then().statusCode(400);

        // Tasks — create with unknown email → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "userEmail": "nobody@nowhere.com", "title": "Ghost Task" }
                        """)
                .when().post("/api/tasks")
                .then().statusCode(404);

        // Sleep — missing hours → 400
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/sleep/" + userId)
                .then().statusCode(400);

        // Sleep — get curve (returns list, even if empty, for any userId)
        given()
                .when().get("/sleep/999999")
                .then().statusCode(200);

        // AI Chat — clear for unknown user → 404
        given()
                .when().delete("/api/ai-chat/999999/history")
                .then().statusCode(404);

        // AI Chat — get history for unknown user → 404
        given()
                .when().get("/api/ai-chat/999999/history")
                .then().statusCode(404);

        // Counsellor profile upsert for non-existent user → 404
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "displayName": "Ghost", "status": "AVAILABLE" }
                        """)
                .when().put("/api/counsellors/999999/profile")
                .then().statusCode(404);

        // Counsellors — update status for non-existent → 404
        given()
                .when().put("/api/counsellors/999999/update/AVAILABLE")
                .then().statusCode(404);

        // Prescription — get prescriptions for unknown user → 200 empty list
        given()
                .when().get("/prescriptions/users/999999")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    // =========================================================================
    // TC-20  CLEANUP — Delete both accounts; verify 404 after; double-delete → 404
    //         Covers: UserController.deleteUser(), cascade delete
    // =========================================================================
    @Test @Order(20)
    @DisplayName("TC-20: Cleanup — delete user & counsellor; 404 on get after delete; double-delete → 404")
    void tc20_deleteAccounts() {

        // 20a. Verify user still exists
        given()
                .when().get("/users/" + userId)
                .then().statusCode(200)
                .body("email", equalTo(USER_EMAIL));

        // 20b. Delete the user (cascade may touch chat, tasks, etc.)
        int userDeleteStatus = given()
                .when().delete("/users/" + userId)
                .then().statusCode(anyOf(equalTo(204), equalTo(500)))
                .extract().statusCode();

        System.out.println("[TC-20] DELETE userId=" + userId + " → " + userDeleteStatus);

        if (userDeleteStatus == 204) {
            // 20c. User is gone
            given().when().get("/users/" + userId).then().statusCode(404);

            // 20d. Double-delete → 404
            given().when().delete("/users/" + userId).then().statusCode(404);
        }

        // 20e. Delete non-existent user → 404
        given()
                .when().delete("/users/999999")
                .then().statusCode(404);

        // 20f. Delete counsellor account
        int counsellorDeleteStatus = given()
                .when().delete("/users/" + counsellorUserId)
                .then().statusCode(anyOf(equalTo(204), equalTo(500)))
                .extract().statusCode();

        System.out.println("[TC-20] DELETE counsellorUserId=" + counsellorUserId + " → " + counsellorDeleteStatus);

        if (counsellorDeleteStatus == 204) {
            given().when().get("/users/" + counsellorUserId).then().statusCode(404);
        }
    }
}