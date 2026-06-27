package com.example.swp391.aistudenthub.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 150, message = "Ho ten toi da 150 ky tu")
    private String fullName;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    @Size(max = 255, message = "Email toi da 255 ky tu")
    private String email;

    @Size(max = 1000, message = "Avatar URL toi da 1000 ky tu")
    private String avatarUrl;
}
