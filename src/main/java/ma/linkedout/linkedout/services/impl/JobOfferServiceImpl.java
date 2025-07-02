package ma.linkedout.linkedout.services.impl;

import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.entities.Recruiter;
import ma.linkedout.linkedout.repositories.JobOfferRepository;
import ma.linkedout.linkedout.services.JobOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import ma.linkedout.linkedout.dtos.JobOfferResponseDto;
import ma.linkedout.linkedout.dtos.JobOfferResponseDto.CompanyDto;
import java.util.stream.Collectors;
import ma.linkedout.linkedout.entities.JobOfferStatus;

@Service
public class JobOfferServiceImpl implements JobOfferService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobOfferServiceImpl.class);
    
    @Autowired
    private JobOfferRepository jobOfferRepository;
    
    @Override
    @Transactional
    public JobOffer saveJobOffer(JobOffer jobOffer) {
        try {
            logger.info("Saving job offer: {}", jobOffer.getTitle());
            return jobOfferRepository.save(jobOffer);
        } catch (Exception e) {
            logger.error("Error saving job offer: {}", e.getMessage());
            throw new RuntimeException("Failed to save job offer: " + e.getMessage());
        }
    }
    
    @Override
    public List<JobOffer> getAllJobOffers() {
        logger.info("Fetching all job offers");
        return jobOfferRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getActiveJobOffers() {
        logger.info("Fetching active job offers");
        return jobOfferRepository.findByActiveTrue();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<JobOfferResponseDto> getActiveJobOfferDtos() {
        logger.info("Fetching and mapping active and accepted job offers to DTOs");
        List<JobOffer> activeAcceptedJobOffers = jobOfferRepository.findAll().stream()
            .filter(offer -> offer.isActive() && offer.getStatus() != null && offer.getStatus().name().equals("ACCEPTED"))
            .collect(Collectors.toList());
        logger.info("Found {} active and accepted job offers from repository.", activeAcceptedJobOffers.size());
        List<JobOfferResponseDto> dtoList = activeAcceptedJobOffers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        logger.info("Generated {} job offer DTOs.", dtoList.size());
        return dtoList;
    }
    
    private JobOfferResponseDto convertToDto(JobOffer jobOffer) {
        JobOfferResponseDto dto = new JobOfferResponseDto();
        dto.setId(jobOffer.getId());
        dto.setTitle(jobOffer.getTitle());
        dto.setDescription(jobOffer.getDescription());
        dto.setRequirements(jobOffer.getRequirements());
        dto.setLocation(jobOffer.getLocation());
        dto.setSalary(jobOffer.getSalary());
        dto.setContractType(jobOffer.getContractType());
        dto.setCreatedAt(jobOffer.getCreatedAt());
        dto.setExpiresAt(jobOffer.getExpiresAt());
        dto.setActive(jobOffer.isActive());
        
        if (jobOffer.getCompany() != null) {
            CompanyDto companyDto = new CompanyDto();
            companyDto.setId(jobOffer.getCompany().getId());
            companyDto.setName(jobOffer.getCompany().getName());
            dto.setCompany(companyDto);
        }
        
        return dto;
    }
    
    @Override
    public List<JobOffer> searchJobOffers(String query) {
        logger.info("Searching job offers with query: {}", query);
        return jobOfferRepository.searchJobOffers(query);
    }
    
    @Override
    public List<JobOffer> getJobOffersByRecruiter(Long recruiterId) {
        logger.info("Fetching job offers for recruiter ID: {}", recruiterId);
        return jobOfferRepository.findByRecruiterId(recruiterId);
    }
    
    @Override
    public List<JobOffer> getJobOffersByCompany(Long companyId) {
        logger.info("Fetching job offers for company ID: {}", companyId);
        return jobOfferRepository.findByCompanyId(companyId);
    }
    
    @Override
    public Optional<JobOffer> getJobOfferById(Long id) {
        logger.info("Fetching job offer with ID: {}", id);
        return jobOfferRepository.findById(id);
    }
    
    @Override
    @Transactional
    public void deleteJobOffer(Long id) {
        try {
        logger.info("Deleting job offer with ID: {}", id);
        jobOfferRepository.deleteById(id);
        } catch (Exception e) {
            logger.error("Error deleting job offer: {}", e.getMessage());
            throw new RuntimeException("Failed to delete job offer: " + e.getMessage());
        }
    }
    
    @Override
    public void updateJobOfferStatus(Long id, JobOfferStatus status) {
        JobOffer offer = jobOfferRepository.findById(id).orElseThrow(() -> new RuntimeException("Job offer not found"));
        offer.setStatus(status);
        jobOfferRepository.save(offer);
    }
} 