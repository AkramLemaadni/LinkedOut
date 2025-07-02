package ma.linkedout.linkedout.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.dto.AuthResponse;
import ma.linkedout.linkedout.dto.LoginRequest;
import ma.linkedout.linkedout.dto.RegisterRequest;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.security.JwtService;
import ma.linkedout.linkedout.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());

        User registeredUser = userService.registerUser(user);
        String token = jwtService.generateToken(registeredUser);
        return ResponseEntity.ok(AuthResponse.success("User registered successfully", registeredUser, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.success("Login successful", user, token));
    }

    @GetMapping("/current-user")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(AuthResponse.success("Current user retrieved successfully", currentUser, null));
    }
} 