package ma.linkedout.linkedout.services;

import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.entities.JobOfferStatus;

import java.util.List;
import java.util.Optional;
import ma.linkedout.linkedout.dtos.JobOfferResponseDto;

public interface JobOfferService {
    
    List<JobOffer> getAllJobOffers();
    
    List<JobOffer> getActiveJobOffers();
    
    List<JobOfferResponseDto> getActiveJobOfferDtos();
    
    List<JobOffer> searchJobOffers(String query);
    
    List<JobOffer> getJobOffersByRecruiter(Long recruiterId);
    
    List<JobOffer> getJobOffersByCompany(Long companyId);
    
    Optional<JobOffer> getJobOfferById(Long id);
    
    JobOffer saveJobOffer(JobOffer jobOffer);
    
    void deleteJobOffer(Long id);
    
    void updateJobOfferStatus(Long id, JobOfferStatus status);
} 