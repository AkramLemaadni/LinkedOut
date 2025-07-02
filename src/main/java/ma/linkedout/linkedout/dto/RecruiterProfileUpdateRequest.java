package ma.linkedout.linkedout.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RecruiterProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String position;
    private Long companyId;
    private MultipartFile profilePicture;
} 