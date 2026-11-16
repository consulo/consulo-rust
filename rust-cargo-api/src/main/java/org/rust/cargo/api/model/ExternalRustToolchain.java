/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The Rust toolchain an externally built workspace (see {@link ExternalCargoProjectData}) is compiled with, as the
 * build system that owns it reports it - for example the {@code rustc} of a Bazel {@code rules_rust} toolchain.
 * <p>
 * Only {@code rustc} is required. The toolchain does not have to be a rustup toolchain, does not need {@code cargo}
 * and does not need the {@code rust-src} component: the standard library sources can be named explicitly.
 */
public final class ExternalRustToolchain {
    private final Path myRustc;
    private final @Nullable Path mySysroot;
    private final @Nullable Path myStdlibSources;
    private final @Nullable String myTargetTriple;

    /**
     * @param rustc          the {@code rustc} executable; its directory is the location the toolchain tools are run from
     * @param sysroot        the sysroot the crates are compiled against, {@code null} to ask {@code rustc --print sysroot}
     * @param stdlibSources  the standard library sources: a directory holding the {@code core}, {@code alloc},
     *                       {@code std}... crates, or a {@code lib/rustlib/src/rust} directory; {@code null} to look for
     *                       them in the sysroot and then in the project settings
     * @param targetTriple   the {@code --target} the crates are compiled for, {@code null} for the host of {@code rustc}
     */
    public ExternalRustToolchain(
        Path rustc,
        @Nullable Path sysroot,
        @Nullable Path stdlibSources,
        @Nullable String targetTriple
    ) {
        myRustc = rustc;
        mySysroot = sysroot;
        myStdlibSources = stdlibSources;
        myTargetTriple = targetTriple;
    }

    public Path getRustc() {
        return myRustc;
    }

    public @Nullable Path getSysroot() {
        return mySysroot;
    }

    public @Nullable Path getStdlibSources() {
        return myStdlibSources;
    }

    public @Nullable String getTargetTriple() {
        return myTargetTriple;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExternalRustToolchain that)) {
            return false;
        }
        return myRustc.equals(that.myRustc)
            && Objects.equals(mySysroot, that.mySysroot)
            && Objects.equals(myStdlibSources, that.myStdlibSources)
            && Objects.equals(myTargetTriple, that.myTargetTriple);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myRustc, mySysroot, myStdlibSources, myTargetTriple);
    }

    @Override
    public String toString() {
        return "ExternalRustToolchain(rustc=" + myRustc + ", sysroot=" + mySysroot + ", target=" + myTargetTriple + ")";
    }
}
