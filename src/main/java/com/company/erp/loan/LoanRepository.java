package com.company.erp.loan;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID>, JpaSpecificationExecutor<Loan> {

    boolean existsBySaleId(UUID saleId);

    java.util.Optional<Loan> findBySaleId(UUID saleId);

    /**
     * status filters the persisted lifecycle only (OPEN/PARTIALLY_PAID/PAID
     * - see LoanStatus). overdueOnly is a separate flag rather than a status
     * value, since "overdue" is never a stored status - it's
     * remainingAmount > 0 AND dueDate in the past, evaluated fresh here.
     */
    default Page<Loan> search(List<UUID> branchIds, UUID customerId, LoanStatus status,
                              boolean overdueOnly, Pageable pageable) {
        Specification<Loan> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(SearchSpecs.idIn(root.get("branch").get("id"), branchIds, cb));
            if (customerId != null) {
                p.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (overdueOnly) {
                p.add(cb.greaterThan(root.<BigDecimal>get("remainingAmount"), BigDecimal.ZERO));
                p.add(cb.lessThan(root.<LocalDate>get("dueDate"), LocalDate.now()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, SearchSpecs.withDefaultSort(pageable, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Query("SELECT COALESCE(SUM(l.originalAmount), 0) FROM Loan l WHERE l.customer.id = :customerId")
    BigDecimal sumOriginalForCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT COALESCE(SUM(l.remainingAmount), 0) FROM Loan l WHERE l.customer.id = :customerId")
    BigDecimal sumOutstandingForCustomer(@Param("customerId") UUID customerId);
}
