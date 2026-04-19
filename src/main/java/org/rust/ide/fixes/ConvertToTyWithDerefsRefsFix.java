/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The fix applies path.derefs dereferences to the expression and then references of the mutability given by
 * path.refs.
 */
public class ConvertToTyWithDerefsRefsFix extends ConvertToTyFix {

    @SafeFieldForPreview
    private final DerefRefPath path;

    public ConvertToTyWithDerefsRefsFix(@NotNull RsExpr expr, @NotNull Ty ty, @NotNull DerefRefPath path) {
        super(expr, ty, formatRefs(path));
        this.path = path;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        element.replace(psiFactory.createRefExpr(psiFactory.createDerefExpr(element, path.derefs), path.refs));
    }

    private static String formatRefs(@NotNull DerefRefPath path) {
        List<String> refs = new ArrayList<>();
        for (Mutability m : path.refs) {
            refs.add(m.isMut() ? "&mut " : "&");
        }
        StringBuilder sb = new StringBuilder();
        for (String r : refs) {
            sb.append(r);
        }
        sb.append("*".repeat(path.derefs));
        return sb.toString().stripTrailing();
    }

    /**
     * Represents a sequence of dereferences and references.
     */
    public static class DerefRefPath {
        public final int derefs;
        public final List<Mutability> refs;

        public DerefRefPath(int derefs, @NotNull List<Mutability> refs) {
            this.derefs = derefs;
            this.refs = refs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DerefRefPath)) return false;
            DerefRefPath that = (DerefRefPath) o;
            return derefs == that.derefs && Objects.equals(refs, that.refs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(derefs, refs);
        }

        @Override
        public String toString() {
            return "DerefRefPath(derefs=" + derefs + ", refs=" + refs + ")";
        }
    }
}
