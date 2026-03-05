package onetoone.Notes;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import onetoone.Notes.dto.NoteCreateRequest;
import onetoone.Notes.dto.NoteUpdateRequest;
import java.util.List;

@Service
public class NoteService {

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    UserRepository userRepository;

    public Note createNote(Long userId, NoteCreateRequest req) {  // ← NoteCreateRequest not Note
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        Note note = new Note();                  // ← create fresh Note from request
        note.setTitle(req.getTitle());
        note.setContent(req.getContent());
        note.setUser(user);
        return noteRepository.save(note);
    }

    public List<Note> getUserNotes(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        return noteRepository.findByUserId(userId);
    }

    public Note getNote(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + noteId));
    }

    public Note updateNote(Long noteId, NoteUpdateRequest req) {  // ← NoteUpdateRequest not Note
        Note note = getNote(noteId);
        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getContent() != null) note.setContent(req.getContent());
        return noteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + noteId);
        }
        noteRepository.deleteById(noteId);
    }
}