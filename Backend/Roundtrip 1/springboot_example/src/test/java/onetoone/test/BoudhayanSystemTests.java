package onetoone.test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BoudhayanSystemTests {

    private static String token;
    private static int userId;

    @LocalServerPort
    int port;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Register user
        userId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                {
                    "username": "testuser1",
                    "password": "password123",
                    "email": "test1@gmail.com"
                }
            """)
                        .when()
                        .post("/users/register")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        // Login
        token =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                {
                    "username": "testuser1",
                    "password": "password123"
                }
            """)
                        .when()
                        .post("/users/login")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("token");
    }

    /**
     * TEST 1: Login flow validation
     */
    @Test
    public void testUserLoginSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "username": "testuser1",
                    "password": "password123"
                }
            """)
                .when()
                .post("/users/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    /**
     * TEST 2: Unauthorized access should fail
     */
    @Test
    public void testUnauthorizedAccess() {
        when()
                .get("/users/" + userId)
                .then()
                .statusCode(401); // or 403 depending on your config
    }

    /**
     * TEST 3: Authorized user fetch profile
     */
    @Test
    public void testGetUserProfileWithToken() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/users/" + userId)
                .then()
                .statusCode(200)
                .body("username", equalTo("testuser1"));
    }

    /**
     * TEST 4: Update user profile and verify persistence
     */
    @Test
    public void testUpdateUserProfile() {
        // Update user email
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                {
                    "email": "updated@gmail.com"
                }
            """)
                .when()
                .put("/users/" + userId)
                .then()
                .statusCode(200);

        // Verify update
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/users/" + userId)
                .then()
                .statusCode(200)
                .body("email", equalTo("updated@gmail.com"));
    }
}