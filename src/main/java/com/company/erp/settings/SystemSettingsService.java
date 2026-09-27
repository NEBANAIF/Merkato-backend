package com.company.erp.settings;

import com.company.erp.settings.dto.SystemSettingsRequest;
import com.company.erp.settings.dto.SystemSettingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    @Transactional
    public SystemSettingsResponse get() {
        return SystemSettingsResponse.from(getOrCreate());
    }

    @Transactional
    public SystemSettingsResponse update(SystemSettingsRequest request) {
        SystemSettings settings = getOrCreate();
        settings.setLockCostPrice(request.lockCostPrice());
        settings.setHideOutOfStockAtPos(request.hideOutOfStockAtPos());
        return SystemSettingsResponse.from(settings);
    }

    private SystemSettings getOrCreate() {
        return systemSettingsRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> systemSettingsRepository.save(new SystemSettings()));
    }
}
