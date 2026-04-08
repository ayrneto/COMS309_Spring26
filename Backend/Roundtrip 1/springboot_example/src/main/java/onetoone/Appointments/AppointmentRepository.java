package onetoone.Appointments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUser_Id(Long userId);
    List<Appointment> findByCounsellor_Id(Long counsellorId);
}