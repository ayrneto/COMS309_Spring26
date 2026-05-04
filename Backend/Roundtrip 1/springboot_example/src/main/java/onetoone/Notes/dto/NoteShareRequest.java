package onetoone.Notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for sharing a note with a counsellor")
public class NoteShareRequest {

    @Schema(description = "User ID of the counsellor to share the note with", example = "3")
    private Long counsellorUserId;

    public Long getCounsellorUserId() { return counsellorUserId; }
    public void setCounsellorUserId(Long counsellorUserId) { this.counsellorUserId = counsellorUserId; }
}