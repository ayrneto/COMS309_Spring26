package onetoone.Users;

import onetoone.Users.dto.CreateCounsellorRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/update/{id}")
    public String updateUser(@RequestBody User incoming, @PathVariable long id) {

        return userRepository.findById(id).map(existing -> {

            // update only the fields you allow to change
            existing.setName(incoming.getName());
            existing.setEmail(incoming.getEmail());
            existing.setActive(incoming.isActive());

            // only update password if provided (optional)
            if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
                existing.setPassword(incoming.getPassword());
                existing.setConfirmPassword(incoming.getPassword());// ideally hash
            }

            userRepository.save(existing);
            return "success";

        }).orElse("failure");
    }

    @PostMapping("/counsellors")
    public ResponseEntity<?> createCounsellor(@RequestBody CreateCounsellorRequest req) {
        if (req.name == null || req.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\":\"username required\"}");
        }
        if (req.emailId == null || req.emailId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\":\"emailId required\"}");
        }
        if (req.password == null || req.password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\":\"password required\"}");
        }

        User u = new User();
        u.setName(req.name.trim());
        u.setEmail(req.emailId.trim());
        u.setPassword(req.password); // if that's your field
        userRepository.save(u);

        User saved = userRepository.save(u);
        return ResponseEntity.ok(saved);
    }
}