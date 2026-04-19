/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.CallInfo;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsValueArgumentList;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about arguments from func/method declaration
 */
public class RsArgumentsDescription {
    private final String[] myArguments;
    private final String myPresentText;

    public RsArgumentsDescription(@NotNull String[] arguments) {
        this.myArguments = arguments;
        this.myPresentText = arguments.length == 0 ? "<no arguments>" : String.join(", ", arguments);
    }

    @NotNull
    public String[] getArguments() {
        return myArguments;
    }

    @NotNull
    public String getPresentText() {
        return myPresentText;
    }

    @Nullable
    public static RsArgumentsDescription findDescription(@NotNull RsValueArgumentList args) {
        PsiElement call = args.getParent();
        CallInfo callInfo;
        if (call instanceof RsCallExpr) {
            callInfo = CallInfo.resolve((RsCallExpr) call);
        } else if (call instanceof RsMethodCall) {
            callInfo = CallInfo.resolve((RsMethodCall) call);
        } else {
            callInfo = null;
        }
        if (callInfo == null) return null;

        List<String> params = new ArrayList<>();
        if (callInfo.getSelfParameter() != null && call instanceof RsCallExpr) {
            params.add(callInfo.getSelfParameter());
        }
        for (CallInfo.Parameter param : callInfo.getParameters()) {
            StringBuilder sb = new StringBuilder();
            if (param.getPattern() != null) {
                sb.append(param.getPattern()).append(": ");
            }
            sb.append(param.renderType());
            params.add(sb.toString());
        }
        return new RsArgumentsDescription(params.toArray(new String[0]));
    }
}
