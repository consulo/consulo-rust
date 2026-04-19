/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;

import javax.swing.*;

/**
 * Shows nav bar for items from structure view {@link RsStructureViewModel}
 */
public class RsNavBarModelExtension extends StructureAwareNavBarModelExtension {

    @Override
    @NotNull
    protected Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    @Nullable
    protected StructureViewModel createModel(@NotNull PsiFile file, @Nullable Editor editor) {
        if (!(file instanceof RsFile)) return null;
        return new RsStructureViewModel(editor, (RsFile) file, false);
    }

    @Override
    @Nullable
    public String getPresentableText(@Nullable Object item) {
        if (!(item instanceof RsElement)) return null;
        RsElement element = (RsElement) item;
        if (element instanceof RsFile) {
            return ((RsFile) element).getName();
        }

        RsBreadcrumbsInfoProvider provider = new RsBreadcrumbsInfoProvider();
        return provider.getBreadcrumb(element);
    }

    /**
     * When {@link #getPresentableText} returns null, {@link PsiElement#getText()} will be used, and we want to avoid it
     */
    @Override
    @Nullable
    public PsiElement getLeafElement(@NotNull DataContext dataContext) {
        PsiElement leafElement = super.getLeafElement(dataContext);
        if (!(leafElement instanceof RsElement)) return null;
        if (new RsBreadcrumbsInfoProvider().getBreadcrumb((RsElement) leafElement) == null) return null;
        return leafElement;
    }

    @Override
    @Nullable
    public Icon getIcon(@Nullable Object obj) {
        if (obj instanceof RsAbstractable) {
            RsAbstractable abstractable = (RsAbstractable) obj;
            // The code mostly copied from `NavBarPresentation.getIcon`. The only reason to override it here
            // is setting `allowNameResolution = false` in order to avoid UI freezes
            Icon icon = ReadAction.compute(() -> {
                if (abstractable.isValid()) return abstractable.getIcon(0, false);
                return null;
            });

            int maxDimension = JBUI.scale(16 * 2);
            if (icon != null && (icon.getIconHeight() > maxDimension || icon.getIconWidth() > maxDimension)) {
                icon = IconUtil.cropIcon(icon, maxDimension, maxDimension);
            }
            return icon;
        } else {
            return super.getIcon(obj);
        }
    }
}
