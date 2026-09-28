/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.value;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayOptionInfo;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.language.editor.inlay.DeclarativeInlayTreeSink;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPatRange;
import org.rust.lang.core.psi.RsRangeExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPatRangeUtil;
import org.rust.lang.core.psi.ext.impl.RsRangeExprUtil;
import org.rust.lang.core.psi.impl.RsFile;

import java.util.Set;

@ExtensionImpl
public class RsInlayValueHintsProvider implements DeclarativeInlayHintsProvider {

    public static final String PROVIDER_ID = "rust.values";

    private static final String OPTION_EXPRESSIONS = "rust.values.expressions";
    private static final String OPTION_PATTERNS = "rust.values.patterns";

    @Nullable
    @Override
    public DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        if (!(file instanceof RsFile) || DumbService.isDumb(file.getProject())) {
            return null;
        }
        return new Collector();
    }

    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.values"));
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.values"));
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.empty();
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.VALUES_GROUP;
    }

    @Override
    public Set<DeclarativeInlayOptionInfo> getOptions() {
        return Set.of(
            option(OPTION_EXPRESSIONS, "settings.rust.inlay.hints.for.exclusive.range.expressions"),
            option(OPTION_PATTERNS, "settings.rust.inlay.hints.for.exclusive.range.patterns")
        );
    }

    private static DeclarativeInlayOptionInfo option(String id, String bundleKey) {
        LocalizeValue name = LocalizeValue.of(RsBundle.message(bundleKey));
        return new DeclarativeInlayOptionInfo(id, true, name, name);
    }

    private static class Collector implements DeclarativeInlayHintsCollector.SharedBypassCollector {

        @Override
        public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsElement) || element instanceof RsMacroCall) return;

            sink.whenOptionEnabled(OPTION_EXPRESSIONS, () -> presentExpression(element, sink));
            sink.whenOptionEnabled(OPTION_PATTERNS, () -> presentPattern(element, sink));
        }

        private void presentExpression(@Nonnull PsiElement element, @Nonnull DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsRangeExpr rangeExpr)) return;
            if (!RsElementUtil.isEnabledByCfg(rangeExpr)) return;
            present(RsRangeExprUtil.getEnd(rangeExpr), RsRangeExprUtil.getOp(rangeExpr), sink);
        }

        private void presentPattern(@Nonnull PsiElement element, @Nonnull DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsPatRange patRange)) return;
            if (!RsElementUtil.isEnabledByCfg(patRange)) return;
            present(RsPatRangeUtil.getEnd(patRange), RsPatRangeUtil.getOp(patRange), sink);
        }

        private void present(@Nullable PsiElement end, @Nullable PsiElement op, @Nonnull DeclarativeInlayTreeSink sink) {
            if (end == null || op == null) return;
            if (RsElementUtil.getElementType(op) != RsElementTypes.DOTDOT) return;

            sink.addPresentation(
                new DeclarativeInlayPosition.InlineInlayPosition(end.getTextRange().getStartOffset(), false),
                builder -> builder.text("<"));
        }
    }
}
