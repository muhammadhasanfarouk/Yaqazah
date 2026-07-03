package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.CompanyAdminDto;
import com.yaqazah.user.dto.request.DeleteUserRequestDto;
import com.yaqazah.user.dto.request.SwapOwnershipDto;
import com.yaqazah.user.service.AdminService;
import com.yaqazah.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@NullMarked
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(
        name = "Admin User Management",
        description = "Super Admin user management"
)
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    private String getCurrentAdminEmail() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

//    @PostMapping("/register-owner")
//    @Operation(summary = "Register company owner")
//    public ResponseEntity<String> registerCompanyOwner(
//            @Valid @RequestBody CompanyOwnerRegistrationDto request
//    ) {
//
//        adminService.registerCompanyOwner(request);
//
//        return ResponseEntity.ok(
//                "Company owner registered successfully"
//        );
//    }

    @PostMapping("/add-company-admin")
    @Operation(summary = "Add company admin")
    public ResponseEntity<String> addCompanyAdmin(
            @Valid @RequestBody CompanyAdminDto request,
            Principal principal
    ) {

        adminService.addCompanyAdmin(
                request,
                principal.getName()
        );


        return ResponseEntity.ok(
                "Company admin created successfully"
        );
    }

//    @PostMapping("/add")
//    @Operation(summary = "Add fleet driver")
//    public ResponseEntity<String> addFleetDriver(
//            @Valid @RequestBody FleetDriverDto request
//    ) {
//
//        adminService.addFleetDriver(
//                request,
//                getCurrentAdminEmail()
//        );
//
//
//        return ResponseEntity.ok(
//                "Fleet driver added successfully"
//        );
//    }

//    @DeleteMapping("/{userId}")
//    @Operation(summary = "Delete user by id")
//    @Caching(evict = {
//            @CacheEvict(value = "dashboard",              allEntries = true),
//            @CacheEvict(value = "admin:sessions",         allEntries = true),
//            @CacheEvict(value = "admin:session-detail",   allEntries = true),
//            @CacheEvict(value = "admin:drivers",          allEntries = true),
//            @CacheEvict(value = "admin:driver-detail",    allEntries = true),
//            @CacheEvict(value = "user:analytics",         allEntries = true),
//            @CacheEvict(value = "user:sessions",          allEntries = true),
//            @CacheEvict(value = "user:session-detail",    allEntries = true)
//    })
//    public ResponseEntity<String> deleteUser(
//            @PathVariable UUID userId
//    ) {
//        userService.deleteAccount(userId);
//        return ResponseEntity.ok("User deleted successfully");
//    }

    @DeleteMapping
    @Operation(summary = "Delete company admin by email")
    public ResponseEntity<String> deleteUser(
            @Valid @RequestBody DeleteUserRequestDto request
    ) {
        String adminEmail = getCurrentAdminEmail();

        adminService.deleteCompanyAdminByEmail(adminEmail, request.getEmail());

        return ResponseEntity.ok("Company admin deleted successfully");
    }

    @PutMapping("/swap-ownership")
    @Operation(summary = "Swap ownership")
    public ResponseEntity<String> swapOwnership(
            @Valid @RequestBody SwapOwnershipDto request,
            Principal principal
    ) {

        adminService.swapOwnership(principal.getName(), request.getTargetEmail());


        return ResponseEntity.ok(
                "Ownership transferred successfully"
        );
    }
}