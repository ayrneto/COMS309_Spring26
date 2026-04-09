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
    @Schema(description = "Creation timestamp of the note", example = "2026-04-05T12:00:00")
    private String createdAt;
    @Schema(description = "Last updated timestamp of the note", example = "2026-04-05T15:00:00")
    private String updatedAt;
    @Schema(description = "ID of the user who owns the note", example = "2")
    private Long userId;      // just the userId, NOT the whole User object

    // converts Note entity → NoteResponse DTO
    public static NoteResponse from(Note note) {
        NoteResponse r = new NoteResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        r.content = note.getContent();
        r.createdAt = note.getCreatedAt().toString();
        r.updatedAt = note.getUpdatedAt().toString();
        r.userId = note.getUser().getId();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Long getUserId() { return userId; }
}