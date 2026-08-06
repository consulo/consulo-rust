package com.intellij.facet.ui;
/** IntelliJ-compat stub — simple validation-result wrapper. */
public final class ValidationResult {
    public static final ValidationResult OK = new ValidationResult(null);
    private final String message;
    public ValidationResult(String message) { this.message = message; }
    public boolean isOk() { return message == null; }
    public String getMessage() { return message; }
}
