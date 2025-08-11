package dev.taiqane.patches.internal.error;

import lombok.Getter;

@Getter
public enum ExitCodes {

    SUCCESSFUL(0),
    OPERATING_SYSTEM_ERROR(1),
    USAGE_ERROR(8),
    INTERNAL_ERROR(42);

    private final int codeValue;

    private ExitCodes(int codeValue) {
        this.codeValue = codeValue;
    }
}
