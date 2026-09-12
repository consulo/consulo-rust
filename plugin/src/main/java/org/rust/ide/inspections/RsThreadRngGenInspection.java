/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.NameResolution;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.resolve.Namespace;

@ExtensionImpl
public class RsThreadRngGenInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitDotExpr(@Nonnull RsDotExpr o) {
                RsExpr leftExpr = o.getExpr();
                if (!(leftExpr instanceof RsCallExpr)) return;
                RsCallExpr left = (RsCallExpr) leftExpr;
                RsExpr funcExpr = left.getExpr();
                if (!(funcExpr instanceof RsPathExpr)) return;
                RsPathExpr functionPath = (RsPathExpr) funcExpr;
                PsiElement function = functionPath.getPath().getReference() != null ? functionPath.getPath().getReference().resolve() : null;
                KnownItems knownItems = KnownItems.getKnownItems(o);
                if (function == null || function != getThreadRng(knownItems)) return;
                RsMethodCall methodCall = o.getMethodCall();
                if (methodCall == null) return;
                PsiElement method = methodCall.getReference().resolve();
                if (!(method instanceof RsFunction)) return;
                if (method != getGen(knownItems)) return;
                String typeArgument = "";
                if (methodCall.getTypeArgumentList() != null) {
                    typeArgument = methodCall.getTypeArgumentList().getText();
                }
                boolean randomResolvedIncorrectly;
                PsiElement resolved = NameResolution.findInScope(o, "random", org.rust.lang.core.resolve.Namespace.VALUES);
                if (resolved == null) {
                    randomResolvedIncorrectly = false;
                } else {
                    randomResolvedIncorrectly = resolved != getRandom(knownItems);
                }
                holder.registerProblem(
                    o,
                    RsBundle.message("inspection.message.can.be.replaced.with.random", typeArgument),
                    new ReplaceWithRandomCall(o, typeArgument, randomResolvedIncorrectly)
                );
            }
        };
    }

    @Nullable
    private static RsFunction getThreadRng(@Nonnull KnownItems items) {
        return items.findItem("rand::rngs::thread::thread_rng", false, RsFunction.class);
    }

    @Nullable
    private static RsFunction getGen(@Nonnull KnownItems items) {
        return items.findItem("rand::rng::Rng::gen", false, RsFunction.class);
    }

    @Nullable
    private static RsFunction getRandom(@Nonnull KnownItems items) {
        return items.findItem("rand::random", false, RsFunction.class);
    }

    private static class ReplaceWithRandomCall extends RsQuickFixBase<RsDotExpr> {
        
        private final String myRandom;
        
        private final String myText;
        private final String myTypeArgument;
        private final boolean myNeedsQualifiedName;

        ReplaceWithRandomCall(@Nonnull RsDotExpr element, @Nonnull String typeArgument, boolean needsQualifiedName) {
            super(element);
            this.myTypeArgument = typeArgument;
            this.myNeedsQualifiedName = needsQualifiedName;
            this.myRandom = needsQualifiedName ? "rand::random" : "random";
            this.myText = RsBundle.message("intention.name.replace.with2", myRandom + typeArgument + "()");
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(myText);
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(myText);
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsDotExpr element) {
            RsPsiFactory psiFactory = new RsPsiFactory(project);
            RsExpr randomCall = psiFactory.createExpression(myRandom + myTypeArgument + "()");

            PsiElement newElement = element.replace(randomCall);
            RsCallExpr newCallExpr = (RsCallExpr) newElement;

            if (PsiElementExt.isIntentionPreviewElement(newCallExpr)) return;

            if (!myNeedsQualifiedName) {
                RsExpr calleeExpr = newCallExpr.getExpr();
                if (calleeExpr instanceof RsPathExpr) {
                    RsPathExpr pathExpr = (RsPathExpr) calleeExpr;
                    if (pathExpr.getPath().getReference() == null || pathExpr.getPath().getReference().resolve() == null) {
                        RsFunction random = getRandom(KnownItems.getKnownItems(newCallExpr));
                        if (random != null) {
                            RsImportHelper.importElement(newCallExpr, random);
                        }
                    }
                }
            }
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.thread.rng.gen.can.be.replaced.with.random"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
