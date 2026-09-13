/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ui.console.AnalyzeStackTraceFilter;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
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
@ExtensionImpl
public class RsBacktraceFilter implements AnalyzeStackTraceFilter {

    public static final String LINE_REGEX = "\\s+at " + RegexpFileLinkFilter.FILE_POSITION_RE;

    private final Project myProject;
    private final VirtualFile myCargoProjectDir;
    private final CargoWorkspace myWorkspace;

    public RsBacktraceFilter(@Nonnull Project project,
                             @Nullable VirtualFile cargoProjectDir,
                             @Nullable CargoWorkspace workspace) {
        myProject = project;
        myCargoProjectDir = cargoProjectDir;
        myWorkspace = workspace;
    }

    @Inject
    public RsBacktraceFilter(@Nonnull Project project) {
        this(project, null, null);
    }

    @Nonnull
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

    @Nonnull
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
    public Result applyFilter(@Nonnull String line, int entireLength) {
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
