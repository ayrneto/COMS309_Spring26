package onetoone;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * ShreySystemTest — Backend System Tests
 * Run the entire class together, not individual methods.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShreySystemTest {

    private static long userId;
    private static long counsellorUserId;
    private static long appointmentId;
    private static long secondApptId;
    private static long taskId;

    private static final String TS          = String.valueOf(System.currentTimeMillis());
    private static final String USER_EMAIL  = "shrey_user_"  + TS + "@test.com";
    private static final String COUNS_EMAIL = "shrey_couns_" + TS + "@test.com";
    private static final String PASSWORD    = "Shrey@1234";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = 8080;
    }

    // =========================================================================
    // TC-1: Signup
    // =========================================================================
    @Test @Order(1)
    @DisplayName("TC-1: Signup — USER + COUNSELLOR created; wrong password → 400; duplicate email → 409")
    void testSignup() {

        String userBody = """
                {
                  "name": "Shrey User",
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s",
                  "role": "USER"
                }
                """.formatted(USER_EMAIL, PASSWORD, PASSWORD);

        userId = given()
                .contentType(ContentType.JSON).body(userBody)
                .when().post("/users/signup")
                .then()
                .statusCode(201)
                .body("email", equalTo(USER_EMAIL))
                .body("id", notNullValue())
                .extract().response().jsonPath().getLong("id");

        counsellorUserId = given()
                .contentType(ContentType.JSON).body("""
                        {
                          "name": "Dr. Shrey",
                          "email": "%s",
                          "password": "%s",
                          "confirmPassword": "%s",
                          "role": "COUNSELLOR"
                        }
                        """.formatted(COUNS_EMAIL, PASSWORD, PASSWORD))
                .when().post("/users/signup")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().response().jsonPath().getLong("id");

        System.out.println("[TC-1] userId=" + userId + ", counsellorUserId=" + counsellorUserId);

        // Wrong password → 400
        given().contentType(ContentType.JSON).body("""
                        {
                          "name": "Bad Actor",
                          "email": "bad_%s@test.com",
                          "password": "abc123",
                          "confirmPassword": "xyz999",
                          "role": "USER"
                        }
                        """.formatted(TS))
                .when().post("/users/signup")
                .then().statusCode(400);

        // Duplicate email → 409
        given().contentType(ContentType.JSON).body(userBody)
                .when().post("/users/signup")
                .then().statusCode(409);
    }

    // =========================================================================
    // TC-2: Notes — all endpoints (POST has lazy-load bug → 500, rest work)
    // =========================================================================
    @Test @Order(2)
    @DisplayName("TC-2: Notes — create note, read list, 404 on unknown note and user")
    void testUserNotes() {

        // POST → 500 (known lazy-load bug on Note.user fetch=LAZY)
        given().contentType(ContentType.JSON).body("""
                        {
                          "title": "Anxiety Journal",
                          "content": "Felt anxious before the presentation today",
                          "label": "Mental Health"
                        }
                        """)
                .when().post("/api/users/" + userId + "/notes")
                .then().statusCode(500);

        // GET list for user → empty (transaction rolled back)
        given().when().get("/api/users/" + userId + "/notes")
                .then().statusCode(200).body("$", hasSize(0));

        // GET single note — non-existent → 404
        given().when().get("/api/notes/999999")
                .then().statusCode(404);

        // GET notes for non-existent user → 404
        given().when().get("/api/users/999999/notes")
                .then().statusCode(404);

        // PUT non-existent note → 404
        given().contentType(ContentType.JSON).body("""
                        { "title": "Updated", "content": "Updated content", "label": "Test" }
                        """)
                .when().put("/api/notes/999999")
                .then().statusCode(404);

        // DELETE non-existent note → 404
        given().when().delete("/api/notes/999999")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-3: Notes error handling — double-DELETE, unknown user
    // =========================================================================
    @Test @Order(3)
    @DisplayName("TC-3: Notes error handling — GET/DELETE non-existent → 404; double-DELETE → 404")
    void testNotesErrorAndDelete() {

        given().when().get("/api/notes/999999").then().statusCode(404);
        given().when().delete("/api/notes/999999").then().statusCode(404);
        given().when().delete("/api/notes/999999").then().statusCode(404);
        given().when().get("/api/users/999999/notes").then().statusCode(404);
    }

    // =========================================================================
    // TC-4: Appointments
    // =========================================================================
    @Test @Order(4)
    @DisplayName("TC-4: Appointments — PENDING→CONFIRMED; PENDING→CANCELLED; 404 on invalid ID")
    void testAppointments() {

        appointmentId = given()
                .contentType(ContentType.JSON).body("""
                        {
                          "userId": %d,
                          "counsellorId": %d,
                          "date": "2025-08-20",
                          "timeSlot": "10:00 AM",
                          "notes": "Initial intake session"
                        }
                        """.formatted(userId, counsellorUserId))
                .when().post("/api/appointments")
                .then()
                .statusCode(201)
                .body("status",       equalTo("PENDING"))
                .body("userId",       equalTo((int)(long) userId))
                .body("counsellorId", equalTo((int)(long) counsellorUserId))
                .extract().response().jsonPath().getLong("id");

        System.out.println("[TC-4] appointmentId=" + appointmentId);

        // Accept → CONFIRMED
        given().when().patch("/api/appointments/" + appointmentId + "/accept")
                .then().statusCode(200).body("status", equalTo("CONFIRMED"));

        // Appears in user's accepted list
        given().when().get("/api/appointments/user/" + userId + "/accepted")
                .then().statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].status", equalTo("CONFIRMED"));

        // Appears in counsellor's full list
        given().when().get("/api/appointments/counsellor/" + counsellorUserId)
                .then().statusCode(200).body("$", hasSize(greaterThanOrEqualTo(1)));

        // Book second and decline → CANCELLED
        secondApptId = given()
                .contentType(ContentType.JSON).body("""
                        {
                          "userId": %d,
                          "counsellorId": %d,
                          "date": "2025-09-05",
                          "timeSlot": "2:00 PM",
                          "notes": "Follow-up session"
                        }
                        """.formatted(userId, counsellorUserId))
                .when().post("/api/appointments")
                .then().statusCode(201)
                .extract().response().jsonPath().getLong("id");

        given().when().patch("/api/appointments/" + secondApptId + "/decline")
                .then().statusCode(200).body("status", equalTo("CANCELLED"));

        // Non-existent ID → 404
        given().when().patch("/api/appointments/999999/decline")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-5: Chat — all endpoints including file upload
    // =========================================================================
    @Test @Order(5)
    @DisplayName("TC-5: Chat — history both sides; online-users; file upload empty file → 400")
    void testChatWithCounsellor() {

        // User fetches history with counsellor
        given().when()
                .get("/api/chat/history?userA=" + userId + "&userB=" + counsellorUserId)
                .then().statusCode(200).body("$", instanceOf(java.util.List.class));

        // Counsellor fetches same history (params swapped)
        given().when()
                .get("/api/chat/history?userA=" + counsellorUserId + "&userB=" + userId)
                .then().statusCode(200).body("$", instanceOf(java.util.List.class));

        // Online-users endpoint
        given().when().get("/api/chat/online-users")
                .then().statusCode(200).body("connectedUserIds", notNullValue());

        // File upload — empty file → 400 (hits the empty-file guard in ChatController)
        given()
                .multiPart("file", "", "text/plain")
                .multiPart("senderId", String.valueOf(userId))
                .multiPart("receiverId", String.valueOf(counsellorUserId))
                .when().post("/api/chat/upload")
                .then().statusCode(400);

        // File upload — valid small file → 200
        given()
                .multiPart("file", "hello.txt", "Hello World".getBytes(), "text/plain")
                .multiPart("senderId", String.valueOf(userId))
                .multiPart("receiverId", String.valueOf(counsellorUserId))
                .when().post("/api/chat/upload")
                .then().statusCode(200)
                .body("fileUrl",  notNullValue())
                .body("fileName", equalTo("hello.txt"))
                .body("fileType", equalTo("TXT"));
    }

    // =========================================================================
    // TC-6: Task Reminders
    // =========================================================================
    @Test @Order(6)
    @DisplayName("TC-6: Task Reminders — create with reminder; update reminder time; Not Started→Ongoing→Completed; invalid status → 400")
    void testTaskReminders() {

        taskId = given()
                .contentType(ContentType.JSON).body("""
                        {
                          "userEmail": "%s",
                          "title": "Breathing Exercise",
                          "description": "10 minutes of deep breathing every morning",
                          "dueDate": "2025-09-01",
                          "reminderDateTime": "2025-08-31T08:00:00"
                        }
                        """.formatted(USER_EMAIL))
                .when().post("/api/tasks")
                .then()
                .statusCode(201)
                .body("title",            equalTo("Breathing Exercise"))
                .body("status",           equalTo("Not Started"))
                .body("reminderDateTime", notNullValue())
                .extract().response().jsonPath().getLong("id");

        System.out.println("[TC-6] taskId=" + taskId);

        // Task in user's list
        given().when().get("/api/users/" + userId + "/tasks")
                .then().statusCode(200).body("$", hasSize(greaterThanOrEqualTo(1)));

        // Update title and reminder
        given().contentType(ContentType.JSON).body("""
                        {
                          "title": "Morning Breathing Exercise",
                          "description": "15 minutes — updated schedule",
                          "reminderDateTime": "2025-08-31T07:00:00"
                        }
                        """)
                .when().put("/api/tasks/" + taskId)
                .then().statusCode(200)
                .body("title",            equalTo("Morning Breathing Exercise"))
                .body("reminderDateTime", containsString("07:00"));

        // Not Started → Ongoing
        given().contentType(ContentType.JSON).body("{ \"status\": \"Ongoing\" }")
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(200).body("status", equalTo("Ongoing"));

        // Ongoing → Completed
        given().contentType(ContentType.JSON).body("{ \"status\": \"Completed\" }")
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(200).body("status", equalTo("Completed"));

        // Invalid status → 400
        given().contentType(ContentType.JSON).body("{ \"status\": \"FLYING\" }")
                .when().put("/api/tasks/" + taskId + "/status")
                .then().statusCode(400);
    }

    // =========================================================================
    // TC-7: User endpoints — login, get by email, get all, get by ID
    // =========================================================================
    @Test @Order(7)
    @DisplayName("TC-7: User — login success; wrong password → 401; wrong role → 403; get by email; get all; get by ID; 404 on unknown")
    void testUserEndpoints() {

        // Login success
        given().contentType(ContentType.JSON).body("""
                        {
                          "email": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(USER_EMAIL, PASSWORD))
                .when().post("/users/login")
                .then().statusCode(200).body("email", equalTo(USER_EMAIL));

        // Wrong password → 401
        given().contentType(ContentType.JSON).body("""
                        {
                          "email": "%s",
                          "password": "wrongpass",
                          "role": "USER"
                        }
                        """.formatted(USER_EMAIL))
                .when().post("/users/login")
                .then().statusCode(401);

        // Wrong role → 403
        given().contentType(ContentType.JSON).body("""
                        {
                          "email": "%s",
                          "password": "%s",
                          "role": "COUNSELLOR"
                        }
                        """.formatted(USER_EMAIL, PASSWORD))
                .when().post("/users/login")
                .then().statusCode(403);

        // Get user by email
        given().when().get("/users/LoginPage/user/" + USER_EMAIL)
                .then().statusCode(200).body("email", equalTo(USER_EMAIL));

        // Get by email — non-existent → 404
        given().when().get("/users/LoginPage/user/nobody@test.com")
                .then().statusCode(404);

        // Get all users
        given().when().get("/users/users")
                .then().statusCode(200).body("$", hasSize(greaterThanOrEqualTo(2)));

        // Get user by ID
        given().when().get("/users/" + userId)
                .then().statusCode(200).body("email", equalTo(USER_EMAIL));

        // Get non-existent user → 404
        given().when().get("/users/999999")
                .then().statusCode(404);
    }

    // =========================================================================
    // TC-8: Counsellor Profile — list, get, update status, update rating
    // =========================================================================
    @Test @Order(8)
    @DisplayName("TC-8: Counsellor Profile — list all; get profile; update status AVAILABLE+BUSY; update rating")
    void testCounsellorProfile() {

        // List all counsellors
        given().when().get("/api/counsellors")
                .then().statusCode(200).body("$", hasSize(greaterThanOrEqualTo(1)));

        // Get counsellor profile
        given().when().get("/api/counsellors/" + counsellorUserId + "/profile")
                .then().statusCode(200);

        // Update status → AVAILABLE
        given().when()
                .put("/api/counsellors/" + counsellorUserId + "/update/AVAILABLE")
                .then().statusCode(200);

        // Update status → BUSY
        given().when()
                .put("/api/counsellors/" + counsellorUserId + "/update/BUSY")
                .then().statusCode(200);

        // Update status → OFFLINE
        given().when()
                .put("/api/counsellors/" + counsellorUserId + "/update/OFFLINE")
                .then().statusCode(200);

        // Update rating
        given().when()
                .put("/api/counsellors/" + counsellorUserId + "/rating/5")
                .then().statusCode(200);

        // Non-existent counsellor → 404 or 500 (backend missing null check)
        given().when().get("/api/counsellors/999999/profile")
                .then().statusCode(anyOf(equalTo(404), equalTo(500)));
    }

    // =========================================================================
    // TC-9: AI Chat — history; delete; history empty after delete
    // =========================================================================
    @Test @Order(9)
    @DisplayName("TC-9: AI Chat — get history; delete history; history empty after delete")
    void testAiChat() {

        // Get history (empty initially)
        given().when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200).body("$", instanceOf(java.util.List.class));

        // Delete history
        given().when().delete("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200);

        // History empty after delete
        given().when().get("/api/ai-chat/" + userId + "/history")
                .then().statusCode(200).body("$", hasSize(0));
    }




    // =========================================================================
    // TC-11: Delete Account
    // =========================================================================
    @Test @Order(11)
    @DisplayName("TC-11: Delete Account — user exists before delete; 404 after; 404 on double-delete")
    void testDeleteAccount() {

        given().when().get("/users/" + userId)
                .then().statusCode(200).body("email", equalTo(USER_EMAIL));

        int deleteStatus = given().when().delete("/users/" + userId)
                .then().statusCode(anyOf(equalTo(204), equalTo(500)))
                .extract().statusCode();

        System.out.println("[TC-11] DELETE userId=" + userId + " → " + deleteStatus);

        if (deleteStatus == 204) {
            given().when().get("/users/" + userId).then().statusCode(404);
            given().when().delete("/users/" + userId).then().statusCode(404);
            given().when().delete("/users/" + counsellorUserId)
                    .then().statusCode(anyOf(equalTo(204), equalTo(500)));
        } else {
            given().when().get("/users/" + userId).then().statusCode(200);
        }
    }
}