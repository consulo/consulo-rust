/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.injected.DoctestInfo;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocCodeFenceLang;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

/**
 * Utility class for doctest context detection.
 */
public final class DoctestCtxUtil {

    private DoctestCtxUtil() {
    }

    private static final Set<String> KNOWN_DOCTEST_ATTRIBUTES = new HashSet<>(Arrays.asList(
        "compile_fail",
        "ignore",
        "rust",
        "should_panic",
        "no_run",
        "test_harness",
        "allow_fail",
        "edition2015",
        "edition2018",
        "edition2021"
    ));

    @Nullable
    public static DocTestContext getDoctestCtx(@NotNull RsDocCodeFence codeFence) {
        Crate crate = org.rust.lang.core.psi.ext.RsElementUtil.getContainingCrate(codeFence);
        if (crate == null || !crate.getAreDoctestsEnabled()) return null;
        if (DoctestInfo.hasUnbalancedCodeFencesBefore(codeFence)) return null;

        RsDocComment containingDoc = codeFence.getContainingDoc();
        PsiElement docOwner = containingDoc.getOwner();
        if (!(docOwner instanceof RsQualifiedNamedElement)) return null;
        RsQualifiedNamedElement owner = (RsQualifiedNamedElement) docOwner;

        PsiElement originalElement = codeFence.getOriginalElement();
        PsiFile containingFile = originalElement.getContainingFile();
        Document document = OpenApiUtil.getDocument(containingFile);
        if (document == null) return null;
        int textOffset = originalElement.getTextOffset();

        // Cargo uses 1-based line numbers
        int lineNumber = document.getLineNumber(textOffset) + 1;

        RsDocCodeFenceLang lang = codeFence.getLang();
        String text = lang != null ? lang.getText().trim() : "";

        // Ignore test line marker comments in tests
        if (OpenApiUtil.isUnitTestMode() && text.contains("// -")) {
            text = text.substring(0, text.indexOf("// -")).trim();
        }

        List<String> tags = new ArrayList<>();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }

        if (!tags.isEmpty() && !KNOWN_DOCTEST_ATTRIBUTES.contains(tags.get(0))) return null;

        boolean isIgnored = tags.contains("ignore");
        return new DocTestContext(owner, lineNumber, isIgnored);
    }
}
