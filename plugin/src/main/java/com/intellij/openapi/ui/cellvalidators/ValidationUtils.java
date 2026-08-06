package com.intellij.openapi.ui.cellvalidators;
public final class ValidationUtils {
    private ValidationUtils() {}
    public static boolean isEmptyValue(Object value) { return value == null || String.valueOf(value).isEmpty(); }
}
