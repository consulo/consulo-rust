/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.decl.MGNodeData;
import org.rust.lang.core.macros.decl.MacroGraphBuilder;
import org.rust.lang.utils.PresentableGraph;
import org.rust.lang.core.psi.MacroBraces;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RsMacroDefinitionBaseUtil {
    private RsMacroDefinitionBaseUtil() {
    }

    private static final Pattern MACRO_CALL_PATTERN =
        Pattern.compile("(^|[^\\p{Alnum}_])(r#)?(?<name>\\w+)\\s*!\\s*(?<brace>[({\\[])");

    private static final Key<CachedValue<PresentableGraph<MGNodeData, Void>>> MACRO_GRAPH_KEY = Key.create("MACRO_GRAPH_KEY");

    /**
     * Analyses documentation of macro definition to determine what kind of brackets usually used.
     */
    @NotNull
    public static MacroBraces guessPreferredBraces(@NotNull RsDocAndAttributeOwner owner) {
        String documentation = org.rust.lang.doc.RsDocPipeline.documentation(owner, false);
        if (documentation == null || documentation.isEmpty()) return MacroBraces.PARENS;

        Map<MacroBraces, Integer> map = new EnumMap<>(MacroBraces.class);
        Matcher matcher = MACRO_CALL_PATTERN.matcher(documentation);
        String name = owner instanceof RsNameIdentifierOwner ? ((RsNameIdentifierOwner) owner).getName() : null;
        while (matcher.find()) {
            String matchedName = matcher.group("name");
            if (matchedName == null || !matchedName.equals(name)) continue;
            String brace = matcher.group("brace");
            if (brace == null) continue;
            MacroBraces braces = null;
            for (MacroBraces b : MacroBraces.values()) {
                if (b.getOpenText().equals(brace)) {
                    braces = b;
                    break;
                }
            }
            if (braces != null) {
                map.merge(braces, 1, Integer::sum);
            }
        }

        MacroBraces result = MacroBraces.PARENS;
        int maxCount = 0;
        for (Map.Entry<MacroBraces, Integer> entry : map.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                result = entry.getKey();
            }
        }
        return result;
    }

    @NotNull
    public static MacroBraces getPreferredBraces(@NotNull RsMacroDefinitionBase macro) {
        return macro.getPreferredBraces();
    }

    @Nullable
    public static PresentableGraph<MGNodeData, Void> getGraph(@NotNull RsMacroDefinitionBase macro) {
        return CachedValuesManager.getCachedValue(macro, MACRO_GRAPH_KEY, () -> {
            PresentableGraph<MGNodeData, Void> graph = new MacroGraphBuilder(macro).build();
            return CachedValueProvider.Result.create(graph, macro.getModificationTracker());
        });
    }
}
