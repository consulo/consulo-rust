/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsNamedFieldDeclListUtil;
import org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.RsVisibilityOwnerUtil;
import org.rust.lang.core.resolve.ref.RsReferenceCoreUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.resolve.ref.RsReferenceExtUtil;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;

public class CreateStructFieldFromConstructorFix extends RsQuickFixBase<RsStructItem> {

    private final String fieldName;

    @SafeFieldForPreview
    private final Ty fieldType;

    private CreateStructFieldFromConstructorFix(@NotNull RsStructItem struct, @NotNull String fieldName, @NotNull Ty fieldType) {
        super(struct);
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.create.field");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsStructItem element) {
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
    public static CreateStructFieldFromConstructorFix tryCreate(@NotNull RsStructLiteralField field) {
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
    private static RsStructItem resolveToStructItem(@NotNull RsStructLiteralField field) {
        RsStructLiteral parentStructLiteral = RsStructLiteralFieldUtil.getParentStructLiteral(field);
        var resolved = parentStructLiteral.getPath().getReference() != null
            ? RsPathReferenceImpl.deepResolve(parentStructLiteral.getPath().getReference())
            : null;
        return resolved instanceof RsStructItem ? (RsStructItem) resolved : null;
    }

    private static boolean canUse(@NotNull Ty ty) {
        boolean result = ty.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty ty) {
                if (ty instanceof TyUnknown || ty instanceof TyTypeParameter || ty instanceof TyAnon
                    || ty instanceof TyInfer || ty instanceof TyProjection) {
                    return true;
                }
                return ty.superVisitWith(this);
            }

            @Override
            public boolean visitRegion(@NotNull Region region) {
                return !(region instanceof ReStatic);
            }
        });
        return !result;
    }
}
