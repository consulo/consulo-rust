/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.FileAttribute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.decl.DeclMacroExpander;
import org.rust.lang.core.macros.proc.ProcMacroExpander;
import org.rust.openapiext.OpenApiExtUtil;
import org.rust.stdext.HashCode;
import consulo.util.lang.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.ref.WeakReference;

public final class ExpandedMacroStorage {
    private ExpandedMacroStorage() {
    }

    private static final int RANGE_MAP_ATTRIBUTE_VERSION = 2;

    public static final int MACRO_STORAGE_VERSION = 1 +  // self version
        DeclMacroExpander.EXPANDER_VERSION +
        ProcMacroExpander.EXPANDER_VERSION +
        RANGE_MAP_ATTRIBUTE_VERSION;

    /** We use {@link WeakReference} because uncached {@link #loadRangeMap} is quite cheap */
    private static final Key<WeakReference<RangeMap>> MACRO_RANGE_MAP_CACHE_KEY =
        Key.create("MACRO_RANGE_MAP_CACHE_KEY");

    private static final FileAttribute RANGE_MAP_ATTRIBUTE = new FileAttribute(
        "org.rust.macro.RangeMap",
        RANGE_MAP_ATTRIBUTE_VERSION,
        /* fixedSize = */ true
    );

    @SuppressWarnings("UnstableApiUsage")
    public static void writeRangeMap(@Nonnull VirtualFile file, @Nonnull RangeMap ranges) {
        OpenApiExtUtil.checkWriteAccessAllowed();

        try (DataOutputStream stream = RANGE_MAP_ATTRIBUTE.writeAttribute(file)) {
            ranges.writeTo(stream);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        WeakReference<RangeMap> ref = file.getUserData(MACRO_RANGE_MAP_CACHE_KEY);
        if (ref != null && ref.get() != null) {
            file.putUserData(MACRO_RANGE_MAP_CACHE_KEY, new WeakReference<>(ranges));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    public static RangeMap loadRangeMap(@Nonnull VirtualFile file) {
        OpenApiExtUtil.checkReadAccessAllowed();

        WeakReference<RangeMap> ref = file.getUserData(MACRO_RANGE_MAP_CACHE_KEY);
        if (ref != null) {
            RangeMap cached = ref.get();
            if (cached != null) return cached;
        }

        DataInputStream data = RANGE_MAP_ATTRIBUTE.readAttribute(file);
        if (data == null) return null;
        RangeMap ranges;
        try {
            ranges = RangeMap.readFrom(data);
        } catch (java.io.IOException e) {
            return null;
        }
        file.putUserData(MACRO_RANGE_MAP_CACHE_KEY, new WeakReference<>(ranges));
        return ranges;
    }

    /**
     * The second part is a stored {@link #MACRO_STORAGE_VERSION} value.
     */
    @Nullable
    public static Pair<HashCode, Integer> extractMixHashAndMacroStorageVersion(@Nonnull VirtualFile file) {
        String name = file.getName();
        int firstUnderscoreIndex = name.indexOf('_');
        if (firstUnderscoreIndex == -1) return null;
        int lastUnderscoreIndex = name.indexOf('_', firstUnderscoreIndex + 1);
        if (lastUnderscoreIndex == -1) return null;
        HashCode mixHash;
        try {
            mixHash = HashCode.fromHexString(name.substring(0, firstUnderscoreIndex));
        } catch (Exception e) {
            return null;
        }
        int version;
        try {
            version = Integer.parseInt(name.substring(
                lastUnderscoreIndex + 1,
                name.length() - 3 // ".rs".length()
            ));
        } catch (NumberFormatException e) {
            version = -1;
        }
        return new Pair<>(mixHash, version);
    }
}
