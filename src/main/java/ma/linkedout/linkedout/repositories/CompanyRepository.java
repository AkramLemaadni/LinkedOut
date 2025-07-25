package ma.linkedout.linkedout.repositories;

import ma.linkedout.linkedout.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
} 