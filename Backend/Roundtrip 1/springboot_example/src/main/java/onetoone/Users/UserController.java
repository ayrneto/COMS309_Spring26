package onetoone.Users;

import java.util.List;

import onetoone.Assignments.UserCounsellorAssignmentRepository;
import onetoone.Users.LoginResponse;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import onetoone.Counsellors.CounsellorProfile;
import onetoone.Counsellors.CounsellorProfileController;
import onetoone.Counsellors.CounsellorProfileRepository;
import onetoone.Counsellors.CounsellorStatus;
import onetoone.Notification.NotificationService;
import onetoone.realtime_chat.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 *
 * @author Boudhayan Chakraborty
 *
 */

@RequestMapping("/users")
@RestController
@Tag(name = "User Controller", description = "Handles user authentication and user management APIs")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CounsellorProfileRepository counsellorProfileRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    UserCounsellorAssignmentRepository assignmentRepository;

    CounsellorProfileController counsellorProfileController;
    CounsellorProfile counsellorProfile;
    CounsellorStatus counsellorStatus;

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @Operation(summary = "Get all users", description = "Admin only - returns all users")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
     @Operation(summary = "Get user by email", description = "Fetch user details using email")
     @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
     })

    @GetMapping("/LoginPage/user/{email}")
    public ResponseEntity<User> getUserByEmail(
            @Parameter(description = "User email", example = "test@example.com")
            @PathVariable("email") String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // Delete assignments where this user is the patient or the counsellor
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user by ID", description = "Fetch user using unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public User getUserById(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Daily Check-in endpoint
     * User submits their anxiety level (1-10) and an optional note
     *
     * POST /users/{id}/checkin
     * Body: { "anxietyLevel": 7, "note": "Feeling stressed about exams" }
     */
    @PostMapping("/{id}/checkin")
    public ResponseEntity<?> dailyCheckIn(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        // Validate user exists
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate anxiety level is present
        if (!body.containsKey("anxietyLevel")) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"anxietyLevel is required (1-10)\"}");
        }

        int anxietyLevel;
        try {
            anxietyLevel = Integer.parseInt(body.get("anxietyLevel").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"anxietyLevel must be a number\"}");
        }

        // Validate range
        if (anxietyLevel < 1 || anxietyLevel > 10) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"anxietyLevel must be between 1 and 10\"}");
        }

        String note = body.containsKey("note") ? body.get("note").toString() : "";
        String anxietyCategory;
        String message;

        if (anxietyLevel <= 3) {
            anxietyCategory = "LOW";
            message = "Your anxiety level is low today. Keep it up!";
        } else if (anxietyLevel <= 6) {
            anxietyCategory = "MEDIUM";
            message = "Your anxiety level is moderate. Remember to take breaks and breathe.";
        } else {
            anxietyCategory = "HIGH";
            message = "Your anxiety level is high. Please reach out to your counsellor for support.";
        }

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("userName", user.getName());
        response.put("date", LocalDate.now().toString());
        response.put("anxietyLevel", anxietyLevel);
        response.put("anxietyCategory", anxietyCategory);
        response.put("note", note);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }



    @Operation(summary = "Register user", description = "Creates a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Passwords do not match"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<UserResponse> signup(
            @Parameter(description = "User signup details")
            @RequestBody User req) {

        if (req.getPassword() == null || !req.getPassword().equals(req.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        User existing = userRepository.findByEmail(req.getEmail());
        if (existing != null && existing.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());
        user.setConfirmPassword(req.getConfirmPassword());
        user.setActive(true);

        user.setRole(req.getRole() != null ? req.getRole() : Role.USER);

        User saved = userRepository.save(user);

        if (saved.getRole() == Role.COUNSELLOR) {
            CounsellorProfile profile = new CounsellorProfile();
            profile.setUser(saved);
            profile.setDisplayName(saved.getName());
            profile.setSpecialization("");
            profile.setBio("");
            profile.setProfilePictureUrl("");
            profile.setStatus(CounsellorStatus.AVAILABLE);
            profile.setRatingCount(0);
            profile.setRatingAverage(0.0);

            saved.setCounsellorProfile(profile);
            userRepository.save(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(saved.getId(), saved.getEmail()));
    }





    // ===================== LOGIN =====================
    @Operation(summary = "Login user", description = "Authenticate using email, password, and role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Role mismatch")
    })

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "Login credentials")
            @RequestBody LoginRequest req) {

        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.getPassword().equals(req.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (req.getRole() != user.getRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a " + req.getRole());
        }

        return ResponseEntity.ok(
                new LoginResponse(user.getId(), user.getEmail(), user.getRole().name())
        );
    }
}