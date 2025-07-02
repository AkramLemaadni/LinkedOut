package ma.linkedout.linkedout.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.dto.CandidateProfileRequest;
import ma.linkedout.linkedout.dto.RecruiterProfileRequest;
import ma.linkedout.linkedout.dto.RecruiterProfileUpdateRequest;
import ma.linkedout.linkedout.entities.Candidate;
import ma.linkedout.linkedout.entities.Company;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.UserRole;
import ma.linkedout.linkedout.services.CompanyService;
import ma.linkedout.linkedout.services.FileStorageService;
import ma.linkedout.linkedout.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final CompanyService companyService;
    private final DataSource dataSource;
    
    @Autowired
    public ProfileController(UserService userService, FileStorageService fileStorageService, 
                            CompanyService companyService, DataSource dataSource) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.companyService = companyService;
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();
        
        // Log for debugging
        logger.info("Getting profile for user: {}, role: {}, class: {}", 
                currentUser.getEmail(), currentUser.getRole(), currentUser.getClass().getName());
        
        if (currentUser.getRole() == UserRole.CANDIDATE) {
            if (currentUser instanceof Candidate) {
                Candidate candidate = (Candidate) currentUser;
                
                // Log collections for debugging
                logger.info("Candidate skills: {}", candidate.getSkills());
                logger.info("Candidate education: {}", candidate.getEducation());
                logger.info("Candidate experience: {}", candidate.getExperience());
                
                response.put("userType", "candidate");
                response.put("user", candidate);
            } else {
                // User has CANDIDATE role but isn't a Candidate instance
                // Fallback to treating as basic user with candidate role
                response.put("userType", "candidate");
                response.put("user", currentUser);
            }
        } else if (currentUser.getRole() == UserRole.RECRUITER) {
            if (currentUser instanceof Recruiter) {
                Recruiter recruiter = (Recruiter) currentUser;
                response.put("userType", "recruiter");
                response.put("user", recruiter);
                if (recruiter.getCompany() != null) {
                    response.put("company", recruiter.getCompany());
                }
            } else {
                // User has RECRUITER role but isn't a Recruiter instance
                // Fallback to treating as basic user with recruiter role
                response.put("userType", "recruiter");
                response.put("user", currentUser);
            }
        } else {
            // User has a different role (e.g., ADMIN)
            response.put("userType", "user");
            response.put("user", currentUser);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/candidate")
    public ResponseEntity<Map<String, Object>> updateCandidateProfile(
            @Valid @RequestBody CandidateProfileRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.CANDIDATE) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "You are not a candidate"
            ));
        }
        try {
            // Ensure the user is a Candidate instance
            Candidate candidate;
            if (currentUser instanceof Candidate) {
                candidate = (Candidate) currentUser;
            } else {
                candidate = userService.createCandidateFromUser(currentUser);
                logger.info("Created new candidate entity for user: {}", currentUser.getEmail());
            }

            // Preserve file paths
            String oldProfilePicturePath = candidate.getProfilePicturePath();
            String oldCvPath = candidate.getCvPath();

            // Update candidate data
            candidate.setFirstName(request.getFirstName());
            candidate.setLastName(request.getLastName());
            candidate.setPhone(request.getPhone());
            candidate.setAddress(request.getAddress());
            candidate.setBio(request.getBio());

            // Only update file paths if present in the request (should not be, so always preserve)
            candidate.setProfilePicturePath(oldProfilePicturePath);
            candidate.setCvPath(oldCvPath);

            // Clear and set collections to avoid duplicates
            if (candidate.getSkills() == null) {
                candidate.setSkills(request.getSkills());
            } else {
                candidate.getSkills().clear();
                if (request.getSkills() != null) {
                    candidate.getSkills().addAll(request.getSkills());
                }
            }
            if (candidate.getEducation() == null) {
                candidate.setEducation(request.getEducation());
            } else {
                candidate.getEducation().clear();
                if (request.getEducation() != null) {
                    candidate.getEducation().addAll(request.getEducation());
                }
            }
            if (candidate.getExperience() == null) {
                candidate.setExperience(request.getExperience());
            } else {
                candidate.getExperience().clear();
                if (request.getExperience() != null) {
                    candidate.getExperience().addAll(request.getExperience());
                }
            }
            Candidate updatedCandidate = userService.updateCandidate(candidate);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile updated successfully",
                "user", updatedCandidate
            ));
        } catch (Exception e) {
            logger.error("Error updating candidate profile", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update profile: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/recruiter")
    public ResponseEntity<Map<String, Object>> updateRecruiterProfile(
            @Valid @ModelAttribute RecruiterProfileUpdateRequest request) {
        
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.RECRUITER) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "You are not a recruiter"
            ));
        }
        
        try {
            // Ensure the user is a Recruiter instance
            Recruiter recruiter;
            if (currentUser instanceof Recruiter) {
                recruiter = (Recruiter) currentUser;
            } else {
                // Create a new Recruiter entity from the existing User
                recruiter = userService.createRecruiterFromUser(currentUser);
                logger.info("Created new recruiter entity for user: {}", currentUser.getEmail());
            }
            
            // Update recruiter data from the request
            recruiter.setFirstName(request.getFirstName());
            recruiter.setLastName(request.getLastName());
            recruiter.setPhone(request.getPhone());
            recruiter.setPosition(request.getPosition());
            
            // Handle profile picture upload
            if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
                User currentUserForFile = userService.getCurrentUser();
                // Delete old profile picture if exists
                if (recruiter.getProfilePicturePath() != null) {
                    fileStorageService.deleteFile(recruiter.getProfilePicturePath());
                }
                // Store new profile picture in the 'profile' subdirectory
                String filePath = fileStorageService.storeFile(request.getProfilePicture(), "profile");
                recruiter.setProfilePicturePath(filePath);
                logger.info("Stored new profile picture at path: {}", filePath);
            }

            // Save the updated recruiter entity
            Recruiter updatedRecruiter = userService.updateRecruiter(recruiter);
            logger.info("Updated recruiter profile with ID: {}", updatedRecruiter.getId());
            
            // Simplified company handling: if companyId is provided, link the recruiter to that company.
            if (request.getCompanyId() != null) {
                Company companyToLink = companyService.getCompanyById(request.getCompanyId());
                if (companyToLink != null) {
                    recruiter.setCompany(companyToLink);
                } else {
                    // Handle case where provided companyId does not exist
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Company with provided ID not found."
                    ));
                }
            } else {
                 // If no companyId is provided in the request, we don't change the existing company association.
                 // If you intended to allow unlinking, additional logic would be needed here.
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            // Fetch the updated recruiter with potentially new company info for the response
            User reFetchedUser = userService.getCurrentUser(); // Re-fetch to ensure company is loaded
            if (reFetchedUser instanceof Recruiter) {
                 response.put("user", reFetchedUser);
                 if (((Recruiter) reFetchedUser).getCompany() != null) {
                      response.put("company", ((Recruiter) reFetchedUser).getCompany());
                 }
            } else {
                 // Fallback if re-fetching somehow doesn't yield a Recruiter
                 response.put("user", recruiter); // Use the directly updated object
                 if (recruiter.getCompany() != null) {
                      response.put("company", recruiter.getCompany());
                 }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating recruiter profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to update profile: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/candidate/cv")
    public ResponseEntity<Map<String, Object>> uploadCv(@RequestParam("file") MultipartFile file) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.CANDIDATE) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "You are not a candidate"
            ));
        }
        
        try {
            // Ensure the user is a Candidate instance
            Candidate candidate;
            if (currentUser instanceof Candidate) {
                candidate = (Candidate) currentUser;
            } else {
                // Create a new Candidate entity from the existing User
                candidate = userService.createCandidateFromUser(currentUser);
                logger.info("Created new candidate entity for user: {}", currentUser.getEmail());
            }
            
            // Delete old CV if exists
            if (candidate.getCvPath() != null) {
                fileStorageService.deleteFile(candidate.getCvPath());
            }
            
            // Store new CV
            String cvPath = fileStorageService.storeFile(file, "cv");
            candidate.setCvPath(cvPath);
            userService.updateCandidate(candidate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "CV uploaded successfully",
                "cvPath", cvPath
            ));
        } catch (IOException e) {
            logger.error("Failed to upload CV", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to upload CV: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/candidate/profile-picture")
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.CANDIDATE) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "You are not a candidate"
            ));
        }
        
        try {
            // Ensure the user is a Candidate instance
            Candidate candidate;
            if (currentUser instanceof Candidate) {
                candidate = (Candidate) currentUser;
            } else {
                // Create a new Candidate entity from the existing User
                candidate = userService.createCandidateFromUser(currentUser);
                logger.info("Created new candidate entity for user: {}", currentUser.getEmail());
            }
            
            // Delete old profile picture if exists
            if (candidate.getProfilePicturePath() != null) {
                fileStorageService.deleteFile(candidate.getProfilePicturePath());
            }
            
            // Store new profile picture
            String profilePicturePath = fileStorageService.storeFile(file, "profile");
            candidate.setProfilePicturePath(profilePicturePath);
            userService.updateCandidate(candidate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile picture uploaded successfully",
                "profilePicturePath", profilePicturePath
            ));
        } catch (IOException e) {
            logger.error("Failed to upload profile picture", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to upload profile picture: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/company/logo")
    public ResponseEntity<Map<String, Object>> uploadCompanyLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") Long companyId) {
        
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.RECRUITER) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "You are not a recruiter"
            ));
        }
        
        try {
            // Ensure the user is a Recruiter instance
            Recruiter recruiter;
            if (currentUser instanceof Recruiter) {
                recruiter = (Recruiter) currentUser;
            } else {
                // Create a new Recruiter entity from the existing User
                recruiter = userService.createRecruiterFromUser(currentUser);
                logger.info("Created new recruiter entity for user: {}", currentUser.getEmail());
            }
            
            Company company = companyService.getCompanyById(companyId);
            
            if (company == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Company not found"
                ));
            }
            
            // Check if the recruiter belongs to the company
            if (recruiter.getCompany() == null || !recruiter.getCompany().getId().equals(companyId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You don't have permission to update this company"
                ));
            }
            
            // Delete old logo if exists
            if (company.getLogoPath() != null) {
                fileStorageService.deleteFile(company.getLogoPath());
            }
            
            // Store new logo
            String logoPath = fileStorageService.storeFile(file, "company");
            company.setLogoPath(logoPath);
            companyService.saveCompany(company);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Company logo uploaded successfully",
                "logoPath", logoPath
            ));
        } catch (IOException e) {
            logger.error("Failed to upload company logo", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to upload company logo: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/candidate/skill")
    public ResponseEntity<?> deleteSkill(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName());
            if (!(user instanceof Candidate)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Only candidates can delete skills"));
            }
            
            Candidate candidate = (Candidate) user;
            String skill = request.get("skill");
            
            if (skill == null || skill.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Skill cannot be empty"));
            }
            
            // Use JDBC to delete directly from the junction table
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            int deleted = jdbcTemplate.update(
                "DELETE FROM candidate_skills WHERE candidate_id = ? AND skills = ?",
                candidate.getId(), skill.trim()
            );
            
            logger.info("Deleted {} skill entry for candidate: {}", deleted, candidate.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Skill deleted successfully",
                "user", userService.findByEmail(authentication.getName())
            ));
        } catch (Exception e) {
            logger.error("Error deleting skill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete skill: " + e.getMessage()));
        }
    }

    @DeleteMapping("/candidate/education")
    public ResponseEntity<?> deleteEducation(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName());
            if (!(user instanceof Candidate)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Only candidates can delete education"));
            }
            
            Candidate candidate = (Candidate) user;
            String education = request.get("education");
            
            if (education == null || education.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Education cannot be empty"));
            }
            
            // Use JDBC to delete directly from the junction table
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            int deleted = jdbcTemplate.update(
                "DELETE FROM candidate_education WHERE candidate_id = ? AND education = ?",
                candidate.getId(), education.trim()
            );
            
            logger.info("Deleted {} education entry for candidate: {}", deleted, candidate.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Education deleted successfully",
                "user", userService.findByEmail(authentication.getName())
            ));
        } catch (Exception e) {
            logger.error("Error deleting education: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete education: " + e.getMessage()));
        }
    }

    @DeleteMapping("/candidate/experience")
    public ResponseEntity<?> deleteExperience(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName());
            if (!(user instanceof Candidate)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Only candidates can delete experience"));
            }
            
            Candidate candidate = (Candidate) user;
            String experience = request.get("experience");
            
            if (experience == null || experience.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Experience cannot be empty"));
            }
            
            // Use JDBC to delete directly from the junction table
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            int deleted = jdbcTemplate.update(
                "DELETE FROM candidate_experience WHERE candidate_id = ? AND experience = ?",
                candidate.getId(), experience.trim()
            );
            
            logger.info("Deleted {} experience entry for candidate: {}", deleted, candidate.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Experience deleted successfully",
                "user", userService.findByEmail(authentication.getName())
            ));
        } catch (Exception e) {
            logger.error("Error deleting experience: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete experience: " + e.getMessage()));
        }
    }

    @DeleteMapping("/company/logo/{companyId}")
    public ResponseEntity<Map<String, Object>> deleteCompanyLogo(@PathVariable Long companyId) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();

        if (!(currentUser instanceof Recruiter)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "You are not authorized to access this resource"
            ));
        }

        Recruiter recruiter = (Recruiter) currentUser;

        try {
            Company company = companyService.getCompanyById(companyId);

            if (company == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Company not found"
                ));
            }

            // Check if the recruiter belongs to the company
            if (recruiter.getCompany() == null || !recruiter.getCompany().getId().equals(companyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", "You don't have permission to delete this company's logo"
                ));
            }

            // Delete the logo file if it exists
            if (company.getLogoPath() != null) {
                fileStorageService.deleteFile(company.getLogoPath());
                company.setLogoPath(null); // Set logoPath to null in the entity
                companyService.saveCompany(company); // Save the updated company entity
                logger.info("Deleted logo for company ID: {}", companyId);
            }

            response.put("success", true);
            response.put("message", "Company logo deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting company logo for company ID: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to delete company logo: " + e.getMessage()
            ));
        }
    }
} 