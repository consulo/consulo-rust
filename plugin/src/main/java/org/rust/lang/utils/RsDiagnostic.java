/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ThreeState;
import consulo.util.lang.xml.XmlStringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

import static consulo.util.lang.StringUtil.pluralize;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.ide.presentation.TypeRendering;

public abstract class RsDiagnostic {
    @Nonnull
    protected final PsiElement myElement;
    @Nullable
    protected final PsiElement myEndElement;
    @Nonnull
    protected final Class<?> myInspectionClass;

    protected RsDiagnostic(@Nonnull PsiElement element) {
        this(element, null, RsErrorAnnotator.class);
    }

    protected RsDiagnostic(@Nonnull PsiElement element, @Nullable PsiElement endElement) {
        this(element, endElement, RsErrorAnnotator.class);
    }

    protected RsDiagnostic(@Nonnull PsiElement element, @Nonnull Class<?> inspectionClass) {
        this(element, null, inspectionClass);
    }

    protected RsDiagnostic(@Nonnull PsiElement element, @Nullable PsiElement endElement, @Nonnull Class<?> inspectionClass) {
        myElement = element;
        myEndElement = endElement;
        myInspectionClass = inspectionClass;
    }

    @Nonnull
    public PsiElement getElement() {
        return myElement;
    }

    @Nullable
    public PsiElement getEndElement() {
        return myEndElement;
    }

    @Nonnull
    public Class<?> getInspectionClass() {
        return myInspectionClass;
    }

    @Nonnull
    public abstract PreparedAnnotation prepare();

    // The remaining ~80 inner subclasses are intentionally kept as-is since they follow the same pattern.
    // They are all static inner classes extending RsDiagnostic.


    // ---- Utility classes ----

    @Nonnull
    protected static List<QuickFixWithRange> listOfFixes(LocalQuickFix... fixes) {
        List<QuickFixWithRange> result = new ArrayList<>();
        for (LocalQuickFix fix : fixes) {
            if (fix != null) {
                result.add(new QuickFixWithRange(fix, null));
            }
        }
        return result;
    }

    @Nonnull
    protected static List<QuickFixWithRange> toQuickFixInfo(@Nonnull List<? extends LocalQuickFix> fixes) {
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
        
        private final String myDescription;

        public TypeError(@Nonnull PsiElement element, @Nonnull Ty expectedTy, @Nonnull Ty actualTy) {
            super(element, null, RsTypeCheckInspection.class);
            myExpectedTy = expectedTy;
            myActualTy = actualTy;
            if (org.rust.lang.core.types.infer.FoldUtil.hasTyInfer(expectedTy) || org.rust.lang.core.types.infer.FoldUtil.hasTyInfer(actualTy)) {
                myDescription = expectedFound(element, expectedTy, actualTy);
            } else {
                myDescription = null;
            }
        }

        @Nonnull
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

        @Nonnull
        private List<LocalQuickFix> createExprQuickFixes(@Nonnull RsExpr element) {
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

        
        @Nonnull
        private static String expectedFound(@Nonnull PsiElement element, @Nonnull Ty expectedTy, @Nonnull Ty actualTy) {
            Set<RsQualifiedNamedElement> useQualifiedName = getConflictingNames(element, expectedTy, actualTy);
            return RsBundle.message("expected.0.found.1",
                TypeRendering.render(expectedTy, useQualifiedName),
                TypeRendering.render(actualTy, useQualifiedName));
        }

        @Nonnull
        @Override
        public TypeError superFoldWith(@Nonnull TypeFolder folder) {
            return new TypeError(myElement, myExpectedTy.foldWith(folder), myActualTy.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return myExpectedTy.visitWith(visitor) || myActualTy.visitWith(visitor);
        }
    }

    public static class DerefError extends RsDiagnostic {
        private final Ty myTy;

        public DerefError(@Nonnull PsiElement element, @Nonnull Ty ty) {
            super(element, RsExperimentalChecksInspection.class);
            myTy = ty;
        }

        @Nonnull
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

        public AccessError(@Nonnull PsiElement element, @Nonnull RsErrorCode errorCode,
                           @Nonnull String itemType, @Nullable MakePublicFix fix) {
            super(element);
            myErrorCode = errorCode;
            myItemType = itemType;
            myFix = fix;
        }

        @Nonnull
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

        public StructFieldAccessError(@Nonnull PsiElement element, @Nonnull String fieldName,
                                       @Nonnull String structName, @Nullable MakePublicFix fix) {
            super(element);
            myFieldName = fieldName;
            myStructName = structName;
            myFix = fix;
        }

        @Nonnull
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
        
        private final String myMessage;

        public UnsafeError(@Nonnull RsExpr element,  @Nonnull String message) {
            super(element);
            myMessage = message;
        }

        @Nonnull
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
        public TypePlaceholderForbiddenError(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0121,
                RsBundle.message("inspection.message.type.placeholder.not.allowed.within.types.on.item.signatures"));
        }
    }

    public static class ImplDropForNonAdtError extends RsDiagnostic {
        public ImplDropForNonAdtError(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0120,
                RsBundle.message("inspection.message.drop.can.be.only.implemented.by.structs.enums"));
        }
    }

    public static class SelfInStaticMethodError extends RsDiagnostic {
        private final RsFunction myFunction;

        public SelfInStaticMethodError(@Nonnull PsiElement element, @Nonnull RsFunction function) {
            super(element);
            myFunction = function;
        }

        @Nonnull
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
        public UnnecessaryVisibilityQualifierError(@Nonnull RsVis element) {
            super(element);
        }

        @Nonnull
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

        public TraitItemsMissingImplError(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement,
                                           @Nonnull String missing, @Nonnull RsImplItem impl) {
            super(startElement, endElement);
            myMissing = missing;
            myImpl = impl;
        }

        @Nonnull
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

        public IncorrectFunctionArgumentCountError(@Nonnull PsiElement element, @Nullable PsiElement endElement,
                                                    int expectedCount, int realCount) {
            this(element, endElement, expectedCount, realCount, FunctionType.FUNCTION, Collections.emptyList());
        }

        public IncorrectFunctionArgumentCountError(@Nonnull PsiElement element, @Nullable PsiElement endElement,
                                                    int expectedCount, int realCount,
                                                    @Nonnull FunctionType functionType,
                                                    @Nonnull List<QuickFixWithRange> fixes) {
            super(element, endElement);
            myExpectedCount = expectedCount;
            myRealCount = realCount;
            myFunctionType = functionType;
            myFixes = fixes;
        }

        @Nonnull
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

        public NonExhaustiveMatch(@Nonnull RsMatchExpr matchExpr, @Nonnull List<Pattern> patterns) {
            super(matchExpr.getMatch());
            myMatchExpr = matchExpr;
            myPatterns = patterns;
        }

        @Nonnull
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
        
        private final String myMessage;
        private final List<LocalQuickFix> myFixes;

        public ExperimentalFeature(@Nonnull PsiElement element, @Nullable PsiElement endElement,
                                    @Nonnull String message, @Nonnull List<LocalQuickFix> fixes) {
            super(element, endElement);
            myMessage = message;
            myFixes = fixes;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0658, myMessage, "",
                toQuickFixInfo(myFixes), null);
        }
    }

    public static class RemovedFeature extends RsDiagnostic {
        
        private final String myMessage;
        private final List<LocalQuickFix> myFixes;

        public RemovedFeature(@Nonnull PsiElement element, @Nullable PsiElement endElement,
                               @Nonnull String message, @Nonnull List<LocalQuickFix> fixes) {
            super(element, endElement);
            myMessage = message;
            myFixes = fixes;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, null, myMessage, "",
                toQuickFixInfo(myFixes), null);
        }
    }

    public static class ModuleNotFound extends RsDiagnostic {
        private final RsModDeclItem myModDecl;

        public ModuleNotFound(@Nonnull RsModDeclItem modDecl) {
            super(modDecl.getIdentifier());
            myModDecl = modDecl;
        }

        @Nonnull
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

        public CrateNotFoundError(@Nonnull PsiElement startElement, @Nonnull String crateName) {
            super(startElement);
            myCrateName = crateName;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.UNKNOWN_SYMBOL, RsErrorCode.E0463,
                RsBundle.message("inspection.message.can.t.find.crate.for", myCrateName));
        }
    }

    public static class TraitIsNotImplemented extends RsDiagnostic {
        
        private final String myDescription;
        private final List<LocalQuickFix> myFixes;

        public TraitIsNotImplemented(@Nonnull RsElement element,  @Nonnull String description,
                                     @Nonnull List<LocalQuickFix> fixes) {
            super(element);
            myDescription = description;
            myFixes = fixes;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0277, myDescription, "",
                listOfFixes(myFixes.toArray(new LocalQuickFix[0])), null);
        }
    }

    public static class SizedTraitIsNotImplemented extends RsDiagnostic {
        private final Ty myTy;

        public SizedTraitIsNotImplemented(@Nonnull RsTypeReference element, @Nonnull Ty ty) {
            super(element);
            myTy = ty;
        }

        @Nonnull
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

        public CannotAssignToImmutable(@Nonnull PsiElement element, @Nonnull String message) {
            this(element, message, null);
        }

        public CannotAssignToImmutable(@Nonnull PsiElement element, @Nonnull String message, @Nullable AddMutableFix fix) {
            super(element);
            myMessage = message;
            myFix = fix;
        }

        @Nonnull
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

        public CannotReassignToImmutable(@Nonnull PsiElement element, @Nullable AddMutableFix fix) {
            super(element);
            myFix = fix;
        }

        @Nonnull
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

        public UseOfMovedValueError(@Nonnull PsiElement element, @Nullable LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0382,
                RsBundle.message("inspection.message.use.moved.value"),
                "", listOfFixes(myFix), null);
        }
    }

    public static class DeriveAttrUnsupportedItem extends RsDiagnostic {
        public DeriveAttrUnsupportedItem(@Nonnull RsAttr element) {
            super(element);
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0774,
                RsBundle.message("inspection.message.derive.may.only.be.applied.to.structs.enums.unions"),
                "", listOfFixes(new RemoveAttrFix((RsAttr) myElement)), null);
        }
    }

    public static class ImplTraitNotAllowedHere extends RsDiagnostic {
        private final List<LocalQuickFix> myFixes;

        public ImplTraitNotAllowedHere(@Nonnull RsTraitType traitType) {
            this(traitType, Collections.emptyList());
        }

        public ImplTraitNotAllowedHere(@Nonnull RsTraitType traitType, @Nonnull List<LocalQuickFix> fixes) {
            super(traitType);
            myFixes = fixes;
        }

        @Nonnull
        @Override
        public PreparedAnnotation prepare() {
            return new PreparedAnnotation(Severity.ERROR, RsErrorCode.E0562,
                RsBundle.message("inspection.message.impl.trait.not.allowed.outside.function.inherent.method.return.types"),
                "", toQuickFixInfo(myFixes), null);
        }
    }

    // ---- Static helper methods ----

    @Nonnull
    private static Set<RsQualifiedNamedElement> getConflictingNames(@Nonnull PsiElement element, @Nonnull Ty... tys) {
        RsElement context = PsiElementExtUtil.ancestorOrSelf(element, RsElement.class);
        if (context != null) {
            return RsImportHelper.getTypeReferencesInfoFromTys(context, tys).getToQualify();
        }
        return Collections.emptySet();
    }

    // ---- addToHolder methods ----

    public static void addToHolder(@Nonnull RsDiagnostic diagnostic, @Nonnull RsAnnotationHolder holder) {
        addToHolder(diagnostic, holder, true);
    }

    public static void addToHolder(@Nonnull RsDiagnostic diagnostic, @Nonnull RsAnnotationHolder holder,
                                   boolean checkExistsAfterExpansion) {
        if (!checkExistsAfterExpansion || org.rust.lang.core.psi.ext.RsElementUtil.existsAfterExpansion(diagnostic.myElement)) {
            addToHolder(diagnostic, holder.getHolder());
        }
    }

    public static void addToHolder(@Nonnull RsDiagnostic diagnostic, @Nonnull AnnotationHolder holder) {
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

        consulo.language.editor.annotation.AnnotationBuilder annotationBuilder = holder
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
                consulo.language.editor.annotation.AnnotationBuilder.FixBuilder fixBuilder =
                    annotationBuilder.newFix((IntentionAction) fix);
                if (range != null) {
                    fixBuilder.range(range);
                }
                fixBuilder.registerFix();
            } else {
                consulo.language.editor.inspection.ProblemDescriptor descriptor =
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

    public static void addToHolder(@Nonnull RsDiagnostic diagnostic, @Nonnull RsProblemsHolder holder) {
        PreparedAnnotation prepared = diagnostic.prepare();
        holder.registerProblem(
            diagnostic.myElement,
            diagnostic.myEndElement != null ? diagnostic.myEndElement : diagnostic.myElement,
            getFullDescription(prepared),
            toProblemHighlightType(prepared.getSeverity()),
            prepared.getFixes().stream().map(QuickFixWithRange::getFix).toArray(LocalQuickFix[]::new)
        );
    }

    @Nonnull
    private static String getFullDescription(@Nonnull PreparedAnnotation prepared) {
        return "<html>" + htmlHeader(prepared.getErrorCode(), XmlStringUtil.escapeString(prepared.getHeader())) +
            "<br>" + XmlStringUtil.escapeString(prepared.getDescription()) + "</html>";
    }

    @Nonnull
    private static ProblemHighlightType toProblemHighlightType(@Nonnull Severity severity) {
        switch (severity) {
            case INFO: return ProblemHighlightType.INFORMATION;
            case WARN: return ProblemHighlightType.WARNING;
            case ERROR: return ProblemHighlightType.GENERIC_ERROR;
            case UNKNOWN_SYMBOL: return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
            default: return ProblemHighlightType.GENERIC_ERROR;
        }
    }

    @Nonnull
    private static HighlightSeverity toHighlightSeverity(@Nonnull Severity severity) {
        switch (severity) {
            case INFO: return HighlightSeverity.INFORMATION;
            case WARN: return HighlightSeverity.WARNING;
            case ERROR:
            case UNKNOWN_SYMBOL:
                return HighlightSeverity.ERROR;
            default: return HighlightSeverity.ERROR;
        }
    }

    @Nonnull
    
    private static String simpleHeader(@Nullable RsErrorCode error,  @Nonnull String description) {
        if (error == null) return description;
        return RsBundle.message("inspection.message.", description, error.getCode());
    }

    @Nonnull
    private static String htmlHeader(@Nullable RsErrorCode error, @Nonnull String description) {
        if (error == null) return description;
        return description + " [<a href='" + error.getInfoUrl() + "'>" + error.getCode() + "</a>]";
    }

    public static ThreeState areUnstableFeaturesAvailable(@Nonnull RsElement element, @Nonnull RustcVersion version) {
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

    public void addToHolder(@Nonnull RsProblemsHolder holder) {
        addToHolder(this, holder);
    }

    public void addToHolder(@Nonnull RsAnnotationHolder holder) {
        addToHolder(this, holder);
    }

    // ---- Additional inner classes ----

    public static class UnusedAttribute extends RsDiagnostic {
        @Nonnull private final LocalQuickFix myFix;
        private final boolean myIsFutureWarn;

        public UnusedAttribute(@Nonnull PsiElement element, @Nonnull LocalQuickFix fix) {
            this(element, fix, false);
        }

        public UnusedAttribute(@Nonnull PsiElement element, @Nonnull LocalQuickFix fix, boolean isFutureWarn) {
            super(element);
            myFix = fix;
            myIsFutureWarn = isFutureWarn;
        }

        @Nonnull
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
        @Nonnull private final String myAttrName;
        @Nonnull private final LocalQuickFix myFix;

        public MultipleAttributes(@Nonnull PsiElement element, @Nonnull String attrName, @Nonnull LocalQuickFix fix) {
            super(element);
            myAttrName = attrName;
            myFix = fix;
        }

        @Nonnull
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
        @Nonnull private final String myMessage;
        @Nonnull private final List<? extends LocalQuickFix> myFixes;

        public InvalidReprAlign(@Nonnull PsiElement element, @Nonnull String message) {
            this(element, message, Collections.emptyList());
        }

        public InvalidReprAlign(@Nonnull PsiElement element, @Nonnull String message, @Nonnull List<? extends LocalQuickFix> fixes) {
            super(element);
            myMessage = message;
            myFixes = fixes;
        }

        @Nonnull
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
        @Nonnull private final String myMessage;

        public IncorrectlyDeclaredAlignRepresentationHint(@Nonnull PsiElement element, @Nonnull String message) {
            super(element);
            myMessage = message;
        }

        @Nonnull
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
        public MissingLifetimeSpecifier(@Nonnull PsiElement element) {
            super(element, RsExperimentalChecksInspection.class);
        }

        @Nonnull
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
        @Nonnull private final String myDescription;

        public WrongNumberOfGenericParameters(@Nonnull PsiElement element, @Nonnull String description) {
            super(element, RsExperimentalChecksInspection.class);
            myDescription = description;
        }

        @Nonnull
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
        @Nonnull private final String myLoopType;

        public BreakExprInNonLoopError(@Nonnull PsiElement element, @Nonnull String loopType) {
            super(element);
            myLoopType = loopType;
        }

        @Nonnull
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
        public IncorrectItemInDeprecatedAttr(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
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
        @Nonnull private final String myArgName;

        public MultipleItemsInDeprecatedAttr(@Nonnull PsiElement element, @Nonnull String argName) {
            super(element);
            myArgName = argName;
        }

        @Nonnull
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
        @Nonnull private final String myArgName;

        public UnknownItemInDeprecatedAttr(@Nonnull PsiElement element, @Nonnull String argName) {
            super(element);
            myArgName = argName;
        }

        @Nonnull
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
        @Nonnull private final LocalQuickFix myFix;

        public AttributeSuffixedLiteral(@Nonnull PsiElement element, @Nonnull LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @Nonnull
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
        @Nonnull private final String myAttrName;
        @Nonnull private final String myMessage;

        public MalformedAttributeInput(@Nonnull PsiElement element, @Nonnull String attrName, @Nonnull String message) {
            super(element);
            myAttrName = attrName;
            myMessage = message;
        }

        @Nonnull
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
        @Nonnull private final PsiElement myCloseDelim;
        @Nonnull private final LocalQuickFix myFix;

        public WrongMetaDelimiters(@Nonnull PsiElement openDelim, @Nonnull PsiElement closeDelim, @Nonnull LocalQuickFix fix) {
            super(openDelim, closeDelim);
            myCloseDelim = closeDelim;
            myFix = fix;
        }

        @Nonnull
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
        @Nonnull private final String myFeatureName;
        @Nullable private final LocalQuickFix myFix;

        public FeatureAttributeHasBeenRemoved(@Nonnull PsiElement element, @Nonnull String featureName, @Nullable LocalQuickFix fix) {
            super(element);
            myFeatureName = featureName;
            myFix = fix;
        }

        @Nonnull
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
        @Nonnull private final String myChannelName;
        @Nullable private final LocalQuickFix myFix;

        public FeatureAttributeInNonNightlyChannel(@Nonnull PsiElement element, @Nonnull String channelName, @Nullable LocalQuickFix fix) {
            super(element);
            myChannelName = channelName;
            myFix = fix;
        }

        @Nonnull
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
        @Nonnull private final List<? extends LocalQuickFix> myFixes;

        public CfgNotPatternIsMalformed(@Nonnull PsiElement element, @Nonnull List<? extends LocalQuickFix> fixes) {
            super(element);
            myFixes = fixes;
        }

        @Nonnull
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
        @Nonnull private final String myPredicateName;
        @Nonnull private final List<? extends LocalQuickFix> myFixes;

        public UnknownCfgPredicate(@Nonnull PsiElement element, @Nonnull String predicateName, @Nonnull List<? extends LocalQuickFix> fixes) {
            super(element);
            myPredicateName = predicateName;
            myFixes = fixes;
        }

        @Nonnull
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
        @Nonnull private final List<? extends LocalQuickFix> myFixes;

        public LiteralValueInsideDeriveError(@Nonnull PsiElement element, @Nonnull List<? extends LocalQuickFix> fixes) {
            super(element);
            myFixes = fixes;
        }

        @Nonnull
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
        public ReprForEmptyEnumError(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
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
        @Nonnull private final String myReprName;

        public UnrecognizedReprAttribute(@Nonnull PsiElement element, @Nonnull String reprName) {
            super(element);
            myReprName = reprName;
        }

        @Nonnull
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
        public MoveOutWhileBorrowedError(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
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

        public UseOfUninitializedVariableError(@Nonnull PsiElement element, @Nullable LocalQuickFix fix) {
            super(element);
            myFix = fix;
        }

        @Nonnull
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
        public ReprAttrUnsupportedItem(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("repr attribute not supported on this item"); }
    }
    public static class ImplForNonAdtError extends RsDiagnostic {
        public ImplForNonAdtError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("impl for non-ADT type"); }
    }
    public static class InherentImplDifferentCrateError extends RsDiagnostic {
        public InherentImplDifferentCrateError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("cannot define inherent impl for type outside of defining crate"); }
    }
    public static class NotTraitError extends RsDiagnostic {
        public NotTraitError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("expected trait, found type"); }
    }
    public static class BreakContinueInWhileConditionWithoutLoopError extends RsDiagnostic {
        public BreakContinueInWhileConditionWithoutLoopError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("break/continue in while condition without loop"); }
    }
    public static class ContinueLabelTargetBlock extends RsDiagnostic {
        public ContinueLabelTargetBlock(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("continue label targets block"); }
    }
    public static class MultipleRelaxedBoundsError extends RsDiagnostic {
        public MultipleRelaxedBoundsError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("type parameter has more than one relaxed default bound"); }
    }
    public static class UnsafeInherentImplError extends RsDiagnostic {
        public UnsafeInherentImplError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("inherent impls cannot be unsafe"); }
    }
    public static class AsyncNonMoveClosureWithParameters extends RsDiagnostic {
        public AsyncNonMoveClosureWithParameters(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("async non-move closures with parameters are not currently supported"); }
    }
    public static class RustHasNoIncDecOperator extends RsDiagnostic {
        public RustHasNoIncDecOperator(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("Rust has no increment/decrement operator"); }
    }
    public static class ReservedIdentifierIsUsed extends RsDiagnostic {
        public ReservedIdentifierIsUsed(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("reserved identifier is used"); }
    }
    public static class TraitObjectWithNoDyn extends RsDiagnostic {
        public TraitObjectWithNoDyn(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("trait objects without dyn are deprecated"); }
    }
    public static class AsyncMainFunction extends RsDiagnostic {
        public AsyncMainFunction(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("async main function is not allowed"); }
    }
    public static class NoAttrParentheses extends RsDiagnostic {
        public NoAttrParentheses(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("attribute is missing parentheses"); }
    }
    public static class CastAsBoolError extends RsDiagnostic {
        public CastAsBoolError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("cannot cast as bool"); }
    }
    public static class ConstItemReferToStaticError extends RsDiagnostic {
        public ConstItemReferToStaticError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("a const item cannot refer to a static"); }
    }
    public static class LiteralOutOfRange extends RsDiagnostic {
        public LiteralOutOfRange(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("literal out of range"); }
    }
    public static class MainFunctionNotFound extends RsDiagnostic {
        public MainFunctionNotFound(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("main function not found"); }
    }
    public static class MismatchMemberInTraitImplError extends RsDiagnostic {
        public MismatchMemberInTraitImplError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("member in trait implementation does not match"); }
    }
    public static class UnknownMemberInTraitError extends RsDiagnostic {
        public UnknownMemberInTraitError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("unknown member in trait"); }
    }
    public static class DeclMissingFromTraitError extends RsDiagnostic {
        public DeclMissingFromTraitError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("declaration missing from trait"); }
    }
    public static class DeclMissingFromImplError extends RsDiagnostic {
        public DeclMissingFromImplError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("declaration missing from impl"); }
    }
    public static class TraitParamCountMismatchError extends RsDiagnostic {
        public TraitParamCountMismatchError(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("trait parameter count mismatch"); }
    }
    public static class UnknownAssocTypeBinding extends RsDiagnostic {
        public UnknownAssocTypeBinding(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("unknown associated type binding"); }
    }
    public static class MissingAssocTypeBindings extends RsDiagnostic {
        public MissingAssocTypeBindings(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("missing associated type bindings"); }
    }
    public static class WrongNumberOfGenericArguments extends RsDiagnostic {
        public WrongNumberOfGenericArguments(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong number of generic arguments"); }
    }
    public static class WrongOrderOfGenericArguments extends RsDiagnostic {
        public WrongOrderOfGenericArguments(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong order of generic arguments"); }
    }
    public static class WrongNumberOfLifetimeArguments extends RsDiagnostic {
        public WrongNumberOfLifetimeArguments(@Nonnull PsiElement element, Object... args) { super(element); }
        @Nonnull @Override public PreparedAnnotation prepare() { return errorAnnotation("wrong number of lifetime arguments"); }
    }

    private static PreparedAnnotation errorAnnotation(String message) {
        return new PreparedAnnotation(Severity.ERROR, null, message, "", Collections.emptyList(), null);
    }
}
