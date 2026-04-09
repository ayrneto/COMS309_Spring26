package onetoone.Notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for updating a note")
public class NoteUpdateRequest {

    @Schema(description = "Updated title of the note", example = "Updated Meeting Summary")
    private String title;
    @Schema(description = "Updated content of the note", example = "Added additional discussion points...")
    private String content;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}