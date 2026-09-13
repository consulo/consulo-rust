/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsReplCodeFragment;
import org.rust.lang.core.psi.RsUseItem;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RsConsoleOneCommandContext {
    @Nonnull
    private final String command;
    @Nonnull
    private final Set<String> itemsNames;
    private final boolean containsUseDirective;

    public RsConsoleOneCommandContext(@Nonnull RsReplCodeFragment codeFragment) {
        this.itemsNames = codeFragment.getNamedElementsUnique().keySet();
        List<? extends PsiElement> elements = codeFragment.getStmtList();
        this.command = elements.stream()
            .map(PsiElement::getText)
            .collect(Collectors.joining("\n"));
        this.containsUseDirective = elements.stream().anyMatch(it -> it instanceof RsUseItem);
    }

    @Nonnull
    public String getCommand() {
        return command;
    }

    @Nonnull
    public Set<String> getItemsNames() {
        return itemsNames;
    }

    public boolean isContainsUseDirective() {
        return containsUseDirective;
    }
}
