package com.company.erp.transfer;

import com.company.erp.common.query.SearchSpecs;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID>, JpaSpecificationExecutor<StockTransfer> {

    boolean existsByTransferNumber(String transferNumber);

    /**
     * A transfer is visible to a caller if EITHER the source or the
     * destination branch is in their accessible branch set - unlike a
     * single-branch document (a Sale, an Expense), a transfer legitimately
     * belongs to two branches at once.
     */
    default Page<StockTransfer> search(List<UUID> branchIds, TransferStatus status, String search, Pageable pageable) {
        Specification<StockTransfer> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.or(
                    SearchSpecs.idIn(root.get("sourceBranch").get("id"), branchIds, cb),
                    SearchSpecs.idIn(root.get("destinationBranch").get("id"), branchIds, cb)));
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (SearchSpecs.hasText(search)) {
                p.add(SearchSpecs.containsIgnoreCase(root.<String>get("transferNumber"), search.trim(), cb));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return findAll(spec, SearchSpecs.withDefaultSort(pageable, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
