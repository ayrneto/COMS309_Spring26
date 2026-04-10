package onetoone.Notes;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import onetoone.Notes.dto.NoteCreateRequest;
import onetoone.Notes.dto.NoteUpdateRequest;
import onetoone.Users.Role;
import java.util.List;

@Service
public class NoteService {

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    UserRepository userRepository;

    public Note createNote(Long userId, NoteCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        Note note = new Note();
        note.setTitle(req.getTitle());
        note.setContent(req.getContent());
        note.setLabel(req.getLabel());
        note.setDueDate(req.getDueDate());
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

    public Note updateNote(Long noteId, NoteUpdateRequest req) {
        Note note = getNote(noteId);
        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getContent() != null) note.setContent(req.getContent());
        if (req.getLabel() != null) note.setLabel(req.getLabel());
        if (req.getDueDate() != null) note.setDueDate(req.getDueDate());
        return noteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + noteId);
        }
        noteRepository.deleteById(noteId);
    }

    public Note shareNote(Long noteId, Long counsellorUserId) {
        Note note = getNote(noteId);
        User counsellor = userRepository.findById(counsellorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Counsellor not found: " + counsellorUserId));

        // ADD THIS — reject if the user is not actually a counsellor
        if (counsellor.getRole() != Role.COUNSELLOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User " + counsellorUserId + " is not a counsellor");
        }

        note.setShared(true);
        note.setSharedWithCounsellor(counsellor);
        return noteRepository.save(note);
    }

    public Note unshareNote(Long noteId) {
        Note note = getNote(noteId);
        note.setShared(false);
        note.setSharedWithCounsellor(null);
        return noteRepository.save(note);
    }

    public List<Note> getNotesSharedWithCounsellor(Long counsellorUserId) {
        if (!userRepository.existsById(counsellorUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Counsellor not found: " + counsellorUserId);
        }
        return noteRepository.findBySharedWithCounsellorId(counsellorUserId);
    }
}