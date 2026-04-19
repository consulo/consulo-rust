/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.impl.RustcVersion;

import java.util.List;
import java.util.Objects;

public final class RustcInfo {

    @NotNull
    private final String sysroot;

    @Nullable
    private final RustcVersion version;

    @Nullable
    private final String rustupActiveToolchain;

    @Nullable
    private final List<String> targets;

    /**
     * In production environments it is always equal to {@link #version}.
     * In unit tests it is real, non-mocked toolchain version.
     */
    @Nullable
    private final RustcVersion realVersion;

    public RustcInfo(@NotNull String sysroot, @Nullable RustcVersion version, @Nullable String rustupActiveToolchain, @Nullable List<String> targets, @Nullable RustcVersion realVersion) {
        this.sysroot = sysroot;
        this.version = version;
        this.rustupActiveToolchain = rustupActiveToolchain;
        this.targets = targets;
        this.realVersion = realVersion;
    }

    public RustcInfo(@NotNull String sysroot, @Nullable RustcVersion version, @Nullable String rustupActiveToolchain, @Nullable List<String> targets) {
        this(sysroot, version, rustupActiveToolchain, targets, version);
    }

    public RustcInfo(@NotNull String sysroot, @Nullable RustcVersion version) {
        this(sysroot, version, null, null, version);
    }

    @NotNull
    public String getSysroot() {
        return sysroot;
    }

    @Nullable
    public RustcVersion getVersion() {
        return version;
    }

    @Nullable
    public String getRustupActiveToolchain() {
        return rustupActiveToolchain;
    }

    @Nullable
    public List<String> getTargets() {
        return targets;
    }

    @Nullable
    public RustcVersion getRealVersion() {
        return realVersion;
    }

    public RustcInfo copy(@NotNull String sysroot, @Nullable RustcVersion version, @Nullable String rustupActiveToolchain, @Nullable List<String> targets, @Nullable RustcVersion realVersion) {
        return new RustcInfo(sysroot, version, rustupActiveToolchain, targets, realVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RustcInfo that)) return false;
        return Objects.equals(sysroot, that.sysroot) &&
            Objects.equals(version, that.version) &&
            Objects.equals(rustupActiveToolchain, that.rustupActiveToolchain) &&
            Objects.equals(targets, that.targets) &&
            Objects.equals(realVersion, that.realVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sysroot, version, rustupActiveToolchain, targets, realVersion);
    }

    @Override
    public String toString() {
        return "RustcInfo(sysroot=" + sysroot + ", version=" + version +
            ", rustupActiveToolchain=" + rustupActiveToolchain +
            ", targets=" + targets + ", realVersion=" + realVersion + ")";
    }
}
