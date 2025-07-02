package ma.linkedout.linkedout.controllers;

import jakarta.validation.Valid;
import ma.linkedout.linkedout.entities.Application;
import ma.linkedout.linkedout.entities.ApplicationStatus;
import ma.linkedout.linkedout.entities.Candidate;
import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.services.ApplicationService;
import ma.linkedout.linkedout.services.JobOfferService;
import ma.linkedout.linkedout.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/candidate")
public class CandidateController {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateController.class);
    private final UserService userService;
    private final JobOfferService jobOfferService;
    private final ApplicationService applicationService;
    
    @Autowired
    public CandidateController(UserService userService, JobOfferService jobOfferService, ApplicationService applicationService) {
        this.userService = userService;
        this.jobOfferService = jobOfferService;
        this.applicationService = applicationService;
    }
    
    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> getCandidateApplications() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Candidate)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Candidate candidate = (Candidate) currentUser;
        List<Application> applications = applicationService.getApplicationsByCandidate(candidate.getId());
        
        response.put("success", true);
        response.put("data", applications);
        response.put("message", "Applications retrieved successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/apply/{jobOfferId}")
    public ResponseEntity<Map<String, Object>> applyForJob(
            @PathVariable Long jobOfferId, 
            @Valid @RequestBody Map<String, String> applicationDetails) {
        
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Candidate)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Candidate candidate = (Candidate) currentUser;
        Optional<JobOffer> jobOfferOpt = jobOfferService.getJobOfferById(jobOfferId);
        
        if (jobOfferOpt.isPresent()) {
            JobOffer jobOffer = jobOfferOpt.get();
            
            // Vérifier si l'offre d'emploi est active
            if (!jobOffer.isActive()) {
                response.put("success", false);
                response.put("message", "This job offer is no longer active");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Vérifier si le candidat a déjà postulé à cette offre
            if (applicationService.hasApplied(candidate.getId(), jobOfferId)) {
                response.put("success", false);
                response.put("message", "You have already applied for this job");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Créer une nouvelle candidature
            Application application = new Application();
            application.setCandidate(candidate);
            application.setJobOffer(jobOffer);
            application.setCoverLetter(applicationDetails.get("coverLetter"));
            application.setStatus(ApplicationStatus.PENDING);
            
            Application savedApplication = applicationService.saveApplication(application);
            
            response.put("success", true);
            response.put("data", savedApplication);
            response.put("message", "Application submitted successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("success", false);
            response.put("message", "Job offer not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> getApplicationById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Candidate)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Candidate candidate = (Candidate) currentUser;
        Optional<Application> applicationOpt = applicationService.getApplicationById(id);
        
        if (applicationOpt.isPresent()) {
            Application application = applicationOpt.get();
            
            // Vérifier si la candidature appartient au candidat actuel
            if (!application.getCandidate().getId().equals(candidate.getId())) {
                response.put("success", false);
                response.put("message", "You are not authorized to access this application");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            response.put("success", true);
            response.put("data", application);
            response.put("message", "Application retrieved successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> withdrawApplication(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Candidate)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Candidate candidate = (Candidate) currentUser;
        Optional<Application> applicationOpt = applicationService.getApplicationById(id);
        
        if (applicationOpt.isPresent()) {
            Application application = applicationOpt.get();
            
            // Vérifier si la candidature appartient au candidat actuel
            if (!application.getCandidate().getId().equals(candidate.getId())) {
                response.put("success", false);
                response.put("message", "You are not authorized to withdraw this application");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Vérifier si la candidature est déjà acceptée
            if (application.getStatus() == ApplicationStatus.ACCEPTED) {
                response.put("success", false);
                response.put("message", "You cannot withdraw an accepted application");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Mettre à jour le statut de la candidature à "WITHDRAWN"
            application.setStatus(ApplicationStatus.WITHDRAWN);
            applicationService.saveApplication(application);
            
            response.put("success", true);
            response.put("message", "Application withdrawn successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/job-offers/public")
    public ResponseEntity<Map<String, Object>> getPublicJobOffers() {
        Map<String, Object> response = new HashMap<>();
        List<JobOffer> allOffers = jobOfferService.getAllJobOffers();
        List<JobOffer> filteredOffers = allOffers.stream()
            .filter(offer -> offer.getStatus() != null && offer.getStatus().name().equals("ACCEPTED") && offer.isActive())
            .toList();
        response.put("success", true);
        response.put("data", filteredOffers);
        response.put("message", "Filtered job offers for candidates");
        return ResponseEntity.ok(response);
    }
} 