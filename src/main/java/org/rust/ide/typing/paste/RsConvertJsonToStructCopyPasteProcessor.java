/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.ide.inspections.lints.RsNamingInspection;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve2.FacadeMetaInfo;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.openapiext.PsiFileExtUtil;
import org.rust.openapiext.OpenApiUtil;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.regex.Pattern;
import com.intellij.psi.PsiFile;

public class RsConvertJsonToStructCopyPasteProcessor extends CopyPastePostProcessor<TextBlockTransferableData> {

    public static final String CONVERT_JSON_TO_STRUCT_SETTING_KEY = "org.rust.convert.json.to.struct";

    private static boolean CONVERT_JSON_SERDE_PRESENT = false;

    @TestOnly
    public static void convertJsonWithSerdePresent(boolean hasSerde, Runnable action) {
        boolean original = CONVERT_JSON_SERDE_PRESENT;
        CONVERT_JSON_SERDE_PRESENT = hasSerde;
        try {
            action.run();
        } finally {
            CONVERT_JSON_SERDE_PRESENT = original;
        }
    }

    @Override
    public List<TextBlockTransferableData> collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
        return Collections.emptyList();
    }

    @Override
    public List<TextBlockTransferableData> extractTransferableData(Transferable content) {
        try {
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) content.getTransferData(DataFlavor.stringFlavor);
                return Collections.singletonList(new PotentialJsonTransferableData(text));
            }
            return Collections.emptyList();
        } catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void processTransferableData(
        Project project,
        Editor editor,
        RangeMarker bounds,
        int caretOffset,
        Ref<? super Boolean> indented,
        List<? extends TextBlockTransferableData> values
    ) {
        PsiFile psiFile = PsiFileExtUtil.toPsiFile(editor.getDocument(), project);
        if (!(psiFile instanceof RsFile)) return;
        RsFile file = (RsFile) psiFile;

        TextBlockTransferableData value = values.isEmpty() ? null : values.get(0);
        if (!(value instanceof PotentialJsonTransferableData)) return;
        String text = ((PotentialJsonTransferableData) value).getText();

        PsiElement elementAtCaret = file.findElementAt(caretOffset);
        if (elementAtCaret != null && !(elementAtCaret.getParent() instanceof RsMod)) return;

        List<Struct> structs = JsonStructParser.extractStructsFromJson(text);
        if (structs == null) return;
        if (!shouldConvertJson(project)) return;

        RsPsiFactory factory = new RsPsiFactory(project);
        RsMod parentMod = elementAtCaret != null ? (RsMod) elementAtCaret.getParent() : file;
        Set<String> existingNames = FacadeMetaInfo.allScopeItemNames(parentMod);
        Map<Struct, String> nameMap = generateStructNames(structs, existingNames);

        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        List<SmartPsiElementPointer<RsStructItem>> insertedItems = new ArrayList<>();

        boolean hasSerdeDependency = hasSerdeDependency(file);

        ApplicationManager.getApplication().runWriteAction(() -> {
            editor.getDocument().deleteString(bounds.getStartOffset(), bounds.getEndOffset());
            psiDocumentManager.commitDocument(editor.getDocument());

            PsiElement element = file.findElementAt(caretOffset);
            PsiElement parent = element != null ? element.getParent() : file;
            PsiElement anchor = element;

            for (Struct struct : structs) {
                RsStructItem inserted = createAndInsertStruct(factory, anchor, parent, struct, nameMap, hasSerdeDependency);
                if (inserted == null) continue;
                anchor = inserted;
                insertedItems.add(SmartPointerManager.createPointer(inserted));
            }
        });

        if (!insertedItems.isEmpty()) {
            replacePlaceholders(editor, insertedItems, nameMap, file);
        }
    }

    private static boolean hasSerdeDependency(RsFile file) {
        if (OpenApiUtil.isUnitTestMode() && CONVERT_JSON_SERDE_PRESENT) {
            return true;
        }
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(file);
        if (pkg == null) return false;
        for (Object dep : pkg.getDependencies()) {
            // check dependency name equals "serde"
        }
        return false;
    }

    private enum StoredPreference { YES, NO, ASK_EVERY_TIME }

    private static boolean shouldConvertJson(Project project) {
        if (OpenApiUtil.isUnitTestMode()) return true;

        String prefStr = AdvancedSettings.getString(CONVERT_JSON_TO_STRUCT_SETTING_KEY);
        StoredPreference pref;
        try {
            pref = StoredPreference.valueOf(prefStr);
        } catch (IllegalArgumentException e) {
            pref = StoredPreference.ASK_EVERY_TIME;
        }
        switch (pref) {
            case YES:
                return true;
            case NO:
                return false;
            case ASK_EVERY_TIME:
                return MessageDialogBuilder.yesNo(
                    RsBundle.message("copy.paste.convert.json.to.struct.dialog.title"),
                    RsBundle.message("copy.paste.convert.json.to.struct.dialog.text")
                )
                    .yesText(Messages.getYesButton())
                    .noText(Messages.getNoButton())
                    .icon(Messages.getQuestionIcon())
                    .doNotAsk(new DoNotAskOption.Adapter() {
                        @Override
                        public void rememberChoice(boolean isSelected, int exitCode) {
                            if (isSelected) {
                                StoredPreference value = exitCode == Messages.YES
                                    ? StoredPreference.YES
                                    : StoredPreference.NO;
                                AdvancedSettings.setString(CONVERT_JSON_TO_STRUCT_SETTING_KEY, value.name());
                            }
                        }
                    })
                    .ask(project);
            default:
                return false;
        }
    }

    private static Map<Struct, String> generateStructNames(List<Struct> structs, Set<String> existingNames) {
        Map<Struct, String> map = new LinkedHashMap<>();
        if (structs.isEmpty()) return map;

        Set<String> assignedNames = new HashSet<>(existingNames);

        assignName(map, assignedNames, structs.get(structs.size() - 1), "Root");

        Map<Struct, Set<String>> structEmbeddedFields = new LinkedHashMap<>();
        DataTypeVisitor visitor = new DataTypeVisitor() {
            @Override
            public void visitStruct(DataType.StructRef dataType) {
                for (Map.Entry<String, DataType> entry : dataType.getStruct().getFields().entrySet()) {
                    DataType innerType = entry.getValue().unwrap();
                    if (innerType instanceof DataType.StructRef) {
                        structEmbeddedFields
                            .computeIfAbsent(((DataType.StructRef) innerType).getStruct(), k -> new LinkedHashSet<>())
                            .add(entry.getKey());
                    }
                }
                super.visitStruct(dataType);
            }
        };
        visitor.visit(new DataType.StructRef(structs.get(structs.size() - 1)));

        List<Struct> reversed = new ArrayList<>(structs);
        Collections.reverse(reversed);
        for (Struct struct : reversed) {
            if (!map.containsKey(struct)) {
                Set<String> fields = structEmbeddedFields.getOrDefault(struct, Collections.emptySet());
                if (fields.size() == 1) {
                    assignName(map, assignedNames, struct, fields.iterator().next());
                } else {
                    assignName(map, assignedNames, struct, "Struct");
                }
            }
        }
        return map;
    }

    private static void assignName(Map<Struct, String> map, Set<String> assignedNames, Struct struct, String name) {
        String normalizedName = normalizeStructName(name);
        String actualName;
        if (assignedNames.contains(normalizedName)) {
            int counter = 1;
            while (assignedNames.contains(normalizedName + counter)) {
                counter++;
            }
            actualName = normalizedName + counter;
        } else {
            actualName = normalizedName;
        }
        assignedNames.add(actualName);
        map.put(struct, actualName);
    }

    private static final Pattern NON_IDENTIFIER_REGEX = Pattern.compile("[^a-zA-Z_0-9]");

    private static String normalizeStructName(String name) {
        return normalizeName(name, "Struct", RsNamingInspection::toCamelCase);
    }

    private static String normalizeFieldName(String field) {
        return normalizeName(field, "field", s -> org.rust.ide.inspections.lints.RsNamingInspection.toSnakeCase(s, false));
    }

    private static String normalizeName(String name, String placeholder, java.util.function.Function<String, String> caseConversion) {
        String normalized = NON_IDENTIFIER_REGEX.matcher(name).replaceAll("_");
        normalized = caseConversion.apply(normalized);

        if (!normalized.isEmpty() && Character.isDigit(normalized.charAt(0))) {
            normalized = "_" + normalized;
        }

        boolean allUnderscores = true;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) != '_') {
                allUnderscores = false;
                break;
            }
        }
        if (allUnderscores) {
            normalized += placeholder;
        }

        return org.rust.lang.core.psi.RsRawIdentifiers.escapeIdentifierIfNeeded(normalized);
    }

    private static String createFieldName(String field, Set<String> generatedFieldNames) {
        String normalizedName = normalizeFieldName(field);
        if (!generatedFieldNames.contains(normalizedName)) return normalizedName;

        int counter = 0;
        while (generatedFieldNames.contains(normalizedName + "_" + counter)) {
            counter++;
        }
        return normalizedName + "_" + counter;
    }

    private static String getSerdeType(DataType type, Map<Struct, String> nameMap) {
        if (type instanceof DataType.BooleanType) return "bool";
        if (type instanceof DataType.StringType) return "String";
        if (type instanceof DataType.IntegerType) return "i64";
        if (type instanceof DataType.FloatType) return "f64";
        if (type instanceof DataType.Nullable) {
            return "Option<" + getSerdeType(((DataType.Nullable) type).getType(), nameMap) + ">";
        }
        if (type instanceof DataType.StructRef) {
            String name = nameMap.get(((DataType.StructRef) type).getStruct());
            return name != null ? name : "_";
        }
        if (type instanceof DataType.Array) {
            return "Vec<" + getSerdeType(((DataType.Array) type).getType(), nameMap) + ">";
        }
        return "_";
    }

    private static RsStructItem createAndInsertStruct(
        RsPsiFactory factory,
        PsiElement anchor,
        PsiElement parent,
        Struct struct,
        Map<Struct, String> nameMap,
        boolean hasSerdeDependency
    ) {
        RsStructItem structPsi = generateStruct(factory, struct, nameMap, hasSerdeDependency);
        if (structPsi == null) return null;

        RsStructItem inserted;
        if (anchor == null) {
            inserted = (RsStructItem) parent.add(structPsi);
        } else {
            inserted = (RsStructItem) parent.addAfter(structPsi, anchor);
        }

        if (hasSerdeDependency) {
            org.rust.lang.core.resolve.KnownItems knownItems = KnownItems.getKnownItems(inserted);
            RsQualifiedNamedElement serialize = knownItems.findItem("serde::Serialize", false, RsQualifiedNamedElement.class);
            RsQualifiedNamedElement deserialize = knownItems.findItem("serde::Deserialize", false, RsQualifiedNamedElement.class);
            if (serialize != null) RsImportHelper.importElement(inserted, serialize);
            if (deserialize != null) RsImportHelper.importElement(inserted, deserialize);
        }

        return inserted;
    }

    private static RsStructItem generateStruct(
        RsPsiFactory factory,
        Struct struct,
        Map<Struct, String> nameMap,
        boolean hasSerdeDependency
    ) {
        StringBuilder sb = new StringBuilder();
        if (hasSerdeDependency) {
            sb.append("#[derive(Serialize, Deserialize)]\n");
        }
        sb.append("struct ").append(nameMap.get(struct)).append(" {\n");

        Set<String> names = new HashSet<>();
        for (Map.Entry<String, DataType> entry : struct.getFields().entrySet()) {
            writeStructField(sb, entry.getKey(), entry.getValue(), nameMap, names, hasSerdeDependency);
        }
        sb.append("}");
        return factory.tryCreateStruct(sb.toString());
    }

    private static void writeStructField(
        StringBuilder sb,
        String field,
        DataType type,
        Map<Struct, String> structNameMap,
        Set<String> generatedFieldNames,
        boolean hasSerdeDependency
    ) {
        String normalizedName = createFieldName(field, generatedFieldNames);
        String serdeType = getSerdeType(type, structNameMap);
        if (hasSerdeDependency && !field.equals(normalizedName)) {
            String rawField = field.replace("\"", "\\\"");
            sb.append("#[serde(rename = \"").append(rawField).append("\")]\n");
        }
        sb.append("pub ").append(normalizedName).append(": ").append(serdeType).append(",\n");
        generatedFieldNames.add(normalizedName);
    }

    private static void replacePlaceholders(
        Editor editor,
        List<SmartPsiElementPointer<RsStructItem>> insertedItems,
        Map<Struct, String> nameMap,
        RsFile file
    ) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor.isDisposed()) return;
            Project project = editor.getProject();
            if (project == null) return;

            OpenApiUtil.runWriteCommandAction(project,
                RsBundle.message("copy.paste.convert.json.to.struct.dialog.title"),
                () -> {
                    if (!file.isValid()) return;
                    // Template builder logic would go here
                });
        });
    }

    private static class PotentialJsonTransferableData implements TextBlockTransferableData {
        private static final DataFlavor DATA_FLAVOR = new DataFlavor(
            RsConvertJsonToStructCopyPasteProcessor.class,
            "class: RsConvertJsonToStructCopyPasteProcessor"
        );

        private final String myText;

        PotentialJsonTransferableData(String text) {
            myText = text;
        }

        public String getText() {
            return myText;
        }

        @Override
        public DataFlavor getFlavor() {
            return DATA_FLAVOR;
        }

        @Override
        public int getOffsetCount() {
            return 0;
        }

        @Override
        public int getOffsets(int[] offsets, int index) {
            return index;
        }

        @Override
        public int setOffsets(int[] offsets, int index) {
            return index;
        }
    }
}
