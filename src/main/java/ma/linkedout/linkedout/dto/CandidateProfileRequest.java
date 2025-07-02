package ma.linkedout.linkedout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String address;

    private String bio;

    // Lists for profile details
    private List<String> skills = new ArrayList<>();
    private List<String> education = new ArrayList<>();
    private List<String> experience = new ArrayList<>();
} 