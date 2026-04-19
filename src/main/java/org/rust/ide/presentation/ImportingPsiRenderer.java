/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.lang.core.types.Substitution;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A PSI renderer that also collects import candidates while rendering.
 * Used primarily for generating trait member implementations where we need
 * to know which types need to be imported.
 */
public class ImportingPsiRenderer extends TypeSubstitutingPsiRenderer {

    private final Set<ImportCandidate> itemsToImport = new HashSet<>();
    @Nullable
    private final PsiElement context;

    public ImportingPsiRenderer(
        @NotNull PsiRenderingOptions options,
        @NotNull List<Substitution> substitutions,
        @Nullable PsiElement context
    ) {
        super(options, substitutions.isEmpty() ? new Substitution() : substitutions.get(0));
        this.context = context;
    }

    @NotNull
    public Set<ImportCandidate> getItemsToImport() {
        return Collections.unmodifiableSet(itemsToImport);
    }
}
