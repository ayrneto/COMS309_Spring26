package onetoone.Counsellors.dto;

import onetoone.Counsellors.CounsellorStatus;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Response object for counsellor profile")
public class CounsellorProfileResponse {

    @Schema(description = "Profile ID", example = "1")
    public Long id;
    @Schema(description = "User ID associated with this profile", example = "2")
    public Integer userId;

    @Schema(description = "Display name of the counsellor", example = "Dr. John Doe")
    public String displayName;
    @Schema(description = "Specialization of the counsellor", example = "Cognitive Behavioral Therapy")
    public String specialization;
    @Schema(description = "Bio of the counsellor", example = "Experienced therapist specializing in CBT")
    public String bio;
    @Schema(description = "URL to profile picture", example = "https://example.com/profile.jpg")
    public String profilePictureUrl;

    @Schema(description = "Average rating of the counsellor", example = "4.8")
    public Double ratingAverage;
    @Schema(description = "Total number of ratings received", example = "25")
    public Integer ratingCount;

    @Schema(description = "Availability status of the counsellor", example = "AVAILABLE")
    public CounsellorStatus status;
}