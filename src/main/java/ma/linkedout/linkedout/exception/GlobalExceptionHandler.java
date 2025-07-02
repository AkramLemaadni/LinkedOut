package ma.linkedout.linkedout.exception;

import ma.linkedout.linkedout.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(AuthResponse.error(errors.toString()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.toLowerCase().contains("deactivated")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error("Votre compte a été désactivé par l'administrateur."));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error("Invalid email or password"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AuthResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(AuthResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("An unexpected error occurred"));
    }
} 