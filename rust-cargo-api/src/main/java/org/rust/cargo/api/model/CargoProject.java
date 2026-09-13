/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import consulo.project.Project;

import consulo.util.dataholder.UserDataHolderEx;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.UserDisabledFeatures;
import org.rust.cargo.api.workspace.CargoWorkspace;

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

    @Nonnull
    Project getProject();

    @Nonnull
    Path getManifest();

    @Nullable
    VirtualFile getRootDir();

    @Nullable
    VirtualFile getWorkspaceRootDir();

    @Nonnull
    String getPresentableName();

    @Nullable
    CargoWorkspace getWorkspace();

    @Nullable
    RustcInfo getRustcInfo();

    @Nullable
    Path getProcMacroExpanderPath();

    @Nonnull
    UpdateStatus getWorkspaceStatus();

    @Nonnull
    UpdateStatus getStdlibStatus();

    @Nonnull
    UpdateStatus getRustcInfoStatus();

    @Nonnull
    default UpdateStatus getMergedStatus() {
        return getWorkspaceStatus()
            .merge(getStdlibStatus())
            .merge(getRustcInfoStatus());
    }

    @Nonnull
    UserDisabledFeatures getUserDisabledFeatures();

    abstract class UpdateStatus {
        private final int priority;

        protected UpdateStatus(int priority) {
            this.priority = priority;
        }

        @Nonnull
        public UpdateStatus merge(@Nonnull UpdateStatus status) {
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
            private final  String reason;

            public UpdateFailed( @Nonnull String reason) {
                super(2);
                this.reason = reason;
            }

            @Nonnull
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
