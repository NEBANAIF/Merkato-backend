package com.company.erp.settings.dto;

import com.company.erp.settings.SystemSettings;

public record SystemSettingsResponse(
        boolean lockCostPrice,
        boolean hideOutOfStockAtPos
) {
    public static SystemSettingsResponse from(SystemSettings settings) {
        return new SystemSettingsResponse(settings.isLockCostPrice(), settings.isHideOutOfStockAtPos());
    }
}
