package ma.linkedout.linkedout.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    @JsonIgnore
    private Candidate candidate;
    
    @ManyToOne
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;
    
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    
    private String cvPath;
    
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    private LocalDateTime appliedAt;
    
    private LocalDateTime lastUpdatedAt;
    
    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
} 