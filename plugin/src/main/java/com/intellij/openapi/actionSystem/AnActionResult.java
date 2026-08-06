package com.intellij.openapi.actionSystem;
/** IntelliJ-compat stub — result of executing an AnAction. */
public final class AnActionResult {
    public static final AnActionResult PERFORMED = new AnActionResult(true, null);
    public static final AnActionResult IGNORED = new AnActionResult(false, null);
    private final boolean performed;
    private final Throwable failureCause;
    private AnActionResult(boolean performed, Throwable failureCause) {
        this.performed = performed;
        this.failureCause = failureCause;
    }
    public boolean isPerformed() { return performed; }
    public Throwable getFailureCause() { return failureCause; }
    public static AnActionResult failed(Throwable cause) { return new AnActionResult(false, cause); }
}
