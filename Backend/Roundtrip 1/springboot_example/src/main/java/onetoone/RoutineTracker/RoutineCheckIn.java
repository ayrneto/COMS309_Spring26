package onetoone.RoutineTracker;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class RoutineCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate checkInDate;

    @ManyToOne
    @JoinColumn(name = "routine_id")
    private Routine routine;

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public Routine getRoutine() {
        return routine;
    }

    public void setRoutine(Routine routine) {
        this.routine = routine;
    }
}
