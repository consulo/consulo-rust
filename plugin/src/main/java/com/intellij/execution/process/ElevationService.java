package com.intellij.execution.process;
import consulo.process.cmd.GeneralCommandLine;
/** IntelliJ-compat stub. */
public interface ElevationService {
    static ElevationService getInstance() { return null; }
    Process createProcess(GeneralCommandLine commandLine) throws consulo.process.ExecutionException;
}
