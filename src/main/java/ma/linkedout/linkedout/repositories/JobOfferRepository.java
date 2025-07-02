package ma.linkedout.linkedout.repositories;

import ma.linkedout.linkedout.entities.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    
    // Trouver les offres d'emploi actives
    List<JobOffer> findByActiveTrue();
    
    // Recherche d'offres d'emploi par titre, description, prérequis ou lieu
    @Query("SELECT j FROM JobOffer j WHERE j.active = true AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.requirements) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<JobOffer> searchJobOffers(@Param("query") String query);
    
    // Trouver les offres d'emploi par recruteur
    List<JobOffer> findByRecruiterId(Long recruiterId);
    
    // Trouver les offres d'emploi par entreprise
    List<JobOffer> findByCompanyId(Long companyId);
} 