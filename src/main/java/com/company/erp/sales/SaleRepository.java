package com.company.erp.sales;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    boolean existsBySaleNumber(String saleNumber);

    long countByBranchIdAndStatus(UUID branchId, SaleStatus status);

    /** Revenue for the branch P&L (spec section 24) - gross sales value, before any return refunds are netted out. */
    @Query("""
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.branch.id IN :branchIds AND s.status = :status
              AND s.createdAt >= :from AND s.createdAt <= :to
            """)
    BigDecimal sumRevenueForBranchesInRange(@Param("branchIds") List<UUID> branchIds,
                                             @Param("status") SaleStatus status,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to);

    /**
     * Day-bucketed revenue + sale count for the dashboard/report sales
     * trend - native query (date_trunc is Postgres-specific, which this
     * app targets exclusively - see architecture doc). Each row is
     * [day (Timestamp), revenue (BigDecimal), salesCount (Long)].
     */
    @Query(value = """
            SELECT date_trunc('day', s.created_at) AS day,
                   COALESCE(SUM(s.total_amount), 0) AS revenue,
                   COUNT(*) AS sales_count
            FROM sales s
            WHERE s.branch_id IN (:branchIds) AND s.status = 'COMPLETED'
              AND s.created_at >= :from AND s.created_at <= :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> dailyRevenueTrend(@Param("branchIds") List<UUID> branchIds,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    /**
     * Optional filters are only applied when supplied (Specification), so no
     * null is ever bound to the query - see SearchSpecs.
     */
    default Page<Sale> search(List<UUID> branchIds, UUID customerId, PaymentStatus paymentStatus,
                              SaleStatus status, Instant from, Instant to, String search, Pageable pageable) {
        Specification<Sale> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(SearchSpecs.idIn(root.get("branch").get("id"), branchIds, cb));
            if (customerId != null) {
                p.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (paymentStatus != null) {
                p.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to));
            }
            if (SearchSpecs.hasText(search)) {
                p.add(SearchSpecs.containsIgnoreCase(root.<String>get("saleNumber"), search.trim(), cb));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, SearchSpecs.withDefaultSort(pageable, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
