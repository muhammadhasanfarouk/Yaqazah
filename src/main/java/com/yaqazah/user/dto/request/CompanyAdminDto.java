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
public class CompanyAdminDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email must contain a valid domain (like .com or .net)")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Past
    private LocalDate birthDate;

    @AssertTrue(message = "Must be at least 18")
    public boolean isAdult() {
        return birthDate != null &&
                birthDate.plusYears(18).isBefore(LocalDate.now());
    }
}