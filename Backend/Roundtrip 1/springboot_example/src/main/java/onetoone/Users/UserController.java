package onetoone.Users;

import java.util.List;

import onetoone.Appointments.AppointmentRepository;
import onetoone.Assignments.UserCounsellorAssignmentRepository;

import jakarta.transaction.Transactional;
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
import java.util.stream.Collectors;

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
    DailyCheckInRepository dailyCheckInRepository;

    @Autowired
    CounsellorProfileRepository counsellorProfileRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    UserCounsellorAssignmentRepository assignmentRepository;

    @Autowired
    AppointmentRepository appointmentRepository;

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
            @PathVariable(value = "email") String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/checkins")
    public ResponseEntity<List<DailyCheckIn>> getAllCheckIns(@PathVariable Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<DailyCheckIn> checkIns = dailyCheckInRepository.findByUserId(userId);

        return ResponseEntity.ok(checkIns);
    }

    @PutMapping("/checkins/{checkInId}")
    public ResponseEntity<DailyCheckIn> updateCheckIn(
            @PathVariable Long checkInId,
            @RequestBody Map<String, Object> body) {

        DailyCheckIn checkIn = dailyCheckInRepository.findById(checkInId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Check-in not found"));

        if (!body.containsKey("rating")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating is required (1-5)");
        }

        int rating;
        try {
            rating = Integer.parseInt(body.get("rating").toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be a number");
        }

        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 5");
        }

        checkIn.setRating(rating);

        if (body.containsKey("description")) {
            checkIn.setDescription(body.get("description").toString());
        }

        if (body.containsKey("reminderTime")) {
            checkIn.setReminderTime(body.get("reminderTime").toString());
        }

        if (body.containsKey("date")) {
            try {
                checkIn.setDate(LocalDate.parse(body.get("date").toString()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
            }
        }

        dailyCheckInRepository.save(checkIn);

        return ResponseEntity.ok(checkIn);
    }

    @DeleteMapping("/checkins/{checkInId}")
    public ResponseEntity<Void> deleteCheckIn(@PathVariable Long checkInId) {

        DailyCheckIn checkIn = dailyCheckInRepository.findById(checkInId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Check-in not found"));

        dailyCheckInRepository.delete(checkIn);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

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

    @Operation(
            summary = "Get counsellors connected to a user",
            description = "Returns all counsellors who have a CONFIRMED appointment with this user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Counsellors retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/counsellors")
    public ResponseEntity<List<Map<String, Object>>> getUserCounsellors(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        List<Map<String, Object>> result = appointmentRepository
                .findConfirmedCounsellorsByUserId(userId)
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("email", c.getEmail());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{userId}/checkins")
    public ResponseEntity<?> createCheckIn(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (!body.containsKey("rating")) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"rating is required (1-5)\"}");
        }

        int rating;
        try {
            rating = Integer.parseInt(body.get("rating").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"rating must be a number\"}");
        }

        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"rating must be between 1 and 5\"}");
        }

        String description = body.containsKey("description")
                ? body.get("description").toString()
                : "";

        String reminderTime = body.containsKey("reminderTime")
                ? body.get("reminderTime").toString()
                : null;

        if (!body.containsKey("date")) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"date is required (yyyy-MM-dd)\"}");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(body.get("date").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"message\":\"Invalid date format (yyyy-MM-dd)\"}");
        }

        if (dailyCheckInRepository.findByUserIdAndDate(userId, date).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("{\"message\":\"Check-in already exists for this date\"}");
        }

        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setUser(user);
        checkIn.setRating(rating);
        checkIn.setDescription(description);
        checkIn.setReminderTime(reminderTime);
        checkIn.setDate(date);

        dailyCheckInRepository.save(checkIn);

        return ResponseEntity.status(HttpStatus.CREATED).body(checkIn);
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

        if (!user.isActive())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account deactivated");

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