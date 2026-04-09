package onetoone.Counsellors.dto;

import onetoone.Counsellors.CounsellorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating or updating a counsellor profile")
public class CounsellorProfileRequest {

    @Schema(description = "Display name of the counsellor", example = "Dr. John Doe")
    public String displayName;
    @Schema(description = "Specialization of the counsellor", example = "Cognitive Behavioral Therapy")
    public String specialization;
    @Schema(description = "Bio or description of the counsellor", example = "Experienced therapist specializing in CBT")
    public String bio;
    @Schema(description = "URL to the counsellor's profile picture", example = "https://example.com/profile.jpg")
    public String profilePictureUrl;
    @Schema(description = "Availability status of the counsellor", example = "AVAILABLE")
    public CounsellorStatus status;
}