package com.yaqazah.user.dto.response;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private String email;
    private String fullName;
    private String role;
    @Past
    private LocalDate birthDate;

    @AssertTrue(message = "Must be at least 18")
    public boolean isAdult() {
        return birthDate != null &&
                birthDate.plusYears(18).isBefore(LocalDate.now());
    }
}