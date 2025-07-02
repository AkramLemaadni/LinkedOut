package ma.linkedout.linkedout.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Recruiter extends User {
    @ManyToOne
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;
    
    @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL)
    private List<JobOffer> jobOffers;
    
    private String position;
} 