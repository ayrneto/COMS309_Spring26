package onetoone.Counsellors;

import onetoone.Counsellors.dto.CounsellorProfileRequest;
import onetoone.Counsellors.dto.CounsellorProfileResponse;
import onetoone.Users.Role;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CounsellorProfileService {

    private final CounsellorProfileRepository profileRepo;
    private final UserRepository userRepo;

    public CounsellorProfileService(CounsellorProfileRepository profileRepo, UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
    }

    public CounsellorProfileResponse upsertProfile(long counsellorUserId, CounsellorProfileRequest req) {
        User counsellor = userRepo.findById(counsellorUserId).orElse(null);
        if (counsellor == null) throw new IllegalArgumentException("Counsellor user not found");


        CounsellorProfile profile = profileRepo.findByUser_Id(counsellorUserId)
                .orElseGet(CounsellorProfile::new);

        profile.setUser(counsellor);
        profile.setDisplayName(nonEmptyOrThrow(req.displayName, "displayName"));
        profile.setSpecialization(nonEmptyOrThrow(req.specialization, "specialization"));
        profile.setBio(req.bio);
        profile.setProfilePictureUrl(req.profilePictureUrl);

        if (req.status != null) profile.setStatus(req.status);

        // defaults if first time
        if (profile.getRatingAverage() == null) profile.setRatingAverage(0.0);
        if (profile.getRatingCount() == null) profile.setRatingCount(0);

        CounsellorProfile saved = profileRepo.save(profile);
        return toResponse(saved);
    }

    public CounsellorProfileResponse getProfile(Long userId) {
        CounsellorProfile profile = profileRepo.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return toResponse(profile);
    }

    public List<CounsellorProfileResponse> listAllCounsellors() {
        return profileRepo.findAll().stream().map(this::toResponse).toList();
    }

    private CounsellorProfileResponse toResponse(CounsellorProfile p) {

        CounsellorProfileResponse r = new CounsellorProfileResponse();

        r.id = p.getId();
        r.userId = Math.toIntExact(p.getUser().getId());
        r.displayName = p.getDisplayName();
        r.specialization = p.getSpecialization();
        r.bio = p.getBio();
        r.profilePictureUrl = p.getProfilePictureUrl();
        r.ratingAverage = p.getRatingAverage();
        r.ratingCount = p.getRatingCount();
        r.status = p.getStatus();

        return r;
    }

    private String nonEmptyOrThrow(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return v.trim();
    }

    public CounsellorProfileResponse getProfileByUserId(Long userId) {

        CounsellorProfile profile = profileRepo.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Counsellor profile not found"));

        return toResponse(profile);
    }
}