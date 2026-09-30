package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.NotNull;

public record ResolucaoDuplicatasRequest(
        @NotNull Long vagaMantidaId
) {
}
