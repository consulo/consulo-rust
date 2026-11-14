/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.generated;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;

/**
 * Generates, in memory, a file a build script writes to {@code OUT_DIR} - from the sources the build script reads,
 * for example the {@code .proto} files of prost and tonic.
 * <p>
 * A file a provider generates is never read from {@code OUT_DIR}: {@code include!(concat!(env!("OUT_DIR"), "/x.rs"))}
 * stands for the generated code, whether or not the build script has run, and its items navigate to the sources.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface RsGeneratedIncludeProvider {
    /**
     * The code of {@code fileName} - a path relative to {@code OUT_DIR} - in the build script output of
     * {@code crate}, or {@code null} when this provider does not generate that file.
     * <p>
     * Runs while the def map of the crate is built, which can be while indexing: indexes must not be used.
     */
    @Nullable
    RsGeneratedCode generate(@Nonnull Crate crate, @Nonnull String fileName);
}
