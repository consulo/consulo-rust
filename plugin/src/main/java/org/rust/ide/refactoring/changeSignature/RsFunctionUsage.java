/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import consulo.usage.UsageInfo;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsFunctionUsage extends UsageInfo {
    @Nonnull
    private final RsElement myElement;

    protected RsFunctionUsage(@Nonnull RsElement element) {
        super(element);
        myElement = element;
    }

    @Nonnull
    public RsElement getUsageElement() {
        return myElement;
    }

    public boolean isCallUsage() {
        return false;
    }

    public static class FunctionCall extends RsFunctionUsage {
        @Nonnull
        private final RsCallExpr myCall;

        public FunctionCall(@Nonnull RsCallExpr call) {
            super(call);
            myCall = call;
        }

        @Nonnull
        public RsCallExpr getCall() {
            return myCall;
        }

        @Override
        public boolean isCallUsage() {
            return true;
        }
    }

    public static class MethodCall extends RsFunctionUsage {
        @Nonnull
        private final RsMethodCall myCall;

        public MethodCall(@Nonnull RsMethodCall call) {
            super(call);
            myCall = call;
        }

        @Nonnull
        public RsMethodCall getCall() {
            return myCall;
        }

        @Override
        public boolean isCallUsage() {
            return true;
        }
    }

    public static class Reference extends RsFunctionUsage {
        @Nonnull
        private final RsPath myPath;

        public Reference(@Nonnull RsPath path) {
            super(path);
            myPath = path;
        }

        @Nonnull
        public RsPath getPath() {
            return myPath;
        }
    }

    public static class MethodImplementation extends RsFunctionUsage {
        @Nonnull
        private final RsFunction myOverriddenMethod;

        public MethodImplementation(@Nonnull RsFunction overriddenMethod) {
            super(overriddenMethod);
            myOverriddenMethod = overriddenMethod;
        }

        @Nonnull
        public RsFunction getOverriddenMethod() {
            return myOverriddenMethod;
        }
    }
}
