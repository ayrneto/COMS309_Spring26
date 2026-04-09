package onetoone.Users;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_checkin")
public class DailyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int rating; // 1–5
    private String description;
    private String reminderTime; // "HH:mm"
    private LocalDate date;

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public Long getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public User getUser() {
        return user;
    }
}