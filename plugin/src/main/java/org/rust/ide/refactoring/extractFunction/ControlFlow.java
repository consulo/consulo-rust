/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.Ty;

import java.util.function.Function;

public class ControlFlow {
    @Nonnull
    private final String myText;
    @Nonnull
    private final Ty myType;
    @Nullable
    private final TryOperatorInfo myTryOperatorInfo;

    public ControlFlow(@Nonnull String text, @Nonnull Ty type, @Nullable TryOperatorInfo tryOperatorInfo) {
        myText = text;
        myType = type;
        myTryOperatorInfo = tryOperatorInfo;
    }

    @Nonnull
    public String getText() {
        return myText;
        }

    @Nonnull
    public Ty getType() {
        return myType;
    }

    @Nullable
    public TryOperatorInfo getTryOperatorInfo() {
        return myTryOperatorInfo;
    }

    public static class TryOperatorInfo {
        @Nonnull
        private final String mySuccessVariant;
        @Nonnull
        private final Function<String, String> myGenerateType;

        public TryOperatorInfo(@Nonnull String successVariant, @Nonnull Function<String, String> generateType) {
            mySuccessVariant = successVariant;
            myGenerateType = generateType;
        }

        @Nonnull
        public String getSuccessVariant() {
            return mySuccessVariant;
        }

        @Nonnull
        public String generateType(@Nonnull String input) {
            return myGenerateType.apply(input);
        }
    }
}
