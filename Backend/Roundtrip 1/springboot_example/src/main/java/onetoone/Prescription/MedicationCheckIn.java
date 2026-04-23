package onetoone.Prescription;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class MedicationCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateTaken;

    private boolean taken;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public LocalDate getDateTaken() {
        return dateTaken;
    }

    public boolean isTaken() {
        return taken;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setDateTaken(LocalDate dateTaken) {
        this.dateTaken = dateTaken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
}
