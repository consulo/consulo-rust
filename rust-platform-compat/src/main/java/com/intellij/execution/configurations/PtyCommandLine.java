package com.intellij.execution.configurations;
import consulo.process.cmd.GeneralCommandLine;
/** Command line carrying PTY options; it otherwise behaves like a plain {@link GeneralCommandLine}. */
public final class PtyCommandLine extends GeneralCommandLine {
    public static final int MAX_COLUMNS = 2048;
    private int initialColumns = -1;
    private boolean consoleMode = true;
    public PtyCommandLine() {}
    public PtyCommandLine(GeneralCommandLine original) { super(original); }
    public PtyCommandLine withInitialColumns(int columns) { this.initialColumns = columns; return this; }
    public PtyCommandLine withConsoleMode(boolean consoleMode) { this.consoleMode = consoleMode; return this; }
}
