package ma.linkedout.linkedout.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Candidate extends User {
    private String address;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    private String cvPath;
    
    private String profilePicturePath;
    
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL)
    private List<Application> applications;
    
    @ElementCollection
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skills")
    @JsonProperty("skills")
    private List<String> skills = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "candidate_education", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "education")
    @JsonProperty("education")
    private List<String> education = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "candidate_experience", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "experience")
    @JsonProperty("experience")
    private List<String> experience = new ArrayList<>();
} 