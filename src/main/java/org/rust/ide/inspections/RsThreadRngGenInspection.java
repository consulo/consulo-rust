/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.NameResolution;

public class RsThreadRngGenInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitDotExpr(@NotNull RsDotExpr o) {
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
    private static RsFunction getThreadRng(@NotNull KnownItems items) {
        return items.findItem("rand::rngs::thread::thread_rng", false, RsFunction.class);
    }

    @Nullable
    private static RsFunction getGen(@NotNull KnownItems items) {
        return items.findItem("rand::rng::Rng::gen", false, RsFunction.class);
    }

    @Nullable
    private static RsFunction getRandom(@NotNull KnownItems items) {
        return items.findItem("rand::random", false, RsFunction.class);
    }

    private static class ReplaceWithRandomCall extends RsQuickFixBase<RsDotExpr> {
        @NlsSafe
        private final String myRandom;
        @Nls
        private final String myText;
        private final String myTypeArgument;
        private final boolean myNeedsQualifiedName;

        ReplaceWithRandomCall(@NotNull RsDotExpr element, @NotNull String typeArgument, boolean needsQualifiedName) {
            super(element);
            this.myTypeArgument = typeArgument;
            this.myNeedsQualifiedName = needsQualifiedName;
            this.myRandom = needsQualifiedName ? "rand::random" : "random";
            this.myText = RsBundle.message("intention.name.replace.with2", myRandom + typeArgument + "()");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return myText;
        }

        @NotNull
        @Override
        public String getText() {
            return myText;
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsDotExpr element) {
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
}
