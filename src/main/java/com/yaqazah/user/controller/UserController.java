package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.DeleteUserRequestDto;
import com.yaqazah.user.dto.request.FleetDriverDto;
import com.yaqazah.user.dto.request.LoginRequestDto;
import com.yaqazah.user.dto.response.AdminCompanyDashboardDto;
import com.yaqazah.user.dto.response.UserProfileResponseDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@NullMarked
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "User Profile",
        description = "Endpoints for logged-in users"
)
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;


    private String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }


        return authentication.getName();
    }

    @GetMapping
    @Operation(
            summary = "Get My Profile"
    )
    public ResponseEntity<UserProfileResponseDto> getMyProfile() {


        String email =
                getCurrentUserEmail();


        UserProfileResponseDto response =
                userService.getUserProfileDto(email);


        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update-name")
    @Operation(
            summary = "Update Full Name"
    )
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    public ResponseEntity<String> updateMyName(
            @RequestBody Map<String, String> body
    ) {


        String newName =
                body.get("fullName");


        userService.updateUserName(
                getCurrentUserEmail(),
                newName
        );


        return ResponseEntity.ok(
                "Name updated successfully"
        );
    }

    @DeleteMapping
    @Operation(
            summary = "Delete My Account"
    )
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    public ResponseEntity<String> deleteMyAccount() {


        User user =
                userService.findByEmail(
                        getCurrentUserEmail()
                );


        userService.deleteAccount(
                user.getUserId()
        );


        return ResponseEntity.ok(
                "Account deleted successfully"
        );
    }

    @PostMapping("/restore")
    @Operation(
            summary = "Restore Account"
    )
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    public ResponseEntity<String> restoreAccount(
            @RequestBody LoginRequestDto request
    ) {


        userService.restoreAccount(
                request.getEmail(),
                request.getPassword()
        );


        return ResponseEntity.ok(
                "Account restored successfully"
        );
    }

    @GetMapping("/admin-view")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "Get Admin Company Dashboard",
            description = "Returns profile, company info and admins")
    public ResponseEntity<AdminCompanyDashboardDto> getAdminDashboard() {

        AdminCompanyDashboardDto response = userService.getAdminCompanyDashboard(getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "Add fleet driver")
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    public ResponseEntity<String> addFleetDriver(
            @Valid @RequestBody FleetDriverDto request
    ) {
        userService.addFleetDriver(request, getCurrentUserEmail());

        return ResponseEntity.ok("Fleet driver added successfully");
    }

    @DeleteMapping("/driver")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "Delete driver by email")
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    public ResponseEntity<String> deleteDriver(
            @Valid @RequestBody DeleteUserRequestDto request
    ) {
        String adminEmail = getCurrentUserEmail();

        userService.deleteDriverByEmail(adminEmail, request.getEmail());

        return ResponseEntity.ok("Driver deleted successfully");
    }

    //    @GetMapping("/company-admins")
//    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
//    @Operation(summary = "Get All Company Admins", description = "Fetches a list of all admins and company admins belonging to the requester's company.")
//    public ResponseEntity<List<AdminListDto>> getCompanyAdmins() {
//        String email = getCurrentUserEmail();
//        List<AdminListDto> response = userService.getCompanyAdmins(email);
//
//        return ResponseEntity.ok(response);
//    }

//    @GetMapping("/company-info")
//    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'FLEET_DRIVER')") // Note: Use hasAnyRole if your DB uses the "ROLE_" prefix
//    @Operation(summary = "Get Company Overview", description = "Fetches the company details along with the primary owner's info and total admin count.")
//    public ResponseEntity<CompanyInfoDto> getCompanyInfo() {
//        // Securely extract the email from the logged-in user's token
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        // Fetch the mapped DTO from the service
//        CompanyInfoDto response = userService.getCompanyInfo(email);
//
//        return ResponseEntity.ok(response);
//    }

}