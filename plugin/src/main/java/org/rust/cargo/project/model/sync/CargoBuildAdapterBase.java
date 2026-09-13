/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.sync;
import consulo.application.Application;
import consulo.build.ui.output.BuildOutputService;

import consulo.build.ui.progress.BuildProgressListener;
import consulo.build.ui.output.BuildOutputInstantReader;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.logging.Logger;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class CargoBuildAdapterBase extends ProcessAdapter {
    private static final Logger LOG = Logger.getInstance(CargoBuildAdapterBase.class);

    private final CargoBuildContextBase context;
    protected final BuildProgressListener buildProgressListener;
    private final BuildOutputInstantReader.Primary instantReader;

    public CargoBuildAdapterBase(CargoBuildContextBase context, BuildProgressListener buildProgressListener) {
        this.context = context;
        this.buildProgressListener = buildProgressListener;
        // BuildOutputInstantReaderImpl is platform-internal; BuildOutputService is the public factory.
        this.instantReader = Application.get().getInstance(BuildOutputService.class)
            .createBuildOutputInstantReader(
                context.getBuildId(),
                context.getParentId(),
                buildProgressListener,
                List.of(new RsBuildEventsConverter(context))
            );
    }

    @Override
    public void processTerminated(ProcessEvent event) {
        // The internal impl exposed closeAndGetFuture(); the public Primary interface only extends
        // Closeable, so the completion callback runs right after the synchronous close.
        Throwable error = null;
        try {
            instantReader.close();
        }
        catch (IOException e) {
            error = e;
        }
        boolean isSuccess = event.getExitCode() == 0 && context.getErrors().get() == 0;
        boolean isCanceled = context.getIndicator() != null && context.getIndicator().isCanceled();
        onBuildOutputReaderFinish(event, isSuccess, isCanceled, error);
    }

    public void onBuildOutputReaderFinish(ProcessEvent event, boolean isSuccess, boolean isCanceled, Throwable error) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
        // Progress messages end with '\r' instead of '\n'. We want to replace '\r' with '\n'
        // so that `instantReader` sends progress messages to parsers separately from other messages.
        String text = StringUtil.convertLineSeparators(event.getText());
        try {
            // BuildOutputInstantReader.Primary appends via Appendable, whose append is checked;
            // the internal impl this replaced declared no exception.
            instantReader.append(text);
        }
        catch (IOException e) {
            LOG.warn("Failed to forward build output to the parsers", e);
        }
    }
}
