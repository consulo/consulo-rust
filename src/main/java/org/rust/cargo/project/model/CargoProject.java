/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.impl.UserDisabledFeatures;
import org.rust.cargo.project.workspace.CargoWorkspace;

import java.nio.file.Path;

/**
 * See docs for {@link CargoProjectsService}.
 *
 * Instances of this class are immutable and will be re-created on each project refresh.
 * This class implements {@link UserDataHolderEx} interface and therefore any data can be attached
 * to it. Note that since instances of this class are re-created on each project refresh,
 * user data will be flushed on project refresh too.
 */
public interface CargoProject extends UserDataHolderEx {

    @NotNull
    Project getProject();

    @NotNull
    Path getManifest();

    @Nullable
    VirtualFile getRootDir();

    @Nullable
    VirtualFile getWorkspaceRootDir();

    @NotNull
    String getPresentableName();

    @Nullable
    CargoWorkspace getWorkspace();

    @Nullable
    RustcInfo getRustcInfo();

    @Nullable
    Path getProcMacroExpanderPath();

    @NotNull
    UpdateStatus getWorkspaceStatus();

    @NotNull
    UpdateStatus getStdlibStatus();

    @NotNull
    UpdateStatus getRustcInfoStatus();

    @NotNull
    default UpdateStatus getMergedStatus() {
        return getWorkspaceStatus()
            .merge(getStdlibStatus())
            .merge(getRustcInfoStatus());
    }

    @NotNull
    UserDisabledFeatures getUserDisabledFeatures();

    abstract class UpdateStatus {
        private final int priority;

        protected UpdateStatus(int priority) {
            this.priority = priority;
        }

        @NotNull
        public UpdateStatus merge(@NotNull UpdateStatus status) {
            return this.priority >= status.priority ? this : status;
        }

        public static final class UpToDate extends UpdateStatus {
            public static final UpToDate INSTANCE = new UpToDate();

            private UpToDate() {
                super(0);
            }
        }

        public static final class NeedsUpdate extends UpdateStatus {
            public static final NeedsUpdate INSTANCE = new NeedsUpdate();

            private NeedsUpdate() {
                super(1);
            }
        }

        public static final class UpdateFailed extends UpdateStatus {
            @SuppressWarnings("UnstableApiUsage")
            private final @Tooltip String reason;

            public UpdateFailed(@Tooltip @NotNull String reason) {
                super(2);
                this.reason = reason;
            }

            @NotNull
            public String getReason() {
                return reason;
            }

            @Override
            public String toString() {
                return reason;
            }
        }
    }
}
