package com.yaqazah.user.dto.request;

import com.yaqazah.user.model.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email must contain a valid domain (like .com or .net)")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "Password must contain at least one number, one lowercase, and one uppercase letter"
    )
    private String password;

    @NotBlank(message = "Name must be specified")
    private String fullName;

    @NotNull(message = "Gender must be specified")
    private Gender gender;

    @Past
    private LocalDate birthDate;

    @AssertTrue(message = "Must be at least 18")
    public boolean isAdult() {
        return birthDate != null &&
                birthDate.plusYears(18).isBefore(LocalDate.now());
    }
}