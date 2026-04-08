package onetoone.Users;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Response for user creation/signup")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "example@domain.com")
    private String email;

    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
}
