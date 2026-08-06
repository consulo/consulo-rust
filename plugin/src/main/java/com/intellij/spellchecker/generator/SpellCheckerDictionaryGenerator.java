package com.intellij.spellchecker.generator;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.HashSet;

/** IntelliJ-compat stub: spellchecker dictionary generator. Consulo doesn't expose this dev-tool. */
public abstract class SpellCheckerDictionaryGenerator {
    protected final Project myProject;
    protected final String myOutputFolder;
    protected final String myDictionaryName;

    public SpellCheckerDictionaryGenerator(@Nonnull Project project, @Nonnull String outputFolder, @Nonnull String dictionaryName) {
        this.myProject = project;
        this.myOutputFolder = outputFolder;
        this.myDictionaryName = dictionaryName;
    }

    protected abstract void processFile(@Nonnull PsiFile file, @Nonnull HashSet<String> seenNames);

    protected void processLeafsNames(@Nonnull PsiElement element, @Nonnull HashSet<String> seenNames) {}

    public void addFolder(@Nonnull String dictionaryName, @Nonnull consulo.virtualFileSystem.VirtualFile folder) {}

    public void excludeFolder(@Nonnull consulo.virtualFileSystem.VirtualFile folder) {}

    public void generate() {}
}
