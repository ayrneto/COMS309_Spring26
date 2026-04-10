package onetoone.Notes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import onetoone.Notes.dto.NoteCreateRequest;
import onetoone.Notes.dto.NoteUpdateRequest;
import onetoone.Notes.dto.NoteResponse;
import onetoone.Notes.dto.NoteShareRequest;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api")
@Tag(name = "Note Controller", description = "APIs for managing user notes")
public class NoteController {

    @Autowired
    NoteService noteService;

    @Operation(summary = "Create a note", description = "Creates a new note for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Note created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/users/{userId}/notes")
    public ResponseEntity<NoteResponse> createNote(
            @PathVariable Long userId,
            @RequestBody NoteCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NoteResponse.from(noteService.createNote(userId, req)));
    }

    @Operation(summary = "Get all notes for a user", description = "Retrieves all notes associated with a given user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{userId}/notes")
    public ResponseEntity<List<NoteResponse>> getUserNotes(@PathVariable Long userId) {
        List<NoteResponse> notes = noteService.getUserNotes(userId)
                .stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notes);
    }

    @Operation(summary = "Get a single note", description = "Retrieves a note by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Note retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @GetMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(NoteResponse.from(noteService.getNote(noteId)));
    }

    @Operation(summary = "Update a note", description = "Updates the title and content of an existing note")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Note updated successfully"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest req) {
        return ResponseEntity.ok(NoteResponse.from(noteService.updateNote(noteId, req)));
    }

    @Operation(summary = "Delete a note", description = "Deletes a note by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Share a note with a counsellor", description = "Shares a note with the specified counsellor using their user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Note shared successfully"),
            @ApiResponse(responseCode = "404", description = "Note or counsellor not found")
    })
    @PatchMapping("/notes/{noteId}/share")
    public ResponseEntity<NoteResponse> shareNote(
            @PathVariable Long noteId,
            @RequestBody NoteShareRequest req) {
        return ResponseEntity.ok(NoteResponse.from(noteService.shareNote(noteId, req.getCounsellorUserId())));
    }

    @Operation(summary = "Unshare a note", description = "Removes the shared status from a note")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Note unshared successfully"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @PatchMapping("/notes/{noteId}/unshare")
    public ResponseEntity<NoteResponse> unshareNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(NoteResponse.from(noteService.unshareNote(noteId)));
    }

    @Operation(summary = "Get notes shared with a counsellor", description = "Retrieves all notes shared with the counsellor identified by their user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shared notes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Counsellor not found")
    })
    @GetMapping("/counsellors/{counsellorUserId}/shared-notes")
    public ResponseEntity<List<NoteResponse>> getSharedNotes(@PathVariable Long counsellorUserId) {
        List<NoteResponse> notes = noteService.getNotesSharedWithCounsellor(counsellorUserId)
                .stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notes);
    }
}