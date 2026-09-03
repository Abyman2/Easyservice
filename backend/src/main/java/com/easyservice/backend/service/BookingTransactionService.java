package com.easyservice.backend.service;

import com.easyservice.backend.model.Listing;
import com.easyservice.backend.model.Transaction;
import com.easyservice.backend.model.User;
import com.easyservice.backend.model.enums.IdentityStatus;
import com.easyservice.backend.model.enums.ListingStatus;
import com.easyservice.backend.model.enums.TransactionStatus;
import com.easyservice.backend.repository.ListingRepository;
import com.easyservice.backend.repository.TransactionRepository;
import com.easyservice.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BookingTransactionService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final PromotionService promotionService;
    private final NotificationService notificationService;

    public BookingTransactionService(UserRepository userRepository,
                                     ListingRepository listingRepository,
                                     TransactionRepository transactionRepository,
                                     PaymentService paymentService,
                                     PromotionService promotionService,
                                     NotificationService notificationService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.transactionRepository = transactionRepository;
        this.paymentService = paymentService;
        this.promotionService = promotionService;
        this.notificationService = notificationService;
    }

    public synchronized Transaction createBooking(String customerId, String listingId, int quantity, String promoCode) {
        // BR-02: Only verified customers can transact
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (customer.getIdentityStatus() != IdentityStatus.VERIFIED) {
            throw new IllegalStateException("Unverified customer cannot initiate a booking (BR-02)");
        }

        // BR-04: Only published listings are available
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot book an unpublished listing (BR-04)");
        }

        // BR-06: Quantity > 0
        if (quantity <= 0) {
            throw new IllegalArgumentException("Booking quantity must be greater than zero (BR-06)");
        }

        // BR-07: Requested quantity cannot exceed available quantity
        if (quantity > listing.getAvailableQuantity()) {
            throw new IllegalStateException("Requested quantity exceeds available capacity (BR-07)");
        }

        BigDecimal baseTotal = listing.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal finalTotal = promotionService.calculateDiscountedTotal(
                promoCode, listingId, baseTotal, LocalDateTime.now().toLocalDate()
        );

        // Initial transaction state: PENDING
        Transaction transaction = new Transaction(
                null,
                customerId,
                listingId,
                quantity,
                finalTotal,
                TransactionStatus.PENDING,
                LocalDateTime.now()
        );

        Transaction savedTx = transactionRepository.save(transaction);

        // Process Payment (BR-08, BR-09, BR-10)
        PaymentService.PaymentResult paymentResult = paymentService.processPayment(customerId, finalTotal);

        if (paymentResult.isSuccess()) {
            // BR-13: Valid transition PENDING -> CONFIRMED
            savedTx.setStatus(TransactionStatus.CONFIRMED);

            // BR-15: Successful transaction reduces inventory availability
            listing.setAvailableQuantity(listing.getAvailableQuantity() - quantity);
            listingRepository.save(listing);

            // Trigger notification spy/mock verification
            notificationService.sendNotification(customer.getEmail(), 
                    "Booking confirmed for listing: " + listing.getTitle() + ". Total: " + finalTotal);
        } else {
            // Payment failed or declined -> EXPIRED
            savedTx.setStatus(TransactionStatus.EXPIRED);
        }

        savedTx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.save(savedTx);
    }

    public Transaction completeTransaction(String transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // BR-13: State Machine Validation: Only CONFIRMED -> COMPLETED is valid
        if (tx.getStatus() != TransactionStatus.CONFIRMED) {
            throw new IllegalStateException("Invalid State Transition: Cannot complete transaction from state " + tx.getStatus() + " (BR-13)");
        }

        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    public Transaction cancelTransaction(String transactionId, String customerId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // Ownership check
        if (!tx.getCustomerId().equals(customerId)) {
            throw new SecurityException("Unauthorized: Cannot cancel another customer's transaction");
        }

        // BR-14: Permitted cancellation states: PENDING or CONFIRMED only
        // BR-13 State Machine: COMPLETED or EXPIRED or CANCELLED cannot be cancelled
        if (tx.getStatus() != TransactionStatus.PENDING && tx.getStatus() != TransactionStatus.CONFIRMED) {
            throw new IllegalStateException("Invalid State Transition: Cannot cancel transaction in state " + tx.getStatus() + " (BR-14)");
        }

        boolean wasConfirmed = (tx.getStatus() == TransactionStatus.CONFIRMED);
        tx.setStatus(TransactionStatus.CANCELLED);
        tx.setUpdatedAt(LocalDateTime.now());

        Transaction cancelledTx = transactionRepository.save(tx);

        // BR-16: Eligible cancellation restores inventory availability
        if (wasConfirmed) {
            Listing listing = listingRepository.findById(tx.getListingId()).orElse(null);
            if (listing != null) {
                listing.setAvailableQuantity(listing.getAvailableQuantity() + tx.getQuantity());
                listingRepository.save(listing);
            }

            // Refund simulated balance to customer
            User customer = userRepository.findById(customerId).orElse(null);
            if (customer != null && customer.getBalance() != null) {
                customer.setBalance(customer.getBalance().add(tx.getTotalAmount()));
                userRepository.save(customer);
            }
        }

        return cancelledTx;
    }
}
