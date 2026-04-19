/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.codeInsight.hints.HintInfo;
import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.Option;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

@SuppressWarnings("UnstableApiUsage")
public class RsInlayParameterHintsProvider implements InlayParameterHintsProvider {

    @NotNull
    @Override
    public List<Option> getSupportedOptions() {
        return Collections.singletonList(RsInlayParameterHints.smartOption);
    }

    @NotNull
    @Override
    public Set<String> getDefaultBlackList() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public HintInfo getHintInfo(@NotNull PsiElement element) {
        if (element instanceof RsCallExpr) {
            return resolveCall((RsCallExpr) element);
        }
        if (element instanceof RsMethodCall) {
            return resolveMethodCall((RsMethodCall) element);
        }
        return null;
    }

    @NotNull
    @Override
    public List<InlayInfo> getParameterHints(@NotNull PsiElement element) {
        return RsInlayParameterHints.provideHints(element);
    }

    @NotNull
    @Override
    public String getInlayPresentation(@NotNull String inlayText) {
        return inlayText;
    }

    @NotNull
    @Override
    public String getBlacklistExplanationHTML() {
        return RsBundle.message("code.vision.disable.hints.message").trim();
    }

    @Nullable
    private static HintInfo.MethodInfo resolveCall(@NotNull RsCallExpr call) {
        PsiElement expr = call.getExpr();
        if (!(expr instanceof RsPathExpr)) return null;
        RsPath path = ((RsPathExpr) expr).getPath();
        if (path.getReference() == null) return null;
        PsiElement resolved = path.getReference().resolve();
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction fn = (RsFunction) resolved;
        List<String> parameters = new ArrayList<>();
        for (RsValueParameter param : RsFunctionUtil.getValueParameters(fn)) {
            String patText = RsValueParameterUtil.getPatText(param);
            parameters.add(patText != null ? patText : "_");
        }
        return createMethodInfo(fn, parameters);
    }

    @Nullable
    private static HintInfo.MethodInfo resolveMethodCall(@NotNull RsMethodCall methodCall) {
        PsiElement resolved = methodCall.getReference().resolve();
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction fn = (RsFunction) resolved;
        List<String> parameters = new ArrayList<>();
        RsSelfParameter selfParam = RsFunctionUtil.getSelfParameter(fn);
        if (selfParam != null) {
            parameters.add(selfParam.getName());
        }
        for (RsValueParameter param : RsFunctionUtil.getValueParameters(fn)) {
            String patText = RsValueParameterUtil.getPatText(param);
            parameters.add(patText != null ? patText : "_");
        }
        return createMethodInfo(fn, parameters);
    }

    @Nullable
    private static HintInfo.MethodInfo createMethodInfo(@NotNull RsFunction function, @NotNull List<String> parameters) {
        String path = function.getQualifiedName();
        if (path == null) return null;
        return new HintInfo.MethodInfo(path, parameters, RsLanguage.INSTANCE);
    }
}
