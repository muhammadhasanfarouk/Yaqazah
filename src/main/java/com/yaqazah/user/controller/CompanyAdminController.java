package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.UpdateFleetDriverDto;
import com.yaqazah.user.service.CompanyAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@NullMarked
@RestController
@RequestMapping("/api/company/drivers")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Company Drivers")
public class CompanyAdminController {


    private final CompanyAdminService service;


    private String email() {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return auth.getName();
    }


//    @PostMapping("/add")
//    public ResponseEntity<String> add(
//            @Valid @RequestBody FleetDriverDto dto
//    ){
//
//        service.addFleetDriver(dto,email());
//
//        return ResponseEntity.ok(
//                "Driver added"
//        );
//    }


    @PutMapping("/edit/{id}")
    public ResponseEntity<String> edit(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFleetDriverDto dto
    ) {

        service.updateFleetDriver(
                id,
                dto,
                email()
        );


        return ResponseEntity.ok(
                "Driver updated"
        );
    }

}