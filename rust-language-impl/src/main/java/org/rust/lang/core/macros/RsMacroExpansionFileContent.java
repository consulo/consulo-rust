/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.ast.LighterAST;
import consulo.language.ast.TreeBackedLighterAST;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.stub.PsiDependentFileContent;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;

import java.nio.charset.StandardCharsets;

/**
 * The {@link consulo.language.psi.stub.FileContent} handed to
 * {@link consulo.language.psi.stub.StubTreeBuilder} when building a stub tree for an in-memory macro
 * expansion.
 * <p>
 * Replaces {@code consulo.language.impl.internal.psi.stub.FileContentImpl}, which is exported only to
 * platform modules — the {@code FileContent} / {@code PsiDependentFileContent} interfaces themselves
 * are public, but no public factory for them exists.
 * <p>
 * The PSI file is built lazily and cached: stub building asks for it, and re-parsing per call would
 * be wasteful.
 */
public class RsMacroExpansionFileContent extends UserDataHolderBase implements PsiDependentFileContent {

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VirtualFile myFile;
    @Nonnull
    private final CharSequence myText;

    private volatile PsiFile myPsiFile;

    public RsMacroExpansionFileContent(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull CharSequence text) {
        myProject = project;
        myFile = file;
        myText = text;
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return myFile.getFileType();
    }

    @Nonnull
    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    @Nonnull
    @Override
    public String getFileName() {
        return myFile.getName();
    }

    @Override
    public byte[] getContent() {
        return myText.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    @Override
    public CharSequence getContentAsText() {
        return myText;
    }

    @Nullable
    @Override
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    @Override
    public PsiFile getPsiFile() {
        PsiFile psiFile = myPsiFile;
        if (psiFile == null) {
            synchronized (this) {
                psiFile = myPsiFile;
                if (psiFile == null) {
                    psiFile = PsiFileFactory.getInstance(myProject)
                        .createFileFromText(getFileName(), RsLanguage.INSTANCE, myText, false, false);
                    myPsiFile = psiFile;
                }
            }
        }
        return psiFile;
    }

    @Nonnull
    @Override
    public LighterAST getLighterAST() {
        return new TreeBackedLighterAST(getPsiFile().getNode());
    }

    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
        return super.getUserData(key);
    }
}
