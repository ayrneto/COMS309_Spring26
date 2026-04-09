package onetoone.Appointments.dto;

public class AppointmentResponse {
    public Long id;
    public Long userId;
    public Long counsellorId;
    public String date;
    public String timeSlot;
    public String notes;
    public String status;
    public String userName;       // name of the user who booked
    public String counsellorName;
}