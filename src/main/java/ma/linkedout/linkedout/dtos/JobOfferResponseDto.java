package ma.linkedout.linkedout.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobOfferResponseDto {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String salary;
    private String contractType;
    private CompanyDto company;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private int candidatesCount;
    private String status;

    @Data
    public static class CompanyDto {
        private Long id;
        private String name;
    }
} 