package onetoone.Notes.dto;

import onetoone.Notes.Note;

public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private String createdAt;
    private String updatedAt;
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