package onetoone.Assignments;

import onetoone.Counsellors.CounsellorProfile;
import onetoone.Counsellors.CounsellorProfileRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< Backend/Roundtrip 1/springboot_example/src/main/java/onetoone/Assignments/UserCounsellorAssignmentController.java
import java.util.List;
import java.util.Random;

=======

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/assignments")
@Tag(name = "User-Counsellor Assignment", description = "APIs for fetching assigned counsellors for users")
public class UserCounsellorAssignmentController {

    private final UserCounsellorAssignmentRepository assignmentRepo;
    private final CounsellorProfileRepository profileRepo;
    private final UserRepository userRepository;

    public UserCounsellorAssignmentController(
            UserCounsellorAssignmentRepository assignmentRepo,
            CounsellorProfileRepository profileRepo,
            UserRepository userRepository
    ) {
        this.assignmentRepo = assignmentRepo;
        this.profileRepo = profileRepo;
        this.userRepository = userRepository;
    }


    @Operation(
            summary = "Get assigned counsellor card",
            description = "Fetches the counsellor profile assigned to a given user. Returns empty JSON if no counsellor is assigned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned counsellor card fetched successfully or empty if none assigned"),
            @ApiResponse(responseCode = "400", description = "Assigned counsellor has no profile")
    })
    // User profile screen: fetch assigned counsellor "card"
>>>>>>> Backend/Roundtrip 1/springboot_example/src/main/java/onetoone/Assignments/UserCounsellorAssignmentController.java
    @GetMapping("/user/{userId}/counsellor-card")
    public ResponseEntity<?> getAssignedCounsellorCard(@PathVariable long userId) {
        var assignment = assignmentRepo.findByUserId((int) userId).orElse(null);
        if (assignment == null) return ResponseEntity.ok("{}");

        long counsellorUserId = assignment.getCounsellor().getId();
        return profileRepo.findByUser_Id(counsellorUserId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body("{\"message\":\"Assigned counsellor has no profile\"}"));
    }

    // POST - user chooses a specific counsellor by counsellor's userId
    @PostMapping("/user/{userId}/choose/{counsellorId}")
    public ResponseEntity<?> chooseCounsellor(
            @PathVariable long userId,
            @PathVariable long counsellorId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ResponseEntity.status(404).body("{\"message\":\"User not found\"}");

        User counsellorUser = userRepository.findById(counsellorId).orElse(null);
        if (counsellorUser == null)
            return ResponseEntity.status(404).body("{\"message\":\"Counsellor not found\"}");

        // Check counsellor has a profile
        var counsellorProfile = profileRepo.findByUser_Id(counsellorId).orElse(null);
        if (counsellorProfile == null)
            return ResponseEntity.status(404).body("{\"message\":\"Counsellor has no profile\"}");

        // Remove existing assignment if any
        assignmentRepo.findByUserId((int) userId)
                .ifPresent(assignmentRepo::delete);

        // Create new assignment
        UserCounsellorAssignment assignment = new UserCounsellorAssignment();
        assignment.setUser(user);
        assignment.setCounsellor(counsellorUser);
        assignmentRepo.save(assignment);

        return ResponseEntity.ok("{\"message\":\"Counsellor assigned successfully\"}");
    }

    // POST - randomly assign an available counsellor to a user
    @PostMapping("/user/{userId}/random")
    public ResponseEntity<?> randomlyAssignCounsellor(@PathVariable long userId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ResponseEntity.status(404).body("{\"message\":\"User not found\"}");

        // Get all counsellor profiles
        List<CounsellorProfile> allCounsellors = profileRepo.findAll();
        if (allCounsellors.isEmpty())
            return ResponseEntity.status(404).body("{\"message\":\"No counsellors available\"}");

        // Pick a random one
        CounsellorProfile randomProfile = allCounsellors
                .get(new Random().nextInt(allCounsellors.size()));

        User counsellorUser = randomProfile.getUser();

        // Remove existing assignment if any
        assignmentRepo.findByUserId((int) userId)
                .ifPresent(assignmentRepo::delete);

        // Create new assignment
        UserCounsellorAssignment assignment = new UserCounsellorAssignment();
        assignment.setUser(user);
        assignment.setCounsellor(counsellorUser);
        assignmentRepo.save(assignment);

        return ResponseEntity.ok(randomProfile);
    }

    // DELETE - unassign counsellor from user
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> unassignCounsellor(@PathVariable long userId) {
        var assignment = assignmentRepo.findByUserId((int) userId).orElse(null);
        if (assignment == null)
            return ResponseEntity.status(404).body("{\"message\":\"No assignment found\"}");

        assignmentRepo.delete(assignment);
        return ResponseEntity.ok("{\"message\":\"Counsellor unassigned successfully\"}");
    }
}