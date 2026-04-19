/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

/**
 * <p>
 * Delegates to {@link AutoImportFixBridge} for all operations.
 * This class provides backward compatibility for existing Java code that references
 * {@code AutoImportFixFactory} by name.
 */
public final class AutoImportFixFactory {

    private AutoImportFixFactory() {
    }

    @NotNull
    public static Object create(@NotNull RsElement element, @NotNull Object context) {
        return AutoImportFixBridge.create(element, context);
    }

    @Nullable
    public static Object findApplicableContext(@NotNull RsPath path) {
        return AutoImportFixBridge.findApplicableContext(path);
    }

    @Nullable
    public static Object findApplicableContext(@NotNull RsPatBinding pat) {
        return AutoImportFixBridge.findApplicableContext(pat);
    }

    @Nullable
    public static Object findApplicableContext(@NotNull RsMethodCall methodCall) {
        return AutoImportFixBridge.findApplicableContext(methodCall);
    }

    public static void invoke(@NotNull RsElement element, @NotNull Object context,
                              @NotNull Project project, @Nullable Editor editor) {
        AutoImportFixBridge.invoke(element, context, project, editor);
    }

    @NotNull
    public static List<Object> getCandidates(@NotNull Object context) {
        return AutoImportFixBridge.getCandidates(context);
    }

    public static void importCandidate(@NotNull Object candidate, @NotNull RsElement contextElement) {
        AutoImportFixBridge.importCandidate(candidate, contextElement);
    }
}
