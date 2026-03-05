package onetoone.Notes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import onetoone.Notes.dto.NoteCreateRequest;
import onetoone.Notes.dto.NoteUpdateRequest;
import onetoone.Notes.dto.NoteResponse;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class NoteController {

    @Autowired
    NoteService noteService;

    // POST - create a note for a user
    @PostMapping("/users/{userId}/notes")
    public ResponseEntity<NoteResponse> createNote(
            @PathVariable Long userId,
            @RequestBody NoteCreateRequest req) {       // ← NoteCreateRequest not Note
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NoteResponse.from(noteService.createNote(userId, req)));
    }

    // GET - all notes for a user
    @GetMapping("/users/{userId}/notes")
    public ResponseEntity<List<NoteResponse>> getUserNotes(@PathVariable Long userId) {
        List<NoteResponse> notes = noteService.getUserNotes(userId)
                .stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notes);
    }

    // GET - single note
    @GetMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(NoteResponse.from(noteService.getNote(noteId)));
    }

    // PUT - update a note
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest req) {       // ← NoteUpdateRequest not Note
        return ResponseEntity.ok(NoteResponse.from(noteService.updateNote(noteId, req)));
    }

    // DELETE - delete a note
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }
}