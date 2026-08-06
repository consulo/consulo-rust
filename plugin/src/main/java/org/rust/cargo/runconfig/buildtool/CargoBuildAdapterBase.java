/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;
import consulo.build.ui.impl.internal.output.BuildOutputInstantReaderImpl;

import consulo.build.ui.progress.BuildProgressListener;
import consulo.build.ui.output.BuildOutputInstantReader;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class CargoBuildAdapterBase extends ProcessAdapter {
    private final CargoBuildContextBase context;
    protected final BuildProgressListener buildProgressListener;
    private final BuildOutputInstantReaderImpl instantReader;

    public CargoBuildAdapterBase(CargoBuildContextBase context, BuildProgressListener buildProgressListener) {
        this.context = context;
        this.buildProgressListener = buildProgressListener;
        this.instantReader = new BuildOutputInstantReaderImpl(
            context.getBuildId(),
            context.getParentId(),
            buildProgressListener,
            List.of(new RsBuildEventsConverter(context))
        );
    }

    @Override
    public void processTerminated(ProcessEvent event) {
        instantReader.closeAndGetFuture().whenComplete((result, error) -> {
            boolean isSuccess = event.getExitCode() == 0 && context.getErrors().get() == 0;
            boolean isCanceled = context.getIndicator() != null && context.getIndicator().isCanceled();
            onBuildOutputReaderFinish(event, isSuccess, isCanceled, error);
        });
    }

    public void onBuildOutputReaderFinish(ProcessEvent event, boolean isSuccess, boolean isCanceled, Throwable error) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
        // Progress messages end with '\r' instead of '\n'. We want to replace '\r' with '\n'
        // so that `instantReader` sends progress messages to parsers separately from other messages.
        String text = StringUtil.convertLineSeparators(event.getText());
        instantReader.append(text);
    }
}
