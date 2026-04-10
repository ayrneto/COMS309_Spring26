package onetoone.Notes.dto;

import onetoone.Notes.Note;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object for a note")
public class NoteResponse {

    @Schema(description = "Note ID", example = "1")
    private Long id;

    @Schema(description = "Title of the note", example = "Meeting Summary")
    private String title;

    @Schema(description = "Content of the note", example = "Discussed project timelines...")
    private String content;

    @Schema(description = "Label/category of the note", example = "Work")
    private String label;

    @Schema(description = "Due date of the note", example = "2026-05-01")
    private String dueDate;

    @Schema(description = "Creation timestamp of the note", example = "2026-04-05T12:00:00Z")
    private String createdAt;

    @Schema(description = "Last updated timestamp of the note", example = "2026-04-05T15:00:00Z")
    private String updatedAt;

    @Schema(description = "ID of the user who owns the note", example = "2")
    private Long userId;

    @Schema(description = "Whether this note has been shared with a counsellor", example = "true")
    private boolean shared;

    @Schema(description = "User ID of the counsellor this note is shared with", example = "3")
    private Long sharedWithCounsellorId;

    @Schema(description = "Name of the counsellor this note is shared with", example = "Dr. Jane Doe")
    private String sharedWithCounsellorName;

    public static NoteResponse from(Note note) {
        NoteResponse r = new NoteResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        r.content = note.getContent();
        r.label = note.getLabel();
        r.dueDate = note.getDueDate() != null ? note.getDueDate().toString() : null;
        r.createdAt = note.getCreatedAt().toString();
        r.updatedAt = note.getUpdatedAt().toString();
        r.userId = note.getUser().getId();
        r.shared = note.isShared();
        if (note.getSharedWithCounsellor() != null) {
            r.sharedWithCounsellorId   = note.getSharedWithCounsellor().getId();
            r.sharedWithCounsellorName = note.getSharedWithCounsellor().getName();
        }
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getLabel() { return label; }
    public String getDueDate() { return dueDate; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Long getUserId() { return userId; }
    public boolean isShared() { return shared; }
    public Long getSharedWithCounsellorId() { return sharedWithCounsellorId; }
    public String getSharedWithCounsellorName() { return sharedWithCounsellorName; }
}