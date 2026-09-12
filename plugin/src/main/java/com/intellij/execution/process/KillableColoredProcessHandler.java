package com.intellij.execution.process;

import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessListener;
import consulo.process.ProcessHandlerFeature;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

/**
 * Colored, killable process handler that delegates to a handler built with
 * {@link ProcessHandlerBuilder}. The PTY and destroy-recursively flags are tracked here because
 * the built handler does not expose them.
 */
public class KillableColoredProcessHandler extends UserDataHolderBase implements ProcessHandler {
    private final ProcessHandler delegate;
    private boolean hasPty;
    private boolean shouldDestroyRecursively = true;

    public KillableColoredProcessHandler(GeneralCommandLine commandLine) throws ExecutionException {
        this(ProcessHandlerBuilder.create(commandLine).colored().killable().build());
    }

    protected KillableColoredProcessHandler(ProcessHandler delegate) {
        this.delegate = delegate;
    }

    public final boolean hasPty() { return hasPty; }
    public final void setHasPty(boolean hasPty) { this.hasPty = hasPty; }
    public final void setShouldDestroyProcessRecursively(boolean v) { this.shouldDestroyRecursively = v; }
    public final boolean shouldDestroyProcessRecursively() { return shouldDestroyRecursively; }
    public void setShouldKillProcessSoftly(boolean b) {}

    // ---- ProcessHandler delegation -------------------------------------------------------------
    @Override public void startNotify() { delegate.startNotify(); }
    @Override public long getId() { return delegate.getId(); }
    @Override public boolean detachIsDefault() { return delegate.detachIsDefault(); }
    @Override public boolean waitFor() { return delegate.waitFor(); }
    @Override public boolean waitFor(long timeout) { return delegate.waitFor(timeout); }
    @Override public void destroyProcess() { delegate.destroyProcess(); }
    @Override public void detachProcess() { delegate.detachProcess(); }
    @Override public boolean isProcessTerminated() { return delegate.isProcessTerminated(); }
    @Override public boolean isProcessTerminating() { return delegate.isProcessTerminating(); }
    @Override public Integer getExitCode() { return delegate.getExitCode(); }
    @Override public void addProcessListener(ProcessListener l) { delegate.addProcessListener(l); }
    @Override public void removeProcessListener(ProcessListener l) { delegate.removeProcessListener(l); }
    @Override public void notifyTextAvailable(String text, Key outputType) { delegate.notifyTextAvailable(text, outputType); }
    @Override public OutputStream getProcessInput() { return delegate.getProcessInput(); }
    @Override public boolean isStartNotified() { return delegate.isStartNotified(); }
    @Override public boolean isSilentlyDestroyOnClose() { return delegate.isSilentlyDestroyOnClose(); }
    @Override public <F extends ProcessHandlerFeature> F getFeature(Class<F> cls) { return delegate.getFeature(cls); }
    @Override public Charset getCharset() { return delegate.getCharset(); }
    @Override public String getCommandLine() { return delegate.getCommandLine(); }
    @Override public Future<?> executeTask(Runnable r) { return delegate.executeTask(r); }
}
