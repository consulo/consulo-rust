/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiFileRange;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.annotator.format.FormatParameter;
import org.rust.ide.annotator.format.FormatParameterUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.openapiext.SmartPointerExtUtil;
import org.rust.openapiext.DocumentExtUtil;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;

import java.util.Objects;

public class DeriveDebugAndChangeFormatToDebugFix extends RsQuickFixBase<RsExpr> {

    @Nls
    private final String _text;

    @SafeFieldForPreview
    private final SmartPsiFileRange argument;

    public DeriveDebugAndChangeFormatToDebugFix(@NotNull RsExpr expr, @NotNull FormatParameter.Value parameter) {
        super(expr);
        String baseName = Objects.requireNonNull(FormatParameterUtil.baseType(RsTypesUtil.getType(expr))).getName();
        this._text = RsBundle.message(
            "intention.name.derive.debug.and.replace.display.to.debug",
            baseName
        );
        this.argument = SmartPointerManager.getInstance(expr.getProject())
            .createSmartPsiFileRangePointer(expr.getContainingFile(), parameter.getRange());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return _text;
    }

    @NotNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        var adt = SmartPointerExtUtil.createSmartPointer(
            RsElementUtil.findPreviewCopyIfNeeded(
                Objects.requireNonNull(FormatParameterUtil.baseType(RsTypesUtil.getType(element)))
            )
        );
        com.intellij.openapi.util.Segment segment = argument.getRange();
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
