/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsFunctionUsage extends UsageInfo {
    @NotNull
    private final RsElement myElement;

    protected RsFunctionUsage(@NotNull RsElement element) {
        super(element);
        myElement = element;
    }

    @NotNull
    public RsElement getUsageElement() {
        return myElement;
    }

    public boolean isCallUsage() {
        return false;
    }

    public static class FunctionCall extends RsFunctionUsage {
        @NotNull
        private final RsCallExpr myCall;

        public FunctionCall(@NotNull RsCallExpr call) {
            super(call);
            myCall = call;
        }

        @NotNull
        public RsCallExpr getCall() {
            return myCall;
        }

        @Override
        public boolean isCallUsage() {
            return true;
        }
    }

    public static class MethodCall extends RsFunctionUsage {
        @NotNull
        private final RsMethodCall myCall;

        public MethodCall(@NotNull RsMethodCall call) {
            super(call);
            myCall = call;
        }

        @NotNull
        public RsMethodCall getCall() {
            return myCall;
        }

        @Override
        public boolean isCallUsage() {
            return true;
        }
    }

    public static class Reference extends RsFunctionUsage {
        @NotNull
        private final RsPath myPath;

        public Reference(@NotNull RsPath path) {
            super(path);
            myPath = path;
        }

        @NotNull
        public RsPath getPath() {
            return myPath;
        }
    }

    public static class MethodImplementation extends RsFunctionUsage {
        @NotNull
        private final RsFunction myOverriddenMethod;

        public MethodImplementation(@NotNull RsFunction overriddenMethod) {
            super(overriddenMethod);
            myOverriddenMethod = overriddenMethod;
        }

        @NotNull
        public RsFunction getOverriddenMethod() {
            return myOverriddenMethod;
        }
    }
}
