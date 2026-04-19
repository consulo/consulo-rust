/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import com.intellij.codeInsight.daemon.ReferenceImporter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.inspections.RsUnresolvedReferenceInspection;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportUtil;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.CommandUtil;

import java.util.function.BooleanSupplier;

public class RsReferenceImporter implements ReferenceImporter {

    @Override
    public boolean autoImportReferenceAtCursor(@NotNull Editor editor, @NotNull PsiFile file) {
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    @Override
    public BooleanSupplier computeAutoImportAtOffset(@NotNull Editor editor, @NotNull PsiFile file, int offset, boolean allowCaretNearReference) {
        PsiReference reference = file.findReferenceAt(offset);
        if (reference == null) return null;
        if (!(reference.getElement() instanceof RsElement)) return null;
        RsElement element = (RsElement) reference.getElement();

        AutoImportFix.Context context;
        if (element instanceof RsPath) {
            RsUnresolvedReferenceInspection.PathInfo pathInfo = RsUnresolvedReferenceInspection.processPath((RsPath) element);
            context = pathInfo != null ? pathInfo.myContext : null;
        } else if (element instanceof RsMethodCall) {
            context = AutoImportFix.findApplicableContext((RsMethodCall) element);
        } else {
            return null;
        }
        if (context == null) return null;
        java.util.List<ImportCandidate> candidates = context.getCandidates();
        if (candidates.size() != 1) return null;
        ImportCandidate candidate = candidates.get(0);

        if (RsUnresolvedReferenceInspection.shouldIgnoreUnresolvedReference(element)) return null;

        return () -> {
            CommandUtil.runUndoTransparentWriteCommandAction(file.getProject(), () -> {
                ImportUtil.doImport(candidate, element);
            });
            return true;
        };
    }

    @Override
    public boolean isAddUnambiguousImportsOnTheFlyEnabled(@NotNull PsiFile file) {
        return file instanceof RsFile && RsCodeInsightSettings.getInstance().addUnambiguousImportsOnTheFly;
    }
}
