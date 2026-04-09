package onetoone.Appointments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import onetoone.Appointments.dto.AppointmentRequest;
import onetoone.Appointments.dto.AppointmentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "APIs for booking and managing appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // POST /api/appointments — book an appointment
    @Operation(summary = "Book an appointment")
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@RequestBody AppointmentRequest req) {
        AppointmentResponse response = appointmentService.bookAppointment(req);
        return ResponseEntity.status(201).body(response);
    }

    // GET /api/appointments/counsellor/{id} — all appointments for a counsellor
    @Operation(summary = "Get all appointments for a counsellor")
    @GetMapping("/counsellor/{id}")
    public ResponseEntity<List<AppointmentResponse>> getByCounsellor(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByCounsellor(id));
    }

    // GET /api/appointments/counsellor/{id}/accepted — confirmed only (for chat list)
    @Operation(summary = "Get confirmed appointments for a counsellor")
    @GetMapping("/counsellor/{id}/accepted")
    public ResponseEntity<List<AppointmentResponse>> getAcceptedByCounsellor(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAcceptedByCounsellor(id));
    }

    // GET /api/appointments/user/{id}/accepted — confirmed only (for chat list)
    @Operation(summary = "Get confirmed appointments for a user")
    @GetMapping("/user/{id}/accepted")
    public ResponseEntity<List<AppointmentResponse>> getAcceptedByUser(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAcceptedByUser(id));
    }

    // PATCH /api/appointments/{id}/accept
    @Operation(summary = "Accept an appointment")
    @PatchMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(appointmentService.updateStatus(id, "CONFIRMED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    // PATCH /api/appointments/{id}/decline
    @Operation(summary = "Decline an appointment")
    @PatchMapping("/{id}/decline")
    public ResponseEntity<?> decline(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(appointmentService.updateStatus(id, "CANCELLED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}