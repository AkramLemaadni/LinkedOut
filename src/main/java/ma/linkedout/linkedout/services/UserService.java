package ma.linkedout.linkedout.services;

import ma.linkedout.linkedout.entities.Candidate;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.Admin;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;

public interface UserService extends UserDetailsService {
    User registerUser(User user);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    User getCurrentUser();
    
    <T extends User> T updateUser(T user);
    Candidate updateCandidate(Candidate candidate);
    Recruiter updateRecruiter(Recruiter recruiter);
    
    Candidate createCandidateFromUser(User user);
    Recruiter createRecruiterFromUser(User user);

    // Add this method for admin dashboard
    List<User> getAllUsers();

    Admin createAdminFromUser(User user);

    User findById(Long id);
    void changeUserPassword(Long id, String newPassword);
    User updateUserInfo(Long id, String firstName, String lastName, String email, String phone);
    User toggleUserActivation(Long id);
} 