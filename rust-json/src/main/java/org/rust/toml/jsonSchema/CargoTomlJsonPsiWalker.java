/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.intellij.json.pointer.JsonPointerPosition;
import com.jetbrains.jsonSchema.extension.adapter.JsonPropertyAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonValueAdapter;
import com.jetbrains.jsonSchema.walker.JsonLikePsiWalker;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlArrayTable;
import org.toml.lang.psi.TomlElement;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlHeaderOwner;
import org.toml.lang.psi.TomlInlineTable;
import org.toml.lang.psi.TomlKey;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlKeyValueOwner;
import org.toml.lang.psi.TomlTable;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets the JSON schema engine walk a TOML file as if it were JSON: tables are objects, arrays are
 * arrays and a dotted key or a {@code [table.header]} contributes one pointer step per segment.
 */
final class CargoTomlJsonPsiWalker implements JsonLikePsiWalker {
    static final CargoTomlJsonPsiWalker INSTANCE = new CargoTomlJsonPsiWalker();

    private CargoTomlJsonPsiWalker() {
    }

    @Override
    public ThreeState isName(PsiElement element) {
        return element instanceof TomlKeySegment ? ThreeState.YES : ThreeState.NO;
    }

    @Override
    public boolean isPropertyWithValue(PsiElement element) {
        return element instanceof TomlKeyValue keyValue && keyValue.getValue() != null;
    }

    @Nullable
    @Override
    public PsiElement findElementToCheck(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, TomlElement.class);
    }

    /**
     * Builds the pointer from the element up to the file. A TOML table is not nested in the tree,
     * so the walk also steps back over the preceding top-level tables: those decide which headers
     * enclose the element and, for {@code [[array.table]]}, which array index it sits at.
     * <p>
     * {@code forceLastTransition} is set by inspections; completion leaves it unset so that the
     * segment being typed is not part of the path.
     */
    @Nullable
    @Override
    public JsonPointerPosition findPosition(PsiElement element, boolean forceLastTransition) {
        JsonPointerPosition position = new JsonPointerPosition();
        List<String> tableHeaderSegments = new ArrayList<>();
        Integer nestedIndex = null;

        PsiElement current = element;
        while (current != null && !(current instanceof PsiFile)) {
            PsiElement parent = current.getParent();

            if (current instanceof TomlKeySegment segment && parent instanceof TomlKey key) {
                if (current != element || forceLastTransition) {
                    position.addPrecedingStep(segment.getName());
                }
                List<TomlKeySegment> preceding = new ArrayList<>();
                for (TomlKeySegment candidate : key.getSegments()) {
                    if (candidate == current) {
                        break;
                    }
                    preceding.add(candidate);
                }
                for (int i = preceding.size() - 1; i >= 0; i--) {
                    position.addPrecedingStep(preceding.get(i).getName());
                }
            }
            else if (current instanceof TomlKeyValue keyValue && parent instanceof TomlHeaderOwner headerOwner) {
                TomlKey parentKey = headerOwner.getHeader().getKey();
                if (parentKey == null) {
                    break;
                }
                // Collected so the preceding top-level tables can be matched against this header.
                for (TomlKeySegment headerSegment : parentKey.getSegments()) {
                    tableHeaderSegments.add(headerSegment.getName());
                }
                // An inspection walks whole properties, so the key path has to be added here.
                if (element instanceof TomlKeyValue) {
                    addKeySegments(position, keyValue.getKey(), element, forceLastTransition);
                }
            }
            else if (current instanceof TomlValue && parent instanceof TomlArray array) {
                if (current != element || forceLastTransition) {
                    position.addPrecedingStep(array.getElements().indexOf(current));
                }
            }
            else if (current instanceof TomlValue && parent instanceof TomlKeyValue keyValue) {
                addKeySegments(position, keyValue.getKey(), element, forceLastTransition);
            }
            else if (current instanceof TomlHeaderOwner headerOwner && parent instanceof PsiFile) {
                List<String> currentSegments = headerSegmentNames(headerOwner);

                // Unwind the collected header until it is no longer nested in this table's header.
                while (tableHeaderSegments.size() > currentSegments.size()
                    && startsWith(tableHeaderSegments, currentSegments)) {
                    if (nestedIndex != null) {
                        position.addPrecedingStep(nestedIndex);
                        nestedIndex = null;
                    }
                    position.addPrecedingStep(tableHeaderSegments.remove(tableHeaderSegments.size() - 1));
                }

                // Each earlier [[array.table]] with the same header moves the element one index on.
                if (currentSegments.equals(tableHeaderSegments) && current instanceof TomlArrayTable) {
                    nestedIndex = nestedIndex == null ? 0 : nestedIndex + 1;
                }
            }

            if (current instanceof TomlHeaderOwner && parent instanceof PsiFile && current.getPrevSibling() != null) {
                do {
                    current = current.getPrevSibling();
                }
                while (!(current instanceof TomlHeaderOwner) && current.getPrevSibling() != null);
            }
            else {
                current = parent;
            }
        }

        while (!tableHeaderSegments.isEmpty()) {
            if (nestedIndex != null) {
                position.addPrecedingStep(nestedIndex);
                nestedIndex = null;
            }
            position.addPrecedingStep(tableHeaderSegments.remove(tableHeaderSegments.size() - 1));
        }
        return position;
    }

    private static void addKeySegments(@Nonnull JsonPointerPosition position,
                                       @Nonnull TomlKey key,
                                       @Nonnull PsiElement element,
                                       boolean forceLastTransition) {
        List<TomlKeySegment> segments = key.getSegments();
        for (int i = segments.size() - 1; i >= 0; i--) {
            TomlKeySegment segment = segments.get(i);
            if (segment != element || forceLastTransition) {
                position.addPrecedingStep(segment.getName());
            }
        }
    }

    @Nonnull
    private static List<String> headerSegmentNames(@Nonnull TomlHeaderOwner headerOwner) {
        TomlKey key = headerOwner.getHeader().getKey();
        if (key == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (TomlKeySegment segment : key.getSegments()) {
            names.add(segment.getName());
        }
        return names;
    }

    private static boolean startsWith(@Nonnull List<String> list, @Nonnull List<String> prefix) {
        return list.size() >= prefix.size() && list.subList(0, prefix.size()).equals(prefix);
    }

    @Override
    public Set<String> getPropertyNamesOfParentObject(PsiElement originalPosition, PsiElement computedPosition) {
        TomlKeyValueOwner table = PsiTreeUtil.getParentOfType(originalPosition, TomlTable.class);
        if (table == null) {
            table = PsiTreeUtil.getParentOfType(originalPosition, TomlInlineTable.class);
        }
        if (table == null && originalPosition.getPrevSibling() instanceof TomlTable previous) {
            table = previous;
        }
        if (table == null) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        for (JsonPropertyAdapter property : new TomlJsonObjectAdapter(table).getPropertyList()) {
            String name = property.getName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    @Nullable
    @Override
    public JsonPropertyAdapter getParentPropertyAdapter(PsiElement element) {
        TomlKeyValue property = PsiTreeUtil.getParentOfType(element, TomlKeyValue.class, false);
        return property == null ? null : TomlJsonPropertyAdapter.of(property);
    }

    @Override
    public boolean isTopJsonElement(PsiElement element) {
        return element instanceof TomlFile;
    }

    @Nullable
    @Override
    public JsonValueAdapter createValueAdapter(PsiElement element) {
        if (element instanceof TomlFile file) {
            return TomlJsonObjectAdapter.forFile(file);
        }
        return element instanceof TomlElement tomlElement ? TomlJsonValueAdapter.createAdapterByType(tomlElement) : null;
    }

    @Override
    public Collection<PsiElement> getRoots(PsiFile file) {
        // The file itself is the root object; tables are its members, not separate roots.
        return file instanceof TomlFile ? List.of(file) : List.of();
    }

    @Nullable
    @Override
    public PsiElement getPropertyNameElement(@Nullable PsiElement property) {
        return property instanceof TomlKeyValue keyValue ? keyValue.getKey() : null;
    }

    @Override
    public boolean hasMissingCommaAfter(PsiElement element) {
        return false;
    }

    @Override
    public boolean acceptsEmptyRoot() {
        return true;
    }

    @Override
    public boolean requiresNameQuotes() {
        return false;
    }

    @Override
    public boolean allowsSingleQuotes() {
        return false;
    }
}
