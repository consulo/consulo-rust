/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;

public class AddFeatureAttributeFix extends RsQuickFixBase<PsiElement> {
    private final String featureName;

    public AddFeatureAttributeFix(@NotNull String featureName, @NotNull PsiElement element) {
        super(element);
        this.featureName = featureName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.feature.attribute");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.feature", featureName);
    }

    @Nullable
    @Override
    public FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        addFeatureAttribute(project, element, featureName);
    }

    public static void addFeatureAttribute(@NotNull Project project, @NotNull PsiElement context, @NotNull String featureName) {
        RsElement rsElement = RsPsiJavaUtil.ancestorOrSelf(context, RsElement.class);
        if (rsElement == null) return;
        var mod = rsElement.getCrateRoot();
        if (mod == null) return;

        List<RsInnerAttr> attrs = RsPsiJavaUtil.childrenOfType(mod, RsInnerAttr.class);
        RsInnerAttr lastFeatureAttribute = null;
        for (RsInnerAttr attr : attrs) {
            if ("feature".equals(attr.getMetaItem().getName())) {
                lastFeatureAttribute = attr;
            }
        }

        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsInnerAttr attr = psiFactory.createInnerAttr("feature(" + featureName + ")");
        if (lastFeatureAttribute != null) {
            mod.addAfter(attr, lastFeatureAttribute);
        } else {
            PsiElement insertedElement = mod.addBefore(attr, mod.getFirstChild());
            mod.addAfter(psiFactory.createNewline(), insertedElement);
        }
    }
}
