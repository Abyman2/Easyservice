package com.easyservice.backend.controller;

import com.easyservice.backend.model.Transaction;
import com.easyservice.backend.service.BookingTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingTransactionService bookingTransactionService;

    public BookingController(BookingTransactionService bookingTransactionService) {
        this.bookingTransactionService = bookingTransactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createBooking(@RequestParam String customerId,
                                                      @RequestParam String listingId,
                                                      @RequestParam int quantity,
                                                      @RequestParam(required = false) String promoCode) {
        Transaction tx = bookingTransactionService.createBooking(customerId, listingId, quantity, promoCode);
        return ResponseEntity.ok(tx);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Transaction> completeBooking(@PathVariable String id) {
        Transaction tx = bookingTransactionService.completeTransaction(id);
        return ResponseEntity.ok(tx);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Transaction> cancelBooking(@PathVariable String id, @RequestParam String customerId) {
        Transaction tx = bookingTransactionService.cancelTransaction(id, customerId);
        return ResponseEntity.ok(tx);
    }
}
