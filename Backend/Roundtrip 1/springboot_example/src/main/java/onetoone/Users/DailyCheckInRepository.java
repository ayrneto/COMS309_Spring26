package onetoone.Users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    List<DailyCheckIn> findByUserId(Long userId);

    Optional<DailyCheckIn> findByUserIdAndDate(Long userId, LocalDate date);
}