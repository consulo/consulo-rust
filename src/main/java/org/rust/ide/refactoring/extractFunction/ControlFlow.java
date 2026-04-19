/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.ty.Ty;

import java.util.function.Function;

public class ControlFlow {
    @NotNull
    private final String myText;
    @NotNull
    private final Ty myType;
    @Nullable
    private final TryOperatorInfo myTryOperatorInfo;

    public ControlFlow(@NotNull String text, @NotNull Ty type, @Nullable TryOperatorInfo tryOperatorInfo) {
        myText = text;
        myType = type;
        myTryOperatorInfo = tryOperatorInfo;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    @NotNull
    public Ty getType() {
        return myType;
    }

    @Nullable
    public TryOperatorInfo getTryOperatorInfo() {
        return myTryOperatorInfo;
    }

    public static class TryOperatorInfo {
        @NotNull
        private final String mySuccessVariant;
        @NotNull
        private final Function<String, String> myGenerateType;

        public TryOperatorInfo(@NotNull String successVariant, @NotNull Function<String, String> generateType) {
            mySuccessVariant = successVariant;
            myGenerateType = generateType;
        }

        @NotNull
        public String getSuccessVariant() {
            return mySuccessVariant;
        }

        @NotNull
        public String generateType(@NotNull String input) {
            return myGenerateType.apply(input);
        }
    }
}
