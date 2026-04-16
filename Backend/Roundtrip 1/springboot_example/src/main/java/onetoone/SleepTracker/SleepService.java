package onetoone.SleepTracker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SleepService {

    private final SleepRepository sleepRepository;

    public SleepService(SleepRepository sleepRepository) {
        this.sleepRepository = sleepRepository;
    }

    @Transactional
    public SleepRecord logSleep(Long userId, LocalDate date, double hours) {
        return sleepRepository.findByUserIdAndDate(userId, date)
                .map(existing -> {
                    existing.setHours(hours);
                    return sleepRepository.save(existing);
                })
                .orElseGet(() -> sleepRepository.save(
                        new SleepRecord(userId, date, hours)
                ));
    }

    // Get all sleep records sorted (for plotting curve)
    public List<SleepRecord> getSleepCurve(Long userId) {
        return sleepRepository.findByUserIdOrderByDateAsc(userId);
    }
}