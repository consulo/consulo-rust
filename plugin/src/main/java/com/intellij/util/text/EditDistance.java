package com.intellij.util.text;
/** Levenshtein edit distance between two character sequences. */
public final class EditDistance {
    private EditDistance() {}
    public static int levenshtein(CharSequence s1, CharSequence s2, boolean caseSensitive) {
        String a = caseSensitive ? s1.toString() : s1.toString().toLowerCase();
        String b = caseSensitive ? s2.toString() : s2.toString().toLowerCase();
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }
    public static int optimalAlignment(CharSequence s1, CharSequence s2, boolean caseSensitive) {
        return levenshtein(s1, s2, caseSensitive);
    }
}
