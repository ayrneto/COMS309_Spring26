package onetoone.SleepTracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sleep")
public class SleepController {

    private final SleepService sleepService;

    @Autowired
    SleepRepository sleepRepository;

    public SleepController(SleepService sleepService) {
        this.sleepService = sleepService;
    }

    // Log daily sleep
    @PostMapping("/{userId}")
    public ResponseEntity<?> logSleep(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body
    ) {
        if (!body.containsKey("hours")) {
            return ResponseEntity.badRequest().body("Missing hours");
        }

        double hours = Double.parseDouble(body.get("hours").toString());

        LocalDate date = body.containsKey("date")
                ? LocalDate.parse(body.get("date").toString())
                : LocalDate.now();

        SleepRecord record = sleepService.logSleep(userId, date, hours);
        sleepRepository.save(record);

        return ResponseEntity.ok(record);
    }

    // Get data for curve plotting
    @GetMapping("/{userId}")
    public ResponseEntity<List<SleepRecord>> getSleepCurve(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(sleepService.getSleepCurve(userId));
    }
}