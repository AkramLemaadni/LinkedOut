package ma.linkedout.linkedout.services;

import ma.linkedout.linkedout.entities.Application;

import java.util.List;
import java.util.Optional;

public interface ApplicationService {
    
    List<Application> getAllApplications();
    
    List<Application> getApplicationsByCandidate(Long candidateId);
    
    List<Application> getApplicationsByJobOffer(Long jobOfferId);
    
    Optional<Application> getApplicationById(Long id);
    
    Application saveApplication(Application application);
    
    void deleteApplication(Long id);
    
    boolean hasApplied(Long candidateId, Long jobOfferId);
    
    long countByJobOfferId(Long jobOfferId);
    
    long countByRecruiterId(Long recruiterId);
} 