package com.intellij.execution.configurations;
import consulo.process.cmd.GeneralCommandLine;
/** IntelliJ-compat stub. Consulo doesn't bundle a PTY command-line; falls back to GeneralCommandLine. */
public final class PtyCommandLine extends GeneralCommandLine {
    public static final int MAX_COLUMNS = 2048;
    private int initialColumns = -1;
    private boolean consoleMode = true;
    public PtyCommandLine() {}
    public PtyCommandLine(GeneralCommandLine original) { super(original); }
    public PtyCommandLine withInitialColumns(int columns) { this.initialColumns = columns; return this; }
    public PtyCommandLine withConsoleMode(boolean consoleMode) { this.consoleMode = consoleMode; return this; }
}
