package consulo.rust.test;

import consulo.annotation.component.ServiceImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.file.FileViewProvider;
import consulo.document.DocumentWindow;
import consulo.util.lang.Pair;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

/**
 * Language injection is irrelevant to parser tests; this satisfies the binding without doing any work.
 */
@Singleton
@ServiceImpl
public class RsStubInjectedLanguageManager implements InjectedLanguageManager {
    @Override
    public PsiLanguageInjectionHost getInjectionHost(FileViewProvider injectedProvider) {
        return null;
    }

    @Nullable
    @Override
    public PsiLanguageInjectionHost getInjectionHost(PsiElement injectedElement) {
        return null;
    }

    @Override
    public TextRange injectedToHost(PsiElement injectedContext, TextRange injectedTextRange) {
        return injectedTextRange;
    }

    @Override
    public int injectedToHost(PsiElement injectedContext, int injectedOffset) {
        return injectedOffset;
    }

    @Override
    public int injectedToHost(PsiElement injectedContext, int injectedOffset, boolean minHostOffset) {
        return injectedOffset;
    }

    @Nullable
    @Override
    public String getUnescapedLeafText(PsiElement element, boolean strict) {
        return null;
    }

    @Override
    public String getUnescapedText(PsiElement injectedNode) {
        return injectedNode.getText();
    }

    @Override
    public List<TextRange> intersectWithAllEditableFragments(PsiFile injectedPsi, TextRange rangeToEdit) {
        return Collections.emptyList();
    }

    @Override
    public boolean isInjectedFragment(PsiFile injectedFile) {
        return false;
    }

    @Override
    public PsiFile findInjectedPsiNoCommit(PsiFile host, int offset) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement findInjectedElementAt(PsiFile hostFile, int hostDocumentOffset) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement findElementAtNoCommit(PsiFile file, int offset) {
        return null;
    }

    @Nullable
    @Override
    public List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(PsiElement host) {
        return null;
    }

    @Override
    public void dropFileCaches(PsiFile file) {
    }

    @Nullable
    @Override
    public PsiFile getTopLevelFile(PsiElement element) {
        return element.getContainingFile();
    }

    @Override
    public List<DocumentWindow> getCachedInjectedDocumentsInRange(PsiFile hostPsiFile, TextRange range) {
        return Collections.emptyList();
    }

    @Override
    public void enumerate(PsiElement host, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    }

    @Override
    public void enumerate(DocumentWindow documentWindow, PsiFile hostPsiFile, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    }

    @Override
    public void enumerateEx(PsiElement host, PsiFile containingFile, boolean probeUp, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    }

    @Override
    public List<TextRange> getNonEditableFragments(DocumentWindow window) {
        return Collections.emptyList();
    }

    @Override
    public boolean mightHaveInjectedFragmentAtOffset(Document hostDocument, int hostOffset) {
        return false;
    }

    @Override
    public DocumentWindow freezeWindow(DocumentWindow document) {
        return document;
    }

    @Nullable
    @Override
    public PsiLanguageInjectionHost.Place getShreds(PsiFile injectedFile) {
        return null;
    }

    @Nullable
    @Override
    public PsiLanguageInjectionHost.Place getShreds(FileViewProvider viewProvider) {
        return null;
    }

    @Override
    public PsiLanguageInjectionHost.Place getShreds(DocumentWindow documentWindow) {
        return null;
    }
}
