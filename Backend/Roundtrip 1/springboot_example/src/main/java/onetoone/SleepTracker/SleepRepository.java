package onetoone.SleepTracker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepRepository extends JpaRepository<SleepRecord, Long> {

    List<SleepRecord> findByUserIdOrderByDateAsc(Long userId);

    Optional<SleepRecord> findByUserIdAndDate(Long userId, LocalDate date);
}