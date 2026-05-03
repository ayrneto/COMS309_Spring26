package onetoone.Prescription;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.LocalDate;

@Entity
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String medicationName;
    private String dosage;
    private String instructions;

    private LocalDate startDate;
    private LocalDate endDate;

    private int durationDays;

    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "counsellor_id")
    private User counsellor;

    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isActive() {
        return active;
    }

    public User getUser() {
        return user;
    }

    public User getCounsellor() {
        return counsellor;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setCounsellor(User counsellor) {
        this.counsellor = counsellor;
    }
}
