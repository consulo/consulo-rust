/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.toml.RsTomlBundle;
import org.toml.lang.psi.*;

import java.util.*;
import java.util.stream.Collectors;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class TomlDuplicatedKeyInspection extends TomlLocalInspectionToolBase {
    @Nonnull
    @Override
    protected TomlVisitor buildVisitorInternal(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new TomlVisitor() {
            @Override
            public void visitKeyValueOwner(@Nonnull TomlKeyValueOwner element) {
                List<TomlKeyValue> keyValues = element.getEntries();
                highlightDuplicates(keyValues);
            }

            @Override
            public void visitFile(@Nonnull PsiFile file) {
                List<TomlKeyValue> keyValues = new ArrayList<>();
                for (PsiElement child : file.getChildren()) {
                    if (child instanceof TomlKeyValue) {
                        keyValues.add((TomlKeyValue) child);
                    }
                }
                highlightDuplicates(keyValues);
            }

            private void highlightDuplicates(@Nonnull List<TomlKeyValue> entries) {
                Map<String, List<TomlKeyValue>> grouped = new HashMap<>();
                for (TomlKeyValue entry : entries) {
                    String key = entry.getKey().getSegments().stream()
                        .map(s -> s.getName() != null ? s.getName() : "")
                        .collect(Collectors.joining("."));
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
                }
                for (List<TomlKeyValue> group : grouped.values()) {
                    if (group.size() > 1) {
                        for (TomlKeyValue kv : group) {
                            holder.registerProblem(kv.getKey(),
                                RsTomlBundle.message("inspection.duplicated.key.problem"));
                        }
                    }
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.duplicated.key.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
