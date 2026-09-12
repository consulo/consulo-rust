/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.utils.GenericConstraints;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFieldLookupUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.types.ExtensionsUtil;
import consulo.localize.LocalizeValue;

public class CreateFunctionIntention extends RsElementBaseIntentionAction<CreateFunctionIntention.Context> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.create.function"));
        }

    public interface Context {
        @Nonnull
        String getName();

        @Nonnull
        RsElement getCallElement();

        @Nonnull
        FunctionInsertionPlace getPlace();

        @Nonnull
        RsValueArgumentList getArguments();

        @Nonnull
        ReturnType getReturnType();

        @Nonnull
        String getVisibility();

        default boolean isAsync() {
            return RsElementUtil.isAtLeastEdition2018(getCallElement());
        }
    }

    public static class FunctionContext implements Context {
        @Nonnull
        private final RsCallExpr callElement;
        @Nonnull
        private final String name;
        @Nonnull
        private final FunctionInsertionPlace place;
        @Nonnull
        private final RsMod module;

        public FunctionContext(@Nonnull RsCallExpr callElement, @Nonnull String name,
                               @Nonnull FunctionInsertionPlace place, @Nonnull RsMod module) {
            this.callElement = callElement;
            this.name = name;
            this.place = place;
            this.module = module;
        }

        @Override
        @Nonnull
        public String getName() {
            return name;
        }

        @Override
        @Nonnull
        public RsCallExpr getCallElement() {
            return callElement;
        }

        @Override
        @Nonnull
        public FunctionInsertionPlace getPlace() {
            return place;
        }

        @Override
        @Nonnull
        public RsValueArgumentList getArguments() {
            return callElement.getValueArgumentList();
        }

        @Override
        @Nonnull
        public ReturnType getReturnType() {
            return ReturnType.create(callElement);
        }

        @Override
        @Nonnull
        public String getVisibility() {
            return CreateFromUsageUtils.getVisibility(module, RsElementUtil.getContainingMod((RsElement) callElement));
        }

        @Override
        public boolean isAsync() {
            boolean base = Context.super.isAsync();
            if (!base) return false;
            PsiElement parent = callElement.getParent();
            if (parent instanceof RsDotExpr) {
                RsFieldLookup fieldLookup = ((RsDotExpr) parent).getFieldLookup();
                return fieldLookup != null && RsFieldLookupUtil.isAsync(fieldLookup);
            }
            return false;
        }
    }

    public static class MethodContext implements Context {
        @Nonnull
        private final RsMethodCall callElement;
        @Nonnull
        private final String name;
        @Nonnull
        private final FunctionInsertionPlace place;
        @Nonnull
        private final RsStructOrEnumItemElement item;
        private final boolean async;

        public MethodContext(@Nonnull RsMethodCall callElement, @Nonnull String name,
                             @Nonnull FunctionInsertionPlace place, @Nonnull RsStructOrEnumItemElement item) {
            this.callElement = callElement;
            this.name = name;
            this.place = place;
            this.item = item;
            boolean baseAsync = Context.super.isAsync();
            if (baseAsync) {
                PsiElement parent = RsMethodCallUtil.getParentDotExpr(callElement).getParent();
                if (parent instanceof RsDotExpr) {
                    RsFieldLookup fieldLookup = ((RsDotExpr) parent).getFieldLookup();
                    this.async = fieldLookup != null && RsFieldLookupUtil.isAsync(fieldLookup);
                } else {
                    this.async = false;
                }
            } else {
                this.async = false;
            }
        }

        @Override
        @Nonnull
        public String getName() {
            return name;
        }

        @Override
        @Nonnull
        public RsMethodCall getCallElement() {
            return callElement;
        }

        @Override
        @Nonnull
        public FunctionInsertionPlace getPlace() {
            return place;
        }

        @Override
        @Nonnull
        public RsValueArgumentList getArguments() {
            return callElement.getValueArgumentList();
        }

        @Override
        @Nonnull
        public ReturnType getReturnType() {
            return ReturnType.create(RsMethodCallUtil.getParentDotExpr(callElement));
        }

        @Override
        @Nonnull
        public String getVisibility() {
            RsImplItem parentImpl = RsElementUtil.contextStrict(callElement, RsImplItem.class);
            if (parentImpl != null) {
                Ty rawType = RsTypesUtil.getRawType(parentImpl.getTypeReference());
                if (rawType instanceof TyAdt && ((TyAdt) rawType).getItem().equals(item) && parentImpl.getTraitRef() == null) {
                    return "";
                }
            }
            if (!java.util.Objects.equals(RsElementUtil.getContainingCrate((RsElement) callElement), RsElementUtil.getContainingCrate((RsElement) item))) {
                return "pub ";
            }
            return "pub(crate)";
        }

        @Override
        public boolean isAsync() {
            return async;
        }
    }

    public static class ReturnType {
        @Nonnull
        private final Ty type;
        private final boolean needsTemplate;

        public ReturnType(@Nonnull Ty type, boolean needsTemplate) {
            this.type = type;
            this.needsTemplate = needsTemplate;
        }

        @Nonnull
        public Ty getType() {
            return type;
        }

        public boolean needsTemplate() {
            return needsTemplate;
        }

        @Nonnull
        public static ReturnType create(@Nonnull RsExpr expr) {
            Ty expected = ExtensionsUtil.getExpectedType(expr);
            if (expected == null) expected = TyUnknown.INSTANCE;

            PsiElement parent = expr.getParent();
            boolean needsTemplate = expected instanceof TyUnknown;
            if (needsTemplate) {
                if (parent instanceof RsExprStmt) {
                    needsTemplate = false;
                } else if (parent instanceof RsDotExpr) {
                    RsFieldLookup fieldLookup = ((RsDotExpr) parent).getFieldLookup();
                    needsTemplate = fieldLookup == null || !"await".equals(fieldLookup.getIdentifier().getText());
                }
            }

            if (needsTemplate) {
                return new ReturnType(expected, true);
            } else {
                Ty finalType = (expected instanceof TyUnknown) ? TyUnit.INSTANCE : expected;
                return new ReturnType(finalType, false);
            }
        }
    }

    public interface FunctionInsertionPlace {
    }

    public static class InPlace implements FunctionInsertionPlace {
        @Nonnull
        public final PsiInsertionPlace place;

        public InPlace(@Nonnull PsiInsertionPlace place) {
            this.place = place;
        }
    }

    public static class InNewImplIn implements FunctionInsertionPlace {
        @Nonnull
        public final PsiInsertionPlace placeForImpl;
        @Nonnull
        public final String itemName;
        @Nonnull
        public final RsStructOrEnumItemElement item;

        public InNewImplIn(@Nonnull PsiInsertionPlace placeForImpl, @Nonnull String itemName,
                            @Nonnull RsStructOrEnumItemElement item) {
            this.placeForImpl = placeForImpl;
            this.itemName = itemName;
            this.item = item;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsPath path = RsElementUtil.contextStrict(element, RsPath.class);
        RsCallExpr functionCall = path != null ? RsElementUtil.contextStrict(path, RsCallExpr.class) : null;
        if (functionCall != null) {
            if (!RsElementUtil.isContextOf(functionCall.getExpr(), path)) return null;
            if (RsPathUtil.getResolveStatus(path) != PathResolveStatus.UNRESOLVED) return null;

            RsElement target = CreateFromUsageUtils.getTargetItemForFunctionCall(path);
            if (target == null) return null;
            String name = path.getReferenceName();
            if (name == null) return null;

            if (target instanceof RsMod) {
                PsiInsertionPlace insertPlace = PsiInsertionPlace.forItemInModAfter((RsMod) target, functionCall);
                if (insertPlace == null) return null;
                InPlace place = new InPlace(insertPlace);
                setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.function", name)));
                return new FunctionContext(functionCall, name, place, (RsMod) target);
            }
            if (target instanceof RsStructOrEnumItemElement) {
                RsStructOrEnumItemElement structOrEnum = (RsStructOrEnumItemElement) target;
                PsiInsertionPlace insertPlace = PsiInsertionPlace.forItemAfter(structOrEnum);
                if (insertPlace == null) return null;
                String targetName = structOrEnum.getName();
                if (targetName == null) return null;
                InNewImplIn place = new InNewImplIn(insertPlace, targetName, structOrEnum);
                setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.associated.function", targetName, name)));
                return new FunctionContext(functionCall, name, place, RsElementUtil.getContainingMod((RsElement) structOrEnum));
            }
            if (target instanceof RsImplItem) {
                PsiInsertionPlace insertPlace = PsiInsertionPlace.forTraitOrImplMember((RsImplItem) target);
                if (insertPlace == null) return null;
                InPlace place = new InPlace(insertPlace);
                setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.associated.function.self", name)));
                return new FunctionContext(functionCall, name, place, RsElementUtil.getContainingMod((RsElement) target));
            }
            return null;
        }

        RsMethodCall methodCall = RsElementUtil.contextStrict(element, RsMethodCall.class);
        if (methodCall != null) {
            if (!methodCall.getReference().multiResolve().isEmpty()) return null;
            if (element != methodCall.getIdentifier()) return null;

            String name = methodCall.getIdentifier().getText();
            Ty exprType = RsTypesUtil.getType(RsMethodCallUtil.getParentDotExpr(methodCall).getExpr());
            Ty stripped = TyUtil.stripReferences(exprType);
            if (!(stripped instanceof TyAdt)) return null;
            TyAdt adtType = (TyAdt) stripped;
            if (RsElementUtil.getContainingCargoPackage(adtType.getItem()) == null
                || RsElementUtil.getContainingCargoPackage(adtType.getItem()).getOrigin() != PackageOrigin.WORKSPACE) return null;

            RsImplItem ancestorImpl = RsElementUtil.contextStrict(methodCall, RsImplItem.class);
            FunctionInsertionPlace place;
            if (ancestorImpl != null && ancestorImpl.getTraitRef() == null) {
                Ty implType = ancestorImpl.getTypeReference() != null ? RsTypesUtil.getNormType(ancestorImpl.getTypeReference()) : null;
                if (implType instanceof TyAdt && ((TyAdt) implType).getItem().equals(adtType.getItem())) {
                    PsiInsertionPlace insertPlace = PsiInsertionPlace.forTraitOrImplMember(ancestorImpl);
                    if (insertPlace == null) return null;
                    place = new InPlace(insertPlace);
                } else {
                    PsiInsertionPlace insertPlace = PsiInsertionPlace.forItemAfter(adtType.getItem());
                    if (insertPlace == null) return null;
                    String itemName = adtType.getItem().getName();
                    if (itemName == null) return null;
                    place = new InNewImplIn(insertPlace, itemName, adtType.getItem());
                }
            } else {
                PsiInsertionPlace insertPlace = PsiInsertionPlace.forItemAfter(adtType.getItem());
                if (insertPlace == null) return null;
                String itemName = adtType.getItem().getName();
                if (itemName == null) return null;
                place = new InNewImplIn(insertPlace, itemName, adtType.getItem());
            }

            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.create.method", name)));
            return new MethodContext(methodCall, name, place, adtType.getItem());
        }
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsFunction function = buildCallable(project, ctx);
        RsFunction inserted = insertCallable(ctx, function);

        List<Ty> types = new ArrayList<>();
        for (RsExpr expr : ctx.getArguments().getExprList()) {
            types.add(RsTypesUtil.getType(expr));
        }
        types.add(ctx.getReturnType().getType());
        ImportBridge.importTypeReferencesFromTys(inserted, types);

        if (EditorExt.canRunTemplateFor(editor, inserted)) {
            List<PsiElement> toBeReplaced = new ArrayList<>();
            for (RsValueParameter param : inserted.getRawValueParameters()) {
                if (param.getPat() != null) toBeReplaced.add(param.getPat());
                if (param.getTypeReference() != null) toBeReplaced.add(param.getTypeReference());
            }

            if (ctx.getReturnType().needsTemplate()) {
                RsRetType retType = inserted.getRetType();
                if (retType != null && retType.getTypeReference() != null) {
                    toBeReplaced.add(retType.getTypeReference());
                }
            }

            RsBlock block = RsFunctionUtil.getBlock(inserted);
            if (block != null && RsBlockUtil.getSyntaxTailStmt(block) != null) {
                toBeReplaced.add(RsBlockUtil.getSyntaxTailStmt(block));
            }
            EditorExt.buildAndRunTemplate(editor, inserted, toBeReplaced);
        } else {
            inserted.navigate(true);
        }
    }

    @Nonnull
    private RsFunction buildCallable(@Nonnull Project project, @Nonnull Context ctx) {
        String functionName = ctx.getName();
        RsPsiFactory factory = new RsPsiFactory(project);
        CallableConfig config = getCallableConfig(ctx);

        String genericParams = config.genericConstraints.buildTypeParameters();
        List<String> parameters = new ArrayList<>(config.parameters);
        String whereClause = config.genericConstraints.buildWhereClause();
        String visibility = ctx.getVisibility();
        String async = ctx.isAsync() ? "async" : "";
        if (ctx instanceof MethodContext) {
            parameters.add(0, "&self");
        }
        String returnType = !(config.returnType instanceof TyUnit)
            ? " -> " + TypeRendering.renderInsertionSafe(config.returnType, false, false, false)
            : "";
        String paramsText = String.join(", ", parameters);

        return factory.createFunction(
            visibility + " " + async + " fn " + functionName + genericParams + "(" + paramsText + ")" + returnType + " " + whereClause + " {\n    todo!()\n}"
        );
    }

    private static class CallableConfig {
        @Nonnull
        final List<String> parameters;
        @Nonnull
        final Ty returnType;
        @Nonnull
        final GenericConstraints genericConstraints;

        CallableConfig(@Nonnull List<String> parameters, @Nonnull Ty returnType, @Nonnull GenericConstraints genericConstraints) {
            this.parameters = parameters;
            this.returnType = returnType;
            this.genericConstraints = genericConstraints;
        }
    }

    @Nonnull
    private CallableConfig getCallableConfig(@Nonnull Context ctx) {
        RsElement callExpr = ctx.getCallElement();
        RsValueArgumentList arguments = ctx.getArguments();

        List<String> parameters = new ArrayList<>();
        List<RsExpr> exprList = arguments.getExprList();
        for (int i = 0; i < exprList.size(); i++) {
            parameters.add("p" + i + ": " + TypeRendering.renderInsertionSafe(RsTypesUtil.getType(exprList.get(i)), false, false, false));
        }

        Ty returnType = ctx.getReturnType().getType();
        if (returnType instanceof TyUnknown) returnType = TyUnit.INSTANCE;

        List<Ty> allTypes = exprList.stream().map(org.rust.lang.core.types.RsTypesUtil::getType).collect(Collectors.toList());
        allTypes.add(returnType);
        GenericConstraints genericConstraints = GenericConstraints.create(callExpr)
            .filterByTypes(allTypes);

        if (ctx instanceof MethodContext) {
            RsImplItem implItem = RsElementUtil.contextStrict(callExpr, RsImplItem.class);
            List<RsTypeParameter> params = implItem != null ? implItem.getTypeParameters() : java.util.Collections.emptyList();
            genericConstraints = genericConstraints.withoutTypes(params);
        }

        return new CallableConfig(parameters, ctx.getReturnType().getType(), genericConstraints);
    }

    @Nonnull
    private RsFunction insertCallable(@Nonnull Context ctx, @Nonnull RsFunction function) {
        FunctionInsertionPlace place = ctx.getPlace();
        if (place instanceof InPlace) {
            return ((InPlace) place).place.insert(function);
        }
        if (place instanceof InNewImplIn) {
            InNewImplIn inNewImpl = (InNewImplIn) place;
            RsPsiFactory psiFactory = new RsPsiFactory(function.getProject());
            RsImplItem newImpl = psiFactory.createInherentImplItem(
                inNewImpl.itemName,
                inNewImpl.item.getTypeParameterList(),
                inNewImpl.item.getWhereClause()
            );
            RsImplItem insertedImpl = inNewImpl.placeForImpl.insert(newImpl);
            RsMembers members = insertedImpl.getMembers();
            if (members != null) {
                return (RsFunction) members.addBefore(function, members.getRbrace());
            }
        }
        throw new IllegalStateException("Unknown place type: " + place);
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
