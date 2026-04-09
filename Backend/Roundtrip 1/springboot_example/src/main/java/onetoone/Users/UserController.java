package onetoone.Users;

import java.util.List;
import onetoone.Users.LoginResponse;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
//import onetoone.Login.LoginPage;
import onetoone.Counsellors.CounsellorProfile;
import onetoone.Counsellors.CounsellorProfileController;
import onetoone.Counsellors.CounsellorProfileRepository;
import onetoone.Counsellors.CounsellorStatus;
import org.hibernate.usertype.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

@RestController
@Tag(name = "User Controller", description = "Handles user authentication and user management APIs")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CounsellorProfileRepository counsellorProfileRepository;

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

        // if role can be null, default it
        user.setRole(req.getRole() != null ? req.getRole() : Role.USER);

        User saved = userRepository.save(user);

        // ONLY create counsellor profile when role is counsellor
        if (saved.getRole() == Role.COUNSELLOR) {
            CounsellorProfile profile = new CounsellorProfile();
            profile.setUser(saved);                       // important if you have a relation
            profile.setDisplayName(saved.getName());
            profile.setSpecialization("");
            profile.setBio("");
            profile.setProfilePictureUrl("");
            profile.setStatus(CounsellorStatus.AVAILABLE);
            profile.setRatingCount(0);
            profile.setRatingAverage(0.0);

            saved.setCounsellorProfile(profile);  // set on user side
            userRepository.save(saved);     // <-- save directly
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(saved.getId(), saved.getEmail()));
    }


    // Delete user

    @Operation(summary = "Delete user", description = "Deletes user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.getPassword().equals(req.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (req.getRole() != user.getRole()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not a " + req.getRole());
        }

        return ResponseEntity.ok(
                new LoginResponse(user.getId(), user.getEmail(), user.getRole().name())  // ← add role
        );
    }

}
