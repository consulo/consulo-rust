/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.search.LightIndexPatternSearcher;
import consulo.project.util.query.QueryExecutorBase;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.todo.TodoCacheManager;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternOccurrence;
import consulo.language.psi.search.IndexPatternProvider;
import consulo.language.psi.search.IndexPatternSearch;
import java.util.function.Predicate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsRecursiveVisitor;
import org.rust.lang.core.psi.ext.RsElement;

@ExtensionImpl
public class RsTodoSearcher extends QueryExecutorBase<IndexPatternOccurrence, IndexPatternSearch.SearchParameters>
    implements LightIndexPatternSearcher {

    public RsTodoSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@jakarta.annotation.Nonnull IndexPatternSearch.SearchParameters queryParameters, @jakarta.annotation.Nonnull Predicate<? super IndexPatternOccurrence> consumer) {
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
                    consumer.test(new RsTodoOccurrence(file, range, finalPattern));
                }
            }
        });
    }

    public static boolean isTodoPattern(IndexPattern pattern) {
        return pattern.getPatternString().toUpperCase().contains("TODO");
    }
}
