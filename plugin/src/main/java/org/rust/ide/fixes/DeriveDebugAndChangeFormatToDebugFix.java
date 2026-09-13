/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiFileRange;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.format.FormatParameter;
import org.rust.lang.core.format.FormatParameterUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.openapiext.SmartPointerExtUtil;
import org.rust.openapiext.DocumentExtUtil;

import consulo.document.Document;
import consulo.document.util.TextRange;

import java.util.Objects;
import consulo.document.util.Segment;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;

public class DeriveDebugAndChangeFormatToDebugFix extends RsQuickFixBase<RsExpr> {

    
    private final String _text;

    @SafeFieldForPreview
    private final SmartPsiFileRange argument;

    public DeriveDebugAndChangeFormatToDebugFix(@Nonnull RsExpr expr, @Nonnull FormatParameter.Value parameter) {
        super(expr);
        String baseName = Objects.requireNonNull(FormatParameterUtil.baseType(RsTypesUtil.getType(expr))).getName();
        this._text = RsBundle.message(
            "intention.name.derive.debug.and.replace.display.to.debug",
            baseName
        );
        this.argument = SmartPointerManager.getInstance(expr.getProject())
            .createSmartPsiFileRangePointer(expr.getContainingFile(), parameter.getRange());
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        var adt = SmartPointerExtUtil.createSmartPointer(
            RsElementUtil.findPreviewCopyIfNeeded(
                Objects.requireNonNull(FormatParameterUtil.baseType(RsTypesUtil.getType(element)))
            )
        );
        consulo.document.util.Segment segment = argument.getRange();
        if (segment == null) return;
        TextRange range = TextRange.create(segment.getStartOffset(), segment.getEndOffset());
        Document document = DocumentExtUtil.getDocument(element.getContainingFile());
        if (document == null) return;
        document.replaceString(range.getStartOffset(), range.getEndOffset(), "{:?}");
        PsiDocumentManager.getInstance(project).commitDocument(document);
        var adtElement = adt.getElement();
        if (adtElement == null) return;
        DeriveTraitsFix.invokeStatic((org.rust.lang.core.psi.ext.RsStructOrEnumItemElement) adtElement, "Debug");
    }
}
