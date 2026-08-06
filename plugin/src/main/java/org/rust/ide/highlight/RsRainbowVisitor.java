/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.language.editor.rawHighlight.RainbowVisitor;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.*;

public class RsRainbowVisitor extends RainbowVisitor {
    public boolean suitableForFile(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Nonnull
    @Override
    public HighlightVisitor clone() {
        return new RsRainbowVisitor();
    }

    @Override
    public void visit(@Nonnull PsiElement function) {
        if (!(function instanceof RsFunction)) return;

        List<RsPatBinding> allBindings = new ArrayList<>();
        for (RsPatBinding binding : RsElementUtil.descendantsOfType(function, RsPatBinding.class)) {
            if (binding.getName() != null) {
                allBindings.add(binding);
            }
        }

        Map<String, List<RsPatBinding>> byName = new HashMap<>();
        for (RsPatBinding binding : allBindings) {
            byName.computeIfAbsent(binding.getName(), k -> new ArrayList<>()).add(binding);
        }

        Map<RsPatBinding, String> bindingToUniqueName = new LinkedHashMap<>();
        for (RsPatBinding binding : allBindings) {
            String name = binding.getName();
            List<RsPatBinding> group = byName.get(name);
            bindingToUniqueName.put(binding, name + "#" + group.indexOf(binding));
        }

        for (Map.Entry<RsPatBinding, String> entry : bindingToUniqueName.entrySet()) {
            RsPatBinding binding = entry.getKey();
            String name = entry.getValue();
            addInfo(getInfo(function, binding.getReferenceNameElement(), name, null));
        }

        for (RsPath path : RsElementUtil.descendantsOfType(function, RsPath.class)) {
            if (path.getReference() == null) continue;
            PsiElement target = path.getReference().resolve();
            if (!(target instanceof RsPatBinding)) continue;
            String colorTag = bindingToUniqueName.get(target);
            if (colorTag == null) return;
            PsiElement ident = path.getReferenceNameElement();
            if (ident == null) return;
            addInfo(getInfo(function, ident, colorTag, null));
        }
    }
}
