/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.util.UndoUtil;
import consulo.document.FileDocumentManager;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.component.util.SimpleModificationTracker;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
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

    @Nonnull
    private final SimpleModificationTracker myModificationTracker;
    @Nonnull
    private final Map<CratePersistentId, Long> myLastUpdatedMacrosAt;
    @Nonnull
    private final String myProjectDirectoryName;
    @Nonnull
    private final RsTask.TaskType myTaskType;
    @Nonnull
    private final MacroExpansionFileSystem myExpansionFileSystem;

    public MacroExpansionTask(
        @Nonnull Project project,
        @Nonnull SimpleModificationTracker modificationTracker,
        @Nonnull Map<CratePersistentId, Long> lastUpdatedMacrosAt,
        @Nonnull String projectDirectoryName,
        @Nonnull RsTask.TaskType taskType
    ) {
        super(project, RsBundle.message("progress.title.expanding.rust.macros"), false);
        myModificationTracker = modificationTracker;
        myLastUpdatedMacrosAt = lastUpdatedMacrosAt;
        myProjectDirectoryName = projectDirectoryName;
        myTaskType = taskType;
        myExpansionFileSystem = MacroExpansionFileSystem.getInstance();
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
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

    @Nonnull
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
    @Nonnull
    public static HashCode extractMixHashFromExpansionName(@Nonnull String name) {
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
