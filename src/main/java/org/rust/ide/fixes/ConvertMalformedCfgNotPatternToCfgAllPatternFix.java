/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.stream.Collectors;

public class ConvertMalformedCfgNotPatternToCfgAllPatternFix extends RsQuickFixBase<RsMetaItem> {
    @IntentionName
    private final String myFixText;

    public ConvertMalformedCfgNotPatternToCfgAllPatternFix(@NotNull RsMetaItem element) {
        super(element);
        RsMetaItemArgs metaItemList = element.getMetaItemArgs();
        if (metaItemList != null) {
            myFixText = RsBundle.message("intention.name.convert.to", convertToAllPatternWithNegatedArguments(metaItemList));
        } else {
            myFixText = "";
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.not.b.cfg.pattern.to.all.not.not.b");
    }

    @NotNull
    @Override
    public String getText() {
        return myFixText;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMetaItem element) {
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

    @NotNull
    private String convertToAllPatternWithNegatedArguments(@NotNull RsMetaItemArgs metaItemList) {
        return metaItemList.getMetaItemList().stream()
            .map(it -> "not(" + it.getText() + ")")
            .collect(Collectors.joining(", ", "all(", ")"));
    }

    @Nullable
    public static ConvertMalformedCfgNotPatternToCfgAllPatternFix createIfCompatible(@NotNull PsiElement element) {
        if (!(element instanceof RsMetaItem)) return null;
        RsMetaItem metaItem = (RsMetaItem) element;
        if (metaItem.getMetaItemArgsList().size() > 1) {
            return new ConvertMalformedCfgNotPatternToCfgAllPatternFix(metaItem);
        }
        return null;
    }
}
