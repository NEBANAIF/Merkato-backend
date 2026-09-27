package com.company.erp.payment;

import com.company.erp.common.query.SearchSpecs;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
            PaymentReferenceType referenceType, UUID referenceId);

    /**
     * The Payments page table. Optional filters are only applied when given (Specification), so no
     * null is ever bound to the query - see SearchSpecs for why that matters on PostgreSQL.
     */
    default Page<Payment> search(List<UUID> branchIds, PaymentMethod method, UUID bankId,
                                 PaymentReferenceType referenceType, Instant from, Instant to,
                                 Pageable pageable) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(SearchSpecs.idIn(root.get("branch").get("id"), branchIds, cb));
            if (method != null) {
                p.add(cb.equal(root.get("method"), method));
            }
            if (bankId != null) {
                p.add(cb.equal(root.get("bank").get("id"), bankId));
            }
            if (referenceType != null) {
                p.add(cb.equal(root.get("referenceType"), referenceType));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, SearchSpecs.withDefaultSort(pageable, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
