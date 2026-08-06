/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTraitAlias;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;

/**
 * Maps Rust PSI element classes to icons. Consulo calls this via the
 * {@code consulo.language.icon.IconDescriptorUpdater} extension point whenever an icon is needed.
 *
 * Replaces the IntelliJ pattern {@code psiElement.getIcon(flags)} which relied on
 * {@code PsiElement implements Iconable} — not the case in Consulo.
 */
@ExtensionImpl
public class RsIconDescriptorUpdater implements IconDescriptorUpdater {

    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
        if (element instanceof RsFunction) {
            iconDescriptor.setMainIcon(RsIcons.FUNCTION);
        } else if (element instanceof RsStructItem) {
            iconDescriptor.setMainIcon(RsIcons.STRUCT);
        } else if (element instanceof RsTraitItem) {
            iconDescriptor.setMainIcon(RsIcons.TRAIT);
        } else if (element instanceof RsTraitAlias) {
            iconDescriptor.setMainIcon(RsIcons.TYPE_ALIAS);
        } else if (element instanceof RsEnumItem) {
            iconDescriptor.setMainIcon(RsIcons.ENUM);
        } else if (element instanceof RsEnumVariant) {
            iconDescriptor.setMainIcon(RsIcons.ENUM_VARIANT);
        } else if (element instanceof RsImplItem) {
            iconDescriptor.setMainIcon(RsIcons.IMPL);
        } else if (element instanceof RsModItem || element instanceof RsModDeclItem) {
            iconDescriptor.setMainIcon(RsIcons.MODULE);
        } else if (element instanceof RsConstant) {
            iconDescriptor.setMainIcon(RsIcons.CONSTANT);
        } else if (element instanceof RsTypeAlias) {
            iconDescriptor.setMainIcon(RsIcons.TYPE_ALIAS);
        } else if (element instanceof RsMacro) {
            iconDescriptor.setMainIcon(RsIcons.MACRO);
        } else if (element instanceof RsFieldDecl) {
            iconDescriptor.setMainIcon(RsIcons.FIELD);
        } else if (element instanceof RsPatBinding) {
            iconDescriptor.setMainIcon(RsIcons.BINDING);
        }
    }
}
