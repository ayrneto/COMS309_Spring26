package onetoone.Login;

import onetoone.Users.Role;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/LoginPage")
public class LoginPage {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) {
            return ResponseEntity.status(401).body("{\"message\":\"failure\"}");
        }
        if (!user.getPasswordHash().equals(req.getPassword())) {
            return ResponseEntity.status(401).body("{\"message\":\"failure\"}");
        }

        return ResponseEntity.ok(user); // or return success message
    }

    @PutMapping("/forgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) {
            return ResponseEntity.status(404).body("{\"message\":\"failure\"}");
        }

        user.setPasswordHash(req.getNewPassword());
        userRepository.save(user);

        return ResponseEntity.ok("{\"message\":\"success\"}");
    }

    @PutMapping("/editUser/{adminId}")
    public ResponseEntity<String> editUser(@PathVariable long adminId, @RequestBody User updatedUser) {

        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty() || adminOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("{\"message\":\"failure\"}");
        }

        userRepository.save(updatedUser);
        return ResponseEntity.ok("{\"message\":\"success\"}");
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ForgotPasswordRequest {
        private String email;
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}