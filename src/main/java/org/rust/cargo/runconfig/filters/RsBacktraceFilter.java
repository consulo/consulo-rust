/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.serviceContainer.NonInjectable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.resolve.NameResolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adds features to stack backtraces:
 * - Wrap function calls into hyperlinks to source code.
 * - Turn source code links into hyperlinks.
 * - Dims function hash codes to reduce noise.
 */
public class RsBacktraceFilter implements Filter {

    public static final String LINE_REGEX = "\\s+at " + RegexpFileLinkFilter.FILE_POSITION_RE;

    private final Project myProject;
    private final VirtualFile myCargoProjectDir;
    private final CargoWorkspace myWorkspace;

    @NonInjectable
    public RsBacktraceFilter(@NotNull Project project,
                             @Nullable VirtualFile cargoProjectDir,
                             @Nullable CargoWorkspace workspace) {
        myProject = project;
        myCargoProjectDir = cargoProjectDir;
        myWorkspace = workspace;
    }

    public RsBacktraceFilter(@NotNull Project project) {
        this(project, null, null);
    }

    @NotNull
    private List<RsBacktraceItemFilter> getBacktraceItemFilters() {
        if (myWorkspace == null) {
            List<RsBacktraceItemFilter> filters = CargoProjectServiceUtil.getCargoProjects(myProject).getAllProjects().stream()
                .filter(p -> p.getWorkspace() != null)
                .map(p -> new RsBacktraceItemFilter(myProject, p.getWorkspace()))
                .collect(Collectors.toList());
            if (!filters.isEmpty()) return filters;
        }
        return Collections.singletonList(new RsBacktraceItemFilter(myProject, myWorkspace));
    }

    @NotNull
    private List<RegexpFileLinkFilter> getSourceLinkFilters() {
        if (myCargoProjectDir == null) {
            return CargoProjectServiceUtil.getCargoProjects(myProject).getAllProjects().stream()
                .filter(p -> p.getRootDir() != null)
                .map(p -> new RegexpFileLinkFilter(myProject, p.getRootDir(), LINE_REGEX))
                .collect(Collectors.toList());
        }
        return Collections.singletonList(new RegexpFileLinkFilter(myProject, myCargoProjectDir, LINE_REGEX));
    }

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        for (RsBacktraceItemFilter filter : getBacktraceItemFilters()) {
            Result result = filter.applyFilter(line, entireLength);
            if (result != null) return result;
        }
        for (RegexpFileLinkFilter filter : getSourceLinkFilters()) {
            Result result = filter.applyFilter(line, entireLength);
            if (result != null) return result;
        }
        return null;
    }
}
