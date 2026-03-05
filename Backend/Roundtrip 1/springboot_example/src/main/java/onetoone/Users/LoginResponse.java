package onetoone.Users;

public class LoginResponse {
    private Long id;
    private String email;
    private String role;  // ← add

    public LoginResponse(Long id, String email, String role) {  // ← add role
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }  // ← add
}