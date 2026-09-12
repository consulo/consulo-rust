package com.intellij.execution.target;
import java.util.List;
import java.util.concurrent.CompletableFuture;
/** A command line already resolved against a target environment. */
public final class TargetedCommandLine {
    private final List<String> command;
    public TargetedCommandLine(List<String> command) { this.command = command; }
    public java.nio.charset.Charset getCharset() { return java.nio.charset.StandardCharsets.UTF_8; }
    public CompletableFuture<List<String>> collectCommandsSynchronously() { return CompletableFuture.completedFuture(command); }
    public List<String> getCommandPresentation(TargetEnvironment target) { return command; }
}
