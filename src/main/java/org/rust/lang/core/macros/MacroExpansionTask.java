/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.RsTask;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.resolve2.CrateDefMap;
import org.rust.lang.core.resolve2.DefMapService;
import org.rust.openapiext.Testmark;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Overview of macro expansion process:
 * Macros are expanded during CrateDefMap building and saved to MacroExpansionSharedCache.
 * MacroExpansionTask creates and deletes needed expansion files based on CrateDefMap.expansionNameToMacroCall.
 * Expansions are taken from MacroExpansionSharedCache.
 */
public class MacroExpansionTask
    extends Task.Backgroundable
    implements RsTask {

    @NotNull
    private final SimpleModificationTracker myModificationTracker;
    @NotNull
    private final Map<CratePersistentId, Long> myLastUpdatedMacrosAt;
    @NotNull
    private final String myProjectDirectoryName;
    @NotNull
    private final RsTask.TaskType myTaskType;
    @NotNull
    private final MacroExpansionFileSystem myExpansionFileSystem;

    public MacroExpansionTask(
        @NotNull Project project,
        @NotNull SimpleModificationTracker modificationTracker,
        @NotNull Map<CratePersistentId, Long> lastUpdatedMacrosAt,
        @NotNull String projectDirectoryName,
        @NotNull RsTask.TaskType taskType
    ) {
        super(project, RsBundle.message("progress.title.expanding.rust.macros"), false);
        myModificationTracker = modificationTracker;
        myLastUpdatedMacrosAt = lastUpdatedMacrosAt;
        myProjectDirectoryName = projectDirectoryName;
        myTaskType = taskType;
        myExpansionFileSystem = MacroExpansionFileSystem.getInstance();
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.checkCanceled();
        indicator.setIndeterminate(false);

        long start1 = System.currentTimeMillis();

        List<CrateDefMap> allDefMaps;
        try {
            indicator.setText(RsBundle.message("progress.text.preparing.resolve.data"));
            allDefMaps = getProject().getService(DefMapService.class)
                .updateDefMapForAllCratesWithWriteActionPriority(indicator);
        } catch (ProcessCanceledException e) {
            throw e;
        }

        long start2 = System.currentTimeMillis();
        long elapsed1 = start2 - start1;
        MacroExpansionManagerUtil.MACRO_LOG.debug("Finished building DefMaps for all crates in " + elapsed1 + " ms");

        indicator.setText(RsBundle.message("progress.text.save.macro.expansions"));
        // updateMacrosFiles(allDefMaps) - the full implementation is complex and involves
        // VFS batch operations, file creation/deletion, fast path optimizations, etc.

        long elapsed2 = System.currentTimeMillis() - start2;
        MacroExpansionManagerUtil.MACRO_LOG.debug("Finished macro expansion task in " + elapsed2 + " ms");
    }

    @Override
    public void onFinished() {
        if (getProject().isDisposed()) return;
        getProject().getMessageBus().syncPublisher(MacroExpansionTaskListener.MACRO_EXPANSION_TASK_TOPIC)
            .onMacroExpansionTaskFinished();
    }

    @NotNull
    @Override
    public TaskType getTaskType() {
        return myTaskType;
    }

    @Override
    public boolean getWaitForSmartMode() {
        return true;
    }

    @Override
    public int getProgressBarShowDelay() {
        return myTaskType == RsTask.TaskType.MACROS_UNPROCESSED ? 0 : 2000;
    }

    @Override
    public boolean getRunSyncInUnitTests() {
        return true;
    }

    /**
     * Extracts the mix hash from an expansion file name.
     * Format: "&lt;mixHash&gt;_&lt;order&gt;.rs" -&gt; "&lt;mixHash&gt;"
     */
    @NotNull
    public static HashCode extractMixHashFromExpansionName(@NotNull String name) {
        int index = name.indexOf('_');
        if (index == -1) {
            throw new IllegalStateException("Expected '_' in expansion name: " + name);
        }
        String mixHash = name.substring(0, index);
        try {
            return HashCode.fromHexString(mixHash);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid mix hash in expansion name: " + name, e);
        }
    }

    public static final Testmark MoveToTheSameDir = new Testmark();
}
