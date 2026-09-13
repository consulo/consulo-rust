/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import consulo.language.editor.ReferenceImporter;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.inspections.RsUnresolvedReferenceInspection;
import org.rust.settings.RsCodeInsightSettings;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.imports.ImportUtil;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.CommandUtil;

import java.util.function.BooleanSupplier;

public class RsReferenceImporter implements ReferenceImporter {

    @Override
    public boolean autoImportReferenceAtCursor(@Nonnull Editor editor, @Nonnull PsiFile file) {
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    public BooleanSupplier computeAutoImportAtOffset(@Nonnull Editor editor, @Nonnull PsiFile file, int offset, boolean allowCaretNearReference) {
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

    public boolean isAddUnambiguousImportsOnTheFlyEnabled(@Nonnull PsiFile file) {
        return file instanceof RsFile && RsCodeInsightSettings.getInstance().addUnambiguousImportsOnTheFly;
    }
}
