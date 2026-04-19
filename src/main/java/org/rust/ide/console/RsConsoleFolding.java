/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.ConsoleFolding;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.filters.FilterUtils;
import org.rust.cargo.runconfig.filters.RegexpFileLinkFilter;
import org.rust.cargo.runconfig.filters.RsBacktraceFilter;
import org.rust.cargo.runconfig.filters.RsBacktraceItemFilter;
import org.rust.lang.core.resolve.NameResolution;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Folds backtrace items (function names and source code locations) that do not belong to the
 * user's workspace.
 */
public class RsConsoleFolding extends ConsoleFolding {
    @Override
    @Nullable
    public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
        // We assume that each stacktrace record has two lines (function name and source code location).
        // Single line folds also cannot be re-folded after they are opened as of 2020.2
        if (lines.size() < 2) return null;

        int count = lines.size() / 2;
        String callText = StringUtil.pluralize("call", count);
        return "<" + count + " internal " + callText + ">";
    }

    @Override
    public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
        Collection<CargoProject> allProjects = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects();

        boolean functionNameFound = false;
        for (CargoProject cargoProject : allProjects) {
            RsBacktraceItemFilter.BacktraceRecord record = RsBacktraceItemFilter.parseBacktraceRecord(line);
            if (record == null) continue;
            String functionName = record.getFunctionName();
            if (functionName == null) continue;
            String func = FilterUtils.normalizeFunctionPath(functionName);
            var workspace = cargoProject.getWorkspace();
            if (workspace == null) continue;
            var splitResult = NameResolution.splitAbsolutePath(func);
            if (splitResult == null) {
                functionNameFound = true;
                break;
            }
            String pkgName = splitResult.getFirst();
            var pkg = workspace.findPackageByName(pkgName);
            if (pkg == null) {
                functionNameFound = true;
                break;
            }
            if (pkg.getOrigin() != PackageOrigin.WORKSPACE) {
                functionNameFound = true;
                break;
            }
        }

        if (functionNameFound) return true;

        for (CargoProject cargoProject : allProjects) {
            VirtualFile dir = cargoProject.getWorkspaceRootDir();
            if (dir == null) {
                dir = cargoProject.getRootDir();
            }
            if (dir == null) continue;

            RegexpFileLinkFilter filter = new RegexpFileLinkFilter(project, dir, RsBacktraceFilter.LINE_REGEX);
            Matcher matchResult = filter.matchLine(line);
            if (matchResult == null) continue;
            String filePath = matchResult.group(1);
            if (filePath == null) continue;
            String systemIndependentPath = FileUtil.toSystemIndependentName(filePath);

            if (dir.findFileByRelativePath(systemIndependentPath) == null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldBeAttachedToThePreviousLine() {
        return false;
    }
}
