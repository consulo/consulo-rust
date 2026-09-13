/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionUtilCore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RsMacroCallData {
    private final MacroCallBody myMacroBody;
    private final Map<String, String> myEnv;

    public RsMacroCallData(@Nonnull MacroCallBody macroBody, @Nonnull Map<String, String> env) {
        myMacroBody = macroBody;
        myEnv = env;
    }

    @Nonnull
    public MacroCallBody getMacroBody() {
        return myMacroBody;
    }

    @Nonnull
    public Map<String, String> getEnv() {
        return myEnv;
    }

    @Nullable
    public static RsMacroCallData fromPsi(@Nonnull RsPossibleMacroCall call) {
        MacroCallBody macroBody = RsPossibleMacroCallUtil.getMacroBody(call);
        if (macroBody == null) return null;
        boolean isCompletion = CompletionUtilCore.getOriginalElement(call) != null;
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
