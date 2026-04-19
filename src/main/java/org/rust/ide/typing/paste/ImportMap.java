/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import com.intellij.psi.PsiElement;

import java.util.Map;

/**
 * Maps text ranges in a copy-pasted region to qualified paths that can be used to resolve proper imports.
 * The range offsets are relative to the start of the copy-pasted region.
 */
public final class ImportMap {
    private final Map<Integer, QualifiedItemPath> myOffsetToFqnMap;

    public ImportMap(Map<Integer, QualifiedItemPath> offsetToFqnMap) {
        myOffsetToFqnMap = offsetToFqnMap;
    }

    public QualifiedItemPath elementToFqn(PsiElement element, int importOffset) {
        int relativeEndOffset = element.getTextRange().getEndOffset() - importOffset;
        return myOffsetToFqnMap.get(relativeEndOffset);
    }
}
