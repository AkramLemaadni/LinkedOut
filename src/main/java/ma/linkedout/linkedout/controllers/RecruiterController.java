package ma.linkedout.linkedout.controllers;

import jakarta.validation.Valid;
import ma.linkedout.linkedout.entities.Company;
import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.Application;
import ma.linkedout.linkedout.entities.ApplicationStatus;
import ma.linkedout.linkedout.services.CompanyService;
import ma.linkedout.linkedout.services.JobOfferService;
import ma.linkedout.linkedout.services.UserService;
import ma.linkedout.linkedout.services.ApplicationService;
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
@RequestMapping("/api/recruiter")
public class RecruiterController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruiterController.class);
    private final UserService userService;
    private final JobOfferService jobOfferService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    
    @Autowired
    public RecruiterController(UserService userService, JobOfferService jobOfferService, CompanyService companyService, ApplicationService applicationService) {
        this.userService = userService;
        this.jobOfferService = jobOfferService;
        this.companyService = companyService;
        this.applicationService = applicationService;
    }
    
    @GetMapping("/job-offers")
    public ResponseEntity<Map<String, Object>> getRecruiterJobOffers() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Recruiter recruiter = (Recruiter) currentUser;
        List<JobOffer> jobOffers = jobOfferService.getJobOffersByRecruiter(recruiter.getId());
        
        // Map to DTOs and add candidatesCount
        List<ma.linkedout.linkedout.dtos.JobOfferResponseDto> jobOfferDtos = jobOffers.stream().map(offer -> {
            ma.linkedout.linkedout.dtos.JobOfferResponseDto dto = new ma.linkedout.linkedout.dtos.JobOfferResponseDto();
            dto.setId(offer.getId());
            dto.setTitle(offer.getTitle());
            dto.setDescription(offer.getDescription());
            dto.setRequirements(offer.getRequirements());
            dto.setLocation(offer.getLocation());
            dto.setSalary(offer.getSalary());
            dto.setContractType(offer.getContractType());
            dto.setCreatedAt(offer.getCreatedAt());
            dto.setExpiresAt(offer.getExpiresAt());
            dto.setActive(offer.isActive());
            dto.setStatus(offer.getStatus() != null ? offer.getStatus().name() : null);
            if (offer.getCompany() != null) {
                ma.linkedout.linkedout.dtos.JobOfferResponseDto.CompanyDto companyDto = new ma.linkedout.linkedout.dtos.JobOfferResponseDto.CompanyDto();
                companyDto.setId(offer.getCompany().getId());
                companyDto.setName(offer.getCompany().getName());
                dto.setCompany(companyDto);
            }
            dto.setCandidatesCount((int) applicationService.countByJobOfferId(offer.getId()));
            return dto;
        }).toList();

        long totalCandidatesCount = applicationService.countByRecruiterId(recruiter.getId());

        response.put("success", true);
        response.put("data", jobOfferDtos);
        response.put("totalCandidatesCount", totalCandidatesCount);
        response.put("message", "Job offers retrieved successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/job-offers")
    public ResponseEntity<Map<String, Object>> createJobOffer(@Valid @RequestBody JobOffer jobOffer) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Recruiter recruiter = (Recruiter) currentUser;
        
        // Vérifier si le recruteur a une entreprise
        if (recruiter.getCompany() == null) {
            response.put("success", false);
            response.put("message", "You need to be associated with a company to post job offers");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Définir le recruteur et l'entreprise pour cette offre
        jobOffer.setRecruiter(recruiter);
        jobOffer.setCompany(recruiter.getCompany());
        
        JobOffer savedJobOffer = jobOfferService.saveJobOffer(jobOffer);
        
        response.put("success", true);
        response.put("data", savedJobOffer);
        response.put("message", "Job offer created successfully");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/job-offers/{id}")
    public ResponseEntity<Map<String, Object>> getJobOfferById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Recruiter recruiter = (Recruiter) currentUser;
        Optional<JobOffer> jobOfferOpt = jobOfferService.getJobOfferById(id);
        
        if (jobOfferOpt.isPresent()) {
            JobOffer jobOffer = jobOfferOpt.get();
            
            // Vérifier si l'offre d'emploi appartient au recruteur actuel
            if (!jobOffer.getRecruiter().getId().equals(recruiter.getId())) {
                response.put("success", false);
                response.put("message", "You are not authorized to access this job offer");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            response.put("success", true);
            response.put("data", jobOffer);
            response.put("message", "Job offer retrieved successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Job offer not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/job-offers/{id}")
    public ResponseEntity<Map<String, Object>> updateJobOffer(@PathVariable Long id, @Valid @RequestBody JobOffer jobOfferDetails) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Recruiter recruiter = (Recruiter) currentUser;
        Optional<JobOffer> jobOfferOpt = jobOfferService.getJobOfferById(id);
        
        if (jobOfferOpt.isPresent()) {
            JobOffer jobOffer = jobOfferOpt.get();
            
            // Vérifier si l'offre d'emploi appartient au recruteur actuel
            if (!jobOffer.getRecruiter().getId().equals(recruiter.getId())) {
                response.put("success", false);
                response.put("message", "You are not authorized to update this job offer");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Mettre à jour les détails de l'offre
            jobOffer.setTitle(jobOfferDetails.getTitle());
            jobOffer.setDescription(jobOfferDetails.getDescription());
            jobOffer.setLocation(jobOfferDetails.getLocation());
            jobOffer.setContractType(jobOfferDetails.getContractType());
            jobOffer.setSalary(jobOfferDetails.getSalary());
            jobOffer.setRequirements(jobOfferDetails.getRequirements());
            jobOffer.setActive(jobOfferDetails.isActive());
            
            JobOffer updatedJobOffer = jobOfferService.saveJobOffer(jobOffer);
            
            response.put("success", true);
            response.put("data", updatedJobOffer);
            response.put("message", "Job offer updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Job offer not found");
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/job-offers/{id}")
    public ResponseEntity<Map<String, Object>> deleteJobOffer(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Recruiter recruiter = (Recruiter) currentUser;
        Optional<JobOffer> jobOfferOpt = jobOfferService.getJobOfferById(id);
        
        if (jobOfferOpt.isPresent()) {
            JobOffer jobOffer = jobOfferOpt.get();
            
            // Vérifier si l'offre d'emploi appartient au recruteur actuel
            if (!jobOffer.getRecruiter().getId().equals(recruiter.getId())) {
                response.put("success", false);
                response.put("message", "You are not authorized to delete this job offer");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            jobOfferService.deleteJobOffer(id);
            
            response.put("success", true);
            response.put("message", "Job offer deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Job offer not found");
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam("status") String status) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();

        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Optional<Application> appOpt = applicationService.getApplicationById(applicationId);
        if (appOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
        Application app = appOpt.get();

        // Vérifie que l'offre appartient bien au recruteur courant
        if (!app.getJobOffer().getRecruiter().getId().equals(((Recruiter) currentUser).getId())) {
            response.put("success", false);
            response.put("message", "You are not authorized to update this application");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Change le statut
        try {
            app.setStatus(ApplicationStatus.valueOf(status));
            applicationService.saveApplication(app);
            response.put("success", true);
            response.put("message", "Status updated");
            response.put("data", app);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid status value");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/job-offers/{jobOfferId}/applications")
    public ResponseEntity<Map<String, Object>> getApplicationsForJobOffer(@PathVariable Long jobOfferId) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();

        if (!(currentUser instanceof Recruiter)) {
            response.put("success", false);
            response.put("message", "You are not authorized to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Optional<JobOffer> jobOfferOpt = jobOfferService.getJobOfferById(jobOfferId);
        if (jobOfferOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Job offer not found");
            return ResponseEntity.notFound().build();
        }
        JobOffer jobOffer = jobOfferOpt.get();
        // Vérifie que l'offre appartient bien au recruteur courant
        if (!jobOffer.getRecruiter().getId().equals(((Recruiter) currentUser).getId())) {
            response.put("success", false);
            response.put("message", "You are not authorized to view applications for this offer");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        List<Application> applications = applicationService.getApplicationsByJobOffer(jobOfferId);
        // Map to DTOs
        List<Map<String, Object>> appDtos = applications.stream().map(app -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", app.getId());
            dto.put("status", app.getStatus());
            dto.put("jobOffer", app.getJobOffer());
            if (app.getCandidate() != null) {
                Map<String, Object> candidateDto = new HashMap<>();
                candidateDto.put("firstName", app.getCandidate().getFirstName());
                candidateDto.put("lastName", app.getCandidate().getLastName());
                candidateDto.put("email", app.getCandidate().getEmail());
                candidateDto.put("cvPath", app.getCandidate().getCvPath());
                dto.put("candidate", candidateDto);
            } else {
                dto.put("candidate", null);
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        response.put("success", true);
        response.put("data", appDtos);
        response.put("message", "Applications retrieved successfully");
        return ResponseEntity.ok(response);
    }
} 