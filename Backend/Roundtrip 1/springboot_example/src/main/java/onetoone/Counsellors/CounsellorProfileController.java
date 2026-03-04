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
public class CounsellorProfileController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounsellorProfileRepository counsellorProfileRepository;

    private final CounsellorProfileService service;

    public CounsellorProfileController(CounsellorProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<CounsellorProfileResponse> listAll() {
        return service.listAllCounsellors();
    }

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

    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(service.getProfileByUserId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(msg(e.getMessage()));
        }
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> upsert(@PathVariable Long id,
                                    @RequestBody CounsellorProfileRequest req) {

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

        profile.setDisplayName(req.displayName);
        profile.setSpecialization(req.specialization);
        profile.setBio(req.bio);
        profile.setProfilePictureUrl(req.profilePictureUrl);
        profile.setStatus(req.status);


        CounsellorProfile saved = counsellorProfileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    private String msg(String m) {
        return "{\"message\":\"" + m.replace("\"", "'") + "\"}";
    }

    @PutMapping("/{userId}/update")
    public String updateProfile(@PathVariable Long userId,
                                 @RequestBody CounsellorProfileRequest req) {
        CounsellorProfile counsellor =
                counsellorProfileRepository.findByUser_Id(userId).orElse(null);

        if (counsellor == null) {
            return msg("Counsellor user not found");
        }

        counsellor.setProfilePictureUrl(req.profilePictureUrl);
        counsellor.setDisplayName(req.displayName);
        counsellor.setSpecialization(req.specialization);
        counsellor.setBio(req.bio);
        counsellor.setProfilePictureUrl(req.profilePictureUrl);
        counsellor.setStatus(req.status);
        counsellorProfileRepository.save(counsellor);
        return msg("success");
    }
}