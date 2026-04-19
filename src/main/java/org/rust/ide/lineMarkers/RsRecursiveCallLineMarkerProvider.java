/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

public class RsRecursiveCallLineMarkerProvider extends LineMarkerProviderDescriptor {

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("gutter.rust.recursive.call.name");
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RsIcons.RECURSIVE_CALL;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
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

    private static boolean isReferenceRecursive(@NotNull RsReference ref, @NotNull PsiElement element) {
        PsiElement def = ref.resolve();
        if (def == null) return false;
        RsFunction enclosingFn = RsElementUtil.ancestorStrict(element, RsFunction.class);
        return enclosingFn != null && enclosingFn == def;
    }
}
