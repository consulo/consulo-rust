/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import consulo.language.editor.inlay.HintInfo;
import consulo.language.editor.inlay.InlayInfo;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.inlay.Option;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

@SuppressWarnings("UnstableApiUsage")
public class RsInlayParameterHintsProvider implements InlayParameterHintsProvider {

    @Nonnull
    @Override
    public List<Option> getSupportedOptions() {
        return Collections.singletonList(RsInlayParameterHints.smartOption);
    }

    @Nonnull
    @Override
    public Set<String> getDefaultBlackList() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public HintInfo getHintInfo(@Nonnull PsiElement element) {
        if (element instanceof RsCallExpr) {
            return resolveCall((RsCallExpr) element);
        }
        if (element instanceof RsMethodCall) {
            return resolveMethodCall((RsMethodCall) element);
        }
        return null;
    }

    @Nonnull
    @Override
    public List<InlayInfo> getParameterHints(@Nonnull PsiElement element) {
        return RsInlayParameterHints.provideHints(element);
    }

    @Nonnull
    @Override
    public String getInlayPresentation(@Nonnull String inlayText) {
        return inlayText;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getBlacklistExplanationHTML() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("code.vision.disable.hints.message").trim());
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getPreviewFileText() {
        return consulo.localize.LocalizeValue.empty();
    }

    @Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }

    @Nullable
    private static HintInfo.MethodInfo resolveCall(@Nonnull RsCallExpr call) {
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
    private static HintInfo.MethodInfo resolveMethodCall(@Nonnull RsMethodCall methodCall) {
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
    private static HintInfo.MethodInfo createMethodInfo(@Nonnull RsFunction function, @Nonnull List<String> parameters) {
        String path = function.getQualifiedName();
        if (path == null) return null;
        return new HintInfo.MethodInfo(path, parameters, RsLanguage.INSTANCE);
    }
}
