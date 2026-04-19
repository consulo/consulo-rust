/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.NameResolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class RsUnnecessaryQualificationsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnusedQualifications;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPath(@NotNull RsPath path) {
                boolean shouldCheckPath = RsPsiJavaUtil.parentOfType(path, RsUseItem.class) == null
                    && RsPsiJavaUtil.parentOfType(path, RsVisRestriction.class) == null
                    && RsPathUtil.rootPath(path) == path
                    && canBeShortened(path);
                if (shouldCheckPath) {
                    RsPath target = getUnnecessarilyQualifiedPath(path);
                    if (target != null) {
                        PsiElement refNameElement = target.getReferenceNameElement();
                        if (refNameElement != null) {
                            int pathRestStart = refNameElement.getTextOffset();
                            int unnecessaryLength = pathRestStart - path.getTextOffset();
                            TextRange range = new TextRange(0, unnecessaryLength);

                            SubstituteTextFix fix = SubstituteTextFix.delete(
                                RsBundle.message("intention.name.remove.unnecessary.path.prefix"),
                                path.getContainingFile(),
                                new TextRange(path.getTextOffset(), pathRestStart)
                            );
                            registerLintProblem(holder, path, RsBundle.message("inspection.message.unnecessary.qualification"),
                                range, RsLintHighlightingType.UNUSED_SYMBOL, Collections.singletonList(fix));
                        }
                    }
                }
                super.visitPath(path);
            }
        };
    }

    /**
     * Consider a path like a::b::c::d.
     * We will try to walk it from the "root" (the rightmost sub-path).
     * First we try d, then c::d, then b::c::d, etc.
     * Once we find a path that can be resolved and which is shorter than the original path, we will return it.
     */
    @Nullable
    private RsPath getUnnecessarilyQualifiedPath(@NotNull RsPath path) {
        if (RsPathUtil.getResolveStatus(path) == PathResolveStatus.UNRESOLVED) return null;

        PsiElement target = path.getReference() != null ? path.getReference().resolve() : null;
        if (target == null) return null;

        // From a::b::c::d generates paths a::b::c::d, a::b::c, a::b and a.
        List<RsPath> subPaths = new ArrayList<>();
        RsPath current = path;
        while (current != null) {
            subPaths.add(current);
            current = RsPathUtil.getQualifier(current);
        }

        // If any part of the path has type qualifiers/arguments, we don't want to erase parts
        int lastSubPathWithType = 0;
        for (int i = 0; i < subPaths.size(); i++) {
            RsPath sp = subPaths.get(i);
            if (sp.getTypeQual() != null || sp.getTypeArgumentList() != null) {
                lastSubPathWithType = i;
            }
        }

        String rootPathText = subPaths.size() > 0 ? subPaths.get(0).getReferenceName() : null;
        if (rootPathText == null) return null;

        // From a::b::c::d generates strings d, c::d, b::c::d, a::b::c::d.
        List<String> pathTexts = new ArrayList<>();
        pathTexts.add(rootPathText);
        for (int i = 1; i < subPaths.size(); i++) {
            String prev = pathTexts.get(i - 1);
            String subPathName = subPaths.get(i).getReferenceName();
            pathTexts.add(subPathName + "::" + prev);
        }

        RsPath basePath = RsPathUtil.basePath(path);
        for (int i = lastSubPathWithType; i < subPaths.size(); i++) {
            RsPath subPath = subPaths.get(i);
            String subPathText = pathTexts.get(i);

            RsPathCodeFragment fragment = new RsPathCodeFragment(
                path.getProject(), subPathText, false, path, RustParserUtil.PathParsingMode.TYPE,
                NameResolution.TYPES_N_VALUES_N_MACROS
            );

            boolean sameAsBase = subPath == basePath && !RsPathUtil.getHasColonColon(basePath);
            RsPath fragmentPath = fragment.getPath();
            if (fragmentPath != null && fragmentPath.getReference() != null
                && fragmentPath.getReference().resolve() == target && !sameAsBase) {
                return subPath;
            }
        }
        return null;
    }

    private static boolean canBeShortened(@NotNull RsPath path) {
        return path.getPath() != null || path.getColoncolon() != null;
    }
}
