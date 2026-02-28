package onetoone.Users;

public record LoginResponse(
        Long id,
        String email,
        Role role
) {}