/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.coverage.SimpleCoverageAnnotator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.io.File;

public class RsCoverageAnnotator extends SimpleCoverageAnnotator {

    public RsCoverageAnnotator(@NotNull Project project) {
        super(project);
    }

    @NotNull
    @Override
    protected FileCoverageInfo fillInfoForUncoveredFile(@NotNull File file) {
        return new FileCoverageInfo();
    }

    @Nullable
    @Override
    protected String getLinesCoverageInformationString(@NotNull FileCoverageInfo info) {
        if (info.totalLineCount == 0) {
            return null;
        } else if (info.coveredLineCount == 0) {
            return RsBundle.message("no.lines.covered");
        } else if (info.coveredLineCount * 100 < info.totalLineCount) {
            return RsBundle.message("1.lines.covered");
        } else {
            return RsBundle.message("0.lines.covered", calcCoveragePercentage(info));
        }
    }

    @Nullable
    @Override
    protected String getFilesCoverageInformationString(@NotNull DirCoverageInfo info) {
        if (info.totalFilesCount == 0) {
            return null;
        } else if (info.coveredFilesCount == 0) {
            return RsBundle.message("0.of.1.files.covered", info.coveredFilesCount, info.totalFilesCount);
        } else {
            return RsBundle.message("0.of.1.files", info.coveredFilesCount, info.totalFilesCount);
        }
    }

    @NotNull
    public static RsCoverageAnnotator getInstance(@NotNull Project project) {
        return project.getService(RsCoverageAnnotator.class);
    }
}
