package ma.linkedout.linkedout.services;

import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.entities.Company;
import ma.linkedout.linkedout.repositories.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company getCompanyById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }
} 