/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsNamedFieldDeclListUtil;
import org.rust.lang.core.psi.ext.impl.RsStructLiteralFieldUtil;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.impl.RsVisibilityOwnerUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;

public class CreateStructFieldFromConstructorFix extends RsQuickFixBase<RsStructItem> {

    private final String fieldName;

    @SafeFieldForPreview
    private final Ty fieldType;

    private CreateStructFieldFromConstructorFix(@Nonnull RsStructItem struct, @Nonnull String fieldName, @Nonnull Ty fieldType) {
        super(struct);
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.field"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsStructItem element) {
        boolean pub = RsVisibilityUtil.getVisibility(element) == RsVisibility.Public.INSTANCE;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsBlockFields structBlockFields = element.getBlockFields();
        if (structBlockFields != null) {
            if (structBlockFields.getRbrace() == null) return;
            RsNamedFieldDeclListUtil.ensureTrailingComma(structBlockFields.getNamedFieldDeclList());
            RsBlockFields created = psiFactory.createBlockFields(
                Collections.singletonList(new RsPsiFactory.BlockField(fieldName, fieldType, pub))
            );
            structBlockFields.addBefore(created.getChildren()[0], structBlockFields.getRbrace());
            structBlockFields.addBefore(psiFactory.createComma(), structBlockFields.getRbrace());
        } else {
            if (element.getIdentifier() == null) return;
            RsBlockFields blockFields = (RsBlockFields) element.addAfter(
                psiFactory.createBlockFields(
                    Collections.singletonList(new RsPsiFactory.BlockField(fieldName, fieldType, pub))
                ),
                element.getIdentifier()
            );
            if (element.getSemicolon() != null) {
                element.getSemicolon().delete();
            }
            blockFields.addBefore(psiFactory.createComma(), blockFields.getRbrace());
        }
    }

    @Nullable
    public static CreateStructFieldFromConstructorFix tryCreate(@Nonnull RsStructLiteralField field) {
        if (field.getIdentifier() == null) return null;
        String fieldName = field.getIdentifier().getText();
        Ty fieldType = RsTypesUtil.getType(field);
        if (!canUse(fieldType)) return null;
        RsStructItem struct = resolveToStructItem(field);
        if (struct == null) return null;
        if (struct.getTupleFields() != null) return null;
        RsBlockFields structBlockFields = struct.getBlockFields();
        if (structBlockFields != null && structBlockFields.getRbrace() == null || struct.getIdentifier() == null) return null;
        return new CreateStructFieldFromConstructorFix(struct, fieldName, fieldType);
    }

    @Nullable
    private static RsStructItem resolveToStructItem(@Nonnull RsStructLiteralField field) {
        RsStructLiteral parentStructLiteral = RsStructLiteralFieldUtil.getParentStructLiteral(field);
        var resolved = parentStructLiteral.getPath().getReference() != null
            ? RsPathReferenceImpl.deepResolve(parentStructLiteral.getPath().getReference())
            : null;
        return resolved instanceof RsStructItem ? (RsStructItem) resolved : null;
    }

    private static boolean canUse(@Nonnull Ty ty) {
        boolean result = ty.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty ty) {
                if (ty instanceof TyUnknown || ty instanceof TyTypeParameter || ty instanceof TyAnon
                    || ty instanceof TyInfer || ty instanceof TyProjection) {
                    return true;
                }
                return ty.superVisitWith(this);
            }

            @Override
            public boolean visitRegion(@Nonnull Region region) {
                return !(region instanceof ReStatic);
            }
        });
        return !result;
    }
}
