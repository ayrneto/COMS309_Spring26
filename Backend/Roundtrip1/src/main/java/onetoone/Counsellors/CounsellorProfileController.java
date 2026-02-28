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
    public CounsellorStatus updateStatus(@PathVariable long userId, @PathVariable CounsellorStatus counsellorstatus) {
        CounsellorProfile counsellor = counsellorProfileRepository.findById(userId).orElse(null);
        if(counsellor == null)
            throw new RuntimeException("Counsellor not found");
        counsellor.setStatus(counsellorstatus);

        counsellorProfileRepository.save(counsellor);  // <-- persist
        return ResponseEntity.ok(counsellor.getStatus()).getBody();
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

        if (u.getRole() != Role.COUNSELLOR)
            return ResponseEntity.badRequest().body(msg("User is not a counsellor"));

        CounsellorProfile profile = counsellorProfileRepository.findByUser_Id(id).orElse(null);

        if (profile == null) {
            profile = new CounsellorProfile();
            profile.setUser(u);
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
}