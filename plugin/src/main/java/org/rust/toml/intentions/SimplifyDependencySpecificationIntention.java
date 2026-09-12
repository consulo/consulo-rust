/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.*;

import java.util.List;
import consulo.localize.LocalizeValue;

public class SimplifyDependencySpecificationIntention extends RsTomlElementBaseIntentionAction<SimplifyDependencySpecificationIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.simplify.dependency.specification"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nullable
    @Override
    protected Context findApplicableContextInternal(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        PsiElement replaced = ctx.myValue.replace(ctx.myVersion.copy());
        if (replaced != null) {
            editor.getCaretModel().moveToOffset(replaced.getTextRange().getEndOffset());
        }
    }

    public static class Context {
        private final TomlInlineTable myValue;
        private final TomlValue myVersion;

        public Context(@Nonnull TomlInlineTable value, @Nonnull TomlValue version) {
            myValue = value;
            myVersion = version;
        }
    }
}
