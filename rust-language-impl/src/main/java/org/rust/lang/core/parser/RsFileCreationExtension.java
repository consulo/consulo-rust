package org.rust.lang.core.parser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Builds a PSI file for view providers that need more than a plain {@code RsFile} — a debugger
 * expression or a REPL fragment, for instance. Returning null means "not mine"; when nothing
 * handles the provider the parser definition creates an ordinary file.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RsFileCreationExtension {
    @Nullable
    PsiFile createFile(@Nonnull FileViewProvider viewProvider, @Nullable PsiElement injectionHost);
}
