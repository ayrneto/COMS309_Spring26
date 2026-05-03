package onetoone.SleepTracker;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sleep_records")
public class SleepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private LocalDate date;

    // hours slept (e.g., 7.5)
    private double hours;

    public SleepRecord() {}

    public SleepRecord(Long userId, LocalDate date, double hours) {
        this.userId = userId;
        this.date = date;
        this.hours = hours;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public double getHours() { return hours; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setHours(double hours) { this.hours = hours; }
}