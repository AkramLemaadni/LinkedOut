package ma.linkedout.linkedout.config;

import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.entities.UserRole;
import ma.linkedout.linkedout.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // Disable CSRF for API endpoints
                .ignoringRequestMatchers("/login", "/register")  // Disable CSRF for login/register forms
            )
            .authorizeHttpRequests(auth -> auth
                // Allow access to all Thymeleaf pages and static resources
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**"
                ).permitAll()
                // API endpoints security
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole(UserRole.ADMIN.name())
                .requestMatchers("/api/recruiter/**").hasRole(UserRole.RECRUITER.name())
                .requestMatchers("/api/candidate/**").hasRole(UserRole.CANDIDATE.name())
                // Dashboard requires authentication
                .requestMatchers("/dashboard").authenticated()
                .anyRequest().authenticated()
            )
            // Form login configuration
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Ajout : gestion d'exception pour les endpoints API
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setContentType("application/json");
                        response.setStatus(401);
                        response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
} 
