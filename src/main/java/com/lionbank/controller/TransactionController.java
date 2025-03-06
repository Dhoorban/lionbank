package com.lionbank.controller;
import com.lionbank.dao.TransactionRecord;
import com.lionbank.dao.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping
    public Page<TransactionRecord> getTransactions(
            @RequestParam Optional<String> accountNumber,
            @RequestParam Optional<String> customerId,
            @RequestParam Optional<String> description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionRepository.searchTransactions(customerId.orElse(null),accountNumber.orElse(null),description.orElse(null),PageRequest.of(page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id, @RequestBody String description) {


        try {
            TransactionRecord transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction with id " + id + " not found"));

            transaction.setDescription(description);

            try {
                return ResponseEntity.ok(transactionRepository.save(transaction));
            } catch (OptimisticLockException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Transaction has been modified by another user. Please try again.");
            }
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}
