/* Stub utility for IntelliJ's withLastChildSkipping pattern helper,
 * absent in Consulo. Returns the receiver unchanged (loses precision). */
package org.rust.ide.utils;
import consulo.language.pattern.PsiElementPattern;
public final class PatternUtilExt {
    private PatternUtilExt() {}
    public static <T extends PsiElementPattern<?, T>> T withLastChildSkipping(T self, Object skipper, Object childPattern) { return self; }
}
