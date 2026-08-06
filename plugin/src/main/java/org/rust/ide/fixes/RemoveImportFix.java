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
import org.rust.ide.intentions.RemoveCurlyBracesIntention;
import org.rust.lang.core.psi.RsUseGroup;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

/**
 * Fix that removes a use speck or a whole use item.
 */
public class RemoveImportFix extends RsQuickFixBase<PsiElement> {

    public RemoveImportFix(@Nonnull PsiElement element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.unused.import"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        if (!(element instanceof RsElement)) return;
        deleteUseSpeckOrUseItem((RsElement) element);
    }

    private static void deleteUseSpeckOrUseItem(@Nonnull RsElement element) {
        PsiElement parent = element.getParent();
        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(element);

        if (parent instanceof RsUseGroup) {
            RsUseGroup useGroup = (RsUseGroup) parent;
            RsUseSpeck parentSpeck = RsUseSpeckUtil.getParentUseSpeck(useGroup);
            if (useGroup.getUseSpeckList().isEmpty()) {
                deleteUseSpeck(parentSpeck);
            } else {
                var ctx = RemoveCurlyBracesIntention.createContextIfCompatible(parentSpeck);
                if (ctx != null) {
                    RemoveCurlyBracesIntention.removeCurlyBracesFromUseSpeck(ctx);
                }
            }
        }
    }

    public static void deleteUseSpeck(@Nonnull RsUseSpeck useSpeck) {
        PsiElement parent = useSpeck.getParent();
        RsElement element = parent instanceof RsUseItem ? (RsElement) parent : useSpeck;
        deleteUseSpeckOrUseItem(element);
    }
}
