/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import consulo.language.psi.PsiElement;
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor;
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler;
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwner;
import org.rust.lang.core.psi.ext.impl.RsTypeArgumentListUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsStructItemUtil;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.impl.RsLitExprUtil;

public class RsMatchingVisitor extends RsVisitor {
    @Nonnull
    private final GlobalMatchingVisitor matchingVisitor;

    public RsMatchingVisitor(@Nonnull GlobalMatchingVisitor matchingVisitor) {
        this.matchingVisitor = matchingVisitor;
    }

    @Nonnull
    private MatchingHandler getHandler(@Nonnull PsiElement element) {
        return matchingVisitor.getMatchContext().getPattern().getHandler(element);
    }

    private boolean matchTextOrVariable(@Nullable PsiElement templateElement, @Nullable PsiElement treeElement) {
        if (templateElement == null) return true;
        if (treeElement == null) return false;
        MatchingHandler handler = getHandler(templateElement);
        if (handler instanceof SubstitutionHandler) {
            return ((SubstitutionHandler) handler).validate(treeElement, matchingVisitor.getMatchContext());
        } else {
            return matchingVisitor.matchText(templateElement, treeElement);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getElement(@Nonnull Class<T> clazz) {
        PsiElement element = matchingVisitor.getElement();
        if (clazz.isInstance(element)) {
            return (T) element;
        } else {
            matchingVisitor.setResult(false);
            return null;
        }
    }

    private boolean match(@Nullable PsiElement e1, @Nullable PsiElement e2) {
        return matchingVisitor.match(e1, e2);
    }

    @Override
    public void visitStructItem(@Nonnull RsStructItem o) {
        RsStructItem struct = getElement(RsStructItem.class);
        if (struct == null) return;
        matchingVisitor.setResult(
            matchOuterAttrList(o, struct) &&
                match(o.getVis(), struct.getVis()) &&
                matchIdentifier(o.getIdentifier(), struct.getIdentifier()) &&
                match(o.getTypeParameterList(), struct.getTypeParameterList()) &&
                match(o.getWhereClause(), struct.getWhereClause()) &&
                match(o.getBlockFields(), struct.getBlockFields()) &&
                match(o.getTupleFields(), struct.getTupleFields()) &&
                matchLeaf(RsStructItemUtil.getUnion(o), RsStructItemUtil.getUnion(struct))
        );
    }

    @Override
    public void visitTypeParameterList(@Nonnull RsTypeParameterList o) {
        RsTypeParameterList parameters = getElement(RsTypeParameterList.class);
        if (parameters == null) return;
        matchingVisitor.setResult(
            matchInAnyOrder(o.getTypeParameterList(), parameters.getTypeParameterList()) &&
                matchInAnyOrder(o.getLifetimeParameterList(), parameters.getLifetimeParameterList()) &&
                matchInAnyOrder(o.getConstParameterList(), parameters.getConstParameterList())
        );
    }

    @Override
    public void visitWhereClause(@Nonnull RsWhereClause o) {
        RsWhereClause where = getElement(RsWhereClause.class);
        if (where == null) return;
        matchingVisitor.setResult(matchInAnyOrder(o.getWherePredList(), where.getWherePredList()));
    }

    @Override
    public void visitWherePred(@Nonnull RsWherePred o) {
        RsWherePred where = getElement(RsWherePred.class);
        if (where == null) return;
        matchingVisitor.setResult(match(o.getTypeReference(), where.getTypeReference())
            && match(o.getTypeParamBounds(), where.getTypeParamBounds()));
    }

    @Override
    public void visitTypeParameter(@Nonnull RsTypeParameter o) {
        RsTypeParameter parameter = getElement(RsTypeParameter.class);
        if (parameter == null) return;
        matchingVisitor.setResult(
            matchOuterAttrList(o, parameter) &&
                match(o.getTypeParamBounds(), parameter.getTypeParamBounds()) &&
                match(o.getTypeReference(), parameter.getTypeReference()) &&
                matchIdentifier(o.getIdentifier(), parameter.getIdentifier())
        );
    }

    @Override
    public void visitTypeArgumentList(@Nonnull RsTypeArgumentList o) {
        RsTypeArgumentList list = getElement(RsTypeArgumentList.class);
        if (list == null) return;
        matchingVisitor.setResult(matchSequentially(
            RsTypeArgumentListUtil.getTypeArguments(o),
            RsTypeArgumentListUtil.getTypeArguments(list)
        ));
    }

    @Override
    public void visitTypeParamBounds(@Nonnull RsTypeParamBounds o) {
        RsTypeParamBounds bounds = getElement(RsTypeParamBounds.class);
        if (bounds == null) return;
        matchingVisitor.setResult(matchInAnyOrder(o.getPolyboundList(), bounds.getPolyboundList()));
    }

    @Override
    public void visitPolybound(@Nonnull RsPolybound o) {
        RsPolybound polybound = getElement(RsPolybound.class);
        if (polybound == null) return;
        matchingVisitor.setResult(match(o.getBound(), polybound.getBound())
            && match(o.getForLifetimes(), polybound.getForLifetimes()));
    }

    @Override
    public void visitBound(@Nonnull RsBound o) {
        RsBound bound = getElement(RsBound.class);
        if (bound == null) return;
        matchingVisitor.setResult(match(o.getLifetime(), bound.getLifetime())
            && match(o.getTraitRef(), bound.getTraitRef()));
    }

    @Override
    public void visitTraitRef(@Nonnull RsTraitRef o) {
        RsTraitRef trait = getElement(RsTraitRef.class);
        if (trait == null) return;
        matchingVisitor.setResult(match(o.getPath(), trait.getPath()));
    }

    @Override
    public void visitLifetimeParameter(@Nonnull RsLifetimeParameter o) {
        RsLifetimeParameter lifetime = getElement(RsLifetimeParameter.class);
        if (lifetime == null) return;
        matchingVisitor.setResult(matchIdentifier(o.getQuoteIdentifier(), lifetime.getQuoteIdentifier()));
    }

    @Override
    public void visitConstParameter(@Nonnull RsConstParameter o) {
        RsConstParameter constParam = getElement(RsConstParameter.class);
        if (constParam == null) return;
        matchingVisitor.setResult(match(o.getTypeReference(), constParam.getTypeReference())
            && matchIdentifier(o.getIdentifier(), constParam.getIdentifier()));
    }

    @Override
    public void visitConstant(@Nonnull RsConstant o) {
        RsConstant constant = getElement(RsConstant.class);
        if (constant == null) return;
        matchingVisitor.setResult(matchIdentifier(o.getIdentifier(), constant.getIdentifier()) &&
            match(o.getVis(), constant.getVis()) &&
            match(o.getTypeReference(), constant.getTypeReference()) &&
            match(o.getExpr(), constant.getExpr()) &&
            matchOuterAttrList(o, constant) &&
            matchLeaf(o.getStatic(), constant.getStatic()) &&
            matchLeaf(o.getUnderscore(), constant.getUnderscore()) &&
            matchLeaf(o.getMut(), constant.getMut()));
    }

    @Override
    public void visitBlockFields(@Nonnull RsBlockFields o) {
        RsBlockFields fields = getElement(RsBlockFields.class);
        if (fields == null) return;
        matchingVisitor.setResult(matchSequentially(o.getNamedFieldDeclList(), fields.getNamedFieldDeclList()));
    }

    @Override
    public void visitNamedFieldDecl(@Nonnull RsNamedFieldDecl o) {
        RsNamedFieldDecl field = getElement(RsNamedFieldDecl.class);
        if (field == null) return;
        matchingVisitor.setResult(
            matchOuterAttrList(o, field) &&
                match(o.getVis(), field.getVis()) &&
                matchIdentifier(o.getIdentifier(), field.getIdentifier()) &&
                match(o.getTypeReference(), field.getTypeReference())
        );
    }

    @Override
    public void visitTupleFields(@Nonnull RsTupleFields o) {
        RsTupleFields fields = getElement(RsTupleFields.class);
        if (fields == null) return;
        matchingVisitor.setResult(matchSequentially(o.getTupleFieldDeclList(), fields.getTupleFieldDeclList()));
    }

    @Override
    public void visitTupleFieldDecl(@Nonnull RsTupleFieldDecl o) {
        RsTupleFieldDecl field = getElement(RsTupleFieldDecl.class);
        if (field == null) return;
        matchingVisitor.setResult(
            matchOuterAttrList(o, field) &&
                match(o.getVis(), field.getVis()) &&
                match(o.getTypeReference(), field.getTypeReference())
        );
    }

    @Override
    public void visitTypeReference(@Nonnull RsTypeReference o) {
        RsTypeReference typeReference = getElement(RsTypeReference.class);
        if (typeReference == null) return;
        // TODO: implement individual type references
        matchingVisitor.setResult(matchTextOrVariable(o, typeReference));
    }

    @Override
    public void visitRefLikeType(@Nonnull RsRefLikeType o) {
        RsRefLikeType refType = getElement(RsRefLikeType.class);
        if (refType == null) return;
        matchingVisitor.setResult(match(o.getTypeReference(), refType.getTypeReference()) &&
            match(o.getLifetime(), refType.getLifetime()) &&
            matchLeaf(o.getMut(), refType.getMut()) &&
            matchLeaf(o.getConst(), refType.getConst()) &&
            matchLeaf(o.getMul(), refType.getMul()) &&
            matchLeaf(o.getAnd(), refType.getAnd()));
    }

    @Override
    public void visitLifetime(@Nonnull RsLifetime o) {
        RsLifetime lifetime = getElement(RsLifetime.class);
        if (lifetime == null) return;
        matchingVisitor.setResult(matchTextOrVariable(o, lifetime));
    }

    @Override
    public void visitVis(@Nonnull RsVis o) {
        RsVis vis = getElement(RsVis.class);
        if (vis == null) return;
        matchingVisitor.setResult(matchTextOrVariable(o, vis));
    }

    @Override
    public void visitInnerAttr(@Nonnull RsInnerAttr o) {
        RsInnerAttr attr = getElement(RsInnerAttr.class);
        if (attr == null) return;
        matchingVisitor.setResult(match(o.getMetaItem(), attr.getMetaItem()));
    }

    @Override
    public void visitOuterAttr(@Nonnull RsOuterAttr o) {
        RsOuterAttr attr = getElement(RsOuterAttr.class);
        if (attr == null) return;
        matchingVisitor.setResult(match(o.getMetaItem(), attr.getMetaItem()));
    }

    @Override
    public void visitMetaItem(@Nonnull RsMetaItem o) {
        RsMetaItem metaItem = getElement(RsMetaItem.class);
        if (metaItem == null) return;
        matchingVisitor.setResult(match(o.getCompactTT(), metaItem.getCompactTT()) &&
            match(o.getLitExpr(), metaItem.getLitExpr()) &&
            match(o.getMetaItemArgs(), metaItem.getMetaItemArgs()) &&
            match(o.getPath(), metaItem.getPath()));
    }

    @Override
    public void visitMetaItemArgs(@Nonnull RsMetaItemArgs o) {
        RsMetaItemArgs metaItemArgs = getElement(RsMetaItemArgs.class);
        if (metaItemArgs == null) return;
        matchingVisitor.setResult(matchInAnyOrder(o.getMetaItemList(), metaItemArgs.getMetaItemList()));
    }

    @Override
    public void visitPathType(@Nonnull RsPathType o) {
        RsPathType path = getElement(RsPathType.class);
        if (path == null) return;
        matchingVisitor.setResult(match(o.getPath(), path.getPath()));
    }

    @Override
    public void visitPath(@Nonnull RsPath o) {
        RsPath path = getElement(RsPath.class);
        if (path == null) return;

        matchingVisitor.setResult(matchIdentifier(o.getIdentifier(), path.getIdentifier()) &&
            match(o.getTypeArgumentList(), path.getTypeArgumentList()) &&
            match(o.getValueParameterList(), path.getValueParameterList()) &&
            match(o.getPath(), path.getPath()));
    }

    @Override
    public void visitLitExpr(@Nonnull RsLitExpr o) {
        RsLitExpr litExpr = getElement(RsLitExpr.class);
        if (litExpr == null) return;
        matchingVisitor.setResult(
            java.util.Objects.equals(RsLitExprUtil.getBooleanValue(o), RsLitExprUtil.getBooleanValue(litExpr)) &&
                java.util.Objects.equals(RsLitExprUtil.getIntegerValue(o), RsLitExprUtil.getIntegerValue(litExpr)) &&
                java.util.Objects.equals(RsLitExprUtil.getFloatValue(o), RsLitExprUtil.getFloatValue(litExpr)) &&
                java.util.Objects.equals(RsLitExprUtil.getCharValue(o), RsLitExprUtil.getCharValue(litExpr)) &&
                java.util.Objects.equals(RsLitExprUtil.getStringValue(o), RsLitExprUtil.getStringValue(litExpr))
        );
    }

    private boolean matchOuterAttrList(@Nonnull RsOuterAttributeOwner e1, @Nonnull RsOuterAttributeOwner e2) {
        return matchInAnyOrder(e1.getOuterAttrList(), e2.getOuterAttrList());
    }

    private boolean matchIdentifier(@Nullable PsiElement templateIdentifier, @Nullable PsiElement treeIdentifier) {
        return matchTextOrVariable(templateIdentifier, treeIdentifier);
    }

    private boolean matchLeaf(@Nullable PsiElement templateElement, @Nullable PsiElement treeElement) {
        var templateType = templateElement != null ? RsPsiJavaUtil.elementType(templateElement) : null;
        var treeType = treeElement != null ? RsPsiJavaUtil.elementType(treeElement) : null;
        return java.util.Objects.equals(templateType, treeType);
    }

    private boolean matchSequentially(@Nonnull List<? extends PsiElement> elements,
                                      @Nonnull List<? extends PsiElement> elements2) {
        return matchingVisitor.matchSequentially(
            elements.toArray(PsiElement.EMPTY_ARRAY),
            elements2.toArray(PsiElement.EMPTY_ARRAY)
        );
    }

    private boolean matchInAnyOrder(@Nonnull List<? extends PsiElement> elements,
                                    @Nonnull List<? extends PsiElement> elements2) {
        return matchingVisitor.matchInAnyOrder(
            elements.toArray(PsiElement.EMPTY_ARRAY),
            elements2.toArray(PsiElement.EMPTY_ARRAY)
        );
    }
}
