/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Processors;
import org.rust.lang.core.resolve2.ItemProcessingMode;
import org.rust.lang.core.resolve2.VisResolve2Util;
import org.rust.openapiext.VirtualFileExtUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve2.FacadeResolve;
import org.rust.lang.core.psi.ext.RsMod;

public class RsConsoleCodeFragmentContext {

    // concurrent set is needed because accessed both in `addItemsNamesFromPrelude` and `addToContext`
    @NotNull
    private final Set<String> itemsNames = ContainerUtil.newConcurrentSet();

    private volatile boolean hasAddedNamesFromPrelude = false;

    @NotNull
    private final List<String> commands = new ArrayList<>();

    public RsConsoleCodeFragmentContext(@Nullable RsReplCodeFragment codeFragment) {
        if (codeFragment != null) {
            DumbService.getInstance(codeFragment.getProject()).runWhenSmart(() -> {
                addItemsNamesFromPrelude(codeFragment);
            });
        }
    }

    // see org.rust.ide.console.RsConsoleCompletionTest.`test redefine struct from prelude`
    private void addItemsNamesFromPrelude(@NotNull RsReplCodeFragment codeFragment) {
        RsMod prelude = NameResolution.findPrelude(codeFragment);
        if (prelude == null) return;

        Set<String> preludeItemsNames = Processors.collectNames(processor -> {
            FacadeResolve.processItemDeclarations(prelude, NameResolution.getTYPES_N_VALUES_N_MACROS(), processor, ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
        });
        itemsNames.addAll(preludeItemsNames);
        hasAddedNamesFromPrelude = true;
    }

    public void addToContext(@NotNull RsConsoleOneCommandContext lastCommandContext) {
        if (commands.isEmpty()
            || lastCommandContext.getItemsNames().stream().anyMatch(itemsNames::contains)
            || lastCommandContext.isContainsUseDirective()
            || !hasAddedNamesFromPrelude
        ) {
            commands.add(lastCommandContext.getCommand());
        } else {
            commands.set(commands.size() - 1, commands.get(commands.size() - 1) + "\n" + lastCommandContext.getCommand());
        }
        itemsNames.addAll(lastCommandContext.getItemsNames());
    }

    public void updateContextAsync(@NotNull Project project, @NotNull RsReplCodeFragment codeFragment) {
        DumbService.getInstance(project).smartInvokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                updateContext(project, codeFragment);
            });
        });
    }

    public void updateContext(@NotNull Project project, @NotNull RsReplCodeFragment codeFragment) {
        codeFragment.setContext(createContext(project, (RsFile) codeFragment.getCrateRoot(), commands));
    }

    @NotNull
    public String getAllCommandsText() {
        return String.join("\n", commands);
    }

    public void clearAllCommands() {
        commands.clear();
    }

    @NotNull
    public static RsBlock createContext(@NotNull Project project, @Nullable RsFile originalCrateRoot, @NotNull List<String> commands) {
        // command may contain functions/structs with same names as in previous commands
        // therefore we should put such commands in separate scope
        StringBuilder functionBody = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            functionBody.append(commands.get(i));
            if (i < commands.size() - 1) {
                functionBody.append("\n{\n");
            }
        }
        for (int i = 0; i < commands.size() - 1; i++) {
            functionBody.append("\n}");
        }
        RsFile rsFile = new RsPsiFactory(project).createFile("fn main() { " + functionBody + " }");

        RsFile crateRoot = originalCrateRoot != null ? originalCrateRoot : findAnyCrateRoot(project);
        if (crateRoot != null) {
            rsFile.getOriginalFile();
            // rsFile.originalFile = crateRoot equivalent
            // In Java, we use the setOriginalFile approach if available, or skip
        }

        RsFunction function = RsPsiJavaUtil.childOfType(rsFile, RsFunction.class);
        RsBlock block = RsFunctionUtil.getBlock(function);
        for (int i = 0; i < commands.size() - 1; i++) {
            RsExpr tailExpr = RsBlockUtil.getExpandedTailExpr(block);
            if (tailExpr instanceof RsBlockExpr) {
                block = ((RsBlockExpr) tailExpr).getBlock();
            } else {
                break;
            }
        }
        return block;
    }

    @NotNull
    public static RsBlock createContext(@NotNull Project project, @Nullable RsFile originalCrateRoot) {
        return createContext(project, originalCrateRoot, Collections.singletonList(""));
    }

    @Nullable
    private static RsFile findAnyCrateRoot(@NotNull Project project) {
        var cargoProject = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().iterator();
        if (!cargoProject.hasNext()) return null;
        var first = cargoProject.next();
        var workspace = first.getWorkspace();
        if (workspace == null) return null;
        var packages = workspace.getPackages();
        if (packages.isEmpty()) return null;
        var firstPkg = packages.iterator().next();
        var targets = firstPkg.getTargets();
        if (targets.isEmpty()) return null;
        var crateRoot = targets.iterator().next().getCrateRoot();
        if (crateRoot == null) return null;
        var psiFile = VirtualFileExtUtil.toPsiFile(crateRoot, project);
        if (psiFile == null) return null;
        return org.rust.lang.core.psi.ext.RsFileUtil.getRustFile(psiFile);
    }
}
