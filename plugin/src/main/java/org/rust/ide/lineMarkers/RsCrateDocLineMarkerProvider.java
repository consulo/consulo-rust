/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.webBrowser.BrowserUtil;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.docs.RsDocumentationProvider;
import org.rust.ide.icons.RsIcons;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.ext.RsElementUtil;

import javax.swing.Icon;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsCrateDocLineMarkerProvider extends LineMarkerProviderDescriptor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("gutter.rust.open.documentation.name"));
    }

    @Nonnull
    @Override
    public consulo.ui.image.Image getIcon() {
        return RsIcons.DOCS_MARK;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsExternCrateItem)) return null;
        RsExternCrateItem externCrate = (RsExternCrateItem) parent;
        if (externCrate.getCrate() != element) return null;
        String crateName = externCrate.getName();
        if (crateName == null) return null;
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(externCrate);
        if (pkg == null) return null;
        CargoWorkspace.Target crate = pkg.findDependency(crateName);
        if (crate == null) return null;
        CargoWorkspace.Package cratePkg = crate.getPkg();
        if (cratePkg.getSource() == null) return null;

        String baseUrl = RsDocumentationProvider.getExternalDocumentationBaseUrl();
        String url = baseUrl + cratePkg.getName() + "/" + cratePkg.getVersion() + "/" + crate.getNormName();

        return RsLineMarkerInfoUtils.create(
            element,
            element.getTextRange(),
            getIcon(),
            (e, event) -> BrowserUtil.browse(url),
            GutterIconRenderer.Alignment.LEFT,
            () -> RsBundle.message("gutter.rust.open.documentation.for", cratePkg.getNormName())
        );
    }
}
