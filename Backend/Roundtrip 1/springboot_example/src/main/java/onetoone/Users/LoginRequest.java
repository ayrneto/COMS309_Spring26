package onetoone.Users;

import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Request body for user login")
public class LoginRequest {

    @Email
    @NotBlank
    @Schema(description = "User email", example = "example@domain.com")
    private String email;

    @NotBlank
    @Schema(description = "User password", example = "mypassword123")
    private String password;

    @NotBlank
    @Schema(description = "User role", example = "USER")
    private Role role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}