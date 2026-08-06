package com.intellij.ide.nls;
import java.util.Collection;
import java.util.List;
/** IntelliJ-compat stub. */
public final class NlsMessages {
    private NlsMessages() {}
    public static String formatAndList(Collection<?> items) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = items.size();
        for (Object item : items) {
            sb.append(item);
            if (i < n - 2) sb.append(", ");
            else if (i == n - 2) sb.append(" and ");
            i++;
        }
        return sb.toString();
    }
    public static String formatAndList(String... items) { return formatAndList(List.of(items)); }
    public static String formatOrList(Collection<?> items) { return formatAndList(items); }
    public static String formatDuration(long ms) { return ms + " ms"; }
}
