package ma.linkedout.linkedout.controllers;

import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.services.JobOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.services.UserService;
import ma.linkedout.linkedout.entities.Company;
import ma.linkedout.linkedout.services.CompanyService;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.HashMap;
import ma.linkedout.linkedout.dtos.JobOfferResponseDto;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/job-offers")
@CrossOrigin(origins = "*")
public class JobOfferController {

    private static final Logger logger = LoggerFactory.getLogger(JobOfferController.class);

    @Autowired
    private JobOfferService jobOfferService;

    @Autowired
    private UserService userService;

    // CompanyService might not be needed if company is directly on Recruiter
    // @Autowired
    // private CompanyService companyService;

    @PostMapping
    public ResponseEntity<?> createJobOffer(@RequestBody JobOffer jobOffer) {
        try {
            logger.info("Received job offer creation request: {}", jobOffer);

            User currentUser = userService.getCurrentUser();
            if (!(currentUser instanceof Recruiter)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only recruiters can create job offers.");
            }

            Recruiter currentRecruiter = (Recruiter) currentUser;

            // Ensure the recruiter has a company associated
            if (currentRecruiter.getCompany() == null) {
                return ResponseEntity.badRequest().body("Recruiter must be associated with a company to create job offers.");
            }

            // Set the recruiter and company on the job offer
            jobOffer.setRecruiter(currentRecruiter);
            jobOffer.setCompany(currentRecruiter.getCompany());

            JobOffer savedJobOffer = jobOfferService.saveJobOffer(jobOffer);
            logger.info("Job offer created successfully with ID: {}", savedJobOffer.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedJobOffer);

        } catch (Exception e) {
            logger.error("Error creating job offer: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating job offer: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<JobOffer>> getAllJobOffers() {
        List<JobOffer> jobOffers = jobOfferService.getAllJobOffers();
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOffer> getJobOfferById(@PathVariable Long id) {
        return jobOfferService.getJobOfferById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJobOffer(@PathVariable Long id, @RequestBody JobOffer jobOfferDetails) {
        try {
            return jobOfferService.getJobOfferById(id)
                    .map(existingJobOffer -> {
                        // Update existing job offer with details from the request body
                        existingJobOffer.setTitle(jobOfferDetails.getTitle());
                        existingJobOffer.setDescription(jobOfferDetails.getDescription());
                        existingJobOffer.setRequirements(jobOfferDetails.getRequirements());
                        existingJobOffer.setLocation(jobOfferDetails.getLocation());
                        existingJobOffer.setSalary(jobOfferDetails.getSalary());
                        existingJobOffer.setContractType(jobOfferDetails.getContractType());
                        existingJobOffer.setActive(jobOfferDetails.isActive());

                        // Save the updated job offer (company and recruiter will be retained)
                        JobOffer updatedJobOffer = jobOfferService.saveJobOffer(existingJobOffer);
                        return ResponseEntity.ok(updatedJobOffer);
                    })
                    .orElse(ResponseEntity.notFound().build()); // Return 404 if job offer not found
        } catch (Exception e) {
            logger.error("Error updating job offer with ID {}: ", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error updating job offer: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobOffer(@PathVariable Long id) {
        try {
            jobOfferService.deleteJobOffer(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting job offer: " + e.getMessage());
        }
    }

    @GetMapping("/public")
    public ResponseEntity<List<JobOfferResponseDto>> getActiveJobOffersForPublic() {
        logger.info("Fetching all active job offers for public view");
        List<JobOfferResponseDto> jobOffers = jobOfferService.getActiveJobOfferDtos();
        return ResponseEntity.ok(jobOffers);
    }
} 