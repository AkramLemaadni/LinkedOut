package ma.linkedout.linkedout.services.impl;

import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.entities.Candidate;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.UserRole;
import ma.linkedout.linkedout.entities.Admin;
import ma.linkedout.linkedout.repositories.UserRepository;
import ma.linkedout.linkedout.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final DataSource dataSource;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          EntityManager entityManager, DataSource dataSource) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.dataSource = dataSource;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is deactivated");
        }
        return user;
    }

    @Override
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new RuntimeException("No authenticated user found");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        // Conversion automatique en Recruiter/Candidate/Admin si besoin
        if (user.getRole() == UserRole.RECRUITER && !(user instanceof Recruiter)) {
            user = createRecruiterFromUser(user);
        } else if (user.getRole() == UserRole.CANDIDATE && !(user instanceof Candidate)) {
            user = createCandidateFromUser(user);
        } else if (user.getRole() == UserRole.ADMIN && !(user instanceof Admin)) {
            user = createAdminFromUser(user);
        }
        return user;
    }
    
    @Override
    @Transactional
    public <T extends User> T updateUser(T user) {
        return (T) userRepository.save(user);
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Candidate updateCandidate(Candidate candidate) {
        int retryCount = 0;
        int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                // Get the current candidate from the database
                Candidate existingCandidate = (Candidate) userRepository.findById(candidate.getId())
                        .orElseThrow(() -> new RuntimeException("Candidate not found"));
                
                // Update basic fields
                existingCandidate.setFirstName(candidate.getFirstName());
                existingCandidate.setLastName(candidate.getLastName());
                existingCandidate.setPhone(candidate.getPhone());
                existingCandidate.setAddress(candidate.getAddress());
                existingCandidate.setBio(candidate.getBio());
                
                // Save the candidate first
                Candidate savedCandidate = (Candidate) userRepository.save(existingCandidate);
                
                // Use JDBC for direct access to the junction tables
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                
                // Process collections in separate transactions to avoid deadlocks
                processSkills(jdbcTemplate, candidate);
                processEducation(jdbcTemplate, candidate);
                processExperience(jdbcTemplate, candidate);
                
                // Reload the candidate to get the updated collections
                existingCandidate = (Candidate) userRepository.findById(candidate.getId()).orElse(null);
                
                // Log updated counts
                logger.info("Updated candidate: {}, skills: {}, education: {}, experience: {}", 
                    candidate.getId(),
                    existingCandidate.getSkills() != null ? existingCandidate.getSkills().size() : 0,
                    existingCandidate.getEducation() != null ? existingCandidate.getEducation().size() : 0,
                    existingCandidate.getExperience() != null ? existingCandidate.getExperience().size() : 0);
                
                return existingCandidate;
            } catch (Exception e) {
                retryCount++;
                logger.warn("Deadlock detected on attempt {} of {}. Retrying...", retryCount, maxRetries);
                
                if (retryCount >= maxRetries) {
                    logger.error("Failed to update candidate after {} attempts", maxRetries);
                    throw e;
                }
                
                // Wait before retrying
                try {
                    Thread.sleep(500 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        throw new RuntimeException("Failed to update candidate after maximum retries");
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processSkills(JdbcTemplate jdbcTemplate, Candidate candidate) {
        // Handle skills collection
        if (candidate.getSkills() != null) {
            // Remove duplicates using a Set
            Set<String> uniqueSkills = new LinkedHashSet<>();
            for (String skill : candidate.getSkills()) {
                if (skill != null && !skill.trim().isEmpty()) {
                    uniqueSkills.add(skill.trim());
                }
            }
            
            // Clear existing skills
            jdbcTemplate.update("DELETE FROM candidate_skills WHERE candidate_id = ?", candidate.getId());
            
            // Insert new skills (only unique ones)
            for (String skill : uniqueSkills) {
                jdbcTemplate.update(
                    "INSERT INTO candidate_skills (candidate_id, skills) VALUES (?, ?)",
                    candidate.getId(), skill
                );
                logger.info("Added skill: {} for candidate: {}", skill, candidate.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processEducation(JdbcTemplate jdbcTemplate, Candidate candidate) {
        // Handle education collection
        if (candidate.getEducation() != null) {
            // Remove duplicates using a Set
            Set<String> uniqueEducation = new LinkedHashSet<>();
            for (String education : candidate.getEducation()) {
                if (education != null && !education.trim().isEmpty()) {
                    uniqueEducation.add(education.trim());
                }
            }
            
            // Clear existing education
            jdbcTemplate.update("DELETE FROM candidate_education WHERE candidate_id = ?", candidate.getId());
            
            // Insert new education entries (only unique ones)
            for (String education : uniqueEducation) {
                jdbcTemplate.update(
                    "INSERT INTO candidate_education (candidate_id, education) VALUES (?, ?)",
                    candidate.getId(), education
                );
                logger.info("Added education: {} for candidate: {}", education, candidate.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processExperience(JdbcTemplate jdbcTemplate, Candidate candidate) {
        // Handle experience collection exactly like skills
        if (candidate.getExperience() != null) {
            // Simply delete all existing entries first
            jdbcTemplate.update("DELETE FROM candidate_experience WHERE candidate_id = ?", 
                candidate.getId());
            
            // Use a separate Set for de-duplication to match the skills approach
            Set<String> uniqueExperiences = new LinkedHashSet<>();
            
            // Process each experience entry
            for (String experience : candidate.getExperience()) {
                if (experience != null && !experience.trim().isEmpty()) {
                    uniqueExperiences.add(experience.trim());
                }
            }
            
            // Wait for delete to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Insert only unique entries after deletion is complete
            for (String experience : uniqueExperiences) {
                jdbcTemplate.update(
                    "INSERT INTO candidate_experience (candidate_id, experience) VALUES (?, ?)",
                    candidate.getId(), experience
                );
                logger.info("Added experience: {} for candidate: {}", experience, candidate.getId());
            }
        }
    }
    
    @Override
    @Transactional
    public Recruiter updateRecruiter(Recruiter recruiter) {
        return (Recruiter) userRepository.save(recruiter);
    }
    
    @Override
    @Transactional
    public Candidate createCandidateFromUser(User user) {
        logger.info("Converting user ID: {} to candidate entity", user.getId());
        
        // Check if already a candidate
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser instanceof Candidate) {
            logger.info("User is already a Candidate entity");
            return (Candidate) existingUser;
        }
        
        // We need to use a JPQL update directly to change the entity type in the database
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Check if candidate record exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate WHERE id = ?", 
                Integer.class, 
                user.getId());
        
        if (count == null || count == 0) {
            // Insert into candidate table
            jdbcTemplate.update(
                    "INSERT INTO candidate (id, address, bio, cv_path, profile_picture_path) VALUES (?, NULL, NULL, NULL, NULL)",
                    user.getId());
            logger.info("Created candidate record for user ID: {}", user.getId());
        }
        
        // Clear the persistence context to force a fresh load of the entity
        // This is the key part - we need to clear Hibernate's cache
        userRepository.flush();
        entityManager.clear();
        
        // Reload the entity - it should now be loaded as a Candidate
        User refreshedUser = userRepository.findById(user.getId()).orElse(null);
        if (refreshedUser instanceof Candidate) {
            Candidate candidate = (Candidate) refreshedUser;
            
            // Initialize collections if null to avoid NPEs
            if (candidate.getSkills() == null) {
                candidate.setSkills(new ArrayList<>());
            }
            
            if (candidate.getEducation() == null) {
                candidate.setEducation(new ArrayList<>());
            }
            
            if (candidate.getExperience() == null) {
                candidate.setExperience(new ArrayList<>());
            }
            
            logger.info("Successfully converted user to Candidate entity");
            return candidate;
        } else {
            logger.error("Failed to convert user to Candidate entity, still got: {}", 
                    refreshedUser != null ? refreshedUser.getClass().getName() : "null");
            throw new RuntimeException("Failed to convert user to Candidate entity");
        }
    }
    
    @Override
    @Transactional
    public Recruiter createRecruiterFromUser(User user) {
        logger.info("Converting user ID: {} to recruiter entity", user.getId());
        
        // Check if already a recruiter
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser instanceof Recruiter) {
            logger.info("User is already a Recruiter entity");
            return (Recruiter) existingUser;
        }
        
        // We need to use a JPQL update directly to change the entity type in the database
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Check if recruiter record exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recruiter WHERE id = ?", 
                Integer.class, 
                user.getId());
        
        if (count == null || count == 0) {
            // Insert into recruiter table
            jdbcTemplate.update(
                    "INSERT INTO recruiter (id, company_id, position, profile_picture_path) VALUES (?, NULL, NULL, ?)",
                    user.getId(), user.getProfilePicturePath());
            logger.info("Created recruiter record for user ID: {} with profile picture path {}", user.getId(), user.getProfilePicturePath());
        }
        
        // Clear the persistence context to force a fresh load of the entity
        // This is the key part - we need to clear Hibernate's cache
        userRepository.flush();
        entityManager.clear();
        
        // Reload the entity - it should now be loaded as a Recruiter
        User refreshedUser = userRepository.findById(user.getId()).orElse(null);
        if (refreshedUser instanceof Recruiter) {
            logger.info("Successfully converted user to Recruiter entity");
            return (Recruiter) refreshedUser;
        } else {
            logger.error("Failed to convert user to Recruiter entity, still got: {}", 
                    refreshedUser != null ? refreshedUser.getClass().getName() : "null");
            throw new RuntimeException("Failed to convert user to Recruiter entity");
        }
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Admin createAdminFromUser(User user) {
        logger.info("Converting user ID: {} to admin entity", user.getId());
        // Check if already an admin
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser instanceof Admin) {
            logger.info("User is already an Admin entity");
            return (Admin) existingUser;
        }
        // Insert into admin table if not exists
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin WHERE id = ?",
                Integer.class,
                user.getId());
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO admin (id, last_login) VALUES (?, NULL)",
                    user.getId());
            logger.info("Created admin record for user ID: {}", user.getId());
        }
        userRepository.flush();
        entityManager.clear();
        User refreshedUser = userRepository.findById(user.getId()).orElse(null);
        if (refreshedUser instanceof Admin) {
            logger.info("Successfully converted user to Admin entity");
            return (Admin) refreshedUser;
        } else {
            logger.error("Failed to convert user to Admin entity, still got: {}",
                    refreshedUser != null ? refreshedUser.getClass().getName() : "null");
            throw new RuntimeException("Failed to convert user to Admin entity");
        }
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void changeUserPassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateUserInfo(Long id, String firstName, String lastName, String email, String phone) {
        User user = findById(id);
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    @Transactional
    public User toggleUserActivation(Long id) {
        User user = findById(id);
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }
} 