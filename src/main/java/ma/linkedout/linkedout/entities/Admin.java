package ma.linkedout.linkedout.entities;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends User {
    private LocalDateTime lastLogin;
    // Placeholder for future admin-specific fields
} 