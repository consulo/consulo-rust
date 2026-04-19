/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.*;

import java.util.List;

public class SimplifyDependencySpecificationIntention extends RsTomlElementBaseIntentionAction<SimplifyDependencySpecificationIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.simplify.dependency.specification");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Nullable
    @Override
    protected Context findApplicableContextInternal(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!Util.isCargoToml(element.getContainingFile())) return null;

        TomlKeyValue dependency = null;
        PsiElement current = element;
        while (current != null) {
            if (current instanceof TomlKeyValue) {
                TomlKeyValue kv = (TomlKeyValue) current;
                if (kv.getParent() instanceof TomlTable) {
                    TomlTable table = (TomlTable) kv.getParent();
                    if (Util.isDependencyListHeader(table.getHeader())) {
                        dependency = kv;
                        break;
                    }
                }
            }
            current = current.getParent();
        }
        if (dependency == null) return null;

        PsiElement depValue = dependency.getValue();
        if (!(depValue instanceof TomlInlineTable)) return null;
        TomlInlineTable dependencyValue = (TomlInlineTable) depValue;
        List<TomlKeyValue> entries = dependencyValue.getEntries();
        if (entries.size() != 1) return null;
        TomlKeyValue singleEntry = entries.get(0);
        List<TomlKeySegment> segments = singleEntry.getKey().getSegments();
        if (segments.size() != 1) return null;
        if (!"version".equals(segments.get(0).getText())) return null;
        TomlValue version = singleEntry.getValue();
        if (version == null) return null;

        return new Context(dependencyValue, version);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        PsiElement replaced = ctx.myValue.replace(ctx.myVersion.copy());
        if (replaced != null) {
            editor.getCaretModel().moveToOffset(replaced.getTextRange().getEndOffset());
        }
    }

    public static class Context {
        private final TomlInlineTable myValue;
        private final TomlValue myVersion;

        public Context(@NotNull TomlInlineTable value, @NotNull TomlValue version) {
            myValue = value;
            myVersion = version;
        }
    }
}
