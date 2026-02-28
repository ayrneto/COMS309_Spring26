package onetoone.Users;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Vivek Bengre
 *
 */

@RestController
public class UserController {

    @Autowired
    UserRepository userRepository;

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @GetMapping(path = "/LoginPage/user")
    List<User> getAllUsers(){
        return userRepository.findAll();
    }

    @PostMapping("/LoginPage/user")
    User createUser(@RequestBody User user){
        userRepository.save(user);

        return user;
    }

    @GetMapping("/LoginPage/user/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable("email") String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/LoginPage/user/{email}")
    public String deleteUserByEmail(@PathVariable("email") String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            return failure;
        }

        userRepository.delete(user);
        return success;
    }
}
