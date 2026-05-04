package onetoone.Prescription;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Autowired
    private PrescriptionService service;

    @Autowired
    private UserRepository userRepo;

    @PostMapping("/users/{userId}/{counsellorId}")
    public String createPrescription(
            @PathVariable Long userId,@PathVariable Long counsellorId,
            @RequestBody Prescription prescription) {

        prescription.setUser(userRepo.findById(userId).orElse(null));
        service.assignPrescription(userId, counsellorId, prescription);
        return "success";
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserPrescriptions(@PathVariable Long userId) {
        try {
            List<Prescription> prescriptions = service.getUserPrescriptions(userId);
            return ResponseEntity.ok(prescriptions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}