/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.impl.RsSpacingUtil;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.lang.RsLanguage;

import java.util.Objects;

public final class RsFmtContext {
    private final CommonCodeStyleSettings myCommonSettings;
    private final RsCodeStyleSettings myRustSettings;
    private final SpacingBuilder mySpacingBuilder;

    /**
     * Stores shared alignment object, e.g. for function declarations's parameters, return type and where clause.
     */
    private final Alignment mySharedAlignment;

    /**
     * Determine whether we have spotted opening delimiter during
     * construction of a <em>flat block</em>'s sub blocks list.
     *
     * We only care about opening delimiters ({@code (}, {@code [}, <code>{</code>, {@code <}, {@code |}) here,
     * because none of flat blocks has any children after block part (apart
     * from closing delimiter, which we have to handle separately anyways).
     */
    private final boolean myMetLBrace;

    public RsFmtContext(@NotNull CommonCodeStyleSettings commonSettings,
                        @NotNull RsCodeStyleSettings rustSettings,
                        @NotNull SpacingBuilder spacingBuilder,
                        @Nullable Alignment sharedAlignment,
                        boolean metLBrace) {
        myCommonSettings = commonSettings;
        myRustSettings = rustSettings;
        mySpacingBuilder = spacingBuilder;
        mySharedAlignment = sharedAlignment;
        myMetLBrace = metLBrace;
    }

    public RsFmtContext(@NotNull CommonCodeStyleSettings commonSettings,
                        @NotNull RsCodeStyleSettings rustSettings,
                        @NotNull SpacingBuilder spacingBuilder) {
        this(commonSettings, rustSettings, spacingBuilder, null, false);
    }

    @NotNull
    public CommonCodeStyleSettings getCommonSettings() {
        return myCommonSettings;
    }

    @NotNull
    public RsCodeStyleSettings getRustSettings() {
        return myRustSettings;
    }

    @NotNull
    public SpacingBuilder getSpacingBuilder() {
        return mySpacingBuilder;
    }

    @Nullable
    public Alignment getSharedAlignment() {
        return mySharedAlignment;
    }

    public boolean getMetLBrace() {
        return myMetLBrace;
    }

    @NotNull
    public RsFmtContext copy(@Nullable Alignment sharedAlignment, boolean metLBrace) {
        return new RsFmtContext(myCommonSettings, myRustSettings, mySpacingBuilder, sharedAlignment, metLBrace);
    }

    @NotNull
    public static RsFmtContext create(@NotNull CodeStyleSettings settings) {
        CommonCodeStyleSettings commonSettings = settings.getCommonSettings(RsLanguage.INSTANCE);
        RsCodeStyleSettings rustSettings = RsFormatterUtil.getRustSettings(settings);
        return new RsFmtContext(commonSettings, rustSettings, RsSpacingUtil.createSpacingBuilder(commonSettings, rustSettings));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsFmtContext that = (RsFmtContext) o;
        return myMetLBrace == that.myMetLBrace
            && Objects.equals(myCommonSettings, that.myCommonSettings)
            && Objects.equals(myRustSettings, that.myRustSettings)
            && Objects.equals(mySpacingBuilder, that.mySpacingBuilder)
            && Objects.equals(mySharedAlignment, that.mySharedAlignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myCommonSettings, myRustSettings, mySpacingBuilder, mySharedAlignment, myMetLBrace);
    }

    @Override
    public String toString() {
        return "RsFmtContext(" +
            "commonSettings=" + myCommonSettings +
            ", rustSettings=" + myRustSettings +
            ", spacingBuilder=" + mySpacingBuilder +
            ", sharedAlignment=" + mySharedAlignment +
            ", metLBrace=" + myMetLBrace +
            ')';
    }
}
