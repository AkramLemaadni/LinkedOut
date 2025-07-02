package ma.linkedout.linkedout.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirements;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private String salary;
    
    @Column(nullable = false)
    @JsonProperty("type")
    private String contractType;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    @JsonIgnore
    private Recruiter recruiter;
    
    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Application> applications;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt;
    
    private boolean active = true;
    
    @Enumerated(EnumType.STRING)
    private JobOfferStatus status = JobOfferStatus.PENDING;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 