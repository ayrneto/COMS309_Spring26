package onetoone.Users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import onetoone.Counsellors.CounsellorProfile;
import io.swagger.v3.oas.annotations.media.Schema;



@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Schema(description = "User entity representing a registered user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the user", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Schema(description = "Full name of the user", example = "John Doe")
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    @Schema(description = "Email address of the user", example = "john@example.com")
    private String email;

    @NotBlank
    @Column(nullable = false)
    @Schema(description = "Password for authentication", example = "password123", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Column(name = "confirm_password")
    @Schema(description = "Confirmation of password during registration", example = "password123", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String confirmPassword;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Role of the user in the system", example = "USER", allowableValues = {"USER","ADMIN","COUNSELLOR"})
    private Role role;

    @Schema(description = "Indicates if the user is currently logged in", example = "false")
    private boolean isLoggedIn = false;

    @Schema(description = "Indicates if the user account is active", example = "true")
    private boolean active = true;

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "Associated counsellor profile if the user is a counsellor")
    private CounsellorProfile counsellorProfile;

    // Constructors
    public User() {}

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.active = true;
    }

    public void setCounsellorProfile(CounsellorProfile counsellorProfile) {
        this.counsellorProfile = counsellorProfile;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}