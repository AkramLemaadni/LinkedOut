package ma.linkedout.linkedout.services.impl;

import ma.linkedout.linkedout.entities.Application;
import ma.linkedout.linkedout.repositories.ApplicationRepository;
import ma.linkedout.linkedout.services.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private final ApplicationRepository applicationRepository;
    
    @Autowired
    public ApplicationServiceImpl(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }
    
    @Override
    public List<Application> getAllApplications() {
        logger.info("Fetching all applications");
        return applicationRepository.findAll();
    }
    
    @Override
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        logger.info("Fetching applications for candidate ID: {}", candidateId);
        return applicationRepository.findByCandidateId(candidateId);
    }
    
    @Override
    public List<Application> getApplicationsByJobOffer(Long jobOfferId) {
        logger.info("Fetching applications for job offer ID: {}", jobOfferId);
        return applicationRepository.findByJobOfferId(jobOfferId);
    }
    
    @Override
    public Optional<Application> getApplicationById(Long id) {
        logger.info("Fetching application with ID: {}", id);
        return applicationRepository.findById(id);
    }
    
    @Override
    @Transactional
    public Application saveApplication(Application application) {
        logger.info("Saving application for job offer: {}", application.getJobOffer().getTitle());
        return applicationRepository.save(application);
    }
    
    @Override
    @Transactional
    public void deleteApplication(Long id) {
        logger.info("Deleting application with ID: {}", id);
        applicationRepository.deleteById(id);
    }
    
    @Override
    public boolean hasApplied(Long candidateId, Long jobOfferId) {
        logger.info("Checking if candidate ID: {} has applied to job offer ID: {}", candidateId, jobOfferId);
        return applicationRepository.existsByCandidateIdAndJobOfferId(candidateId, jobOfferId);
    }
    
    @Override
    public long countByJobOfferId(Long jobOfferId) {
        return applicationRepository.countByJobOfferId(jobOfferId);
    }

    @Override
    public long countByRecruiterId(Long recruiterId) {
        return applicationRepository.countByRecruiterId(recruiterId);
    }
} 