package ma.linkedout.linkedout.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.linkedout.linkedout.entities.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String message;
    private User user;
    private String token;

    public static AuthResponse success(String message, User user, String token) {
        return new AuthResponse(true, message, user, token);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, message, null, null);
    }
} 