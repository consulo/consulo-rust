/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;

public abstract class RsCodeFragment extends RsFileBase implements PsiCodeFragment, RsItemsOwner {
    @NotNull
    private final RsElement myContext;
    @Nullable
    private final RsItemsOwner myImportTarget;
    @NotNull
    private SingleRootFileViewProvider myViewProvider;
    @Nullable
    private GlobalSearchScope myForcedResolveScope;
    private boolean myIsPhysical = true;

    protected RsCodeFragment(
        @NotNull FileViewProvider fileViewProvider,
        @NotNull IElementType contentElementType,
        @NotNull RsElement context,
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
        @NotNull FileViewProvider fileViewProvider,
        @NotNull IElementType contentElementType,
        @NotNull RsElement context
    ) {
        this(fileViewProvider, contentElementType, context, true, null);
    }

    protected RsCodeFragment(
        @NotNull Project project,
        @NotNull CharSequence text,
        @NotNull IElementType contentElementType,
        @NotNull RsElement context,
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
        @NotNull Project project,
        @NotNull CharSequence text,
        @NotNull IElementType contentElementType,
        @NotNull RsElement context
    ) {
        this(project, text, contentElementType, context, null);
    }

    @NotNull
    public RsElement getCodeFragmentContext() {
        return myContext;
    }

    @Nullable
    public RsItemsOwner getImportTarget() {
        return myImportTarget;
    }

    @NotNull
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
    public void accept(@NotNull PsiElementVisitor visitor) {
        visitor.visitFile(this);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }

    @Override
    public boolean isPhysical() {
        return myIsPhysical;
    }

    @Override
    public void forceResolveScope(@Nullable GlobalSearchScope scope) {
        myForcedResolveScope = scope;
    }

    @Nullable
    @Override
    public GlobalSearchScope getForcedResolveScope() {
        return myForcedResolveScope;
    }

    @NotNull
    @Override
    public PsiElement getContext() {
        return myContext;
    }

    @NotNull
    @Override
    public final SingleRootFileViewProvider getViewProvider() {
        return myViewProvider;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @NotNull
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

    @NotNull
    protected static FileViewProvider createFileViewProvider(
        @NotNull Project project,
        @NotNull CharSequence text,
        boolean eventSystemEnabled
    ) {
        return PsiManagerEx.getInstanceEx(project).getFileManager().createFileViewProvider(
            new LightVirtualFile("fragment.rs", RsLanguage.INSTANCE, text),
            eventSystemEnabled
        );
    }
}
