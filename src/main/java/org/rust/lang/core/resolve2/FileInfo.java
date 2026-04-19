/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.stdext.HashCode;

public class FileInfo {
    private final long modificationStamp;
    @NotNull
    private final ModData modData;
    @NotNull
    private final HashCode hash;
    /** Non-null if the file is included via {@code include!} macro */
    @Nullable
    private final MacroIndex includeMacroIndex;

    public FileInfo(long modificationStamp, @NotNull ModData modData, @NotNull HashCode hash, @Nullable MacroIndex includeMacroIndex) {
        this.modificationStamp = modificationStamp;
        this.modData = modData;
        this.hash = hash;
        this.includeMacroIndex = includeMacroIndex;
    }

    public long getModificationStamp() {
        return modificationStamp;
    }

    @NotNull
    public ModData getModData() {
        return modData;
    }

    @NotNull
    public HashCode getHash() {
        return hash;
    }

    @Nullable
    public MacroIndex getIncludeMacroIndex() {
        return includeMacroIndex;
    }
}
