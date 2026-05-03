package onetoone.Prescription;

import onetoone.Users.Role;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    @Autowired
    private UserRepository userRepository;

    private final PrescriptionRepository prescriptionRepo;

    public PrescriptionService(PrescriptionRepository prescriptionRepo) {
        this.prescriptionRepo = prescriptionRepo;
    }

    public Prescription assignPrescription(
            Long userId,
            Long counsellorId,
            Prescription prescription) {

        User patient = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User counsellor = userRepository.findById(counsellorId)
                .orElseThrow(() -> new RuntimeException("Counsellor not found"));

        if (counsellor.getRole() != Role.COUNSELLOR) {
            throw new RuntimeException("User is not a counsellor");
        }

        prescription.setUser(patient);
        prescription.setCounsellor(counsellor);

        if (prescription.getDurationDays() > 0 && prescription.getStartDate() != null) {
            prescription.setEndDate(
                    prescription.getStartDate().plusDays(prescription.getDurationDays())
            );
        }

        return prescriptionRepo.save(prescription);
    }

    // Get user prescriptions
    public List<Prescription> getUserPrescriptions(Long userId) {
        return prescriptionRepo.findByUserId(userId);
    }
}
