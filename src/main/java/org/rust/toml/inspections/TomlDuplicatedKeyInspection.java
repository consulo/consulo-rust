/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.RsTomlBundle;
import org.toml.lang.psi.*;

import java.util.*;
import java.util.stream.Collectors;

public class TomlDuplicatedKeyInspection extends TomlLocalInspectionToolBase {
    @NotNull
    @Override
    protected TomlVisitor buildVisitorInternal(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new TomlVisitor() {
            @Override
            public void visitKeyValueOwner(@NotNull TomlKeyValueOwner element) {
                List<TomlKeyValue> keyValues = element.getEntries();
                highlightDuplicates(keyValues);
            }

            @Override
            public void visitFile(@NotNull PsiFile file) {
                List<TomlKeyValue> keyValues = new ArrayList<>();
                for (PsiElement child : file.getChildren()) {
                    if (child instanceof TomlKeyValue) {
                        keyValues.add((TomlKeyValue) child);
                    }
                }
                highlightDuplicates(keyValues);
            }

            private void highlightDuplicates(@NotNull List<TomlKeyValue> entries) {
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
}
