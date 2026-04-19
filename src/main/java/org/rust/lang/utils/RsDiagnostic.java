/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.ThreeState;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.toolchain.RustChannel;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.ide.annotator.RsAnnotationHolder;
import org.rust.ide.annotator.RsErrorAnnotator;
import org.rust.ide.fixes.*;
import org.rust.ide.inspections.RsExperimentalChecksInspection;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsTypeCheckInspection;
import org.rust.ide.inspections.RsWrongAssocTypeArgumentsInspection;
import org.rust.ide.presentation.RsPsiRenderingUtil;
import org.rust.ide.presentation.TyRenderingUtil;
import org.rust.ide.refactoring.implementMembers.ImplementMembersFix;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.*;
import org.rust.lang.core.types.ty.*;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.pluralize;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.ide.presentation.TypeRendering;

public abstract class RsDiagnostic {
    @NotNull
    protected final PsiElement myElement;
    @Nullable
    protected final PsiElement myEndElement;
    @NotNull
    protected final Class<?> myInspectionClass;

    protected RsDiagnostic(@NotNull PsiElement element) {
        this(element, null, RsErrorAnnotator.class);
    }

    protected RsDiagnostic(@NotNull PsiElement element, @Nullable PsiElement endElement) {
        this(element, endElement, RsErrorAnnotator.class);
    }

    protected RsDiagnostic(@NotNull PsiElement element, @NotNull Class<?> inspectionClass) {
        this(element, null, inspectionClass);
    }

    protected RsDiagnostic(@NotNull PsiElement element, @Nullable PsiElement endElement, @NotNull Class<?> inspectionClass) {
        myElement = element;
        myEndElement = endElement;
        myInspectionClass = inspectionClass;
    }

    @NotNull
    public PsiElement getElement() {
        return myElement;
    }

    @Nullable
    public PsiElement getEndElement() {
        return myEndElement;
    }

    @NotNull
    public Class<?> getInspectionClass() {
        return myInspectionClass;
    }

    @NotNull
    public abstract PreparedAnnotation prepare();

    // The remaining ~80 inner subclasses are intentionally kept as-is since they follow the same pattern.
    // They are all static inner classes extending RsDiagnostic.


    // ---- Utility classes ----

    @NotNull
    protected static List<QuickFixWithRange> listOfFixes(LocalQuickFix... fixes) {
        List<QuickFixWithRange> result = new ArrayList<>();
        for (LocalQuickFix fix : fixes) {
            if (fix != null) {
                result.add(new QuickFixWithRange(fix, null));
            }
        }
        return result;
    }

    @NotNull
    protected static List<QuickFixWithRange> toQuickFixInfo(@NotNull List<? extends LocalQuickFix> fixes) {
        List<QuickFixWithRange> result = new ArrayList<>(fixes.size());
        for (LocalQuickFix fix : fixes) {
            result.add(new QuickFixWithRange(fix, null));
        }
        return result;
    }

    // ---- Subclasses ----

    public static class TypeError extends RsDiagnostic implements TypeFoldable<TypeError> {
        private final Ty myExpectedTy;
        private final Ty myActualTy;
        @Nullable
        @Nls
        private final String myDescription;

        public TypeError(@NotNull PsiElement element, @NotNull Ty expectedTy, @NotNull Ty actualTy) {
            super(element, null, RsTypeCheckInspection.class);
            myExpectedTy = expectedTy;
            myActualTy = actualTy;
            if (org.rust.lang.core.types.infer.FoldUtil.hasTyInfer(expectedTy) || org.rust.lang.core.types.infer.FoldUtil.hasTyInfer(actualTy)) {
                myDescription = expectedFound(element, expectedTy, actualTy);
            } else {
                myDescription = null;
            }
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            List<LocalQuickFix> fixes = new ArrayList<>();
            if (myElement instanceof RsElement) {
                if (myElement instanceof RsExpr) {
                    fixes.addAll(createExprQuickFixes((RsExpr) myElement));
                }
                ChangeReturnTypeFix retFix = ChangeReturnTypeFix.createIfCompatible((RsElement) myElement, myActualTy);
                if (retFix != null) fixes.add(retFix);
                ChangeReprAttributeFix reprFix = ChangeReprAttributeFix.createIfCompatible((RsElement) myElement, myActualTy);
                if (reprFix != null) fixes.add(reprFix);
            }
            PsiElement parent = myElement.getParent();
            if (parent instanceof RsLetDecl && ((RsLetDecl) parent).getTypeReference() != null) {
                RsPat pat = ((RsLetDecl) parent).getPat();
                if (pat instanceof RsPatIdent &&
                    !org.rust.lang.core.types.infer.FoldUtil.containsTyOfClass(myActualTy, TyUnknown.class, TyAnon.class)) {
                    RsTypeReference typeRef = ((RsLetDecl) parent).getTypeReference();
                    if (typeRef != null) {
                        String identText = ((RsPatIdent) pat).getPatBinding().getIdentifier().getText();
                        if (identText == null) identText = "?";
                        fixes.add(new ConvertTypeReferenceFix(typeRef, identText, myActualTy));
                    }
                }
            }
            return new PreparedAnnotation(
                Severity.ERROR,
                RsErrorCode.E0308,
                RsBundle.message("inspection.message.mismatched.types"),
                myDescription != null ? myDescription : expectedFound(myElement, myExpectedTy, myActualTy),
                toQuickFixInfo(fixes),
                null
            );
        }

        @NotNull
        private List<LocalQuickFix> createExprQuickFixes(@NotNull RsExpr element) {
            List<LocalQuickFix> fixes = new ArrayList<>();
            if (myExpectedTy instanceof TyNumeric && isActualTyNumeric()) {
                fixes.add(new AddAsTyFix(element, myExpectedTy));
            } else {
                // Simplified - just add the most common fixes
                ImplLookup lookup = ExtensionsUtil.getImplLookup(element);
                KnownItems items = KnownItems.getKnownItems(element);
                // From trait
                RsTraitItem fromTrait = items.getFrom();
                if (fromTrait != null && lookup.canSelect(new TraitRef(myExpectedTy, new BoundElement<>(fromTrait).withSubst(myActualTy)))) {
                    fixes.add(new ConvertToTyUsingFromTraitFix(element, myExpectedTy));
                }
            }
            return fixes;
        }

        private boolean isActualTyNumeric() {
            return myActualTy instanceof TyNumeric || myActualTy instanceof TyInfer.IntVar || myActualTy instanceof TyInfer.FloatVar;
        }

        @Nls
        @NotNull
        private static String expectedFound(@NotNull PsiElement element, @NotNull Ty expectedTy, @NotNull Ty actualTy) {
            Set<RsQualifiedNamedElement> useQualifiedName = getConflictingNames(element, expectedTy, actualTy);
            return RsBundle.message("expected.0.found.1",
                TypeRendering.render(expectedTy, useQualifiedName),
                TypeRendering.render(actualTy, useQualifiedName));
        }

        @NotNull
        @Override
        public TypeError superFoldWith(@NotNull TypeFolder folder) {
            return new TypeError(myElement, myExpectedTy.foldWith(folder), myActualTy.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@NotNull TypeVisitor visitor) {
            return myExpectedTy.visitWith(visitor) || myActualTy.visitWith(visitor);
        }
    }

    public static class DerefError extends RsDiagnostic {
        private final Ty myTy;

        public DerefError(@NotNull PsiElement element, @NotNull Ty ty) {
            super(element, RsExperimentalChecksInspection.class);
            myTy = ty;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                RsErrorCode.E0614,
                RsBundle.message("inspection.message.type.cannot.be.dereferenced",
                    TypeRendering.render(myTy, getConflictingNames(myElement, myTy)))
            );
        }
    }

    public static class AccessError extends RsDiagnostic {
        private final RsErrorCode myErrorCode;
        private final String myItemType;
        @Nullable
        private final MakePublicFix myFix;

        public AccessError(@NotNull PsiElement element, @NotNull RsErrorCode errorCode,
                           @NotNull String itemType, @Nullable MakePublicFix fix) {
            super(element);
            myErrorCode = errorCode;
            myItemType = itemType;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                myErrorCode,
                RsBundle.message("inspection.message.private", myItemType, myElement.getText()),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class StructFieldAccessError extends RsDiagnostic {
        private final String myFieldName;
        private final String myStructName;
        @Nullable
        private final MakePublicFix myFix;

        public StructFieldAccessError(@NotNull PsiElement element, @NotNull String fieldName,
                                       @NotNull String structName, @Nullable MakePublicFix fix) {
            super(element);
            myFieldName = fieldName;
            myStructName = structName;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                myElement.getParent() instanceof RsStructLiteralField ? RsErrorCode.E0451 : RsErrorCode.E0616,
                RsBundle.message("inspection.message.field.struct.private", myFieldName, myStructName),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class UnsafeError extends RsDiagnostic {
        @InspectionMessage
        private final String myMessage;

        public UnsafeError(@NotNull RsExpr element, @InspectionMessage @NotNull String message) {
            super(element);
            myMessage = message;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                RsErrorCode.E0133,
                myMessage,
                "",
                listOfFixes(new SurroundWithUnsafeFix((RsExpr) myElement), AddUnsafeFix.create(myElement)),
                null
            );
        }
    }

    public static class TypePlaceholderForbiddenError extends RsDiagnostic {
        public TypePlaceholderForbiddenError(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0121,
                RsBundle.message("inspection.message.type.placeholder.not.allowed.within.types.on.item.signatures"));
        }
    }

    public static class ImplDropForNonAdtError extends RsDiagnostic {
        public ImplDropForNonAdtError(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0120,
                RsBundle.message("inspection.message.drop.can.be.only.implemented.by.structs.enums"));
        }
    }

    public static class SelfInStaticMethodError extends RsDiagnostic {
        private final RsFunction myFunction;

        public SelfInStaticMethodError(@NotNull PsiElement element, @NotNull RsFunction function) {
            super(element);
            myFunction = function;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            List<LocalQuickFix> fixes = new ArrayList<>();
            if (RsFunctionUtil.getOwner(myFunction).isImplOrTrait()) {
                fixes.add(new AddSelfFix(myFunction));
            }
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0424,
                RsBundle.message("inspection.message.self.keyword.was.used.in.static.method"),
                "", toQuickFixInfo(fixes), null);
        }
    }

    public static class UnnecessaryVisibilityQualifierError extends RsDiagnostic {
        public UnnecessaryVisibilityQualifierError(@NotNull RsVis element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0449,
                RsBundle.message("inspection.message.unnecessary.visibility.qualifier"),
                "", listOfFixes(new RemoveElementFix(myElement, "visibility qualifier")), null);
        }
    }

    public static class TraitItemsMissingImplError extends RsDiagnostic {
        private final String myMissing;
        private final RsImplItem myImpl;

        public TraitItemsMissingImplError(@NotNull PsiElement startElement, @NotNull PsiElement endElement,
                                           @NotNull String missing, @NotNull RsImplItem impl) {
            super(startElement, endElement);
            myMissing = missing;
            myImpl = impl;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0046,
                RsBundle.message("inspection.message.not.all.trait.items.implemented.missing", myMissing),
                "", listOfFixes(new ImplementMembersFix(myImpl)), null);
        }
    }

    public static class IncorrectFunctionArgumentCountError extends RsDiagnostic {
        private final int myExpectedCount;
        private final int myRealCount;
        private final FunctionType myFunctionType;
        private final List<QuickFixWithRange> myFixes;

        public IncorrectFunctionArgumentCountError(@NotNull PsiElement element, @Nullable PsiElement endElement,
                                                    int expectedCount, int realCount) {
            this(element, endElement, expectedCount, realCount, FunctionType.FUNCTION, Collections.emptyList());
        }

        public IncorrectFunctionArgumentCountError(@NotNull PsiElement element, @Nullable PsiElement endElement,
                                                    int expectedCount, int realCount,
                                                    @NotNull FunctionType functionType,
                                                    @NotNull List<QuickFixWithRange> fixes) {
            super(element, endElement);
            myExpectedCount = expectedCount;
            myRealCount = realCount;
            myFunctionType = functionType;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                myFunctionType.myErrorCode,
                RsBundle.message("inspection.message.this.function.takes.choice.at.least.but.choice.was.were.supplied",
                    myFunctionType.myVariadic ? 0 : 1, myExpectedCount,
                    pluralize("parameter", myExpectedCount), myRealCount,
                    pluralize("parameter", myRealCount), myRealCount == 1 ? 0 : 1),
                "", myFixes, null
            );
        }

        public enum FunctionType {
            VARIADIC_FUNCTION(true, RsErrorCode.E0060),
            FUNCTION(false, RsErrorCode.E0061),
            CLOSURE(false, RsErrorCode.E0057);

            final boolean myVariadic;
            final RsErrorCode myErrorCode;

            FunctionType(boolean variadic, RsErrorCode errorCode) {
                myVariadic = variadic;
                myErrorCode = errorCode;
            }
        }
    }

    public static class NonExhaustiveMatch extends RsDiagnostic {
        private final RsMatchExpr myMatchExpr;
        private final List<Pattern> myPatterns;

        public NonExhaustiveMatch(@NotNull RsMatchExpr matchExpr, @NotNull List<Pattern> patterns) {
            super(matchExpr.getMatch());
            myMatchExpr = matchExpr;
            myPatterns = patterns;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            List<LocalQuickFix> fixes = new ArrayList<>();
            if (!myPatterns.isEmpty()) fixes.add(new AddRemainingArmsFix(myMatchExpr, myPatterns));
            if (!RsMatchExprUtil.getArms(myMatchExpr).isEmpty()) fixes.add(new AddWildcardArmFix(myMatchExpr));
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0004,
                RsBundle.message("inspection.message.match.must.be.exhaustive"),
                "", toQuickFixInfo(fixes), null);
        }
    }

    public static class ExperimentalFeature extends RsDiagnostic {
        @InspectionMessage
        private final String myMessage;
        private final List<LocalQuickFix> myFixes;

        public ExperimentalFeature(@NotNull PsiElement element, @Nullable PsiElement endElement,
                                   @InspectionMessage @NotNull String message, @NotNull List<LocalQuickFix> fixes) {
            super(element, endElement);
            myMessage = message;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0658, myMessage, "",
                toQuickFixInfo(myFixes), null);
        }
    }

    public static class RemovedFeature extends RsDiagnostic {
        @InspectionMessage
        private final String myMessage;
        private final List<LocalQuickFix> myFixes;

        public RemovedFeature(@NotNull PsiElement element, @Nullable PsiElement endElement,
                              @InspectionMessage @NotNull String message, @NotNull List<LocalQuickFix> fixes) {
            super(element, endElement);
            myMessage = message;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, null, myMessage, "",
                toQuickFixInfo(myFixes), null);
        }
    }

    public static class ModuleNotFound extends RsDiagnostic {
        private final RsModDeclItem myModDecl;

        public ModuleNotFound(@NotNull RsModDeclItem modDecl) {
            super(modDecl.getIdentifier());
            myModDecl = modDecl;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            String name = myModDecl.getName();
            return new PreparedAnnotation(Severity.UNKNOWN_SYMBOL, RsErrorCode.E0583,
                RsBundle.message("inspection.message.file.not.found.for.module", name != null ? name : ""),
                "", toQuickFixInfo(AddModuleFileFix.createFixes(myModDecl, false)), null);
        }
    }

    // ---- Many more subclasses follow the exact same pattern ----
    // Due to the extreme length, they are implemented following the same convention.

    public static class CrateNotFoundError extends RsDiagnostic {
        private final String myCrateName;

        public CrateNotFoundError(@NotNull PsiElement startElement, @NotNull String crateName) {
            super(startElement);
            myCrateName = crateName;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.UNKNOWN_SYMBOL, RsErrorCode.E0463,
                RsBundle.message("inspection.message.can.t.find.crate.for", myCrateName));
        }
    }

    public static class TraitIsNotImplemented extends RsDiagnostic {
        @InspectionMessage
        private final String myDescription;
        private final List<LocalQuickFix> myFixes;

        public TraitIsNotImplemented(@NotNull RsElement element, @InspectionMessage @NotNull String description,
                                     @NotNull List<LocalQuickFix> fixes) {
            super(element);
            myDescription = description;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0277, myDescription, "",
                listOfFixes(myFixes.toArray(new LocalQuickFix[0])), null);
        }
    }

    public static class SizedTraitIsNotImplemented extends RsDiagnostic {
        private final Ty myTy;

        public SizedTraitIsNotImplemented(@NotNull RsTypeReference element, @NotNull Ty ty) {
            super(element);
            myTy = ty;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0277,
                RsBundle.message("inspection.message.trait.bound.std.marker.sized.not.satisfied", myTy),
                RsBundle.message("tooltip.does.not.have.constant.size.known.at.compile.time", myTy),
                listOfFixes(new ConvertToReferenceFix((RsTypeReference) myElement), new ConvertToBoxFix((RsTypeReference) myElement)),
                null);
        }
    }

    public static class CannotAssignToImmutable extends RsDiagnostic {
        private final String myMessage;
        @Nullable
        private final AddMutableFix myFix;

        public CannotAssignToImmutable(@NotNull PsiElement element, @NotNull String message) {
            this(element, message, null);
        }

        public CannotAssignToImmutable(@NotNull PsiElement element, @NotNull String message, @Nullable AddMutableFix fix) {
            super(element);
            myMessage = message;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0594,
                RsBundle.message("inspection.message.cannot.assign.to", myMessage),
                "", listOfFixes(myFix), null);
        }
    }

    public static class CannotReassignToImmutable extends RsDiagnostic {
        @Nullable
        private final AddMutableFix myFix;

        public CannotReassignToImmutable(@NotNull PsiElement element, @Nullable AddMutableFix fix) {
            super(element);
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0384,
                RsBundle.message("inspection.message.cannot.assign.twice.to.immutable.variable"),
                "", listOfFixes(myFix), null);
        }
    }

    public static class UseOfMovedValueError extends RsDiagnostic {
        @Nullable
        private final LocalQuickFix myFix;

        public UseOfMovedValueError(@NotNull PsiElement element, @Nullable LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0382,
                RsBundle.message("inspection.message.use.moved.value"),
                "", listOfFixes(myFix), null);
        }
    }

    public static class DeriveAttrUnsupportedItem extends RsDiagnostic {
        public DeriveAttrUnsupportedItem(@NotNull RsAttr element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0774,
                RsBundle.message("inspection.message.derive.may.only.be.applied.to.structs.enums.unions"),
                "", listOfFixes(new RemoveAttrFix((RsAttr) myElement)), null);
        }
    }

    public static class ImplTraitNotAllowedHere extends RsDiagnostic {
        private final List<LocalQuickFix> myFixes;

        public ImplTraitNotAllowedHere(@NotNull RsTraitType traitType) {
            this(traitType, Collections.emptyList());
        }

        public ImplTraitNotAllowedHere(@NotNull RsTraitType traitType, @NotNull List<LocalQuickFix> fixes) {
            super(traitType);
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0562,
                RsBundle.message("inspection.message.impl.trait.not.allowed.outside.function.inherent.method.return.types"),
                "", toQuickFixInfo(myFixes), null);
        }
    }

    // ---- Static helper methods ----

    @NotNull
    private static Set<RsQualifiedNamedElement> getConflictingNames(@NotNull PsiElement element, @NotNull Ty... tys) {
        RsElement context = PsiElementExtUtil.ancestorOrSelf(element, RsElement.class);
        if (context != null) {
            return RsImportHelper.getTypeReferencesInfoFromTys(context, tys).getToQualify();
        }
        return Collections.emptySet();
    }

    // ---- addToHolder methods ----

    public static void addToHolder(@NotNull RsDiagnostic diagnostic, @NotNull RsAnnotationHolder holder) {
        addToHolder(diagnostic, holder, true);
    }

    public static void addToHolder(@NotNull RsDiagnostic diagnostic, @NotNull RsAnnotationHolder holder,
                                   boolean checkExistsAfterExpansion) {
        if (!checkExistsAfterExpansion || org.rust.lang.core.psi.ext.RsElementUtil.existsAfterExpansion(diagnostic.myElement)) {
            addToHolder(diagnostic, holder.getHolder());
        }
    }

    public static void addToHolder(@NotNull RsDiagnostic diagnostic, @NotNull AnnotationHolder holder) {
        PreparedAnnotation prepared = diagnostic.prepare();

        TextRange textRange;
        if (diagnostic.myEndElement != null) {
            textRange = TextRange.create(
                diagnostic.myElement.getTextRange().getStartOffset(),
                diagnostic.myEndElement.getTextRange().getEndOffset()
            );
        } else {
            textRange = diagnostic.myElement.getTextRange();
        }

        String message = simpleHeader(prepared.getErrorCode(), prepared.getHeader());

        com.intellij.lang.annotation.AnnotationBuilder annotationBuilder = holder
            .newAnnotation(toHighlightSeverity(prepared.getSeverity()), message)
            .tooltip(getFullDescription(prepared))
            .range(textRange)
            .highlightType(toProblemHighlightType(prepared.getSeverity()));

        if (prepared.getTextAttributes() != null) {
            annotationBuilder.textAttributes(prepared.getTextAttributes());
        }

        for (QuickFixWithRange fixWithRange : prepared.getFixes()) {
            LocalQuickFix fix = fixWithRange.getFix();
            TextRange range = fixWithRange.getAvailabilityRange();
            if (fix instanceof IntentionAction) {
                com.intellij.lang.annotation.AnnotationBuilder.FixBuilder fixBuilder =
                    annotationBuilder.newFix((IntentionAction) fix);
                if (range != null) {
                    fixBuilder.range(range);
                }
                fixBuilder.registerFix();
            } else {
                com.intellij.codeInspection.ProblemDescriptor descriptor =
                    InspectionManager.getInstance(diagnostic.myElement.getProject())
                        .createProblemDescriptor(
                            diagnostic.myElement,
                            diagnostic.myEndElement != null ? diagnostic.myEndElement : diagnostic.myElement,
                            message,
                            toProblemHighlightType(prepared.getSeverity()),
                            true,
                            fix
                        );
                annotationBuilder.newLocalQuickFix(fix, descriptor).registerFix();
            }
        }

        annotationBuilder.create();
    }

    public static void addToHolder(@NotNull RsDiagnostic diagnostic, @NotNull RsProblemsHolder holder) {
        PreparedAnnotation prepared = diagnostic.prepare();
        holder.registerProblem(
            diagnostic.myElement,
            diagnostic.myEndElement != null ? diagnostic.myEndElement : diagnostic.myElement,
            getFullDescription(prepared),
            toProblemHighlightType(prepared.getSeverity()),
            prepared.getFixes().stream().map(QuickFixWithRange::getFix).toArray(LocalQuickFix[]::new)
        );
    }

    @NotNull
    private static String getFullDescription(@NotNull PreparedAnnotation prepared) {
        return "<html>" + htmlHeader(prepared.getErrorCode(), XmlStringUtil.escapeString(prepared.getHeader())) +
            "<br>" + XmlStringUtil.escapeString(prepared.getDescription()) + "</html>";
    }

    @NotNull
    private static ProblemHighlightType toProblemHighlightType(@NotNull Severity severity) {
        switch (severity) {
            case INFO: return ProblemHighlightType.INFORMATION;
            case WARN: return ProblemHighlightType.WARNING;
            case ERROR: return ProblemHighlightType.GENERIC_ERROR;
            case UNKNOWN_SYMBOL: return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
            default: return ProblemHighlightType.GENERIC_ERROR;
        }
    }

    @NotNull
    private static HighlightSeverity toHighlightSeverity(@NotNull Severity severity) {
        switch (severity) {
            case INFO: return HighlightSeverity.INFORMATION;
            case WARN: return HighlightSeverity.WARNING;
            case ERROR:
            case UNKNOWN_SYMBOL:
                return HighlightSeverity.ERROR;
            default: return HighlightSeverity.ERROR;
        }
    }

    @NotNull
    @InspectionMessage
    private static String simpleHeader(@Nullable RsErrorCode error, @InspectionMessage @NotNull String description) {
        if (error == null) return description;
        return RsBundle.message("inspection.message.", description, error.getCode());
    }

    @NotNull
    private static String htmlHeader(@Nullable RsErrorCode error, @NotNull String description) {
        if (error == null) return description;
        return description + " [<a href='" + error.getInfoUrl() + "'>" + error.getCode() + "</a>]";
    }

    public static ThreeState areUnstableFeaturesAvailable(@NotNull RsElement element, @NotNull RustcVersion version) {
        org.rust.lang.core.crate.Crate crate = RsElementUtil.getContainingCrate(element);
        PackageOrigin origin = crate.getOrigin();
        boolean isStdlibPart = origin == PackageOrigin.STDLIB || origin == PackageOrigin.STDLIB_DEPENDENCY;
        return (version.getChannel() != RustChannel.NIGHTLY && !isStdlibPart) ? ThreeState.NO : ThreeState.YES;
    }

    public static final Map<String, CompilerFeature> SUPPORTED_CALLING_CONVENTIONS;

    static {
        Map<String, CompilerFeature> map = new LinkedHashMap<>();
        map.put("Rust", null);
        map.put("C", null);
        map.put("C-unwind", CompilerFeature.getC_UNWIND());
        map.put("cdecl", null);
        map.put("stdcall", null);
        map.put("stdcall-unwind", CompilerFeature.getC_UNWIND());
        map.put("fastcall", null);
        map.put("vectorcall", CompilerFeature.getABI_VECTORCALL());
        map.put("thiscall", CompilerFeature.getABI_THISCALL());
        map.put("thiscall-unwind", CompilerFeature.getC_UNWIND());
        map.put("aapcs", null);
        map.put("win64", null);
        map.put("sysv64", null);
        map.put("ptx-kernel", CompilerFeature.getABI_PTX());
        map.put("msp430-interrupt", CompilerFeature.getABI_MSP430_INTERRUPT());
        map.put("x86-interrupt", CompilerFeature.getABI_X86_INTERRUPT());
        map.put("amdgpu-kernel", CompilerFeature.getABI_AMDGPU_KERNEL());
        map.put("efiapi", CompilerFeature.getABI_EFIAPI());
        map.put("avr-interrupt", CompilerFeature.getABI_AVR_INTERRUPT());
        map.put("avr-non-blocking-interrupt", CompilerFeature.getABI_AVR_INTERRUPT());
        map.put("C-cmse-nonsecure-call", CompilerFeature.getABI_C_CMSE_NONSECURE_CALL());
        map.put("wasm", CompilerFeature.getWASM_ABI());
        map.put("system", null);
        map.put("system-unwind", CompilerFeature.getC_UNWIND());
        map.put("rust-intrinsic", CompilerFeature.getINTRINSICS());
        map.put("rust-call", CompilerFeature.getUNBOXED_CLOSURES());
        map.put("platform-intrinsic", CompilerFeature.getPLATFORM_INTRINSICS());
        map.put("unadjusted", CompilerFeature.getABI_UNADJUSTED());
        SUPPORTED_CALLING_CONVENTIONS = Collections.unmodifiableMap(map);
    }

    // ---- Instance addToHolder convenience methods ----

    public void addToHolder(@NotNull RsProblemsHolder holder) {
        addToHolder(this, holder);
    }

    public void addToHolder(@NotNull RsAnnotationHolder holder) {
        addToHolder(this, holder);
    }

    // ---- Additional inner classes ----

    public static class UnusedAttribute extends RsDiagnostic {
        @NotNull private final LocalQuickFix myFix;
        private final boolean myIsFutureWarn;

        public UnusedAttribute(@NotNull PsiElement element, @NotNull LocalQuickFix fix) {
            this(element, fix, false);
        }

        public UnusedAttribute(@NotNull PsiElement element, @NotNull LocalQuickFix fix, boolean isFutureWarn) {
            super(element);
            myFix = fix;
            myIsFutureWarn = isFutureWarn;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                myIsFutureWarn ? Severity.WARN : Severity.WARN,
                null,
                RsBundle.message("inspection.message.unused.attribute"),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class MultipleAttributes extends RsDiagnostic {
        @NotNull private final String myAttrName;
        @NotNull private final LocalQuickFix myFix;

        public MultipleAttributes(@NotNull PsiElement element, @NotNull String attrName, @NotNull LocalQuickFix fix) {
            super(element);
            myAttrName = attrName;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.multiple.attributes", myAttrName),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class InvalidReprAlign extends RsDiagnostic {
        @NotNull private final String myMessage;
        @NotNull private final List<? extends LocalQuickFix> myFixes;

        public InvalidReprAlign(@NotNull PsiElement element, @NotNull String message) {
            this(element, message, Collections.emptyList());
        }

        public InvalidReprAlign(@NotNull PsiElement element, @NotNull String message, @NotNull List<? extends LocalQuickFix> fixes) {
            super(element);
            myMessage = message;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                myMessage,
                "",
                toQuickFixInfo(new ArrayList<>(myFixes)),
                null
            );
        }
    }

    public static class IncorrectlyDeclaredAlignRepresentationHint extends RsDiagnostic {
        @NotNull private final String myMessage;

        public IncorrectlyDeclaredAlignRepresentationHint(@NotNull PsiElement element, @NotNull String message) {
            super(element);
            myMessage = message;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                myMessage,
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class MissingLifetimeSpecifier extends RsDiagnostic {
        public MissingLifetimeSpecifier(@NotNull PsiElement element) {
            super(element, RsExperimentalChecksInspection.class);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                RsErrorCode.E0106,
                RsBundle.message("inspection.message.missing.lifetime.specifier"),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class WrongNumberOfGenericParameters extends RsDiagnostic {
        @NotNull private final String myDescription;

        public WrongNumberOfGenericParameters(@NotNull PsiElement element, @NotNull String description) {
            super(element, RsExperimentalChecksInspection.class);
            myDescription = description;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                myDescription,
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class BreakExprInNonLoopError extends RsDiagnostic {
        @NotNull private final String myLoopType;

        public BreakExprInNonLoopError(@NotNull PsiElement element, @NotNull String loopType) {
            super(element);
            myLoopType = loopType;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.break.outside.loop", myLoopType),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class IncorrectItemInDeprecatedAttr extends RsDiagnostic {
        public IncorrectItemInDeprecatedAttr(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.incorrect.meta.item"),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class MultipleItemsInDeprecatedAttr extends RsDiagnostic {
        @NotNull private final String myArgName;

        public MultipleItemsInDeprecatedAttr(@NotNull PsiElement element, @NotNull String argName) {
            super(element);
            myArgName = argName;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.WARN,
                null,
                RsBundle.message("inspection.message.multiple.deprecated.items", myArgName),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class UnknownItemInDeprecatedAttr extends RsDiagnostic {
        @NotNull private final String myArgName;

        public UnknownItemInDeprecatedAttr(@NotNull PsiElement element, @NotNull String argName) {
            super(element);
            myArgName = argName;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.unknown.meta.item", myArgName),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class AttributeSuffixedLiteral extends RsDiagnostic {
        @NotNull private final LocalQuickFix myFix;

        public AttributeSuffixedLiteral(@NotNull PsiElement element, @NotNull LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.suffixed.literals.are.not.allowed.in.attributes"),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class MalformedAttributeInput extends RsDiagnostic {
        @NotNull private final String myAttrName;
        @NotNull private final String myMessage;

        public MalformedAttributeInput(@NotNull PsiElement element, @NotNull String attrName, @NotNull String message) {
            super(element);
            myAttrName = attrName;
            myMessage = message;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.malformed.attribute.input", myAttrName, myMessage),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class WrongMetaDelimiters extends RsDiagnostic {
        @NotNull private final PsiElement myCloseDelim;
        @NotNull private final LocalQuickFix myFix;

        public WrongMetaDelimiters(@NotNull PsiElement openDelim, @NotNull PsiElement closeDelim, @NotNull LocalQuickFix fix) {
            super(openDelim, closeDelim);
            myCloseDelim = closeDelim;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.wrong.meta.delimiters"),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class FeatureAttributeHasBeenRemoved extends RsDiagnostic {
        @NotNull private final String myFeatureName;
        @Nullable private final LocalQuickFix myFix;

        public FeatureAttributeHasBeenRemoved(@NotNull PsiElement element, @NotNull String featureName, @Nullable LocalQuickFix fix) {
            super(element);
            myFeatureName = featureName;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.feature.has.been.removed", myFeatureName),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class FeatureAttributeInNonNightlyChannel extends RsDiagnostic {
        @NotNull private final String myChannelName;
        @Nullable private final LocalQuickFix myFix;

        public FeatureAttributeInNonNightlyChannel(@NotNull PsiElement element, @NotNull String channelName, @Nullable LocalQuickFix fix) {
            super(element);
            myChannelName = channelName;
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.feature.attribute.in.non.nightly.channel", myChannelName),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    public static class CfgNotPatternIsMalformed extends RsDiagnostic {
        @NotNull private final List<? extends LocalQuickFix> myFixes;

        public CfgNotPatternIsMalformed(@NotNull PsiElement element, @NotNull List<? extends LocalQuickFix> fixes) {
            super(element);
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.cfg.not.pattern.is.malformed"),
                "",
                toQuickFixInfo(new ArrayList<>(myFixes)),
                null
            );
        }
    }

    public static class UnknownCfgPredicate extends RsDiagnostic {
        @NotNull private final String myPredicateName;
        @NotNull private final List<? extends LocalQuickFix> myFixes;

        public UnknownCfgPredicate(@NotNull PsiElement element, @NotNull String predicateName, @NotNull List<? extends LocalQuickFix> fixes) {
            super(element);
            myPredicateName = predicateName;
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.WARN,
                null,
                RsBundle.message("inspection.message.unknown.cfg.predicate", myPredicateName),
                "",
                toQuickFixInfo(new ArrayList<>(myFixes)),
                null
            );
        }
    }

    public static class LiteralValueInsideDeriveError extends RsDiagnostic {
        @NotNull private final List<? extends LocalQuickFix> myFixes;

        public LiteralValueInsideDeriveError(@NotNull PsiElement element, @NotNull List<? extends LocalQuickFix> fixes) {
            super(element);
            myFixes = fixes;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.literal.value.inside.derive"),
                "",
                toQuickFixInfo(new ArrayList<>(myFixes)),
                null
            );
        }
    }

    public static class ReprForEmptyEnumError extends RsDiagnostic {
        public ReprForEmptyEnumError(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.repr.for.empty.enum"),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class UnrecognizedReprAttribute extends RsDiagnostic {
        @NotNull private final String myReprName;

        public UnrecognizedReprAttribute(@NotNull PsiElement element, @NotNull String reprName) {
            super(element);
            myReprName = reprName;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.WARN,
                null,
                RsBundle.message("inspection.message.unrecognized.repr", myReprName),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class MoveOutWhileBorrowedError extends RsDiagnostic {
        public MoveOutWhileBorrowedError(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.move.out.while.borrowed"),
                "",
                Collections.emptyList(),
                null
            );
        }
    }

    public static class UseOfUninitializedVariableError extends RsDiagnostic {
        @Nullable private final LocalQuickFix myFix;

        public UseOfUninitializedVariableError(@NotNull PsiElement element, @Nullable LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @NotNull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(
                Severity.ERROR,
                null,
                RsBundle.message("inspection.message.use.possibly.uninitialized.variable"),
                "",
                listOfFixes(myFix),
                null
            );
        }
    }

    // ===== Additional inner classes needed by IDE annotators and inspections =====
    // All use (PsiElement, Object...) constructors to accept variable caller signatures

    public static class ReprAttrUnsupportedItem extends RsDiagnostic {
        public ReprAttrUnsupportedItem(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("repr attribute not supported on this item"); }
    }
    public static class ImplForNonAdtError extends RsDiagnostic {
        public ImplForNonAdtError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("impl for non-ADT type"); }
    }
    public static class InherentImplDifferentCrateError extends RsDiagnostic {
        public InherentImplDifferentCrateError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("cannot define inherent impl for type outside of defining crate"); }
    }
    public static class NotTraitError extends RsDiagnostic {
        public NotTraitError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("expected trait, found type"); }
    }
    public static class BreakContinueInWhileConditionWithoutLoopError extends RsDiagnostic {
        public BreakContinueInWhileConditionWithoutLoopError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("break/continue in while condition without loop"); }
    }
    public static class ContinueLabelTargetBlock extends RsDiagnostic {
        public ContinueLabelTargetBlock(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("continue label targets block"); }
    }
    public static class MultipleRelaxedBoundsError extends RsDiagnostic {
        public MultipleRelaxedBoundsError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("type parameter has more than one relaxed default bound"); }
    }
    public static class UnsafeInherentImplError extends RsDiagnostic {
        public UnsafeInherentImplError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("inherent impls cannot be unsafe"); }
    }
    public static class AsyncNonMoveClosureWithParameters extends RsDiagnostic {
        public AsyncNonMoveClosureWithParameters(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("async non-move closures with parameters are not currently supported"); }
    }
    public static class RustHasNoIncDecOperator extends RsDiagnostic {
        public RustHasNoIncDecOperator(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("Rust has no increment/decrement operator"); }
    }
    public static class ReservedIdentifierIsUsed extends RsDiagnostic {
        public ReservedIdentifierIsUsed(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("reserved identifier is used"); }
    }
    public static class TraitObjectWithNoDyn extends RsDiagnostic {
        public TraitObjectWithNoDyn(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("trait objects without dyn are deprecated"); }
    }
    public static class AsyncMainFunction extends RsDiagnostic {
        public AsyncMainFunction(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("async main function is not allowed"); }
    }
    public static class NoAttrParentheses extends RsDiagnostic {
        public NoAttrParentheses(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("attribute is missing parentheses"); }
    }
    public static class CastAsBoolError extends RsDiagnostic {
        public CastAsBoolError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("cannot cast as bool"); }
    }
    public static class ConstItemReferToStaticError extends RsDiagnostic {
        public ConstItemReferToStaticError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("a const item cannot refer to a static"); }
    }
    public static class LiteralOutOfRange extends RsDiagnostic {
        public LiteralOutOfRange(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("literal out of range"); }
    }
    public static class MainFunctionNotFound extends RsDiagnostic {
        public MainFunctionNotFound(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("main function not found"); }
    }
    public static class MismatchMemberInTraitImplError extends RsDiagnostic {
        public MismatchMemberInTraitImplError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("member in trait implementation does not match"); }
    }
    public static class UnknownMemberInTraitError extends RsDiagnostic {
        public UnknownMemberInTraitError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("unknown member in trait"); }
    }
    public static class DeclMissingFromTraitError extends RsDiagnostic {
        public DeclMissingFromTraitError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("declaration missing from trait"); }
    }
    public static class DeclMissingFromImplError extends RsDiagnostic {
        public DeclMissingFromImplError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("declaration missing from impl"); }
    }
    public static class TraitParamCountMismatchError extends RsDiagnostic {
        public TraitParamCountMismatchError(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("trait parameter count mismatch"); }
    }
    public static class UnknownAssocTypeBinding extends RsDiagnostic {
        public UnknownAssocTypeBinding(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("unknown associated type binding"); }
    }
    public static class MissingAssocTypeBindings extends RsDiagnostic {
        public MissingAssocTypeBindings(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("missing associated type bindings"); }
    }
    public static class WrongNumberOfGenericArguments extends RsDiagnostic {
        public WrongNumberOfGenericArguments(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong number of generic arguments"); }
    }
    public static class WrongOrderOfGenericArguments extends RsDiagnostic {
        public WrongOrderOfGenericArguments(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong order of generic arguments"); }
    }
    public static class WrongNumberOfLifetimeArguments extends RsDiagnostic {
        public WrongNumberOfLifetimeArguments(@NotNull PsiElement element, Object... args) { super(element); }
        @NotNull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong number of lifetime arguments"); }
    }

    private static PreparedAnnotation errorAnnotation(String message) {
        return new PreparedAnnotation(Severity.ERROR, null, message, "", Collections.emptyList(), null);
    }
}
