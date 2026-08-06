/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.document.Document;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.resolve.NameResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds hyperlinks to function names in backtraces
 */
public class RsBacktraceItemFilter implements Filter {

    public static final TextAttributes DIMMED_TEXT = EditorColorsManager.getInstance().getGlobalScheme()
        .getAttributes(TextAttributesKey.createTextAttributesKey("org.rust.DIMMED_TEXT"));

    public static final String[] SKIP_PREFIXES = {
        "std::rt::lang_start",
        "std::panicking",
        "std::sys::backtrace",
        "std::sys::imp::backtrace",
        "core::panicking"
    };

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^(\\s*\\d+:\\s+(?:0x[a-f0-9]+ - )?)(.+?)(::h[0-9a-f]+)?$");

    private final Project myProject;
    private final CargoWorkspace myWorkspace;
    private final PsiDocumentManager myDocManager;

    public RsBacktraceItemFilter(@Nonnull Project project, @Nullable CargoWorkspace workspace) {
        myProject = project;
        myWorkspace = workspace;
        myDocManager = PsiDocumentManager.getInstance(project);
    }

    @Nullable
    @Override
    public Result applyFilter(@Nonnull String line, int entireLength) {
        BacktraceRecord record = parseBacktraceRecord(line);
        if (record == null) return null;
        String normFuncName = FilterUtils.normalizeFunctionPath(record.myFunctionName);
        List<ResultItem> resultItems = new ArrayList<>(2);

        // Add hyperlink to the function name
        int funcStart = entireLength - line.length() + record.myHeader.length();
        int funcEnd = funcStart + record.myFunctionName.length();
        boolean shouldSkip = false;
        for (String prefix : SKIP_PREFIXES) {
            if (normFuncName.startsWith(prefix)) {
                shouldSkip = true;
                break;
            }
        }
        if (!shouldSkip) {
            ResultItem hyperlink = extractFnHyperlink(normFuncName, funcStart, funcEnd);
            if (hyperlink != null) {
                resultItems.add(hyperlink);
            }
        }

        // Dim the hashcode
        if (record.myFunctionHash != null) {
            resultItems.add(new ResultItem(funcEnd, funcEnd + record.myFunctionHash.length(), null, DIMMED_TEXT));
        }

        return new Result(resultItems);
    }

    @Nullable
    private ResultItem extractFnHyperlink(@Nonnull String funcName, int start, int end) {
        if (myWorkspace == null) return null;
        consulo.util.lang.Pair<? extends PsiElement, CargoWorkspace.Package> resolved = NameResolution.resolveStringPath(funcName, myWorkspace, myProject);
        if (resolved == null) return null;
        PsiElement element = resolved.getFirst();
        CargoWorkspace.Package pkg = resolved.getSecond();
        PsiFile funcFile = element.getContainingFile();
        Document doc = myDocManager.getDocument(funcFile);
        if (doc == null) return null;
        OpenFileHyperlinkInfo link = new OpenFileHyperlinkInfo(myProject, funcFile.getVirtualFile(), doc.getLineNumber(element.getTextOffset()));
        return new ResultItem(start, end, link, pkg.getOrigin() != PackageOrigin.WORKSPACE);
    }

    @Nullable
    public static BacktraceRecord parseBacktraceRecord(@Nonnull String line) {
        Matcher matcher = FUNCTION_PATTERN.matcher(line);
        if (!matcher.find()) return null;
        String header = matcher.group(1);
        String funcName = matcher.group(2);
        String funcHash = matcher.group(3);
        return new BacktraceRecord(header, funcName, funcHash);
    }

    public static class BacktraceRecord {
        private final String myHeader;
        private final String myFunctionName;
        private final String myFunctionHash;

        public BacktraceRecord(@Nonnull String header, @Nonnull String functionName, @Nullable String functionHash) {
            myHeader = header;
            myFunctionName = functionName;
            myFunctionHash = functionHash;
        }

        @Nonnull
        public String getHeader() {
            return myHeader;
        }

        @Nonnull
        public String getFunctionName() {
            return myFunctionName;
        }

        @Nullable
        public String getFunctionHash() {
            return myFunctionHash;
        }
    }
}
