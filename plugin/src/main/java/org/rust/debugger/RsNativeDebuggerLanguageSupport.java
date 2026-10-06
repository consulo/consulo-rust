/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.debugger;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.nativeDev.debugger.NativeDebuggerLanguageSupport;
import consulo.project.Project;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.impl.RsCodeFragmentFactory;
import org.rust.lang.core.psi.impl.RsDebuggerExpressionCodeFragment;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class RsNativeDebuggerLanguageSupport implements NativeDebuggerLanguageSupport {
    private static final List<String> TYPE_PREFIXES = List.of("&mut ", "&", "*mut ", "*const ", "mut ", "dyn ", "impl ");

    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public String getDebuggerLanguage() {
        return "rust";
    }

    @Override
    public PsiFile createExpressionCodeFragment(Project project, String text, @Nullable PsiElement context, boolean isPhysical) {
        RsElement rsContext = PsiTreeUtil.getParentOfType(context, RsElement.class, false);
        if (rsContext != null) {
            return new RsDebuggerExpressionCodeFragment(project, text, rsContext);
        }
        return PsiFileFactory.getInstance(project).createFileFromText("fragment.rs", RsLanguage.INSTANCE, text, isPhysical, false);
    }

    @Nullable
    @Override
    public PsiElement findTypeDeclaration(Project project, PsiElement context, String typeName) {
        RsElement rsContext = PsiTreeUtil.getParentOfType(context, RsElement.class, false);
        String path = typePath(typeName);
        if (rsContext == null || path.isEmpty()) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        String crateName = RsElementUtil.getContainingCrate(rsContext).getNormName();
        if (path.startsWith(crateName + "::")) {
            candidates.add("crate::" + path.substring(crateName.length() + 2));
        }
        candidates.add(path);
        int last = path.lastIndexOf("::");
        if (last >= 0) {
            candidates.add(path.substring(last + 2));
        }

        RsCodeFragmentFactory factory = new RsCodeFragmentFactory(project);
        for (String candidate : candidates) {
            RsPath rsPath = factory.createPath(candidate, rsContext);
            PsiReference reference = rsPath == null ? null : rsPath.getReference();
            PsiElement target = reference == null ? null : reference.resolve();
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private static String typePath(String typeName) {
        String type = typeName.trim();
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            for (String prefix : TYPE_PREFIXES) {
                if (type.startsWith(prefix)) {
                    type = type.substring(prefix.length()).trim();
                    stripped = true;
                }
            }
        }
        int generic = type.indexOf('<');
        if (generic >= 0) {
            type = type.substring(0, generic);
        }
        if (type.startsWith("[") || type.startsWith("(")) {
            return "";
        }
        return type.trim();
    }
}
