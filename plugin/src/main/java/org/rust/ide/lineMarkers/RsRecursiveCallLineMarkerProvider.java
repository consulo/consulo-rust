/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.document.Document;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.DocumentExtUtil;

import javax.swing.Icon;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsRecursiveCallLineMarkerProvider extends LineMarkerProviderDescriptor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("gutter.rust.recursive.call.name"));
    }

    @Nonnull
    @Override
    public consulo.ui.image.Image getIcon() {
        return RsIcons.RECURSIVE_CALL;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@Nonnull List<PsiElement> elements, @Nonnull Collection<LineMarkerInfo> result) {
        Set<Integer> lines = new HashSet<>();

        for (PsiElement el : elements) {
            PsiElement parent = el.getParent();
            boolean isRecursive = false;

            if (parent instanceof RsMethodCall && el == ((RsMethodCall) parent).getIdentifier()) {
                RsReference ref = (RsReference) ((RsMethodCall) parent).getReference();
                isRecursive = isReferenceRecursive(ref, el);
            } else if (parent instanceof RsPath && el == ((RsPath) parent).getIdentifier()) {
                PsiElement expr = parent.getParent();
                if (expr instanceof RsPathExpr) {
                    PsiElement call = expr.getParent();
                    if (call instanceof RsCallExpr && ((RsCallExpr) call).getExpr() == expr) {
                        RsReference ref = (RsReference) ((RsPath) parent).getReference();
                        if (ref != null) {
                            isRecursive = isReferenceRecursive(ref, el);
                        }
                    }
                }
            }

            if (!isRecursive) continue;
            Document doc = DocumentExtUtil.getDocument(el.getContainingFile());
            if (doc == null) continue;
            int lineNumber = doc.getLineNumber(el.getTextOffset());
            if (!lines.contains(lineNumber)) {
                lines.add(lineNumber);
                result.add(RsLineMarkerInfoUtils.create(
                    el,
                    el.getTextRange(),
                    getIcon(),
                    null,
                    GutterIconRenderer.Alignment.RIGHT,
                    () -> RsBundle.message("gutter.rust.recursive.call.name")
                ));
            }
        }
    }

    private static boolean isReferenceRecursive(@Nonnull RsReference ref, @Nonnull PsiElement element) {
        PsiElement def = ref.resolve();
        if (def == null) return false;
        RsFunction enclosingFn = RsElementUtil.ancestorStrict(element, RsFunction.class);
        return enclosingFn != null && enclosingFn == def;
    }
}
