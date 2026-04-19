/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportUtils;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Bridge providing Java-accessible access to {@link AutoImportFix} from the
 * {@code org.rust.ide.inspections.import} package. Java source code cannot easily reference
 * packages named {@code import} since it is a reserved keyword, so this bridge in
 * the {@code imports} (plural) package delegates to the real implementation.
 * <p>
 * All context parameters are typed as {@link Object} so Java callers do not need to
 * reference the {@code AutoImportFix.Context} type in their source code.
 */
public final class AutoImportFixBridge {

    private AutoImportFixBridge() {
    }

    /**
     * Creates a new {@link AutoImportFix} instance.
     *
     * @param element the PSI element to attach the fix to
     * @param context an {@code AutoImportFix.Context} instance (typed as {@link Object} for Java compatibility)
     * @return a new AutoImportFix
     */
    @NotNull
    public static AutoImportFix create(@NotNull RsElement element, @NotNull Object context) {
        return new AutoImportFix(element, (AutoImportFix.Context) context);
    }

    /**
     * Finds applicable import context for a path.
     *
     * @param path the path to find imports for
     * @return an {@code AutoImportFix.Context} instance or null
     */
    @Nullable
    public static Object findApplicableContext(@NotNull RsPath path) {
        return AutoImportFix.findApplicableContext(path);
    }

    /**
     * Finds applicable import context for a pattern binding.
     *
     * @param pat the pattern binding to find imports for
     * @return an {@code AutoImportFix.Context} instance or null
     */
    @Nullable
    public static Object findApplicableContext(@NotNull RsPatBinding pat) {
        return AutoImportFix.findApplicableContext(pat);
    }

    /**
     * Finds applicable import context for a method call.
     *
     * @param methodCall the method call to find imports for
     * @return an {@code AutoImportFix.Context} instance or null
     */
    @Nullable
    public static Object findApplicableContext(@NotNull RsMethodCall methodCall) {
        return AutoImportFix.findApplicableContext(methodCall);
    }

    /**
     * Invokes the auto-import fix.
     *
     * @param element the PSI element the fix is attached to
     * @param context an {@code AutoImportFix.Context} instance (typed as {@link Object} for Java compatibility)
     * @param project the current project
     * @param editor  the editor, or null
     */
    public static void invoke(@NotNull RsElement element, @NotNull Object context,
                              @NotNull Project project, @Nullable Editor editor) {
        AutoImportFix fix = new AutoImportFix(element, (AutoImportFix.Context) context);
        fix.invoke(project, editor, element);
    }

    /**
     * Gets the candidates list from an {@code AutoImportFix.Context}.
     *
     * @param context an {@code AutoImportFix.Context} instance (typed as {@link Object} for Java compatibility)
     * @return list of candidates typed as {@code List<Object>} for Java compatibility
     */
    @NotNull
    public static List<Object> getCandidates(@NotNull Object context) {
        return new ArrayList<>(((AutoImportFix.Context) context).getCandidates());
    }

    /**
     * Imports a candidate at the given context element.
     *
     * @param candidate      an {@link ImportCandidate} instance (typed as {@link Object} for Java compatibility)
     * @param contextElement the PSI element where the import should be added
     */
    public static void importCandidate(@NotNull Object candidate, @NotNull RsElement contextElement) {
        ImportUtils.importCandidate((ImportCandidate) candidate, contextElement);
    }
}
