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
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.stream.Collectors;

public class ConvertMalformedCfgNotPatternToCfgAllPatternFix extends RsQuickFixBase<RsMetaItem> {
    
    private final String myFixText;

    public ConvertMalformedCfgNotPatternToCfgAllPatternFix(@Nonnull RsMetaItem element) {
        super(element);
        RsMetaItemArgs metaItemList = element.getMetaItemArgs();
        if (metaItemList != null) {
            myFixText = RsBundle.message("intention.name.convert.to", convertToAllPatternWithNegatedArguments(metaItemList));
        } else {
            myFixText = "";
        }
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.not.b.cfg.pattern.to.all.not.not.b"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(myFixText);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMetaItem element) {
        RsMetaItemArgs metaItemList = element.getMetaItemArgs();
        if (metaItemList == null) return;

        RsPsiFactory factory = new RsPsiFactory(project);
        RsMetaItem newItem = factory.createMetaItem(convertToAllPatternWithNegatedArguments(metaItemList));
        RsMetaItem replaced = (RsMetaItem) element.replace(newItem);

        if (replaced.getMetaItemArgs() != null && replaced.getMetaItemArgs().getLparen() != null) {
            int offset = replaced.getMetaItemArgs().getLparen().getTextOffset();
            if (editor != null) {
                editor.getCaretModel().moveToOffset(offset + 1);
            }
        }
    }

    @Nonnull
    private String convertToAllPatternWithNegatedArguments(@Nonnull RsMetaItemArgs metaItemList) {
        return metaItemList.getMetaItemList().stream()
            .map(it -> "not(" + it.getText() + ")")
            .collect(Collectors.joining(", ", "all(", ")"));
    }

    @Nullable
    public static ConvertMalformedCfgNotPatternToCfgAllPatternFix createIfCompatible(@Nonnull PsiElement element) {
        if (!(element instanceof RsMetaItem)) return null;
        RsMetaItem metaItem = (RsMetaItem) element;
        if (metaItem.getMetaItemArgsList().size() > 1) {
            return new ConvertMalformedCfgNotPatternToCfgAllPatternFix(metaItem);
        }
        return null;
    }
}
