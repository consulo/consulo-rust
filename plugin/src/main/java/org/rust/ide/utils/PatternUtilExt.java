/* Helper for the "with last child, skipping" pattern constraint.
 * Returns the receiver unchanged, so the constraint is not applied (loses precision). */
package org.rust.ide.utils;
import consulo.language.pattern.PsiElementPattern;
public final class PatternUtilExt {
    private PatternUtilExt() {}
    public static <T extends PsiElementPattern<?, T>> T withLastChildSkipping(T self, Object skipper, Object childPattern) { return self; }
}
