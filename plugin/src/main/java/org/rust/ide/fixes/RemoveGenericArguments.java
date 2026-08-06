/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsWrongGenericArgumentsOrderInspection;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsMethodOrPath;
import org.rust.lang.core.psi.RsPsiImplUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RemoveGenericArguments extends RsQuickFixBase<RsMethodOrPath> {

    private final int startIndex;
    private final int endIndex;

    public RemoveGenericArguments(@Nonnull RsMethodOrPath element, int startIndex, int endIndex) {
        super(element);
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.redundant.generic.arguments"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMethodOrPath element) {
        var result = org.rust.ide.inspections.RsWrongGenericArgumentsNumberInspection.getTypeArgumentsAndDeclaration(element);
        if (result == null) return;
        RsTypeArgumentList typeArguments = result.getFirst();
        if (typeArguments == null) return;
        removeTypeParameters(typeArguments);
    }

    private void removeTypeParameters(@Nonnull RsTypeArgumentList typeArguments) {
        List<PsiElement> combined = new ArrayList<>();
        combined.addAll(typeArguments.getTypeReferenceList());
        combined.addAll(typeArguments.getExprList());
        combined.sort(Comparator.comparingInt(e -> e.getTextOffset()));

        List<PsiElement> toRemove = combined.subList(startIndex, endIndex);
        for (PsiElement el : toRemove) {
            RsElementUtil.deleteWithSurroundingComma(el);
        }

        PsiElement nextSibling = typeArguments.getLt().getNextSibling();
        // Skip whitespace and comments
        while (nextSibling != null && !(nextSibling == typeArguments.getGt())) {
            if (nextSibling.getNode().getElementType().toString().equals("WHITE_SPACE")
                || nextSibling.getNode().getElementType().toString().startsWith("COMMENT")) {
                nextSibling = nextSibling.getNextSibling();
            } else {
                break;
            }
        }
        if (nextSibling == typeArguments.getGt()) {
            typeArguments.delete();
        }
    }
}
