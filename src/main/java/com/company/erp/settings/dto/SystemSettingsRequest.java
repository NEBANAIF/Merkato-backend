package com.company.erp.settings.dto;

import jakarta.validation.constraints.NotNull;

public record SystemSettingsRequest(
        @NotNull Boolean lockCostPrice,
        @NotNull Boolean hideOutOfStockAtPos
) {
}
