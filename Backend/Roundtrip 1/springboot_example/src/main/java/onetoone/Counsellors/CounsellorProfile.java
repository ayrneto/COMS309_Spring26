package onetoone.Counsellors;

import jakarta.persistence.*;
import onetoone.Users.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "counsellor_profiles")
public class CounsellorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"passwordHash"})
    private User user;

    @Column(nullable = false)
    private String displayName;          // what they want shown publicly

    @Column(nullable = false)
    private String specialization;       // e.g., "Anxiety & Depression"

    @Column(length = 1500)
    private String bio;                  // advertising/about text

    private String profilePictureUrl;    // stored URL, not the actual image

    private Double ratingAverage;        // simple numeric average
    private Integer ratingCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounsellorStatus status;

    public CounsellorProfile() {}

    // ---------- getters/setters ----------
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public Double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(Double ratingAverage) { this.ratingAverage = ratingAverage; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public CounsellorStatus getStatus() { return status; }
    public void setStatus(CounsellorStatus status) { this.status = status; }
}