package onetoone.Users;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User roles in the system", allowableValues = {"USER", "ADMIN", "COUNSELLOR"})
public enum Role {
    USER,
    ADMIN,
    COUNSELLOR;
}
