/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.doc.DocElementUtil;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElementTypes;
import org.rust.lang.doc.psi.RsDocGap;
import org.rust.openapiext.Testmark;
import org.rust.openapiext.VirtualFileExtUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.rust.lang.core.psi.ext.RsElement;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class RsDoctestLanguageInjector implements MultiHostInjector {

    public static final String INJECTED_MAIN_NAME = "__main";

    private static final Pattern LANG_SPLIT_REGEX = Pattern.compile("[^\\w-]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final List<String> RUST_LANG_ALIASES = List.of(
        "rust", "allow_fail", "should_panic", "no_run", "test_harness",
        "edition2015", "edition2018", "edition2021"
    );

    @NotNull
    @Override
    public List<Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(RsDocCodeFence.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof RsDocCodeFence) || !((RsDocCodeFence) context).isValidHost()) return;
        Project project = context.getProject();
        if (!RsProjectSettingsServiceUtil.getRustSettings(project).getDoctestInjectionEnabled()) return;

        RsDocCodeFence codeFence = (RsDocCodeFence) context;
        Crate crate = RsElementUtil.getContainingCrate(codeFence);
        if (crate == null || !crate.getAreDoctestsEnabled()) return;
        String crateName = crate.getNormName();

        DoctestInfo info = DoctestInfo.fromCodeFence(codeFence);
        if (info == null) return;
        String text = info.getText();

        List<TextRange> ranges = info.getRangesForInjection();
        List<TextRange> adjustedRanges = new ArrayList<>();
        for (TextRange range : ranges) {
            int codeStart = CharArrayUtil.shiftForward(text, range.getStartOffset(), range.getEndOffset(), " \t");
            if (text.startsWith("##", codeStart)) {
                adjustedRanges.add(new TextRange(codeStart + 1, range.getEndOffset()));
            } else if (text.startsWith("# ", codeStart)) {
                adjustedRanges.add(new TextRange(codeStart + 2, range.getEndOffset()));
            } else if (text.startsWith("#\n", codeStart)) {
                adjustedRanges.add(new TextRange(codeStart + 1, range.getEndOffset()));
            } else {
                adjustedRanges.add(new TextRange(range.getStartOffset(), range.getEndOffset()));
            }
        }

        if (adjustedRanges.isEmpty()) return;

        MultiHostRegistrar inj = registrar.startInjecting(RsLanguage.INSTANCE);

        StringBuilder fullInjectionText = new StringBuilder();
        for (TextRange r : adjustedRanges) {
            fullInjectionText.append(text, r.getStartOffset(), r.getEndOffset());
        }
        String fullText = fullInjectionText.toString();

        boolean containsMain = fullText.contains("main");
        boolean containsExternCrate = fullText.contains("extern") && fullText.contains("crate");
        boolean alreadyHasMain = false;
        boolean alreadyHasExternCrate = false;

        if (containsMain || containsExternCrate) {
            PsiBuilder lexer = ParserUtil.createRustPsiBuilder(project, fullText);
            alreadyHasMain = containsMain && ParserUtil.probe(lexer, () ->
                findTokenSequence(lexer, RsElementTypes.FN, "main", RsElementTypes.LPAREN));
            alreadyHasExternCrate = containsExternCrate &&
                findTokenSequence(lexer, RsElementTypes.EXTERN, RsElementTypes.CRATE, crateName);
        }

        int[] partition = partitionSource(text, adjustedRanges);
        int attrsEndIndex = partition[0];
        int cratesEndIndex = partition[1];

        for (int index = 0; index < adjustedRanges.size(); index++) {
            TextRange range = adjustedRanges.get(index);
            boolean isLastIteration = index == adjustedRanges.size() - 1;

            StringBuilder prefix = null;
            if (index == attrsEndIndex && !alreadyHasExternCrate) {
                boolean isStdCrate = AutoInjectedCrates.STD.equals(crateName) &&
                    crate.getOrigin() == PackageOrigin.STDLIB;
                if (!isStdCrate) {
                    prefix = new StringBuilder();
                    prefix.append("extern crate ");
                    prefix.append(crateName);
                    prefix.append("; ");
                }
            }
            if (index == cratesEndIndex && !alreadyHasMain) {
                if (prefix == null) prefix = new StringBuilder();
                prefix.append("fn ").append(INJECTED_MAIN_NAME).append("() {\n");
            }

            String suffix = (isLastIteration && !alreadyHasMain) ? "\n}" : null;

            inj.addPlace(prefix != null ? prefix.toString() : null, suffix, (RsDocCodeFence) context, range);
        }

        inj.doneInjecting();
    }

    private static boolean findTokenSequence(@NotNull PsiBuilder lexer, @NotNull Object... seq) {
        while (!lexer.eof()) {
            if (isTokenEq(lexer, seq[0])) {
                boolean found = ParserUtil.probe(lexer, () -> {
                    for (int i = 1; i < seq.length; i++) {
                        lexer.advanceLexer();
                        if (!isTokenEq(lexer, seq[i])) {
                            return false;
                        }
                    }
                    return true;
                });
                if (found) return true;
            }
            lexer.advanceLexer();
        }
        return false;
    }

    private static boolean isTokenEq(@NotNull PsiBuilder lexer, @NotNull Object t) {
        if (t instanceof IElementType) {
            return lexer.getTokenType() == t;
        } else {
            return lexer.getTokenType() == RsElementTypes.IDENTIFIER && t.equals(lexer.getTokenText());
        }
    }

    private static int[] partitionSource(@NotNull String text, @NotNull List<TextRange> ranges) {
        int state = 0; // 0=Attrs, 1=Crates, 2=Other
        int attrsEndIndex = 0;
        int cratesEndIndex = 0;

        for (int i = 0; i < ranges.size(); i++) {
            TextRange range = ranges.get(i);
            String trimmedLine = text.substring(range.getStartOffset(), range.getEndOffset()).trim();

            if (state == 0) { // Attrs
                boolean isCrateAttr = trimmedLine.startsWith("#![") || trimmedLine.isBlank() ||
                    (trimmedLine.startsWith("//") && !trimmedLine.startsWith("///"));
                if (!isCrateAttr) {
                    if (trimmedLine.startsWith("extern crate") || trimmedLine.startsWith("#[macro_use] extern crate")) {
                        attrsEndIndex = i;
                        cratesEndIndex = i;
                        state = 1; // Crates
                    } else {
                        attrsEndIndex = i;
                        cratesEndIndex = i;
                        state = 2; // Other
                        break;
                    }
                }
            } else if (state == 1) { // Crates
                boolean isCrate = trimmedLine.startsWith("extern crate") ||
                    trimmedLine.startsWith("#[macro_use] extern crate") ||
                    trimmedLine.isBlank() ||
                    (trimmedLine.startsWith("//") && !trimmedLine.startsWith("///"));
                if (!isCrate) {
                    cratesEndIndex = i;
                    state = 2; // Other
                    break;
                }
            }
        }

        return new int[]{attrsEndIndex, cratesEndIndex};
    }

    public static boolean isDoctestInjection(@NotNull VirtualFile file, @NotNull Project project) {
        if (!(file instanceof VirtualFileWindow)) return false;
        VirtualFileWindow virtualFileWindow = (VirtualFileWindow) file;
        VirtualFile hostFile = virtualFileWindow.getDelegate();
        RsFile rsFile = VirtualFileExtUtil.toPsiFile(hostFile, project) instanceof RsFile ? (RsFile) VirtualFileExtUtil.toPsiFile(hostFile, project) : null;
        if (rsFile == null) return false;
        PsiElement hostElement = rsFile.findElementAt(virtualFileWindow.getDocumentWindow().injectedToHost(0));
        return hostElement != null && hostElement.getParent() instanceof RsDocCodeFence;
    }

    public static boolean isFileDoctestInjection(@NotNull RsFile file) {
        VirtualFile vf = file.getVirtualFile();
        return vf != null && isDoctestInjection(vf, file.getProject());
    }

    public static boolean isDoctestInjection(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            return isElementDoctestInjection((RsElement) element);
        }
        return false;
    }

    public static boolean isElementDoctestInjection(@NotNull RsElement element) {
        PsiFile contextualFile = RsElementExtUtil.getContextualFile(element);
        return contextualFile instanceof RsFile && isFileDoctestInjection((RsFile) contextualFile);
    }

    public static boolean isElementInsideInjection(@NotNull RsElement element) {
        PsiFile contextualFile = RsElementExtUtil.getContextualFile(element);
        return contextualFile instanceof RsFile && ((RsFile) contextualFile).getVirtualFile() instanceof VirtualFileWindow;
    }

    public static boolean isFunctionDoctestInjectedMain(@NotNull RsFunction fn) {
        return INJECTED_MAIN_NAME.equals(fn.getName()) && isElementDoctestInjection(fn) && fn.getParent() instanceof RsFile;
    }
}
