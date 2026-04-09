package onetoone.Users.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Request body for creating a counsellor user")
public class CreateCounsellorRequest {

    @Schema(description = "Full name of the counsellor", example = "John Doe")
    public String name;
    @Schema(description = "Email ID of the counsellor", example = "john.doe@example.com")
    public String emailId;
    @Schema(description = "Password for the counsellor", example = "mypassword123")
    public String password;
}