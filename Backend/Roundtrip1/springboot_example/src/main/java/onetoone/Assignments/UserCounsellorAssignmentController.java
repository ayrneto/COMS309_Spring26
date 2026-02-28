package onetoone.Assignments;

import onetoone.Counsellors.CounsellorProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
public class UserCounsellorAssignmentController {

    private final UserCounsellorAssignmentRepository assignmentRepo;
    private final CounsellorProfileRepository profileRepo;

    public UserCounsellorAssignmentController(
            UserCounsellorAssignmentRepository assignmentRepo,
            CounsellorProfileRepository profileRepo
    ) {
        this.assignmentRepo = assignmentRepo;
        this.profileRepo = profileRepo;
    }

    // User profile screen: fetch assigned counsellor "card"
    @GetMapping("/user/{userId}/counsellor-card")
    public ResponseEntity<?> getAssignedCounsellorCard(@PathVariable long userId) {
        var assignment = assignmentRepo.findByUserId((int) userId).orElse(null);
        if (assignment == null) return ResponseEntity.ok("{}"); // no assigned counsellor yet

        long counsellorUserId = assignment.getCounsellor().getId();
        return profileRepo.findByUser_Id((long) counsellorUserId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("{\"message\":\"Assigned counsellor has no profile\"}"));
    }
}