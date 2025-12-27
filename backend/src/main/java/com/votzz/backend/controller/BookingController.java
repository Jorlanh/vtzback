package com.votzz.backend.controller;

import com.votzz.backend.domain.Booking;
import com.votzz.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID; // Adicionado

@RestController
@RequestMapping("/api/facilities/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingRepository bookingRepository;

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        booking.setStatus("PENDING");
        return bookingRepository.save(booking);
    }

    @PatchMapping("/{id}/status")
    // Alterado de String para UUID no PathVariable
    public ResponseEntity<Booking> updateStatus(@PathVariable UUID id, @RequestBody String status) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(status.replace("\"", ""));
            return ResponseEntity.ok(bookingRepository.save(booking));
        }).orElse(ResponseEntity.notFound().build());
    }
}