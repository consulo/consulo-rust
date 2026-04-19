/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import com.intellij.openapi.util.Key;
import com.intellij.patterns.*;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Set;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.or;
import static org.rust.lang.core.psi.RsElementTypes.*;

/**
 * Rust PSI tree patterns.
 */
public final class RsPsiPattern {
    public static final RsPsiPattern INSTANCE = new RsPsiPattern();

    private RsPsiPattern() {}

    private static final TokenSet STATEMENT_BOUNDARIES = TokenSet.create(SEMICOLON, LBRACE, RBRACE);

    private static final Set<String> LINT_ATTRIBUTES = Set.of("allow", "warn", "deny", "forbid");

    public static final Key<RsAttr> META_ITEM_ATTR = Key.create("META_ITEM_ATTR");

    /** @see RsMetaItem#isRootMetaItem */
    public static final PsiElementPattern.Capture<RsMetaItem> rootMetaItem = rootMetaItem(null, null);

    public static final PsiElementPattern.Capture<PsiElement> onStatementBeginning =
        psiElement().with(new OnStatementBeginning());

    public static PsiElementPattern.Capture<PsiElement> onStatementBeginning(String... startWords) {
        return psiElement().with(new OnStatementBeginning(startWords));
    }

    public static final PsiElementPattern.Capture<PsiElement> onStruct = onItem(RsStructItem.class);
    public static final PsiElementPattern.Capture<PsiElement> onEnum = onItem(RsEnumItem.class);
    public static final PsiElementPattern.Capture<PsiElement> onEnumVariant = onItem(RsEnumVariant.class);
    public static final PsiElementPattern.Capture<PsiElement> onFn = onItem(RsFunction.class);

    public static final PsiElementPattern.Capture<PsiElement> onMod =
        psiElement(PsiElement.class).andOr(onItem(RsModItem.class), onItem(RsModDeclItem.class));

    public static final PsiElementPattern.Capture<PsiElement> onStatic = onItem(
        psiElement(RsConstant.class).with(new PatternCondition<RsConstant>("onStaticCondition") {
            @Override
            public boolean accepts(@NotNull RsConstant e, ProcessingContext context) {
                return RsConstantUtil.getKind(e) == RsConstantKind.STATIC;
            }
        })
    );

    public static final PsiElementPattern.Capture<PsiElement> onStaticMut = onItem(
        psiElement(RsConstant.class).with(new PatternCondition<RsConstant>("onStaticMutCondition") {
            @Override
            public boolean accepts(@NotNull RsConstant e, ProcessingContext context) {
                return RsConstantUtil.getKind(e) == RsConstantKind.MUT_STATIC;
            }
        })
    );

    public static final PsiElementPattern.Capture<PsiElement> onMacro = onItem(RsMacro.class);

    public static final PsiElementPattern.Capture<PsiElement> onTupleStruct = onItem(
        psiElement(RsStructItem.class).withChild(psiElement(RsTupleFields.class))
    );

    public static final PsiElementPattern.Capture<PsiElement> onCrate =
        onItem(RsFile.class).with(new PatternCondition<PsiElement>("onCrateCondition") {
            @Override
            public boolean accepts(@NotNull PsiElement e, ProcessingContext context) {
                PsiFile file = e.getContainingFile().getOriginalFile();
                return file instanceof RsFile && ((RsFile) file).isCrateRoot();
            }
        });

    public static final PsiElementPattern.Capture<PsiElement> onExternBlock = onItem(RsForeignModItem.class);

    public static final PsiElementPattern.Capture<PsiElement> onExternBlockDecl =
        psiElement(PsiElement.class).andOr(
            onItem(RsFunction.class),
            onItem(RsConstant.class),
            onItem(RsForeignModItem.class)
        );

    public static final PsiElementPattern.Capture<PsiElement> onAnyItem = onItem(RsDocAndAttributeOwner.class);
    public static final PsiElementPattern.Capture<PsiElement> onExternCrate = onItem(RsExternCrateItem.class);
    public static final PsiElementPattern.Capture<PsiElement> onTrait = onItem(RsTraitItem.class);

    public static final PsiElementPattern.Capture<PsiElement> onTestFn = onItem(
        psiElement(RsFunction.class).withChild(
            psiElement(RsOuterAttr.class).withText("#[test]")
        )
    );

    public static final PsiElementPattern.Capture<PsiElement> onProcMacroFn =
        onFn.with(new PatternCondition<PsiElement>("proc_macro crate") {
            @Override
            public boolean accepts(@NotNull PsiElement it, ProcessingContext context) {
                RsFile file = PsiTreeUtil.getParentOfType(it, RsFile.class);
                if (file == null) return false;
                org.rust.lang.core.crate.Crate crate = file.getCrate();
                if (crate == null) return false;
                return crate.getKind().isProcMacro();
            }
        });

    public static final PsiElementPattern.Capture<PsiElement> onStructLike =
        psiElement(PsiElement.class).andOr(onStruct, onEnum, onEnumVariant);

    public static final PsiElementPattern.Capture<PsiElement> inAnyLoop =
        psiElement().inside(
            true,
            psiElement(RsBlock.class).withParent(
                or(
                    psiElement(RsForExpr.class),
                    psiElement(RsLoopExpr.class),
                    psiElement(RsWhileExpr.class)
                )
            ),
            psiElement(RsLambdaExpr.class)
        );

    public static final PsiElementPattern.Capture<RsMetaItem> derivedTraitMetaItem =
        psiElement(RsMetaItem.class).withSuperParent(
            2,
            rootMetaItem("derive", psiElement(RsStructOrEnumItemElement.class))
        );

    /**
     * Supposed to capture outer attributes names, like {@code attribute} in {@code #[attribute(par1, par2)]}.
     */
    public static final PsiElementPattern.Capture<RsMetaItem> nonStdOuterAttributeMetaItem =
        psiElement(RsMetaItem.class)
            .with(new PatternCondition<RsMetaItem>("nonBuiltinAttributeCondition") {
                @Override
                public boolean accepts(@NotNull RsMetaItem e, ProcessingContext context) {
                    return RsPossibleMacroCallUtil.getCanBeMacroCall(e);
                }
            });

    public static final PsiElementPattern.Capture<RsMetaItem> lintAttributeMetaItem =
        rootMetaItem.with(new PatternCondition<RsMetaItem>("lintAttributeCondition") {
            @Override
            public boolean accepts(@NotNull RsMetaItem e, ProcessingContext context) {
                return LINT_ATTRIBUTES.contains(e.getName());
            }
        });

    public static final PsiElementPattern.Capture<RsLitExpr> literal = psiElement(RsLitExpr.class);

    public static final PsiElementPattern.Capture<RsLitExpr> includeMacroLiteral = literal
        .withParent(psiElement(RsIncludeMacroArgument.class));

    @SuppressWarnings("unchecked")
    public static final PsiElementPattern.Capture<RsLitExpr> pathAttrLiteral = literal
        .withParent(
            rootMetaItem("path",
                (ElementPattern<? extends RsDocAndAttributeOwner>) (ElementPattern<?>) psiElement(PsiElement.class).andOr(
                    psiElement(RsModDeclItem.class),
                    psiElement(RsModItem.class)
                )
            )
        );

    public static final PsiElementPattern.Capture<PsiElement> whitespace = psiElement().whitespace();

    public static final PsiElementPattern.Capture<PsiErrorElement> error = psiElement(PsiErrorElement.class);

    public static PsiElementPattern.Capture<PsiElement> getSimplePathPattern() {
        PsiElementPattern.Capture<RsPath> simplePath = psiElement(RsPath.class)
            .with(new PatternCondition<RsPath>("SimplePath") {
                @Override
                public boolean accepts(@NotNull RsPath path, ProcessingContext context) {
                    return path.getKind() == PathKind.IDENTIFIER
                        && path.getPath() == null
                        && path.getTypeQual() == null
                        && !path.getHasColonColon()
                        && PsiTreeUtil.getParentOfType(path, RsUseSpeck.class, true) == null;
                }
            });
        return psiElement().withParent(simplePath);
    }

    /** {@code #[cfg()]} */
    private static final PsiElementPattern.Capture<RsMetaItem> cfgAttributeMeta = rootMetaItem("cfg", null);

    /** {@code #[cfg_attr()]} */
    private static final PsiElementPattern.Capture<RsMetaItem> cfgAttrAttributeMeta = rootMetaItem("cfg_attr", null);

    /** {@code #[doc(cfg())]} */
    private static final PsiElementPattern.Capture<RsMetaItem> docCfgAttributeMeta = metaItem("cfg")
        .withSuperParent(2, rootMetaItem("doc", null));

    /**
     * <pre>
     * #[cfg_attr(condition, attr)]
     *           //^
     * </pre>
     */
    private static final PsiElementPattern.Capture<RsMetaItem> cfgAttrCondition = psiElement(RsMetaItem.class)
        .withSuperParent(2, cfgAttrAttributeMeta)
        .with(new PatternCondition<RsMetaItem>("firstItem") {
            @Override
            public boolean accepts(@NotNull RsMetaItem it, ProcessingContext context) {
                PsiElement parent = it.getParent();
                if (!(parent instanceof RsMetaItemArgs)) return false;
                java.util.List<RsMetaItem> list = ((RsMetaItemArgs) parent).getMetaItemList();
                return !list.isEmpty() && list.get(0) == it;
            }
        });

    public static final PsiElementPattern.Capture<RsMetaItem> anyCfgCondition =
        psiElement(RsMetaItem.class).andOr(
            cfgAttrCondition,
            psiElement(RsMetaItem.class).withSuperParent(2,
                psiElement(RsMetaItem.class).andOr(cfgAttributeMeta, docCfgAttributeMeta)
            )
        );

    public static final PsiElementPattern.Capture<RsLitExpr> anyCfgFeature = anyCfgKeyValueFlag("feature");

    public static final PsiElementPattern.Capture<RsLitExpr> insideCrateTypeAttrValue =
        psiElement(RsLitExpr.class).withParent(
            rootMetaItem("crate_type", null)
                .with(new PatternCondition<RsMetaItem>("innerAttribute") {
                    @Override
                    public boolean accepts(@NotNull RsMetaItem item, ProcessingContext context) {
                        if (context == null) return false;
                        return context.get(META_ITEM_ATTR) instanceof RsInnerAttr;
                    }
                })
        );

    private static final PatternCondition<RsFieldDecl> VisibilityOwnerFieldCondition =
        new PatternCondition<RsFieldDecl>("visibilityOwnerField") {
            @Override
            public boolean accepts(@NotNull RsFieldDecl field, ProcessingContext context) {
                return !(RsFieldDeclUtil.getOwner(field) instanceof RsEnumVariant) && field.getVis() == null;
            }
        };

    /**
     * struct S {
     *   here a: u32
     * }
     */
    private static final PsiElementPattern.Capture<PsiElement> namedFieldVisibility =
        psiElement(IDENTIFIER)
            .withParent(psiElement(RsNamedFieldDecl.class).with(VisibilityOwnerFieldCondition));

    /**
     * struct S(here a: u32);
     */
    private static final PsiElementPattern.Capture<PsiElement> tupleFieldVisibility =
        psiElement(IDENTIFIER)
            .withParent(
                psiElement(RsPath.class).withSuperParent(2,
                    psiElement(RsTupleFieldDecl.class).with(VisibilityOwnerFieldCondition))
            );

    public static final PsiElementPattern.Capture<PsiElement> fieldVisibility =
        psiElement(PsiElement.class).andOr(namedFieldVisibility, tupleFieldVisibility);

    /**
     * fn foo(a: &amp;'here static u32) {}
     */
    public static final ElementPattern<PsiElement> lifetimeIdentifier =
        psiElement(QUOTE_IDENTIFIER).withParent(psiElement(RsLifetime.class));

    public static PsiElementPattern.Capture<RsLitExpr> anyCfgKeyValueFlag(String flag) {
        return psiElement(RsLitExpr.class)
            .withParent(metaItem(flag))
            .inside(anyCfgCondition);
    }

    /**
     * A leaf literal inside anyCfgKeyValueFlag or an identifier at the same place.
     */
    public static PsiElementPattern.Capture<PsiElement> insideAnyCfgFlagValue(String flag) {
        return psiElement(PsiElement.class).andOr(
            psiElement().withParent(anyCfgKeyValueFlag(flag)),
            psiElement(IDENTIFIER)
                .withParent(
                    psiElement(RsCompactTT.class)
                        .withParent(
                            psiElement(RsMetaItem.class)
                                .inside(anyCfgCondition)
                        )
                )
                .with(new PatternCondition<PsiElement>(flag) {
                    @Override
                    public boolean accepts(@NotNull PsiElement it, ProcessingContext context) {
                        PsiElement eq = PsiElementUtil.getPrevNonCommentSibling(it);
                        if (eq == null) return false;
                        PsiElement flagEl = PsiElementUtil.getPrevNonCommentSibling(eq);
                        return eq.getNode().getElementType() == EQ
                            && flagEl != null && flagEl.textMatches(flag);
                    }
                })
        );
    }

    private static PsiElementPattern.Capture<PsiElement> identifierStatementBeginningPattern(String... startWords) {
        return psiElement(IDENTIFIER).and(onStatementBeginning(startWords));
    }

    public static PsiElementPattern.Capture<PsiElement> declarationPattern() {
        return baseDeclarationPattern().and(identifierStatementBeginningPattern());
    }

    public static PsiElementPattern.Capture<PsiElement> baseDeclarationPattern() {
        return psiElement()
            .withParent(or(psiElement(RsPath.class), psiElement(RsModItem.class), psiElement(RsFile.class)));
    }

    public static PsiElementPattern.Capture<PsiElement> baseTraitOrImplDeclaration() {
        return psiElement().withParent(
            or(
                psiElement(RsMembers.class),
                psiElement().withParent(RsMembers.class)
            )
        );
    }

    public static PsiElementPattern.Capture<PsiElement> baseInherentImplDeclarationPattern() {
        PsiElementPattern.Capture<RsMembers> membersInInherentImpl = psiElement(RsMembers.class).withParent(
            psiElement(RsImplItem.class).with(new PatternCondition<RsImplItem>("InherentImpl") {
                @Override
                public boolean accepts(@NotNull RsImplItem e, ProcessingContext context) {
                    return e.getTraitRef() == null;
                }
            })
        );
        return psiElement().withParent(
            or(
                membersInInherentImpl,
                psiElement().withParent(membersInInherentImpl)
            )
        );
    }

    public static PsiElementPattern.Capture<PsiElement> inherentImplDeclarationPattern() {
        return baseInherentImplDeclarationPattern().and(identifierStatementBeginningPattern());
    }

    private static <I extends RsDocAndAttributeOwner> PsiElementPattern.Capture<PsiElement> onItem(Class<I> cls) {
        return psiElement().withSuperParent(2, rootMetaItem(null, psiElement(cls)));
    }

    private static PsiElementPattern.Capture<PsiElement> onItem(ElementPattern<? extends RsDocAndAttributeOwner> pattern) {
        return psiElement().withSuperParent(2, rootMetaItem(null, pattern));
    }

    private static PsiElementPattern.Capture<RsMetaItem> metaItem(String key) {
        return psiElement(RsMetaItem.class).with(new PatternCondition<RsMetaItem>("MetaItemName") {
            @Override
            public boolean accepts(@NotNull RsMetaItem item, ProcessingContext context) {
                return key.equals(item.getName());
            }
        });
    }

    /**
     * @param key           required attribute name. {@code null} means any root attribute
     * @param ownerPattern  additional requirements for item owned the corresponding attribute
     * @see RsMetaItem#isRootMetaItem
     * @see RsAttr#getOwner
     */
    public static PsiElementPattern.Capture<RsMetaItem> rootMetaItem(
        @Nullable String key,
        @Nullable ElementPattern<? extends RsDocAndAttributeOwner> ownerPattern
    ) {
        PsiElementPattern.Capture<RsMetaItem> metaItemPattern =
            key == null ? psiElement(RsMetaItem.class) : metaItem(key);
        PsiElementPattern.Capture<RsMetaItem> rootMeta = metaItemPattern.with(RootMetaItemCondition.INSTANCE);
        if (ownerPattern != null) {
            return rootMeta.with(new PatternCondition<RsMetaItem>("ownerPattern") {
                @Override
                public boolean accepts(@NotNull RsMetaItem item, ProcessingContext context) {
                    if (context == null) return false;
                    RsAttr attr = context.get(META_ITEM_ATTR);
                    if (attr == null) return false;
                    return ownerPattern.accepts(RsAttrExtUtil.getOwner(attr), context);
                }
            });
        } else {
            return rootMeta;
        }
    }

    private static class OnStatementBeginning extends PatternCondition<PsiElement> {
        private final String[] myStartWords;

        OnStatementBeginning(String... startWords) {
            super("on statement beginning");
            this.myStartWords = startWords;
        }

        @Override
        public boolean accepts(@NotNull PsiElement t, ProcessingContext context) {
            PsiElement prev = RsPsiPatternUtil.getPrevVisibleOrNewLine(t);
            if (myStartWords.length == 0) {
                return prev == null || prev instanceof PsiWhiteSpace
                    || STATEMENT_BOUNDARIES.contains(prev.getNode().getElementType());
            } else {
                if (prev == null) return false;
                String text = prev.getNode().getText();
                for (String word : myStartWords) {
                    if (word.equals(text)) return true;
                }
                return false;
            }
        }
    }

    /** @see RsMetaItem#isRootMetaItem */
    private static class RootMetaItemCondition extends PatternCondition<RsMetaItem> {
        static final RootMetaItemCondition INSTANCE = new RootMetaItemCondition();

        RootMetaItemCondition() {
            super("rootMetaItem");
        }

        @Override
        public boolean accepts(@NotNull RsMetaItem meta, ProcessingContext context) {
            return RsMetaItemUtil.isRootMetaItem(meta, context);
        }
    }
}
