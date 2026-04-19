/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.build.BuildProgressListener;
import com.intellij.build.output.BuildOutputInstantReaderImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;

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
