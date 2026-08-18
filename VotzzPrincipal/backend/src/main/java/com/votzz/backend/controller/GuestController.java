package com.votzz.backend.controller;

import com.votzz.backend.domain.Guest;
import com.votzz.backend.dto.GuestRequest;
import com.votzz.backend.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping
    public ResponseEntity<List<Guest>> getAllGuests(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(guestService.findAllForCurrentTenant(email));
    }

    @PostMapping
    public ResponseEntity<Guest> createGuest(@RequestBody GuestRequest request) {
        return ResponseEntity.ok(guestService.createGuest(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guest> updateGuest(@PathVariable UUID id, @RequestBody GuestRequest request) {
        return ResponseEntity.ok(guestService.updateGuest(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO', 'ADMIN')")
    public ResponseEntity<Void> deleteGuest(@PathVariable UUID id) {
        guestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }

    // AUDITORIA: Adicionado Authentication para saber quem está autorizando a entrada
    @PutMapping("/{id}/authorize")
    @PreAuthorize("hasAnyRole('SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO', 'ADMIN')")
    public ResponseEntity<Guest> authorizeEntry(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(guestService.authorizeEntry(id, authentication.getName()));
    }

    // AUDITORIA: Adicionado Authentication para saber quem bipou o QR Code
    @GetMapping("/validate/{token}")
    @PreAuthorize("hasAnyRole('SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO', 'ADMIN')")
    public ResponseEntity<Guest> validateToken(@PathVariable String token, Authentication authentication) {
        return ResponseEntity.ok(guestService.findAndAuthorizeByToken(token, authentication.getName()));
    }
}