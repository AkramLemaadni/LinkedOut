package ma.linkedout.linkedout.repositories;

import ma.linkedout.linkedout.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    
    // Trouver les candidatures par candidat
    List<Application> findByCandidateId(Long candidateId);
    
    // Trouver les candidatures par offre d'emploi
    List<Application> findByJobOfferId(Long jobOfferId);
    
    // Vérifier si un candidat a déjà postulé à une offre
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.candidate.id = :candidateId AND a.jobOffer.id = :jobOfferId")
    boolean existsByCandidateIdAndJobOfferId(@Param("candidateId") Long candidateId, @Param("jobOfferId") Long jobOfferId);

    // Compter les candidatures pour une offre d'emploi
    long countByJobOfferId(Long jobOfferId);

    // Compter toutes les candidatures pour tous les offres d'un recruteur
    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobOffer.recruiter.id = :recruiterId")
    long countByRecruiterId(@Param("recruiterId") Long recruiterId);
} 