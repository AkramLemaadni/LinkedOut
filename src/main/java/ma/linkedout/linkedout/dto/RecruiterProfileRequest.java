package ma.linkedout.linkedout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterProfileRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Position is required")
    private String position;

    // Company information
    private Long companyId;
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLocation;
    private String companyIndustry;
} 