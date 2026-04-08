package onetoone.Appointments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import onetoone.Appointments.dto.AppointmentRequest;
import onetoone.Appointments.dto.AppointmentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "APIs for booking appointments with counsellors")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Operation(summary = "Book an appointment", description = "Creates a new appointment between a user and counsellor")
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@RequestBody AppointmentRequest req) {
        AppointmentResponse response = appointmentService.bookAppointment(req);
        return ResponseEntity.status(201).body(response);
    }
}