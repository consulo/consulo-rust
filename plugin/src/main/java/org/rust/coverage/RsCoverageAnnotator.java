/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.execution.coverage.SimpleCoverageAnnotator;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import java.io.File;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RsCoverageAnnotator extends SimpleCoverageAnnotator {

    @Inject
    public RsCoverageAnnotator(@Nonnull Project project) {
        super(project);
    }

    @Nonnull
    @Override
    protected FileCoverageInfo fillInfoForUncoveredFile(@Nonnull File file) {
        return new FileCoverageInfo();
    }

    @Nullable
    @Override
    protected String getLinesCoverageInformationString(@Nonnull FileCoverageInfo info) {
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
    protected String getFilesCoverageInformationString(@Nonnull DirCoverageInfo info) {
        if (info.totalFilesCount == 0) {
            return null;
        } else if (info.coveredFilesCount == 0) {
            return RsBundle.message("0.of.1.files.covered", info.coveredFilesCount, info.totalFilesCount);
        } else {
            return RsBundle.message("0.of.1.files", info.coveredFilesCount, info.totalFilesCount);
        }
    }

    @Nonnull
    public static RsCoverageAnnotator getInstance(@Nonnull Project project) {
        return project.getService(RsCoverageAnnotator.class);
    }
}
