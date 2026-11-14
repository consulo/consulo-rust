/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.macros.generated.RsGeneratedCode;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the items of parsed generated code into light elements: each struct whose name was generated from a source
 * element becomes a {@link RsLightStructItem}, its fields light fields. Done once here for every generator.
 */
public final class RsLightItems {
    private RsLightItems() {
    }

    @Nonnull
    public static List<RsExpandedElement> lighten(@Nonnull List<RsExpandedElement> elements, @Nonnull RsGeneratedCode code) {
        List<RsExpandedElement> result = new ArrayList<>(elements.size());
        for (RsExpandedElement element : elements) {
            PsiElement origin = element instanceof RsStructItem struct ? originOf(struct, code) : null;
            result.add(origin != null ? new RsLightStructItem((RsStructItem) element, origin, field -> originOf(field, code)) : element);
        }
        return result;
    }

    /** The source element the name of {@code element} was generated from, if any. */
    private static PsiElement originOf(@Nonnull PsiElement element, @Nonnull RsGeneratedCode code) {
        if (!(element instanceof RsNameIdentifierOwner owner) || owner.getNameIdentifier() == null) return null;
        return code.findTarget(owner.getNameIdentifier().getTextRange().getStartOffset());
    }
}
