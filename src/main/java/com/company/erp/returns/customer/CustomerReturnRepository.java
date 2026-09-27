package com.company.erp.returns.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerReturnRepository extends JpaRepository<CustomerReturn, UUID> {

    @Query("""
            SELECT r FROM CustomerReturn r
            WHERE r.branch.id IN :branchIds
              AND (:saleId IS NULL OR r.sale.id = :saleId)
              AND (:customerId IS NULL OR r.customer.id = :customerId)
            ORDER BY r.createdAt DESC
            """)
    Page<CustomerReturn> search(@Param("branchIds") List<UUID> branchIds,
                                 @Param("saleId") UUID saleId,
                                 @Param("customerId") UUID customerId,
                                 Pageable pageable);
}
