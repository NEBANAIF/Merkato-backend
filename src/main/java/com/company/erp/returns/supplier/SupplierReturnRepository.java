package com.company.erp.returns.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, UUID> {

    @Query("""
            SELECT r FROM SupplierReturn r
            WHERE r.branch.id IN :branchIds
              AND (:supplierId IS NULL OR r.supplier.id = :supplierId)
              AND (:purchaseOrderId IS NULL OR r.purchaseOrder.id = :purchaseOrderId)
            ORDER BY r.createdAt DESC
            """)
    Page<SupplierReturn> search(@Param("branchIds") List<UUID> branchIds,
                                 @Param("supplierId") UUID supplierId,
                                 @Param("purchaseOrderId") UUID purchaseOrderId,
                                 Pageable pageable);
}
