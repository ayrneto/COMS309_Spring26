package onetoone.Users;

import onetoone.Users.dto.CreateCounsellorRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
@RequestMapping("/api/admin")
@Tag(name = "User Admin Controller", description = "Admin operations for managing users and counsellors")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty())
            return ResponseEntity.ok("{\"message\":\"No users found\"}");
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has related data");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting user");
        }
    }

    @Operation(
            summary = "Update user",
            description = "Updates user details such as name, email, active status, and optionally password"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    
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
                existing.setConfirmPassword(incoming.getConfirmPassword());// ideally hash
            }

            userRepository.save(existing);
            return "success";

        }).orElse("failure");
    }



    @Operation(
            summary = "Create counsellor",
            description = "Creates a new counsellor user with name, email, and password"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Counsellor created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
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