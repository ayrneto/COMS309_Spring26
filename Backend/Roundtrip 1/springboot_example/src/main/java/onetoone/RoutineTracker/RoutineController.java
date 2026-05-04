package onetoone.RoutineTracker;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routines")
public class RoutineController {

    @Autowired
    UserRepository userRepository;

    private final RoutineService routineService;
    private final RoutineRepository routineRepo;

    public RoutineController(RoutineService routineService,
                             RoutineRepository routineRepo) {
        this.routineService = routineService;
        this.routineRepo = routineRepo;
    }

    @PostMapping("/users/{userId}/routines")
    public Routine createRoutine(
            @PathVariable Long userId,
            @RequestBody Routine routine) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        routine.setUser(user);

        return routineRepo.save(routine);
    }

    // Daily Check-In
    @PostMapping("/{id}/checkin")
    public Routine checkIn(@PathVariable Long id) {
        return routineService.checkIn(id);
    }

    @GetMapping("/users/{userId}/checkins")
    public List<RoutineCheckIn> getUserCheckIns(@PathVariable Long userId) {
        return routineService.getCheckInsForUser(userId);
    }

    // Get all routines
    @GetMapping
    public List<Routine> getAll() {
        return routineRepo.findAll();
    }

    @GetMapping("/users/{userId}/routines")
    public List<Routine> getUserRoutines(@PathVariable Long userId) {
        System.out.println("DEBUG: Hit the getUserRoutines method for ID: " + userId);
        return routineRepo.findByUserId(userId);
    }
    @DeleteMapping("/{id}")
    public String deleteRoutine(@PathVariable Long id) {
        if (!routineRepo.existsById(id)) {
            throw new RuntimeException("Routine not found");
        }

        routineRepo.deleteById(id);
        return "Routine deleted successfully";
    }
}
