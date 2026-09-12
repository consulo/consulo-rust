/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;

import java.util.List;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.TyUtil;
import consulo.localize.LocalizeValue;

public class DeriveCopyFix extends RsQuickFixBase<RsPathExpr> {

    public DeriveCopyFix(@Nonnull RsPathExpr element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getName();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.derive.copy.trait"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPathExpr element) {
        Ty type = RsTypesUtil.getType(element);
        if (!(type instanceof TyAdt)) return;
        TyAdt adtType = (TyAdt) type;
        RsItemElement item = RsElementUtil.findPreviewCopyIfNeeded(adtType.getItem());

        ImplLookup implLookup = ImplLookup.relativeTo(item);
        boolean isCloneImplemented = implLookup.isClone(adtType).isTrue();

        String traits = isCloneImplemented ? "Copy" : "Clone, Copy";
        DeriveTraitsFix.invokeStatic((RsStructOrEnumItemElement) item, traits);
    }

    @Nullable
    public static DeriveCopyFix createIfCompatible(@Nonnull RsElement element) {
        if (!(element instanceof RsPathExpr)) return null;
        RsPathExpr pathExpr = (RsPathExpr) element;
        Ty type = RsTypesUtil.getType(pathExpr);
        if (!(type instanceof TyAdt)) return null;
        TyAdt adtType = (TyAdt) type;
        if (RsElementUtil.getContainingCrate(adtType.getItem()).getOrigin() != PackageOrigin.WORKSPACE) return null;

        ImplLookup implLookup = ImplLookup.relativeTo(element);

        if (adtType.getItem() instanceof RsStructItem) {
            List<Ty> fieldTypes = RsFieldDeclUtil.getFieldTypes((RsStructItem) adtType.getItem());
            for (Ty ft : fieldTypes) {
                if (TyUtil.isMovesByDefault(ft, implLookup)) return null;
            }
        } else if (adtType.getItem() instanceof RsEnumItem) {
            for (RsEnumVariant variant : ((RsEnumItem) adtType.getItem()).getEnumBody().getEnumVariantList()) {
                List<Ty> fieldTypes = RsFieldDeclUtil.getFieldTypes(variant);
                for (Ty ft : fieldTypes) {
                    if (TyUtil.isMovesByDefault(ft, implLookup)) return null;
                }
            }
        }

        return new DeriveCopyFix(pathExpr);
    }
}
