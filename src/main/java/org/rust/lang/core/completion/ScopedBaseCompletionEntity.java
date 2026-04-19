/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.FieldResolveVariant;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.stdext.StdextUtil;

import java.util.Set;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsMacroBindingUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.ArithmeticOp;
import org.rust.lang.core.psi.ext.ArithmeticAssignmentOp;
import org.rust.lang.core.psi.ext.ComparisonOp;
import org.rust.lang.core.psi.ext.EqualityOp;

public class ScopedBaseCompletionEntity implements CompletionEntity {
    private final ScopeEntry myScopeEntry;
    private final RsElement myElement;

    private static final Set<String> OPERATOR_TRAIT_LANG_ITEMS;

    static {
        OPERATOR_TRAIT_LANG_ITEMS = new java.util.HashSet<>();
        for (ArithmeticOp op : ArithmeticOp.values()) OPERATOR_TRAIT_LANG_ITEMS.add(op.getItemName());
        for (ArithmeticAssignmentOp op : ArithmeticAssignmentOp.values()) OPERATOR_TRAIT_LANG_ITEMS.add(op.getItemName());
        for (ComparisonOp op : ComparisonOp.values()) OPERATOR_TRAIT_LANG_ITEMS.add(op.getItemName());
        for (EqualityOp op : EqualityOp.values()) OPERATOR_TRAIT_LANG_ITEMS.add(op.getItemName());
    }

    public ScopedBaseCompletionEntity(ScopeEntry scopeEntry) {
        myScopeEntry = scopeEntry;
        myElement = scopeEntry.getElement();
    }

    @Nullable
    @Override
    public Ty retTy(KnownItems items) {
        return asTy(myElement, items);
    }

    @Override
    public RsLookupElementProperties getBaseLookupElementProperties(RsCompletionContext context) {
        boolean isMethodSelfTypeIncompatible = myElement instanceof RsFunction && ((RsFunction) myElement).isMethod()
            && isMutableMethodOnConstReference((RsFunction) myElement, context.getContext());

        boolean isLocal = context.isSimplePath() && !canBeExported(myElement);

        RsLookupElementProperties.ElementKind elementKind;
        if (myElement instanceof RsDocAndAttributeOwner
            && RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) myElement).getDeprecatedAttribute() != null) {
            elementKind = RsLookupElementProperties.ElementKind.DEPRECATED;
        } else if (myElement instanceof RsMacro) {
            elementKind = RsLookupElementProperties.ElementKind.MACRO;
        } else if (myElement instanceof RsPatBinding) {
            elementKind = RsLookupElementProperties.ElementKind.VARIABLE;
        } else if (myElement instanceof RsEnumVariant) {
            elementKind = RsLookupElementProperties.ElementKind.ENUM_VARIANT;
        } else if (myElement instanceof RsFieldDecl) {
            elementKind = RsLookupElementProperties.ElementKind.FIELD_DECL;
        } else if (myElement instanceof RsFunction && RsFunctionUtil.isAssocFn((RsFunction) myElement)) {
            elementKind = RsLookupElementProperties.ElementKind.ASSOC_FN;
        } else {
            elementKind = RsLookupElementProperties.ElementKind.DEFAULT;
        }

        boolean isInherentImplMember = myElement instanceof RsAbstractable
            && RsAbstractableUtil.getOwner((RsAbstractable) myElement).isInherentImpl();

        boolean isOperatorMethod = myElement instanceof RsFunction
            && myScopeEntry instanceof AssocItemScopeEntryBase
            && ((AssocItemScopeEntryBase<?>) myScopeEntry).getSource().getImplementedTrait() != null
            && OPERATOR_TRAIT_LANG_ITEMS.contains(
                RsTraitItemUtil.getLangAttribute((RsTraitItem) ((AssocItemScopeEntryBase<?>) myScopeEntry).getSource().getImplementedTrait().getElement())
            );

        boolean isBlanketImplMember;
        if (myScopeEntry instanceof AssocItemScopeEntryBase) {
            TraitImplSource source = ((AssocItemScopeEntryBase<?>) myScopeEntry).getSource();
            isBlanketImplMember = source instanceof TraitImplSource.ExplicitImpl
                && ((TraitImplSource.ExplicitImpl) source).getType() instanceof TyTypeParameter;
        } else {
            isBlanketImplMember = false;
        }

        return new RsLookupElementProperties(
            false,
            RsLookupElementProperties.KeywordKind.NOT_A_KEYWORD,
            !isMethodSelfTypeIncompatible,
            false,
            isLocal,
            isInherentImplMember,
            elementKind,
            isOperatorMethod,
            isBlanketImplMember,
            myElement instanceof RsFunction && RsFunctionUtil.isActuallyUnsafe((RsFunction) myElement),
            myElement instanceof RsFunction && RsFunctionUtil.isAsync((RsFunction) myElement),
            (myElement instanceof RsFunction && RsFunctionUtil.isConst((RsFunction) myElement))
                || (myElement instanceof RsConstant && RsConstantUtil.isConst((RsConstant) myElement)),
            myElement instanceof RsFunction && RsFunctionUtil.isExtern((RsFunction) myElement)
        );
    }

    @Override
    public LookupElementBuilder createBaseLookupElement(RsCompletionContext context) {
        ImplLookup implLookup = context.getLookup();
        Substitution subst = implLookup != null && implLookup.getCtx() != null
            ? LookupElements.getSubstitution(implLookup.getCtx(), myScopeEntry)
            : SubstitutionUtil.emptySubstitution();
        return getLookupElementBuilder(myElement, context, myScopeEntry.getName(), subst);
    }

    private static LookupElementBuilder getLookupElementBuilder(RsElement element, RsCompletionContext context, String scopeName, Substitution subst) {
        boolean isProcMacroDef = element instanceof RsFunction && RsFunctionUtil.isProcMacroDef((RsFunction) element);
        LookupElementBuilder base = LookupElementBuilder.createWithSmartPointer(scopeName, element)
            .withIcon(element instanceof RsFile ? RsIcons.MODULE : element.getIcon(0))
            .withStrikeoutness(element instanceof RsDocAndAttributeOwner
                && RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) element).getDeprecatedAttribute() != null);

        if (element instanceof RsMod) {
            if (shouldAppendDoubleColonToMod(context.getContext(), scopeName)) {
                return base.withTailText("::");
            } else {
                return base;
            }
        } else if (element instanceof RsConstant) {
            RsConstant constant = (RsConstant) element;
            return base.withTypeText(constant.getTypeReference() != null ? RsPsiRendererUtil.getStubOnlyText(constant.getTypeReference(), subst) : null);
        } else if (element instanceof RsConstParameter) {
            RsConstParameter constParam = (RsConstParameter) element;
            return base.withTypeText(constParam.getTypeReference() != null ? RsPsiRendererUtil.getStubOnlyText(constParam.getTypeReference(), subst) : null);
        } else if (element instanceof RsFieldDecl) {
            RsFieldDecl fieldDecl = (RsFieldDecl) element;
            return base.bold().withTypeText(fieldDecl.getTypeReference() != null ? RsPsiRendererUtil.getStubOnlyText(fieldDecl.getTypeReference(), subst) : null);
        } else if (element instanceof RsTraitItem) {
            return base;
        } else if (element instanceof RsFunction && !isProcMacroDef) {
            RsFunction fn = (RsFunction) element;
            return base.withTypeText(fn.getRetType() != null && fn.getRetType().getTypeReference() != null
                    ? RsPsiRendererUtil.getStubOnlyText(fn.getRetType().getTypeReference(), subst) : "()")
                .withTailText(fn.getValueParameterList() != null
                    ? RsPsiRendererUtil.getStubOnlyText(fn.getValueParameterList(), subst, true) : "()");
        } else if (element instanceof RsFunction && RsFunctionUtil.isBangProcMacroDef((RsFunction) element)) {
            return base.withTailText("!");
        } else if (element instanceof RsFunction) {
            return base;
        } else if (element instanceof RsStructItem) {
            return base.withTailText(getFieldsOwnerTailText((RsStructItem) element, subst));
        } else if (element instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) element;
            RsEnumItem enumItem = RsElementUtil.stubAncestorStrict(variant, RsEnumItem.class);
            return base.withTypeText(enumItem != null ? (enumItem.getName() != null ? enumItem.getName() : "") : "")
                .withTailText(getFieldsOwnerTailText(variant, subst));
        } else if (element instanceof RsPatBinding) {
            Ty type = ExtensionsUtil.getType((RsPatBinding) element);
            return base.withTypeText(type instanceof TyUnknown ? "" : type.toString());
        } else if (element instanceof RsMacroBinding) {
            return base.withTypeText(RsMacroBindingUtil.getFragmentSpecifier((RsMacroBinding) element));
        } else if (element instanceof RsMacroDefinitionBase) {
            return base.withTailText("!");
        } else {
            return base;
        }
    }

    private static String getFieldsOwnerTailText(RsFieldsOwner owner, Substitution subst) {
        if (owner.getBlockFields() != null) {
            return " { ... }";
        } else if (owner.getTupleFields() != null) {
            StringBuilder sb = new StringBuilder("(");
            boolean first = true;
            for (RsFieldDecl field : RsFieldsOwnerUtil.getPositionalFields(owner)) {
                if (!first) sb.append(", ");
                sb.append(RsPsiRendererUtil.getStubOnlyText(field.getTypeReference(), subst));
                first = false;
            }
            sb.append(")");
            return sb.toString();
        }
        return "";
    }

    private static boolean shouldAppendDoubleColonToMod(@Nullable RsElement element, String scopeName) {
        if (!"crate".equals(scopeName) && !"self".equals(scopeName) && !"super".equals(scopeName)) {
            return false;
        }
        if (element == null) return true;
        com.intellij.psi.PsiElement parent = element.getParent();
        if (parent != null && parent.getParent() instanceof RsUseGroup) return false;
        if (parent instanceof RsVisRestriction) return false;
        return true;
    }

    private static boolean isMutableMethodOnConstReference(RsFunction method, @Nullable RsElement call) {
        if (call == null) return false;
        RsSelfParameter self = method.getSelfParameter();
        if (self == null) return false;
        if (!RsSelfParameterUtil.isRef(self) || !RsSelfParameterUtil.getMutability(self).isMut()) return false;
        if (!(call instanceof RsFieldLookup)) return false;
        RsFieldLookup fieldLookup = (RsFieldLookup) call;
        RsExpr expr = RsFieldLookupUtil.getReceiver(fieldLookup);
        Ty type = ExtensionsUtil.getType(expr);
        boolean isMutable;
        if (type instanceof TyReference) {
            isMutable = ((TyReference) type).getMutability().isMut();
        } else {
            isMutable = hasMutBinding(expr);
        }
        return !isMutable;
    }

    private static boolean hasMutBinding(RsExpr expr) {
        if (!(expr instanceof RsPathExpr)) return true;
        com.intellij.psi.PsiElement resolved = ((RsPathExpr) expr).getPath().getReference() != null
            ? ((RsPathExpr) expr).getPath().getReference().resolve()
            : null;
        if (!(resolved instanceof RsPatBinding)) return true;
        return RsPatBindingUtil.getMutability((RsPatBinding) resolved).isMut();
    }

    private static boolean canBeExported(RsElement element) {
        if (element instanceof RsEnumVariant) return true;
        com.intellij.psi.PsiElement context = com.intellij.psi.util.PsiTreeUtil.getContextOfType(element, true, RsItemElement.class, RsFile.class);
        return context == null || context instanceof RsMod;
    }

    @Nullable
    private static Ty asTy(RsElement element, KnownItems items) {
        if (element instanceof RsConstant) {
            return ((RsConstant) element).getTypeReference() != null ? ExtensionsUtil.getNormType(((RsConstant) element).getTypeReference()) : null;
        } else if (element instanceof RsConstParameter) {
            return ((RsConstParameter) element).getTypeReference() != null ? ExtensionsUtil.getNormType(((RsConstParameter) element).getTypeReference()) : null;
        } else if (element instanceof RsFieldDecl) {
            return ((RsFieldDecl) element).getTypeReference() != null ? ExtensionsUtil.getNormType(((RsFieldDecl) element).getTypeReference()) : null;
        } else if (element instanceof RsFunction) {
            RsRetType retType = ((RsFunction) element).getRetType();
            return retType != null && retType.getTypeReference() != null ? ExtensionsUtil.getNormType(retType.getTypeReference()) : null;
        } else if (element instanceof RsStructItem) {
            return RsStructItemUtil.getDeclaredType((RsStructItem) element);
        } else if (element instanceof RsEnumVariant) {
            return RsStructOrEnumItemElementUtil.getDeclaredType(RsEnumVariantUtil.getParentEnum((RsEnumVariant) element));
        } else if (element instanceof RsPatBinding) {
            return ExtensionsUtil.getType((RsPatBinding) element);
        }
        return null;
    }
}
