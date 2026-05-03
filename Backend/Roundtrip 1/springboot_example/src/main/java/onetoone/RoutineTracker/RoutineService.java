package onetoone.RoutineTracker;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RoutineService {

    private final RoutineRepository routineRepo;
    private final RoutineCheckInRepository checkInRepo;

    public RoutineService(RoutineRepository routineRepo,
                          RoutineCheckInRepository checkInRepo) {
        this.routineRepo = routineRepo;
        this.checkInRepo = checkInRepo;
    }

    public Routine checkIn(Long routineId) {
        Routine routine = routineRepo.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Routine not found"));

        LocalDate today = LocalDate.now();

        // Prevent duplicate check-in
        if (checkInRepo.existsByRoutineIdAndCheckInDate(routineId, today)) {
            throw new RuntimeException("Already checked in today");
        }

        // Get last check-in
        var lastCheckInOpt = checkInRepo
                .findTopByRoutineIdOrderByCheckInDateDesc(routineId);

        if (lastCheckInOpt.isPresent()) {
            LocalDate lastDate = lastCheckInOpt.get().getCheckInDate();

            if (lastDate.equals(today.minusDays(1))) {
                // Continue streak
                routine.setStreakCount(routine.getStreakCount() + 1);
            } else {
                // Missed a day → reset streak
                routine.setStreakCount(1);
            }
        } else {
            // First check-in
            routine.setStreakCount(1);
        }

        // Mark completed after 60 days
        if (routine.getStreakCount() >= 60) {
            routine.setCompleted(true);
        }

        // Save check-in
        RoutineCheckIn checkIn = new RoutineCheckIn();
        checkIn.setRoutine(routine);
        checkIn.setCheckInDate(today);

        checkInRepo.save(checkIn);
        return routineRepo.save(routine);
    }

    public List<RoutineCheckIn> getCheckInsForUser(Long userId) {
        return checkInRepo.findByRoutineUserId(userId);
    }
}