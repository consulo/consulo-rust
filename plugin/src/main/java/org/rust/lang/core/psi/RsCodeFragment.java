/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;
import consulo.language.ast.TokenType;
import consulo.language.impl.file.SingleRootFileViewProvider;

import consulo.language.file.FileViewProvider;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.ast.FileElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.ast.IElementType;
import consulo.language.file.light.LightVirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;

public abstract class RsCodeFragment extends RsFileBase implements PsiCodeFragment, RsItemsOwner {
    @Nonnull
    private final RsElement myContext;
    @Nullable
    private final RsItemsOwner myImportTarget;
    @Nonnull
    private SingleRootFileViewProvider myViewProvider;
    @Nullable
    private GlobalSearchScope myForcedResolveScope;
    private boolean myIsPhysical = true;

    protected RsCodeFragment(
        @Nonnull FileViewProvider fileViewProvider,
        @Nonnull IElementType contentElementType,
        @Nonnull RsElement context,
        boolean forceCachedPsi,
        @Nullable RsItemsOwner importTarget
    ) {
        super(fileViewProvider);
        myContext = context;
        myImportTarget = importTarget;
        myViewProvider = (SingleRootFileViewProvider) super.getViewProvider();
        if (forceCachedPsi) {
            getViewProvider().forceCachedPsi(this);
        }
        init(TokenType.CODE_FRAGMENT, contentElementType);
    }

    protected RsCodeFragment(
        @Nonnull FileViewProvider fileViewProvider,
        @Nonnull IElementType contentElementType,
        @Nonnull RsElement context
    ) {
        this(fileViewProvider, contentElementType, context, true, null);
    }

    protected RsCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull IElementType contentElementType,
        @Nonnull RsElement context,
        @Nullable RsItemsOwner importTarget
    ) {
        this(
            PsiManagerEx.getInstanceEx(project).getFileManager().createFileViewProvider(
                new LightVirtualFile("fragment.rs", RsLanguage.INSTANCE, text), true
            ),
            contentElementType,
            context,
            true,
            importTarget
        );
    }

    protected RsCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull IElementType contentElementType,
        @Nonnull RsElement context
    ) {
        this(project, text, contentElementType, context, null);
    }

    @Nonnull
    public RsElement getCodeFragmentContext() {
        return myContext;
    }

    @Nullable
    public RsItemsOwner getImportTarget() {
        return myImportTarget;
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        return myContext.getContainingMod();
    }

    @Nullable
    @Override
    public RsMod getCrateRoot() {
        return myContext.getCrateRoot();
    }

    @Override
    public void accept(@Nonnull PsiElementVisitor visitor) {
        visitor.visitFile(this);
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }

    @Override
    public boolean isPhysical() {
        return myIsPhysical;
    }

    public void forceResolveScope(@Nullable GlobalSearchScope scope) {
        myForcedResolveScope = scope;
    }

    @Nullable
    public GlobalSearchScope getForcedResolveScope() {
        return myForcedResolveScope;
    }

    @Nonnull
    @Override
    public PsiElement getContext() {
        return myContext;
    }

    @Nonnull
    @Override
    public final SingleRootFileViewProvider getViewProvider() {
        return myViewProvider;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Nonnull
    @Override
    public PsiFileImpl clone() {
        RsCodeFragment clone = (RsCodeFragment) cloneImpl((FileElement) calcTreeElement().clone());
        clone.myIsPhysical = false;
        clone.myOriginalFile = this;
        clone.myViewProvider =
            new SingleRootFileViewProvider(PsiManager.getInstance(getProject()),
                new LightVirtualFile(getName(), RsLanguage.INSTANCE, getText()), false);
        clone.myViewProvider.forceCachedPsi(clone);
        return clone;
    }

    @Nonnull
    protected static FileViewProvider createFileViewProvider(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        boolean eventSystemEnabled
    ) {
        return PsiManagerEx.getInstanceEx(project).getFileManager().createFileViewProvider(
            new LightVirtualFile("fragment.rs", RsLanguage.INSTANCE, text),
            eventSystemEnabled
        );
    }
}
