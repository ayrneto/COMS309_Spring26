package onetoone.Assignments;

import onetoone.Counsellors.CounsellorProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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

    public UserCounsellorAssignmentController(
            UserCounsellorAssignmentRepository assignmentRepo,
            CounsellorProfileRepository profileRepo
    ) {
        this.assignmentRepo = assignmentRepo;
        this.profileRepo = profileRepo;
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