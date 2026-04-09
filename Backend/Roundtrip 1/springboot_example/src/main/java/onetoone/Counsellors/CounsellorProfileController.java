package onetoone.Counsellors;

import onetoone.Counsellors.dto.CounsellorProfileRequest;
import onetoone.Counsellors.dto.CounsellorProfileResponse;
import onetoone.Users.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import onetoone.Users.User;
import onetoone.Users.UserRepository;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
/**
 * @author Boudhayan Chakraborty
 *
 * Services:
 * 1. Create counsellor profile - add bio, profile pic, etc.
 * 2. Update profile as a whole
 * 3. Update bio
 * 4. Update profile pic via link
 * 5. Update the availability status
 */


@RestController
@RequestMapping("/api/counsellors")
@Tag(name = "Counsellor Profile Controller", description = "APIs for managing counsellor profiles")
public class CounsellorProfileController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounsellorProfileRepository counsellorProfileRepository;

    private final CounsellorProfileService service;

    public CounsellorProfileController(CounsellorProfileService service) {
        this.service = service;
    }


    @Operation(summary = "List all counsellors", description = "Retrieves all counsellor profiles")
    @ApiResponse(responseCode = "200", description = "Counsellors retrieved successfully")
    @GetMapping
    public List<CounsellorProfileResponse> listAll() {
        return service.listAllCounsellors();
    }


    @Operation(summary = "Update counsellor status", description = "Updates availability status (AVAILABLE, BUSY, OFFLINE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Counsellor not found")
    })
    @PutMapping("/{userId}/update/{counsellorstatus}")
    public ResponseEntity<?> updateStatus(@PathVariable long userId,
                                          @PathVariable CounsellorStatus counsellorstatus) {

        CounsellorProfile counsellor =
                counsellorProfileRepository.findByUser_Id(userId).orElse(null);

        if (counsellor == null) {
            return ResponseEntity.status(404).body("Counsellor profile not found for userId= " + userId);
        }

        counsellor.setStatus(counsellorstatus);
        counsellorProfileRepository.save(counsellor);
        return ResponseEntity.ok(counsellor.getStatus());
    }


    @Operation(summary = "Update counsellor rating", description = "Adds a new rating and recalculates average rating")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating updated successfully"),
            @ApiResponse(responseCode = "404", description = "Counsellor not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{userId}/rating/{rating}")
    public ResponseEntity<?> updateRating(@PathVariable long userId,
                                          @PathVariable double rating) {
        try {
            System.out.println("HIT updateRating userId=" + userId + " rating=" + rating);

            CounsellorProfile counsellor =
                    counsellorProfileRepository.findByUser_Id(userId).orElse(null);

            if (counsellor == null) {
                return ResponseEntity.status(404).body("Counsellor profile not found for userId=" + userId);
            }

            Integer countObj = counsellor.getRatingCount();
            Double avgObj = counsellor.getRatingAverage();
            int oldCount = (countObj == null) ? 0 : countObj;
            double oldAverage = (avgObj == null) ? 0.0 : avgObj;

            double newAverage = ((oldAverage * oldCount) + rating) / (oldCount + 1);

            counsellor.setRatingCount(oldCount + 1);
            counsellor.setRatingAverage(newAverage);

            counsellorProfileRepository.save(counsellor);
            return ResponseEntity.ok("Rating updated");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("ERROR: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }


    @Operation(summary = "Update profile picture", description = "Updates counsellor profile picture URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile picture updated"),
            @ApiResponse(responseCode = "404", description = "Counsellor not found")
    })
    @PutMapping("/{userId}/profilePicture/{profilePictureURL}")
    public ResponseEntity<?> updateProfilepic(@PathVariable long userId,
                                              @PathVariable String profilePictureURL) {
        CounsellorProfile counsellor =
                counsellorProfileRepository.findByUser_Id(userId).orElse(null);

        if (counsellor == null) {
            return ResponseEntity.status(404).body("Counsellor profile not found for userId=" + userId);
        }

        counsellor.setProfilePictureUrl(profilePictureURL);
        counsellorProfileRepository.save(counsellor);
        return ResponseEntity.ok("success");
    }



    @Operation(summary = "Get counsellor profile", description = "Fetches profile details by user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(service.getProfileByUserId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(msg(e.getMessage()));
        }
    }


    @Operation(summary = "Create or update profile", description = "Creates or updates a counsellor profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile created/updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/profile")
    public ResponseEntity<?> upsert(@PathVariable Long id,
                                    @RequestBody CounsellorProfile req) {

        User u = userRepository.findById(id).orElse(null);
        if (u == null)
            return ResponseEntity.status(404).body(msg("Counsellor user not found"));

        CounsellorProfile profile = counsellorProfileRepository.findByUser_Id(id).orElse(null);

        if (profile == null) {
            profile = new CounsellorProfile();
            profile.setUser(u);

            // initialize ONLY once
            profile.setRatingAverage(0.0);
            profile.setRatingCount(0);
        }

        profile.setDisplayName(req.getDisplayName());
        profile.setSpecialization(req.getSpecialization());
        profile.setBio(req.getBio());
        profile.setProfilePictureUrl(req.getProfilePictureUrl());
        profile.setStatus(req.getStatus());


        counsellorProfileRepository.save(profile);
        return ResponseEntity.ok(service.getProfileByUserId(id)); // ← DTO, no circular ref
    }

    private String msg(String m) {
        return "{\"message\":\"" + m.replace("\"", "'") + "\"}";
    }


    @Operation(summary = "Update full profile", description = "Updates all profile fields for a counsellor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "Counsellor not found")
    })
    @PutMapping("/{userId}/update")
    public String updateProfile(@PathVariable Long userId,
                                 @RequestBody CounsellorProfile req) {
        CounsellorProfile counsellor =
                counsellorProfileRepository.findByUser_Id(userId).orElse(null);

        if (counsellor == null) {
            return msg("Counsellor user not found");
        }

        counsellor.setProfilePictureUrl(req.getProfilePictureUrl());
        counsellor.setDisplayName(req.getDisplayName());
        counsellor.setSpecialization(req.getSpecialization());
        counsellor.setBio(req.getBio());
        counsellor.setProfilePictureUrl(req.getProfilePictureUrl());
        counsellor.setStatus(req.getStatus());
        counsellorProfileRepository.save(counsellor);
        return msg("success");
    }
}