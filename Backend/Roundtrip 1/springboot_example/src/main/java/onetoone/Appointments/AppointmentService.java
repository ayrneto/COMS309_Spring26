package onetoone.Appointments;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import onetoone.Appointments.dto.AppointmentRequest;
import onetoone.Appointments.dto.AppointmentResponse;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.realtime_chat.ChatServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppointmentResponse bookAppointment(AppointmentRequest req) {
        User user = userRepository.findById(req.userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userId));

        User counsellor = userRepository.findById(req.counsellorId)
                .orElseThrow(() -> new IllegalArgumentException("Counsellor not found: " + req.counsellorId));

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setCounsellor(counsellor);
        appointment.setDate(req.date);
        appointment.setTimeSlot(req.timeSlot);
        appointment.setNotes(req.notes);
        appointment.setStatus("PENDING");

        Appointment saved = appointmentRepository.save(appointment);

        // Notify counsellor via WebSocket if they are online
        notifyCounsellor(req);

        return toResponse(saved);
    }

    private void notifyCounsellor(AppointmentRequest req) {
        try {
            Session counsellorSession = ChatServer.userSessionMap.get(req.counsellorId);
            if (counsellorSession != null && counsellorSession.isOpen()) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "NEW_APPOINTMENT");
                notification.put("userId", req.userId);
                notification.put("date", req.date);
                notification.put("timeSlot", req.timeSlot);
                notification.put("notes", req.notes);

                String json = objectMapper.writeValueAsString(notification);
                counsellorSession.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            // Don't fail the booking if notification fails
            System.err.println("[AppointmentService] WS notify failed: " + e.getMessage());
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        r.id = a.getId();
        r.userId = a.getUser().getId();
        r.counsellorId = a.getCounsellor().getId();
        r.date = a.getDate();
        r.timeSlot = a.getTimeSlot();
        r.notes = a.getNotes();
        r.status = a.getStatus();
        return r;
    }
}