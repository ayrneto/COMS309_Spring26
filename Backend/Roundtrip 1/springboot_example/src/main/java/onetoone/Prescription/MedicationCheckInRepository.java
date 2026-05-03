package onetoone.Prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationCheckInRepository extends JpaRepository<MedicationCheckIn, Long> {

    List<MedicationCheckIn> findByPrescriptionUserId(Long userId);
}