/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;

import javax.swing.*;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;

/**
 * Shows nav bar for items from structure view {@link RsStructureViewModel}
 */
@ExtensionImpl
public class RsNavBarModelExtension extends StructureAwareNavBarModelExtension {

    @Override
    @Nonnull
    protected Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Nullable
    protected StructureViewModel createModel(@Nonnull PsiFile file, @Nullable Editor editor) {
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
    public PsiElement getLeafElement(@Nonnull DataContext dataContext) {
        PsiElement leafElement = super.getLeafElement(dataContext);
        if (!(leafElement instanceof RsElement)) return null;
        if (new RsBreadcrumbsInfoProvider().getBreadcrumb((RsElement) leafElement) == null) return null;
        return leafElement;
    }

    @Nullable
    public consulo.ui.image.Image getIcon(@Nullable Object obj) {
        if (obj instanceof RsAbstractable) {
            RsAbstractable abstractable = (RsAbstractable) obj;
            // The code mostly copied from `NavBarPresentation.getIcon`. The only reason to override it here
            // is setting `allowNameResolution = false` in order to avoid UI freezes
            return ReadAction.compute(() -> {
                if (abstractable.isValid()) return consulo.language.icon.IconDescriptorUpdaters.getIcon(abstractable, 0);
                return null;
            });
        }
        return null;
    }
}
