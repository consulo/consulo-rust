/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.ast.ICodeFragmentElementType;
import consulo.language.impl.ast.TreeElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.psi.*;

public class RsCodeFragmentElementType extends ICodeFragmentElementType {

    @Nonnull
    private final IElementType elementType;

    public RsCodeFragmentElementType(@Nonnull IElementType elementType, @Nonnull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
        this.elementType = elementType;
    }

    @Nullable
    @Override
    public ASTNode parseContents(@Nonnull ASTNode chameleon) {
        if (!(chameleon instanceof TreeElement)) return null;
        var project = ((TreeElement) chameleon).getManager().getProject();
        var version = org.rust.lang.RsLanguage.INSTANCE.getVersions()[0];
        var builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, version);
        var root = new RustParser().parse(elementType, builder, version);
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
