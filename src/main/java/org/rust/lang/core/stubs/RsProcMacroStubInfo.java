/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.stdext.HashCode;

import java.io.IOException;

public class RsProcMacroStubInfo {
    @NotNull
    private final String myStubbedText;
    @Nullable
    private final HashCode myStubbedTextHash;
    private final int myEndOfAttrsOffset;
    private final int myStartOffset;

    public RsProcMacroStubInfo(@NotNull String stubbedText, @Nullable HashCode stubbedTextHash, int endOfAttrsOffset, int startOffset) {
        this.myStubbedText = stubbedText;
        this.myStubbedTextHash = stubbedTextHash;
        this.myEndOfAttrsOffset = endOfAttrsOffset;
        this.myStartOffset = startOffset;
    }

    @NotNull
    public String getStubbedText() {
        return myStubbedText;
    }

    @Nullable
    public HashCode getStubbedTextHash() {
        return myStubbedTextHash;
    }

    public int getEndOfAttrsOffset() {
        return myEndOfAttrsOffset;
    }

    public int getStartOffset() {
        return myStartOffset;
    }

    public static void serialize(@Nullable RsProcMacroStubInfo info, @NotNull StubOutputStream dataStream) throws IOException {
        if (info == null) {
            dataStream.writeBoolean(false);
            return;
        }
        dataStream.writeBoolean(true);
        dataStream.writeUTFFast(info.myStubbedText);
        HashCode.writeHashCodeNullable(dataStream, info.myStubbedTextHash);
        dataStream.writeVarInt(info.myEndOfAttrsOffset);
        dataStream.writeVarInt(info.myStartOffset);
    }

    @Nullable
    public static RsProcMacroStubInfo deserialize(@NotNull StubInputStream dataStream) throws IOException {
        if (!dataStream.readBoolean()) {
            return null;
        }
        String stubbedText = dataStream.readUTFFast();
        HashCode stubbedTextHash = HashCode.readHashCodeNullable(dataStream);
        int endOfAttrsOffset = dataStream.readVarInt();
        int startOffset = dataStream.readVarInt();
        return new RsProcMacroStubInfo(stubbedText, stubbedTextHash, endOfAttrsOffset, startOffset);
    }
}
