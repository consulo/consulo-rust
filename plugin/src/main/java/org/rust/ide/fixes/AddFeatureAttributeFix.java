/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;
import consulo.localize.LocalizeValue;

public class AddFeatureAttributeFix extends RsQuickFixBase<PsiElement> {
    private final String featureName;

    public AddFeatureAttributeFix(@Nonnull String featureName, @Nonnull PsiElement element) {
        super(element);
        this.featureName = featureName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.feature.attribute"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.feature", featureName));
    }

    @Nullable
    @Override
    public FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        addFeatureAttribute(project, element, featureName);
    }

    public static void addFeatureAttribute(@Nonnull Project project, @Nonnull PsiElement context, @Nonnull String featureName) {
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
