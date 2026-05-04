package onetoone.Appointments;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUser_Id(Long userId);
    List<Appointment> findByCounsellor_Id(Long counsellorId);
    List<Appointment> findByCounsellor_IdAndStatus(Long counsellorId, String status);
    List<Appointment> findByUser_IdAndStatus(Long userId, String status);

    void deleteByUser_Id(Long userId);

    @Query("SELECT DISTINCT a.counsellor FROM Appointment a WHERE a.user.id = :userId AND a.status = 'CONFIRMED'")
    List<User> findConfirmedCounsellorsByUserId(@Param("userId") Long userId);
}