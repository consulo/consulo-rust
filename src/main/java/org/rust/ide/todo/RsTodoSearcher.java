/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.cache.TodoCacheManager;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.IndexPatternOccurrence;
import com.intellij.psi.search.IndexPatternProvider;
import com.intellij.psi.search.searches.IndexPatternSearch;
import com.intellij.util.Processor;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsRecursiveVisitor;
import org.rust.lang.core.psi.ext.RsElement;

public class RsTodoSearcher extends QueryExecutorBase<IndexPatternOccurrence, IndexPatternSearch.SearchParameters> {

    public RsTodoSearcher() {
        super(true);
    }

    @Override
    public void processQuery(IndexPatternSearch.SearchParameters queryParameters, Processor<? super IndexPatternOccurrence> consumer) {
        IndexPattern pattern = queryParameters.getPattern();
        if (pattern != null && !isTodoPattern(pattern)) return;
        if (pattern == null) {
            IndexPatternProvider patternProvider = queryParameters.getPatternProvider();
            if (patternProvider == null) return;
            IndexPattern[] patterns = patternProvider.getIndexPatterns();
            pattern = null;
            for (IndexPattern p : patterns) {
                if (isTodoPattern(p)) {
                    pattern = p;
                    break;
                }
            }
            if (pattern == null) return;
        }

        if (!(queryParameters.getFile() instanceof RsFile)) return;
        RsFile file = (RsFile) queryParameters.getFile();

        TodoCacheManager cacheManager = TodoCacheManager.getInstance(file.getProject());
        IndexPatternProvider patternProvider = queryParameters.getPatternProvider();
        int count;
        if (patternProvider != null) {
            count = cacheManager.getTodoCount(file.getVirtualFile(), patternProvider);
        } else {
            count = cacheManager.getTodoCount(file.getVirtualFile(), pattern);
        }
        if (count == 0) return;

        IndexPattern finalPattern = pattern;
        file.accept(new RsRecursiveVisitor() {
            @Override
            public void visitMacroCall(RsMacroCall call) {
                super.visitMacroCall(call);
                if ("todo".equals(RsElementUtil.getMacroName(call))) {
                    PsiElement refNameElement = call.getPath().getReferenceNameElement();
                    if (refNameElement == null) return;
                    int startOffset = refNameElement.getTextOffset();
                    PsiElement semicolon = call.getSemicolon();
                    int endOffset = semicolon != null ? semicolon.getTextOffset() : call.getTextRange().getEndOffset();
                    TextRange range = new TextRange(startOffset, endOffset);
                    consumer.process(new RsTodoOccurrence(file, range, finalPattern));
                }
            }
        });
    }

    public static boolean isTodoPattern(IndexPattern pattern) {
        return pattern.getPatternString().toUpperCase().contains("TODO");
    }
}
