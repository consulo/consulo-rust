package org.rust.ide.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.debugger.RsDebugInjectionListener;
import org.rust.lang.core.parser.RsFileCreationExtension;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.impl.RsReplCodeFragment;

/**
 * Files the IDE creates instead of a plain source file: a debugger expression placeholder, and the
 * REPL console buffer.
 */
@ExtensionImpl
public class RsIdeFileCreationExtension implements RsFileCreationExtension {
    @Nullable
    @Override
    public PsiFile createFile(@Nonnull FileViewProvider viewProvider, @Nullable PsiElement injectionHost) {
        Project project = viewProvider.getManager().getProject();

        if (injectionHost != null) {
            // the placeholder class ships in clion.jar, so it cannot be named in an instanceof
            if (!"GDBExpressionPlaceholder".equals(injectionHost.getClass().getSimpleName())) {
                return null;
            }
            RsDebugInjectionListener listener =
                project.getMessageBus().syncPublisher(RsDebugInjectionListener.INJECTION_TOPIC);
            RsDebugInjectionListener.DebugContext context = new RsDebugInjectionListener.DebugContext();
            listener.evalDebugContext((PsiLanguageInjectionHost) injectionHost, context);
            if (context.getElement() != null) {
                listener.didInject((PsiLanguageInjectionHost) injectionHost);
            }
            return null;
        }

        if (RsConsoleView.VIRTUAL_FILE_NAME.equals(viewProvider.getVirtualFile().getName())) {
            RsBlock context = RsConsoleCodeFragmentContext.createContext(project, null);
            RsReplCodeFragment fragment = new RsReplCodeFragment(viewProvider);
            fragment.setContext(context);
            return fragment;
        }
        return null;
    }
}
