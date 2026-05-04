package onetoone.Users;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for user login")
public class LoginResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "example@domain.com")
    private String email;

    @Schema(description = "User role", example = "USER")
    private String role;

    @Schema
    private String name; // ← add

    public LoginResponse(Long id, String email, String role, String name) {  // ← add role
        this.id = id;
        this.email = email;
        this.role = role;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getName(){
        return name;
    }// ← add
}