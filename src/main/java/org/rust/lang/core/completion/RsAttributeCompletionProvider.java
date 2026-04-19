/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwnerUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsAttributeCompletionProvider extends RsCompletionProvider {
    public static final RsAttributeCompletionProvider INSTANCE = new RsAttributeCompletionProvider();

    private static final List<RustAttribute> ATTRIBUTES;

    static {
        Map<ElementPattern<PsiElement>, String> attrMap = new LinkedHashMap<>();
        attrMap.put(RsPsiPattern.onCrate, "crate_name crate_type feature() no_builtins no_main no_start no_std recursion_limit type_length_limit windows_subsystem");
        attrMap.put(RsPsiPattern.onExternCrate, "macro_use no_link");
        attrMap.put(RsPsiPattern.onMod, "no_implicit_prelude path macro_use");
        attrMap.put(RsPsiPattern.onFn, "main start test cold naked export_name link_section lang inline track_caller panic_handler must_use target_feature()");
        attrMap.put(RsPsiPattern.onTestFn, "should_panic ignore");
        attrMap.put(RsPsiPattern.onProcMacroFn, "proc_macro proc_macro_derive() proc_macro_attribute");
        attrMap.put(RsPsiPattern.onStaticMut, "thread_local");
        attrMap.put(RsPsiPattern.onExternBlock, "link_args link() linked_from");
        attrMap.put(RsPsiPattern.onExternBlockDecl, "link_name linkage");
        attrMap.put(RsPsiPattern.onStruct, "repr() unsafe_no_drop_flags derive() must_use");
        attrMap.put(RsPsiPattern.onEnum, "repr() derive() must_use");
        attrMap.put(RsPsiPattern.onTrait, "must_use");
        attrMap.put(RsPsiPattern.onMacro, "macro_export");
        attrMap.put(RsPsiPattern.onStatic, "export_name link_section used global_allocator");
        attrMap.put(RsPsiPattern.onAnyItem, "no_mangle doc cfg() cfg_attr() allow() warn() forbid() deny() deprecated");
        attrMap.put(RsPsiPattern.onTupleStruct, "simd");
        attrMap.put(RsPsiPattern.onStructLike, "non_exhaustive");

        List<RustAttribute> attrs = new ArrayList<>();
        for (Map.Entry<ElementPattern<PsiElement>, String> entry : attrMap.entrySet()) {
            for (String attrName : entry.getValue().split(" ")) {
                attrs.add(new RustAttribute(attrName, entry.getKey()));
            }
        }
        ATTRIBUTES = Collections.unmodifiableList(attrs);
    }

    private RsAttributeCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement elem = context.get(RsPsiPattern.META_ITEM_ATTR);
        if (elem == null) return;
        PsiElement owner = RsAttrProcMacroOwnerUtil.getOwner(elem);
        if (owner == null) return;

        Set<String> existingMetaItems = getAttrMetaItems(owner);

        List<LookupElement> suggestions = ATTRIBUTES.stream()
            .filter(attr -> attr.myAppliesTo.accepts(parameters.getPosition()) && !existingMetaItems.contains(attr.myName))
            .map(attr -> createLookupElement(attr.myName))
            .collect(Collectors.toList());
        result.addAllElements(suggestions);
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER)
            .withParent(
                psiElement(RsPath.class)
                    .with(new PatternCondition<RsPath>("Unqualified") {
                        @Override
                        public boolean accepts(@NotNull RsPath it, ProcessingContext ctx) {
                            return !it.getHasColonColon();
                        }
                    })
                    .withParent(RsPsiPattern.rootMetaItem)
            );
    }

    private static LookupElement createLookupElement(String name) {
        if (name.endsWith("()")) {
            return LookupElementBuilder.create(name.substring(0, name.length() - 2))
                .withInsertHandler((ctx, item) -> {
                    ctx.getDocument().insertString(ctx.getSelectionEndOffset(), "()");
                    EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 1);
                })
                .withIcon(RsIcons.ATTRIBUTE);
        } else {
            return LookupElementBuilder.create(name)
                .withIcon(RsIcons.ATTRIBUTE);
        }
    }

    private static Set<String> getAttrMetaItems(PsiElement element) {
        if (element instanceof RsDocAndAttributeOwner) {
            Set<String> result = new HashSet<>();
            ((RsDocAndAttributeOwner) element).getQueryAttributes().getMetaItems().forEach(item -> {
                String name = item.getName();
                if (name != null) {
                    result.add(name);
                }
            });
            return result;
        }
        return Collections.emptySet();
    }

    private static final class RustAttribute {
        final String myName;
        final ElementPattern<PsiElement> myAppliesTo;

        RustAttribute(String name, ElementPattern<PsiElement> appliesTo) {
            myName = name;
            myAppliesTo = appliesTo;
        }
    }
}
