/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.PsiDocCommentBase;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;
import java.util.Map;

/**
 * Psi element for <a href="https://doc.rust-lang.org/reference/comments.html#doc-comments">Rust documentation comments</a>
 */
public interface RsDocComment extends PsiDocCommentBase, RsElement {
    @Override
    @Nullable
    RsDocAndAttributeOwner getOwner();

    List<RsDocCodeFence> getCodeFences();

    List<RsDocLinkDefinition> getLinkDefinitions();

    Map<String, RsDocLinkDefinition> getLinkReferenceMap();
}
