package onetoone.test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import io.restassured.response.Response;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * System / Integration tests for the Calmify backend.
 *
 * Auth note: the application has no JWT / Spring-Security layer, so there
 * is no "Bearer token" in this codebase.  Login returns { id, email, role }.
 * Signup is POST /users/signup and requires { name, email, password,
 * confirmPassword, role }.
 *
 * Unique-email strategy: every @Test that creates a user uses a timestamp
 * suffix so parallel / repeated runs don't collide on the UNIQUE email column.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BoudhayanSystemTests {

    @LocalServerPort
    int port;

    // Shared state populated in @BeforeEach
    private long userId;
    private String userEmail;
    private long counsellorUserId;
    private String counsellorEmail;

    // ── helpers ─────────────────────────────────────────────────────────────

    /** Register a USER-role account and return its id. */
    private long registerUser(String name, String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "name": "%s",
                        "email": "%s",
                        "password": "%s",
                        "confirmPassword": "%s",
                        "role": "USER"
                    }
                    """, name, email, password, password))
                .when().post("/users/signup")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    /** Register a COUNSELLOR-role account and return its id. */
    private long registerCounsellor(String name, String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "name": "%s",
                        "email": "%s",
                        "password": "%s",
                        "confirmPassword": "%s",
                        "role": "COUNSELLOR"
                    }
                    """, name, email, password, password))
                .when().post("/users/signup")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    // ── test setup ──────────────────────────────────────────────────────────

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = port;

        long ts = System.currentTimeMillis();

        userEmail      = "user_"      + ts + "@test.com";
        counsellorEmail = "counsellor_" + ts + "@test.com";

        userId          = registerUser(      "Test User",       userEmail,       "pass123");
        counsellorUserId = registerCounsellor("Test Counsellor", counsellorEmail, "pass456");
    }

    // ════════════════════════════════════════════════════════════════════════
    // USER CONTROLLER  –  /users/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 1 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/signup – happy path
     * A brand-new account should return 201 with the new user's id and email.
     */
    @Test @Order(1)
    void testSignupSuccess() {
        long ts = System.currentTimeMillis();
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "name": "New User",
                    "email": "newuser_%d@test.com",
                    "password": "secret",
                    "confirmPassword": "secret",
                    "role": "USER"
                }
                """, ts))
                .when().post("/users/signup")
                .then()
                .statusCode(201)
                .body("id",    notNullValue())
                .body("email", notNullValue());
    }

    // ── TEST 2 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/signup – duplicate email must return 409.
     */
    @Test @Order(2)
    void testSignupDuplicateEmail() {
        // userEmail was already registered in @BeforeEach
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "name": "Dup User",
                    "email": "%s",
                    "password": "pass123",
                    "confirmPassword": "pass123",
                    "role": "USER"
                }
                """, userEmail))
                .when().post("/users/signup")
                .then()
                .statusCode(409);
    }

    // ── TEST 3 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/signup – mismatched passwords must return 400.
     */
    @Test @Order(3)
    void testSignupPasswordMismatch() {
        long ts = System.currentTimeMillis();
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "name": "Bad Pass",
                    "email": "mismatch_%d@test.com",
                    "password": "abc",
                    "confirmPassword": "xyz",
                    "role": "USER"
                }
                """, ts))
                .when().post("/users/signup")
                .then()
                .statusCode(400);
    }

    // ── TEST 4 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/login – correct credentials must return 200 with id, email,
     * and role.
     */
    @Test @Order(4)
    void testLoginSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "email": "%s",
                    "password": "pass123",
                    "role": "USER"
                }
                """, userEmail))
                .when().post("/users/login")
                .then()
                .statusCode(200)
                .body("id",    notNullValue())
                .body("email", equalTo(userEmail))
                .body("role",  equalTo("USER"));
    }

    // ── TEST 5 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/login – wrong password must return 401.
     */
    @Test @Order(5)
    void testLoginWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "email": "%s",
                    "password": "WRONG",
                    "role": "USER"
                }
                """, userEmail))
                .when().post("/users/login")
                .then()
                .statusCode(401);
    }

    // ── TEST 6 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/login – non-existent email must return 401.
     */
    @Test @Order(6)
    void testLoginNonExistentEmail() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "email": "nobody_xyz_123@nowhere.com",
                    "password": "pass",
                    "role": "USER"
                }
                """)
                .when().post("/users/login")
                .then()
                .statusCode(401);
    }

    // ── TEST 7 ──────────────────────────────────────────────────────────────
    /**
     * POST /users/login – role mismatch must return 403.
     */
    @Test @Order(7)
    void testLoginRoleMismatch() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "email": "%s",
                    "password": "pass123",
                    "role": "COUNSELLOR"
                }
                """, userEmail))
                .when().post("/users/login")
                .then()
                .statusCode(403);
    }

    // ── TEST 8 ──────────────────────────────────────────────────────────────
    /**
     * GET /users/{id} – existing user must return 200 with correct fields.
     */
    @Test @Order(8)
    void testGetUserById() {
        given()
                .when().get("/users/" + userId)
                .then()
                .statusCode(200)
                .body("id",    equalTo((int) userId))
                .body("email", equalTo(userEmail));
    }

    // ── TEST 9 ──────────────────────────────────────────────────────────────
    /**
     * GET /users/{id} – unknown id must return 404.
     */
    @Test @Order(9)
    void testGetUserByIdNotFound() {
        given()
                .when().get("/users/9999999")
                .then()
                .statusCode(404);
    }

    // ── TEST 10 ─────────────────────────────────────────────────────────────
    /**
     * GET /users/LoginPage/user/{email} – known email must return 200.
     */
    @Test @Order(10)
    void testGetUserByEmail() {
        given()
                .when().get("/users/LoginPage/user/" + userEmail)
                .then()
                .statusCode(200)
                .body("email", equalTo(userEmail));
    }

    // ── TEST 11 ─────────────────────────────────────────────────────────────
    /**
     * GET /users/LoginPage/user/{email} – unknown email must return 404.
     */
    @Test @Order(11)
    void testGetUserByEmailNotFound() {
        given()
                .when().get("/users/LoginPage/user/nobody@nowhere.com")
                .then()
                .statusCode(404);
    }

    // ── TEST 12 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /users/{id} – delete an existing user must return 204.
     */
    @Test @Order(12)
    void testDeleteUser() {
        // Create a throwaway user to delete
        long ts   = System.currentTimeMillis();
        String em = "todelete_" + ts + "@test.com";
        long id   = registerUser("To Delete", em, "pw");

        given()
                .when().delete("/users/" + id)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .when().get("/users/" + id)
                .then()
                .statusCode(404);
    }

    // ── TEST 13 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /users/{id} – unknown id must return 404.
     */
    @Test @Order(13)
    void testDeleteUserNotFound() {
        given()
                .when().delete("/users/9999999")
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // DAILY CHECK-IN  –  /users/{userId}/checkins
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 14 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/{userId}/checkins – valid check-in must return 201.
     */
    @Test @Order(14)
    void testCreateCheckIn() {
        Response resp = given()
                .contentType(ContentType.JSON)
                .body("""
            {
                "rating": 3,
                "description": "Feeling okay",
                "date": "2025-01-10",
                "reminderTime": "",
                "sleepTime": "5"
                            
            }
            """)
                .when().post("/users/" + userId + "/checkins");

        System.out.println("STATUS: " + resp.statusCode());
        System.out.println("BODY:   " + resp.body().asString());

        resp.then().statusCode(201);  // will still fail but now you see the error
    }

    // ── TEST 15 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/{userId}/checkins – missing rating must return 400.
     */
    @Test @Order(15)
    void testCreateCheckInMissingRating() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "description": "No rating",
                    "date": "2025-01-11"
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then()
                .statusCode(400);
    }

    // ── TEST 16 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/{userId}/checkins – rating out of range must return 400.
     */
    @Test @Order(16)
    void testCreateCheckInRatingOutOfRange() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 9,
                    "date": "2025-01-12"
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then()
                .statusCode(400);
    }

    // ── TEST 17 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/{userId}/checkins – missing date must return 400.
     */
    @Test @Order(17)
    void testCreateCheckInMissingDate() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 2
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then()
                .statusCode(400);
    }

    // ── TEST 18 ─────────────────────────────────────────────────────────────
    /**
     * GET /users/{userId}/checkins – must return 200 with a list.
     */
    @Test @Order(18)
    void testGetAllCheckIns() {
        // Seed one check-in first
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 4,
                    "description": "Good day",
                    "date": "2025-02-01"
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(201);

        given()
                .when().get("/users/" + userId + "/checkins")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 19 ─────────────────────────────────────────────────────────────
    /**
     * GET /users/{userId}/checkins – unknown userId must return 404.
     */
    @Test @Order(19)
    void testGetCheckInsUserNotFound() {
        given()
                .when().get("/users/9999999/checkins")
                .then()
                .statusCode(404);
    }

    // ── TEST 20 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/{userId}/checkins – duplicate date must return 409.
     */
    @Test @Order(20)
    void testCreateCheckInDuplicate() {
        // First check-in
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 3,
                    "date": "2025-03-01"
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then().statusCode(201);

        // Duplicate same date
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 5,
                    "date": "2025-03-01"
                }
                """)
                .when().post("/users/" + userId + "/checkins")
                .then()
                .statusCode(409);
    }

    // ── TEST 21 ─────────────────────────────────────────────────────────────
    /**
     * PUT /users/checkins/{checkInId} – update an existing check-in.
     */
    @Test @Order(21)
    void testUpdateCheckIn() {
        // Create
        int checkInId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "rating": 2,
                        "description": "Before update",
                        "date": "2025-04-01"
                    }
                    """)
                        .when().post("/users/" + userId + "/checkins")
                        .then().statusCode(201)
                        .extract().path("id");

        // Update
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 5,
                    "description": "After update"
                }
                """)
                .when().put("/users/checkins/" + checkInId)
                .then()
                .statusCode(200)
                .body("rating", equalTo(5));
    }

    // ── TEST 22 ─────────────────────────────────────────────────────────────
    /**
     * PUT /users/checkins/{checkInId} – unknown id must return 404.
     */
    @Test @Order(22)
    void testUpdateCheckInNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "rating": 3
                }
                """)
                .when().put("/users/checkins/9999999")
                .then()
                .statusCode(404);
    }

    // ── TEST 23 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /users/checkins/{checkInId} – must return 204.
     */
    @Test @Order(23)
    void testDeleteCheckIn() {
        int checkInId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "rating": 1,
                        "date": "2025-05-15"
                    }
                    """)
                        .when().post("/users/" + userId + "/checkins")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .when().delete("/users/checkins/" + checkInId)
                .then()
                .statusCode(204);
    }

    // ── TEST 24 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /users/checkins/{checkInId} – unknown id must return 404.
     */
    @Test @Order(24)
    void testDeleteCheckInNotFound() {
        given()
                .when().delete("/users/checkins/9999999")
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ADMIN CONTROLLER  –  /api/admin/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 25 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/admin/users – must return 200 with a list of users.
     */
    @Test @Order(25)
    void testAdminGetAllUsers() {
        given()
                .when().get("/api/admin/users")
                .then()
                .statusCode(200);
    }

    // ── TEST 26 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/admin/update/{id} – admin can update a user's name.
     */
    @Test @Order(26)
    void testAdminUpdateUser() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "name": "Updated Name",
                    "email": "%s",
                    "active": true
                }
                """, userEmail))
                .when().put("/api/admin/update/" + userId)
                .then()
                .statusCode(200);
    }

    // ── TEST 27 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/admin/counsellors – create a counsellor via admin endpoint.
     */
    @Test @Order(27)
    void testAdminCreateCounsellor() {
        long ts = System.currentTimeMillis();
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "name": "Admin Counsellor %d",
                    "emailId": "admincounsellor_%d@test.com",
                    "password": "adminpass"
                }
                """, ts, ts))
                .when().post("/api/admin/counsellors")
                .then()
                .statusCode(200);
    }

    // ── TEST 28 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/admin/counsellors – missing name must return 400.
     */
    @Test @Order(28)
    void testAdminCreateCounsellorMissingName() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "emailId": "missingname@test.com",
                    "password": "pw"
                }
                """)
                .when().post("/api/admin/counsellors")
                .then()
                .statusCode(400);
    }

    // ── TEST 29 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /api/admin/{id} – delete an existing user must return 204.
     */
    @Test @Order(29)
    void testAdminDeleteUser() {
        long ts   = System.currentTimeMillis();
        String em = "admindelete_" + ts + "@test.com";
        long id   = registerUser("Admin Delete", em, "pw");

        given()
                .when().delete("/api/admin/" + id)
                .then()
                .statusCode(204);
    }

    // ── TEST 30 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /api/admin/{id} – unknown id must return 404.
     */
    @Test @Order(30)
    void testAdminDeleteUserNotFound() {
        given()
                .when().delete("/api/admin/9999999")
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // NOTES CONTROLLER  –  /api/users/{userId}/notes  /api/notes/{noteId}
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 31 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/users/{userId}/notes – create a note must return 201.
     */
    @Test @Order(31)
    void testCreateNote() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "title": "Test Note",
                    "content": "Some content here",
                    "label": "Health",
                    "dueDate": "2025-12-31"
                }
                """)
                .when().post("/api/users/" + userId + "/notes")
                .then()
                .statusCode(201)
                .body("title",   equalTo("Test Note"))
                .body("content", equalTo("Some content here"));
    }

    // ── TEST 32 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/users/{userId}/notes – returns list for user.
     */
    @Test @Order(32)
    void testGetUserNotes() {
        // Seed one note
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "title": "List Note",
                    "content": "For listing"
                }
                """)
                .when().post("/api/users/" + userId + "/notes")
                .then().statusCode(201);

        given()
                .when().get("/api/users/" + userId + "/notes")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(1));
    }

    // ── TEST 33 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/notes/{noteId} – fetch single note by id.
     */
    @Test @Order(33)
    void testGetSingleNote() {
        int noteId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "title": "Single Note",
                        "content": "Solo"
                    }
                    """)
                        .when().post("/api/users/" + userId + "/notes")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .when().get("/api/notes/" + noteId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Single Note"));
    }

    // ── TEST 34 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/notes/{noteId} – update note fields.
     */
    @Test @Order(34)
    void testUpdateNote() {
        int noteId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "title": "Before",
                        "content": "Old content"
                    }
                    """)
                        .when().post("/api/users/" + userId + "/notes")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "title": "After",
                    "content": "New content",
                    "label": "Updated",
                    "dueDate": "2026-06-30"
                }
                """)
                .when().put("/api/notes/" + noteId)
                .then()
                .statusCode(200)
                .body("title",   equalTo("After"))
                .body("content", equalTo("New content"));
    }

    // ── TEST 35 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /api/notes/{noteId} – delete note must return 204.
     */
    @Test @Order(35)
    void testDeleteNote() {
        int noteId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "title": "Temp Note",
                        "content": "Will be deleted"
                    }
                    """)
                        .when().post("/api/users/" + userId + "/notes")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .when().delete("/api/notes/" + noteId)
                .then()
                .statusCode(204);

        // Verify deleted → 404
        given()
                .when().get("/api/notes/" + noteId)
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // TASKS CONTROLLER  –  /api/tasks  /api/users/{userId}/tasks
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 36 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/tasks – create a task assigned to user by email.
     */
    @Test @Order(36)
    void testCreateTask() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "userEmail": "%s",
                    "title": "Walk 30 minutes",
                    "description": "Daily exercise",
                    "dueDate": "2025-12-01"
                }
                """, userEmail))
                .when().post("/api/tasks")
                .then()
                .statusCode(201)
                .body("title", equalTo("Walk 30 minutes"));
    }

    // ── TEST 37 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/tasks – missing title must return 400.
     */
    @Test @Order(37)
    void testCreateTaskMissingTitle() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "userEmail": "%s"
                }
                """, userEmail))
                .when().post("/api/tasks")
                .then()
                .statusCode(400);
    }

    // ── TEST 38 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/tasks – missing email must return 400.
     */
    @Test @Order(38)
    void testCreateTaskMissingEmail() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "title": "Some Task"
                }
                """)
                .when().post("/api/tasks")
                .then()
                .statusCode(400);
    }

    // ── TEST 39 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/users/{userId}/tasks – returns list of tasks.
     */
    @Test @Order(39)
    void testGetUserTasks() {
        // Seed a task
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "userEmail": "%s",
                    "title": "List Task"
                }
                """, userEmail))
                .when().post("/api/tasks")
                .then().statusCode(201);

        given()
                .when().get("/api/users/" + userId + "/tasks")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 40 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/tasks/{taskId}/status – update task status.
     */
    @Test @Order(40)
    void testUpdateTaskStatus() {
        int taskId =
                given()
                        .contentType(ContentType.JSON)
                        .body(String.format("""
                    {
                        "userEmail": "%s",
                        "title": "Status Task"
                    }
                    """, userEmail))
                        .when().post("/api/tasks")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                { "status": "COMPLETED" }
                """)
                .when().put("/api/tasks/" + taskId + "/status")
                .then()
                .statusCode(200);
    }

    // ── TEST 41 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/tasks/{taskId}/status – invalid status value must return 400.
     */
    @Test @Order(41)
    void testUpdateTaskStatusInvalid() {
        int taskId =
                given()
                        .contentType(ContentType.JSON)
                        .body(String.format("""
                    {
                        "userEmail": "%s",
                        "title": "Bad Status Task"
                    }
                    """, userEmail))
                        .when().post("/api/tasks")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                { "status": "FLYING" }
                """)
                .when().put("/api/tasks/" + taskId + "/status")
                .then()
                .statusCode(400);
    }

    // ── TEST 42 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/tasks/{taskId} – update task fields.
     */
    @Test @Order(42)
    void testUpdateTask() {
        int taskId =
                given()
                        .contentType(ContentType.JSON)
                        .body(String.format("""
                    {
                        "userEmail": "%s",
                        "title": "Old Title"
                    }
                    """, userEmail))
                        .when().post("/api/tasks")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "title": "New Title",
                    "description": "Updated desc"
                }
                """)
                .when().put("/api/tasks/" + taskId)
                .then()
                .statusCode(200)
                .body("title", equalTo("New Title"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // COUNSELLOR PROFILE CONTROLLER  –  /api/counsellors/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 43 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/counsellors – returns list of all counsellors.
     */
    @Test @Order(43)
    void testListAllCounsellors() {
        given()
                .when().get("/api/counsellors")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 44 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/counsellors/{userId}/profile – counsellor registered via
     * signup automatically gets a profile.
     */
    @Test @Order(44)
    void testGetCounsellorProfile() {
        given()
                .when().get("/api/counsellors/" + counsellorUserId + "/profile")
                .then()
                .statusCode(200);
    }

    // ── TEST 45 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{id}/profile – create/update counsellor profile.
     */
    @Test @Order(45)
    void testUpsertCounsellorProfile() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "displayName": "Dr. Smith",
                    "specialization": "Anxiety",
                    "bio": "Expert in CBT",
                    "profilePictureUrl": "https://example.com/pic.jpg",
                    "status": "AVAILABLE"
                }
                """)
                .when().put("/api/counsellors/" + counsellorUserId + "/profile")
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Dr. Smith"));
    }

    // ── TEST 46 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{userId}/update/{status} – update availability.
     */
    @Test @Order(46)
    void testUpdateCounsellorStatus() {
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/update/BUSY")
                .then()
                .statusCode(200);
    }

    // ── TEST 47 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{userId}/update/{status} – unknown user 404.
     */
    @Test @Order(47)
    void testUpdateCounsellorStatusNotFound() {
        given()
                .when().put("/api/counsellors/9999999/update/AVAILABLE")
                .then()
                .statusCode(404);
    }

    // ── TEST 48 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{userId}/rating/{rating} – update counsellor rating.
     */
    @Test @Order(48)
    void testUpdateCounsellorRating() {
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/rating/4.5")
                .then()
                .statusCode(200);
    }

    // ── TEST 49 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{userId}/profilePicture/{url} – update pic URL.
     */
    @Test @Order(49)
    void testUpdateCounsellorProfilePicture() {
        given()
                .when().put("/api/counsellors/" + counsellorUserId + "/profilePicture/newpic.jpg")
                .then()
                .statusCode(200);
    }

    // ── TEST 50 ─────────────────────────────────────────────────────────────
    /**
     * PUT /api/counsellors/{userId}/update – full profile update.
     */
    @Test @Order(50)
    void testUpdateCounsellorFullProfile() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "displayName": "Dr. Updated",
                    "specialization": "Depression",
                    "bio": "Updated bio",
                    "profilePictureUrl": "https://example.com/updated.jpg",
                    "status": "AVAILABLE"
                }
                """)
                .when().put("/api/counsellors/" + counsellorUserId + "/update")
                .then()
                .statusCode(200);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ASSIGNMENTS CONTROLLER  –  /api/assignments/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 51 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/assignments/user/{userId}/counsellor-card – no assignment yet
     * returns 200 with empty body.
     */
    @Test @Order(51)
    void testGetAssignedCounsellorCardNone() {
        given()
                .when().get("/api/assignments/user/" + userId + "/counsellor-card")
                .then()
                .statusCode(200);
    }

    // ── TEST 52 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/assignments/user/{userId}/choose/{counsellorId} – assign.
     */
    @Test @Order(52)
    void testChooseCounsellor() {
        given()
                .when().post("/api/assignments/user/" + userId + "/choose/" + counsellorUserId)
                .then()
                .statusCode(200);
    }

    // ── TEST 53 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/assignments/user/{userId}/random – random assignment.
     */
    @Test @Order(53)
    void testRandomAssignCounsellor() {
        given()
                .when().post("/api/assignments/user/" + userId + "/random")
                .then()
                .statusCode(200);
    }

    // ── TEST 54 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/assignments/user/{userId}/counsellor-card – after assignment
     * returns counsellor profile data.
     */
    @Test @Order(54)
    void testGetAssignedCounsellorCardAfterAssign() {
        // Assign first
        given()
                .when().post("/api/assignments/user/" + userId + "/choose/" + counsellorUserId)
                .then().statusCode(200);

        given()
                .when().get("/api/assignments/user/" + userId + "/counsellor-card")
                .then()
                .statusCode(200);
    }

    // ── TEST 55 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /api/assignments/user/{userId} – unassign must return 200.
     */
    @Test @Order(55)
    void testUnassignCounsellor() {
        // Assign first
        given()
                .when().post("/api/assignments/user/" + userId + "/choose/" + counsellorUserId)
                .then().statusCode(200);

        given()
                .when().delete("/api/assignments/user/" + userId)
                .then()
                .statusCode(200);
    }

    // ── TEST 56 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /api/assignments/user/{userId} – no assignment returns 404.
     */
    @Test @Order(56)
    void testUnassignCounsellorNoAssignment() {
        given()
                .when().delete("/api/assignments/user/" + userId)
                .then()
                .statusCode(404);
    }

    // ── TEST 57 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/assignments/user/{userId}/choose/{counsellorId} – unknown
     * user returns 404.
     */
    @Test @Order(57)
    void testChooseCounsellorUserNotFound() {
        given()
                .when().post("/api/assignments/user/9999999/choose/" + counsellorUserId)
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // APPOINTMENTS CONTROLLER  –  /api/appointments/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 58 ─────────────────────────────────────────────────────────────
    /**
     * POST /api/appointments – book an appointment must return 201.
     */
    @Test @Order(58)
    void testBookAppointment() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "userId": %d,
                    "counsellorId": %d,
                    "date": "2025-12-01",
                    "timeSlot": "10:00 AM",
                    "notes": "First session"
                }
                """, userId, counsellorUserId))
                .when().post("/api/appointments")
                .then()
                .statusCode(201);
    }

    // ── TEST 59 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/appointments/counsellor/{id} – appointments for counsellor.
     */
    @Test @Order(59)
    void testGetAppointmentsByCounsellor() {
        given()
                .when().get("/api/appointments/counsellor/" + counsellorUserId)
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 60 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/appointments/counsellor/{id}/accepted – confirmed only.
     */
    @Test @Order(60)
    void testGetAcceptedByCounsellor() {
        given()
                .when().get("/api/appointments/counsellor/" + counsellorUserId + "/accepted")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 61 ─────────────────────────────────────────────────────────────
    /**
     * GET /api/appointments/user/{id}/accepted – confirmed for user.
     */
    @Test @Order(61)
    void testGetAcceptedByUser() {
        given()
                .when().get("/api/appointments/user/" + userId + "/accepted")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 62 ─────────────────────────────────────────────────────────────
    /**
     * PATCH /api/appointments/{id}/accept – accept an appointment.
     */
    @Test @Order(62)
    void testAcceptAppointment() {
        int apptId =
                given()
                        .contentType(ContentType.JSON)
                        .body(String.format("""
                    {
                        "userId": %d,
                        "counsellorId": %d,
                        "date": "2025-12-05",
                        "timeSlot": "2:00 PM"
                    }
                    """, userId, counsellorUserId))
                        .when().post("/api/appointments")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .when().patch("/api/appointments/" + apptId + "/accept")
                .then()
                .statusCode(200);
    }

    // ── TEST 63 ─────────────────────────────────────────────────────────────
    /**
     * PATCH /api/appointments/{id}/decline – decline an appointment.
     */
    @Test @Order(63)
    void testDeclineAppointment() {
        int apptId =
                given()
                        .contentType(ContentType.JSON)
                        .body(String.format("""
                    {
                        "userId": %d,
                        "counsellorId": %d,
                        "date": "2025-12-10",
                        "timeSlot": "3:00 PM"
                    }
                    """, userId, counsellorUserId))
                        .when().post("/api/appointments")
                        .then().statusCode(201)
                        .extract().path("id");

        given()
                .when().patch("/api/appointments/" + apptId + "/decline")
                .then()
                .statusCode(200);
    }

    // ── TEST 64 ─────────────────────────────────────────────────────────────
    /**
     * PATCH /api/appointments/{id}/accept – unknown id returns 404.
     */
    @Test @Order(64)
    void testAcceptAppointmentNotFound() {
        given()
                .when().patch("/api/appointments/9999999/accept")
                .then()
                .statusCode(404);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ROUTINE TRACKER  –  /routines/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 65 ─────────────────────────────────────────────────────────────
    /**
     * POST /routines/users/{userId}/routines – create a routine.
     */
    @Test @Order(65)
    void testCreateRoutine() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "name": "Morning Walk",
                    "description": "Walk 30 min after wake up"
                }
                """)
                .when().post("/routines/users/" + userId + "/routines")
                .then()
                .statusCode(200)
                .body("name", equalTo("Morning Walk"));
    }

    // ── TEST 66 ─────────────────────────────────────────────────────────────
    /**
     * GET /routines – returns all routines.
     */
    @Test @Order(66)
    void testGetAllRoutines() {
        given()
                .when().get("/routines")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 67 ─────────────────────────────────────────────────────────────
    /**
     * POST /routines/{id}/checkin – check in to a routine.
     */
    @Test @Order(67)
    void testRoutineCheckIn() {
        int routineId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "name": "Checkin Routine",
                        "description": "For checkin test"
                    }
                    """)
                        .when().post("/routines/users/" + userId + "/routines")
                        .then().statusCode(200)
                        .extract().path("id");

        given()
                .when().post("/routines/" + routineId + "/checkin")
                .then()
                .statusCode(200);
    }

    // ── TEST 68 ─────────────────────────────────────────────────────────────
    /**
     * GET /routines/users/{userId}/checkins – user's routine check-ins.
     */
    @Test @Order(68)
    void testGetUserRoutineCheckIns() {
        given()
                .when().get("/routines/users/" + userId + "/checkins")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 69 ─────────────────────────────────────────────────────────────
    /**
     * DELETE /routines/{id} – delete a routine.
     */
    @Test @Order(69)
    void testDeleteRoutine() {
        int routineId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                        "name": "Temp Routine",
                        "description": "Will be deleted"
                    }
                    """)
                        .when().post("/routines/users/" + userId + "/routines")
                        .then().statusCode(200)
                        .extract().path("id");

        given()
                .when().delete("/routines/" + routineId)
                .then()
                .statusCode(200);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SLEEP TRACKER  –  /sleep/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 70 ─────────────────────────────────────────────────────────────
    /**
     * POST /sleep/{userId} – log sleep hours must return 200.
     */
    @Test @Order(70)
    void testLogSleep() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "hours": 7.5,
                    "date": "2025-06-01"
                }
                """)
                .when().post("/sleep/" + userId)
                .then()
                .statusCode(200);
    }

    // ── TEST 71 ─────────────────────────────────────────────────────────────
    /**
     * POST /sleep/{userId} – missing hours must return 400.
     */
    @Test @Order(71)
    void testLogSleepMissingHours() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "date": "2025-06-02"
                }
                """)
                .when().post("/sleep/" + userId)
                .then()
                .statusCode(400);
    }

    // ── TEST 72 ─────────────────────────────────────────────────────────────
    /**
     * GET /sleep/{userId} – returns sleep curve data.
     */
    @Test @Order(72)
    void testGetSleepCurve() {
        given()
                .when().get("/sleep/" + userId)
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRESCRIPTION CONTROLLER  –  /prescriptions/*
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 73 ─────────────────────────────────────────────────────────────
    /**
     * GET /prescriptions/users/{userId} – returns prescriptions list.
     */
    @Test @Order(73)
    void testGetUserPrescriptions() {
        given()
                .when().get("/prescriptions/users/" + userId)
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── TEST 74 ─────────────────────────────────────────────────────────────
    /**
     * POST /prescriptions/users/{userId} – assign prescription returns 200.
     */
    @Test @Order(74)
    void testCreatePrescription() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "medicationName": "Melatonin",
                    "dosage": "5mg",
                    "frequency": "DAILY"
                }
                """)
                .when().post("/prescriptions/users/" + userId)
                .then()
                .statusCode(200);
    }

    // ════════════════════════════════════════════════════════════════════════
    // COUNSELLOR SIGNUP  (login for COUNSELLOR role)
    // ════════════════════════════════════════════════════════════════════════

    // ── TEST 75 ─────────────────────────────────────────────────────────────
    /**
     * POST /users/login – counsellor can log in with COUNSELLOR role.
     */
    @Test @Order(75)
    void testCounsellorLogin() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                    "email": "%s",
                    "password": "pass456",
                    "role": "COUNSELLOR"
                }
                """, counsellorEmail))
                .when().post("/users/login")
                .then()
                .statusCode(200)
                .body("role", equalTo("COUNSELLOR"));
    }
}