package onetoone.Notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a note")
public class NoteCreateRequest {

    @Schema(description = "Title of the note", example = "Meeting Summary")
    private String title;
    @Schema(description = "Content of the note", example = "Discussed project timelines...")
    private String content;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}