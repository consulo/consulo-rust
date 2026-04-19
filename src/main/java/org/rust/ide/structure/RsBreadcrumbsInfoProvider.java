/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Arrays;
import java.util.List;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsBreadcrumbsInfoProvider implements BreadcrumbsProvider {

    private static final int INFO_MAX_TEXT_LENGTH = 16;
    private static final String ELLIPSIS = "\u2026";
    private static final RsLanguage[] LANGUAGES = {RsLanguage.INSTANCE};

    public static final Class<? extends RsElement>[] NAMED_SUITABLE_CLASSES = createSuitableClasses();

    @SuppressWarnings("unchecked")
    private static Class<? extends RsElement>[] createSuitableClasses() {
        return new Class[]{
            RsStructOrEnumItemElement.class,
            RsTraitItem.class,
            RsModItem.class,
            RsModDeclItem.class,
            RsExternCrateItem.class,
            RsConstant.class,
            RsTypeAlias.class,
            RsTraitAlias.class,
            RsEnumVariant.class,
            RsNamedFieldDecl.class,
        };
    }

    private interface ElementHandler<T extends RsElement> {
        boolean accepts(@NotNull PsiElement e);

        @NotNull
        String elementInfo(@NotNull T e);
    }

    private static final ElementHandler<RsNamedElement> NAMED_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            for (Class<? extends RsElement> clazz : NAMED_SUITABLE_CLASSES) {
                if (clazz.isInstance(e)) return true;
            }
            return false;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsNamedElement e) {
            String name = e.getName();
            return name != null ? name : "";
        }
    };

    private static final ElementHandler<RsImplItem> IMPL_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsImplItem;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsImplItem e) {
            String typeName = null;
            RsTypeReference typeReference = e.getTypeReference();
            if (typeReference != null) {
                RsTypeReference skipped = RsTypeReferenceUtil.skipParens(typeReference);
                if (skipped instanceof RsPathType) {
                    typeName = ((RsPathType) skipped).getPath().getReferenceName();
                }
                if (typeName == null) {
                    typeName = typeReference.getText();
                }
            }
            if (typeName == null) return "";

            RsTraitRef traitRef = e.getTraitRef();
            String traitName = traitRef != null ? traitRef.getPath().getReferenceName() : null;
            String start = traitName != null ? traitName + " for" : "impl";

            return start + " " + typeName;
        }
    };

    private static final ElementHandler<RsBlockExpr> BLOCK_EXPR_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            if (!(e instanceof RsBlockExpr)) return false;
            RsBlockExpr blockExpr = (RsBlockExpr) e;
            return blockExpr.getParent() instanceof RsLetDecl
                || (!DumbService.isDumb(blockExpr.getProject()) && RsExprUtil.isTailExpr(blockExpr));
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsBlockExpr e) {
            StringBuilder sb = new StringBuilder();
            if (e.getLabelDecl() != null) {
                sb.append(e.getLabelDecl().getText()).append(' ');
            }
            sb.append("{...}");
            return sb.toString();
        }
    };

    private static final ElementHandler<RsMacroDefinitionBase> MACRO_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsMacroDefinitionBase;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsMacroDefinitionBase e) {
            String name = e.getName();
            return name != null ? name + "!" : "!";
        }
    };

    private static final ElementHandler<RsFunction> FUNCTION_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsFunction;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsFunction e) {
            String name = e.getName();
            return name != null ? name + "()" : "()";
        }
    };

    private static final ElementHandler<RsIfExpr> IF_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsIfExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsIfExpr e) {
            StringBuilder sb = new StringBuilder();
            sb.append("if");
            RsCondition condition = e.getCondition();
            if (condition != null) {
                if (condition.getExpr() instanceof RsBlockExpr) {
                    sb.append(" {...}");
                } else {
                    sb.append(' ').append(truncate(condition.getText()));
                }
            }
            return sb.toString();
        }
    };

    private static final ElementHandler<RsElseBranch> ELSE_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsElseBranch;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsElseBranch e) {
            return "else";
        }
    };

    private static final ElementHandler<RsLoopExpr> LOOP_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsLoopExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsLoopExpr e) {
            StringBuilder sb = new StringBuilder();
            appendLabelInfo(sb, e.getLabelDecl());
            sb.append("loop");
            return sb.toString();
        }
    };

    private static final ElementHandler<RsForExpr> FOR_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsForExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsForExpr e) {
            StringBuilder sb = new StringBuilder();
            appendLabelInfo(sb, e.getLabelDecl());
            sb.append("for");
            if (e.getBlock() != null) {
                RsPat pat = e.getPat();
                if (pat != null) {
                    sb.append(' ').append(pat.getText());
                }
                RsExpr expr = e.getExpr();
                sb.append(" in ").append(expr != null ? truncate(expr.getText()) : "");
            } else {
                sb.append(" {...}");
            }
            return sb.toString();
        }
    };

    private static final ElementHandler<RsWhileExpr> WHILE_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsWhileExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsWhileExpr e) {
            StringBuilder sb = new StringBuilder();
            appendLabelInfo(sb, e.getLabelDecl());
            sb.append("while");
            RsCondition condition = e.getCondition();
            if (condition != null) {
                if (condition.getExpr() instanceof RsBlockExpr) {
                    sb.append(" {...}");
                } else {
                    sb.append(' ').append(truncate(condition.getText()));
                }
            }
            return sb.toString();
        }
    };

    private static final ElementHandler<RsMatchExpr> MATCH_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsMatchExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsMatchExpr e) {
            StringBuilder sb = new StringBuilder();
            sb.append("match");
            RsExpr expr = e.getExpr();
            if (expr != null) {
                if (expr instanceof RsBlockExpr && e.getMatchBody() == null) {
                    sb.append(" {...}");
                } else {
                    sb.append(' ').append(truncate(expr.getText()));
                }
            }
            return sb.toString();
        }
    };

    private static final ElementHandler<RsMatchArm> MATCH_ARM_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsMatchArm;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsMatchArm e) {
            return truncate(e.getPat().getText()) + " =>";
        }
    };

    private static final ElementHandler<RsLambdaExpr> LAMBDA_HANDLER = new ElementHandler<>() {
        @Override
        public boolean accepts(@NotNull PsiElement e) {
            return e instanceof RsLambdaExpr;
        }

        @Override
        @NotNull
        public String elementInfo(@NotNull RsLambdaExpr e) {
            return e.getValueParameterList().getText() + " {...}";
        }
    };

    @SuppressWarnings("rawtypes")
    private static final List<ElementHandler> HANDLERS = Arrays.asList(
        NAMED_HANDLER,
        IMPL_HANDLER,
        BLOCK_EXPR_HANDLER,
        MACRO_HANDLER,
        FUNCTION_HANDLER,
        IF_HANDLER,
        ELSE_HANDLER,
        LOOP_HANDLER,
        FOR_HANDLER,
        WHILE_HANDLER,
        MATCH_HANDLER,
        MATCH_ARM_HANDLER,
        LAMBDA_HANDLER
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private static ElementHandler<RsElement> handler(@NotNull PsiElement e) {
        if (!(e instanceof RsElement)) return null;
        for (ElementHandler handler : HANDLERS) {
            if (handler.accepts(e)) {
                return handler;
            }
        }
        return null;
    }

    @Override
    @NotNull
    public RsLanguage @NotNull [] getLanguages() {
        return LANGUAGES;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement e) {
        return handler(e) != null;
    }

    @Override
    @NotNull
    public String getElementInfo(@NotNull PsiElement e) {
        ElementHandler<RsElement> h = handler(e);
        assert h != null;
        return h.elementInfo((RsElement) e);
    }

    @Override
    @Nullable
    public String getElementTooltip(@NotNull PsiElement e) {
        return null;
    }

    @Nullable
    public String getBreadcrumb(@NotNull RsElement e) {
        ElementHandler<RsElement> h = handler(e);
        return h != null ? h.elementInfo(e) : null;
    }

    @NotNull
    private static String truncate(@NotNull String text) {
        if (text.length() > INFO_MAX_TEXT_LENGTH) {
            return text.substring(0, INFO_MAX_TEXT_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
        }
        return text;
    }

    private static void appendLabelInfo(@NotNull StringBuilder sb, @Nullable RsLabelDecl labelDecl) {
        if (labelDecl != null) {
            sb.append(labelDecl.getText()).append(' ');
        }
    }
}
