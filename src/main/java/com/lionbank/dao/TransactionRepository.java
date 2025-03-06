package com.lionbank.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
//    List<TransactionRecord> findByCustomerIdOrAccountNumberOrDescription(
//            String customerId, String accountNumber, String description);

    @Query("SELECT t FROM TransactionRecord t WHERE " +
            "(:customerId IS NULL OR t.customerId = :customerId) OR " +
            "(:accountNumber IS NULL OR t.accountNumber = :accountNumber) OR " +
            "(:description IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%')))")
    Page<TransactionRecord> searchTransactions(
            @Param("customerId") String customerId,
            @Param("accountNumber") String accountNumber,
            @Param("description") String description,
            Pageable pageable);
}
