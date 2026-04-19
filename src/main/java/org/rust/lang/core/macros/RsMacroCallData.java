/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RsMacroCallData {
    private final MacroCallBody myMacroBody;
    private final Map<String, String> myEnv;

    public RsMacroCallData(@NotNull MacroCallBody macroBody, @NotNull Map<String, String> env) {
        myMacroBody = macroBody;
        myEnv = env;
    }

    @NotNull
    public MacroCallBody getMacroBody() {
        return myMacroBody;
    }

    @NotNull
    public Map<String, String> getEnv() {
        return myEnv;
    }

    @Nullable
    public static RsMacroCallData fromPsi(@NotNull RsPossibleMacroCall call) {
        MacroCallBody macroBody = RsPossibleMacroCallUtil.getMacroBody(call);
        if (macroBody == null) return null;
        boolean isCompletion = CompletionUtil.getOriginalElement(call) != null;
        Map<String, String> packageEnv = RsElementUtil.getContainingCargoPackage(call) != null
            ? RsElementUtil.getContainingCargoPackage(call).getEnv()
            : Collections.emptyMap();
        Map<String, String> env;
        if (isCompletion) {
            env = new HashMap<>(packageEnv);
            env.put("RUST_IDE_PROC_MACRO_COMPLETION", "1");
            env.put("RUST_IDE_PROC_MACRO_COMPLETION_DUMMY_IDENTIFIER", CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED);
        } else {
            env = packageEnv;
        }
        return new RsMacroCallData(macroBody, env);
    }
}
