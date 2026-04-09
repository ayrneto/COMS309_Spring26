package onetoone.Notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Request body for updating a note")
public class NoteUpdateRequest {

    @Schema(description = "Updated title of the note", example = "Updated Meeting Summary")
    private String title;

    @Schema(description = "Updated content of the note", example = "Added additional discussion points...")
    private String content;

    @Schema(description = "Updated label/category for the note", example = "Personal")
    private String label;

    @Schema(description = "Updated due date for the note", example = "2026-06-01")
    private LocalDate dueDate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}