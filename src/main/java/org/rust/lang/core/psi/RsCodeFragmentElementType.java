/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.parser.RustParser;

public class RsCodeFragmentElementType extends ICodeFragmentElementType {

    @NotNull
    private final IElementType elementType;

    public RsCodeFragmentElementType(@NotNull IElementType elementType, @NotNull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
        this.elementType = elementType;
    }

    @Nullable
    @Override
    public ASTNode parseContents(@NotNull ASTNode chameleon) {
        if (!(chameleon instanceof TreeElement)) return null;
        var project = ((TreeElement) chameleon).getManager().getProject();
        var builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon);
        var root = new RustParser().parse(elementType, builder);
        return root.getFirstChildNode();
    }

    public static final RsCodeFragmentElementType EXPR =
        new RsCodeFragmentElementType(RsElementTypes.EXPRESSION_CODE_FRAGMENT_ELEMENT, "RS_EXPR_CODE_FRAGMENT");
    public static final RsCodeFragmentElementType STMT =
        new RsCodeFragmentElementType(RsElementTypes.STATEMENT_CODE_FRAGMENT_ELEMENT, "RS_STMT_CODE_FRAGMENT");
    public static final RsCodeFragmentElementType TYPE_REF =
        new RsCodeFragmentElementType(RsElementTypes.TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT, "RS_TYPE_REF_CODE_FRAGMENT");
    public static final RsCodeFragmentElementType TYPE_PATH =
        new RsCodeFragmentElementType(RsElementTypes.TYPE_PATH_CODE_FRAGMENT_ELEMENT, "RS_TYPE_PATH_CODE_FRAGMENT");
    public static final RsCodeFragmentElementType VALUE_PATH =
        new RsCodeFragmentElementType(RsElementTypes.VALUE_PATH_CODE_FRAGMENT_ELEMENT, "RS_VALUE_PATH_CODE_FRAGMENT");
    public static final RsCodeFragmentElementType REPL =
        new RsCodeFragmentElementType(RsElementTypes.REPL_CODE_FRAGMENT_ELEMENT, "RS_REPL_CODE_FRAGMENT");
}
