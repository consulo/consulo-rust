/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    public static Object create(@Nonnull RsElement element, @Nonnull Object context) {
        return AutoImportFixBridge.create(element, context);
    }

    @Nullable
    public static Object findApplicableContext(@Nonnull RsPath path) {
        return AutoImportFixBridge.findApplicableContext(path);
    }

    @Nullable
    public static Object findApplicableContext(@Nonnull RsPatBinding pat) {
        return AutoImportFixBridge.findApplicableContext(pat);
    }

    @Nullable
    public static Object findApplicableContext(@Nonnull RsMethodCall methodCall) {
        return AutoImportFixBridge.findApplicableContext(methodCall);
    }

    public static void invoke(@Nonnull RsElement element, @Nonnull Object context,
                              @Nonnull Project project, @Nullable Editor editor) {
        AutoImportFixBridge.invoke(element, context, project, editor);
    }

    @Nonnull
    public static List<Object> getCandidates(@Nonnull Object context) {
        return AutoImportFixBridge.getCandidates(context);
    }

    public static void importCandidate(@Nonnull Object candidate, @Nonnull RsElement contextElement) {
        AutoImportFixBridge.importCandidate(candidate, contextElement);
    }
}
