package onetoone.RoutineTracker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoutineCheckInRepository extends JpaRepository<RoutineCheckIn, Long> {

    boolean existsByRoutineIdAndCheckInDate(Long routineId, LocalDate date);
    List<RoutineCheckIn> findByRoutineUserId(Long userId);
    Optional<RoutineCheckIn> findTopByRoutineIdOrderByCheckInDateDesc(Long routineId);
    void deleteByRoutineId(Long routineId);
}
