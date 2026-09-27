package com.company.erp.purchase;

import com.company.erp.common.query.SearchSpecs;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    boolean existsByOrderNumber(String orderNumber);

    long countByBranchId(UUID branchId);

    default Page<PurchaseOrder> search(List<UUID> branchIds, UUID supplierId, PurchaseOrderStatus status,
                                       String search, Pageable pageable) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(SearchSpecs.idIn(root.get("branch").get("id"), branchIds, cb));
            if (supplierId != null) {
                p.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (SearchSpecs.hasText(search)) {
                p.add(SearchSpecs.containsIgnoreCase(root.<String>get("orderNumber"), search.trim(), cb));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, SearchSpecs.withDefaultSort(pageable,
                Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("createdAt"))));
    }

    @Query("""
            SELECT COALESCE(SUM(i.receivedQuantity * i.unitCost), 0)
            FROM PurchaseOrderItem i
            WHERE i.purchaseOrder.supplier.id = :supplierId
            """)
    BigDecimal sumReceivedValueForSupplier(@Param("supplierId") UUID supplierId);
}
