package onetoone.Prescription;

import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;

    @Autowired
    private final UserRepository userRepository;

    public PrescriptionController(PrescriptionService service,
                                  UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    // Assign prescription to user
    @PostMapping("/users/{userId}")
    public String createPrescription(
            @PathVariable Long userId,
            @RequestBody Prescription prescription) {

        return "success";
    }

    // Get all prescriptions for user
    @GetMapping("/users/{userId}")
    public List<Prescription> getUserPrescriptions(@PathVariable Long userId) {
        return service.getUserPrescriptions(userId);
    }
}