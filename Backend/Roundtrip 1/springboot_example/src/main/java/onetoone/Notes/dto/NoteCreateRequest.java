package onetoone.Notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Request body for creating a note")
public class NoteCreateRequest {

    @Schema(description = "Title of the note", example = "Meeting Summary")
    private String title;

    @Schema(description = "Content of the note", example = "Discussed project timelines...")
    private String content;

    @Schema(description = "Label/category for the note", example = "Work")
    private String label;

    @Schema(description = "Due date for the note", example = "2026-05-01")
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