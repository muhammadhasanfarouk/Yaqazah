package com.yaqazah.user.dto.request;

import com.yaqazah.user.model.Gender;
import com.yaqazah.user.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFleetDriverDto {
    // No @NotBlank here because the admin might only want to update one field at a time
    private String fullName;
    private Gender gender;
    private UserStatus status;
    private String newPassword;
}