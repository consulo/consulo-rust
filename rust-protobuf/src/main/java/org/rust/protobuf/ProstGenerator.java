/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.protobuf;

import com.intellij.protobuf.lang.psi.PbEnumDefinition;
import com.intellij.protobuf.lang.psi.PbEnumValue;
import com.intellij.protobuf.lang.psi.PbField;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbMapField;
import com.intellij.protobuf.lang.psi.PbMessageBody;
import com.intellij.protobuf.lang.psi.PbMessageDefinition;
import com.intellij.protobuf.lang.psi.PbOneofDefinition;
import com.intellij.protobuf.lang.psi.PbSimpleField;
import com.intellij.protobuf.lang.psi.PbTypeName;
import com.intellij.protobuf.lang.psi.SyntaxLevel;
import com.intellij.protobuf.lang.util.BuiltInType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.generated.RsGeneratedCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * The Rust code prost-build generates for the messages and enums of one protobuf package - a port of its
 * {@code code_generator.rs}. Function bodies are left out: the code is only read, never compiled.
 */
final class ProstGenerator {
    private static final Map<String, String> EXTERN_TYPES = Map.ofEntries(
        Map.entry(".google.protobuf.BoolValue", "bool"),
        Map.entry(".google.protobuf.BytesValue", "::prost::alloc::vec::Vec<u8>"),
        Map.entry(".google.protobuf.DoubleValue", "f64"),
        Map.entry(".google.protobuf.Empty", "()"),
        Map.entry(".google.protobuf.FloatValue", "f32"),
        Map.entry(".google.protobuf.Int32Value", "i32"),
        Map.entry(".google.protobuf.Int64Value", "i64"),
        Map.entry(".google.protobuf.StringValue", "::prost::alloc::string::String"),
        Map.entry(".google.protobuf.UInt32Value", "u32"),
        Map.entry(".google.protobuf.UInt64Value", "u64")
    );

    /** A field of a message as prost sees it: a map, or a scalar, enum or message typed field. */
    private record Field(@Nonnull PbField psi, @Nonnull String name, @Nullable BuiltInType builtIn,
                         @Nullable ProtoSymbols.Symbol symbol, boolean repeated, boolean explicitOptional,
                         @Nullable PbOneofDefinition oneof) {
        boolean isMessage() {
            return symbol != null ? symbol.isMessage() : builtIn == null;
        }

        boolean isEnum() {
            return symbol != null && !symbol.isMessage();
        }
    }

    @Nonnull
    private final ProtoSymbols symbols;
    @Nonnull
    private final RsGeneratedCode.Builder out;
    /** The package segments, the start of every local path. */
    @Nonnull
    private final List<String> packagePath;
    private final Map<String, Boolean> canCopy = new HashMap<>();
    private final Map<String, Boolean> canEq = new HashMap<>();

    ProstGenerator(@Nonnull ProtoSymbols symbols, @Nonnull RsGeneratedCode.Builder out, @Nonnull String packageName) {
        this.symbols = symbols;
        this.out = out;
        this.packagePath = packageName.isEmpty() ? List.of() : List.of(packageName.split("\\."));
    }

    void generateFile(@Nonnull PbFile file) {
        boolean proto2 = file.getSyntaxLevel() == SyntaxLevel.PROTO2;
        String scope = ProtoSymbols.packageScope(file);
        for (PbMessageDefinition message : PsiTreeUtil.getChildrenOfTypeAsList(file, PbMessageDefinition.class)) {
            message(message, scope, List.of(), proto2);
        }
        for (PbEnumDefinition enumDefinition : PsiTreeUtil.getChildrenOfTypeAsList(file, PbEnumDefinition.class)) {
            enumeration(enumDefinition);
        }
    }

    // ---- messages ----

    private void message(@Nonnull PbMessageDefinition message, @Nonnull String scope, @Nonnull List<String> typePath, boolean proto2) {
        String name = message.getName();
        if (name == null) return;
        String fqName = scope + "." + name;
        List<Field> fields = fields(message, fqName);

        out.append("#[derive(Clone, ");
        if (canDeriveCopy(fqName)) out.append("Copy, ");
        out.append("PartialEq, ");
        if (canDeriveEq(fqName)) out.append("Eq, Hash, ");
        out.append("Debug, Default)]\npub struct ").name(ProstNames.toUpperCamel(name), message).append(" {\n");
        Set<PbOneofDefinition> oneofs = new LinkedHashSet<>();
        for (Field field : fields) {
            if (field.oneof() != null) {
                oneofs.add(field.oneof());
                continue;
            }
            out.append("    pub ").name(ProstNames.toSnake(field.name()), field.psi()).append(": ")
                .append(fieldType(field, fqName, typePath, proto2)).append(",\n");
        }
        PbMessageBody body = message.getBody();
        if (body != null) {
            for (PbMapField map : body.getMapFieldList()) {
                String mapName = map.getName();
                if (mapName == null) continue;
                out.append("    pub ").name(ProstNames.toSnake(mapName), map).append(": ::std::collections::HashMap<")
                    .append(mapType(map.getKeyType(), fqName, typePath)).append(", ")
                    .append(mapType(map.getValueType(), fqName, typePath)).append(">,\n");
            }
        }
        String nestedModule = ProstNames.toSnake(name);
        for (PbOneofDefinition oneof : oneofs) {
            String oneofName = oneof.getName();
            if (oneofName == null) continue;
            out.append("    pub ").name(ProstNames.toSnake(oneofName), oneof).append(": ::core::option::Option<")
                .append(nestedModule).append("::").append(ProstNames.toUpperCamel(oneofName)).append(">,\n");
        }
        out.append("}\n");
        out.append("impl ::prost::Message for ").append(ProstNames.toUpperCamel(name)).append(" {}\n");
        accessors(name, fields, fqName, typePath, proto2);

        boolean hasNested = !oneofs.isEmpty() || body != null
            && (!body.getMessageDefinitionList().isEmpty() || !body.getEnumDefinitionList().isEmpty());
        if (!hasNested) return;
        List<String> nestedPath = new ArrayList<>(typePath);
        nestedPath.add(nestedModule);
        out.append("/// Nested message and enum types in `").append(name).append("`.\npub mod ").name(nestedModule, message).append(" {\n");
        if (body != null) {
            for (PbMessageDefinition nested : body.getMessageDefinitionList()) {
                message(nested, fqName, nestedPath, proto2);
            }
            for (PbEnumDefinition nested : body.getEnumDefinitionList()) {
                enumeration(nested);
            }
        }
        for (PbOneofDefinition oneof : oneofs) {
            oneof(oneof, fields, fqName, nestedPath);
        }
        out.append("}\n");
    }

    /** The fields of a message in declaration order, including the fields of its oneofs; maps are left out. */
    @Nonnull
    private List<Field> fields(@Nonnull PbMessageDefinition message, @Nonnull String fqName) {
        List<Field> fields = new ArrayList<>();
        PbMessageBody body = message.getBody();
        if (body == null) return fields;
        for (PsiElement child = body.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof PbSimpleField simple) {
                Field field = field(simple, fqName, null);
                if (field != null) fields.add(field);
            }
            else if (child instanceof PbOneofDefinition oneof && oneof.getBody() != null) {
                for (PbSimpleField member : oneof.getBody().getSimpleFieldList()) {
                    Field field = field(member, fqName, oneof);
                    if (field != null) fields.add(field);
                }
            }
        }
        return fields;
    }

    @Nullable
    private Field field(@Nonnull PbSimpleField field, @Nonnull String scope, @Nullable PbOneofDefinition oneof) {
        String name = field.getName();
        PbTypeName typeName = field.getTypeName();
        if (name == null || typeName == null) return null;
        BuiltInType builtIn = typeName.getBuiltInType();
        ProtoSymbols.Symbol symbol = builtIn == null ? symbols.resolve(typeName, scope) : null;
        boolean explicitOptional = field.getDeclaredLabel() != null && "optional".equals(field.getDeclaredLabel().getText());
        return new Field(field, name, builtIn, symbol, field.isRepeated(), explicitOptional, oneof);
    }

    /** The Rust type of a field: {@code Vec} when repeated, {@code Option} for a message or an optional scalar. */
    @Nonnull
    private String fieldType(@Nonnull Field field, @Nonnull String fqMessage, @Nonnull List<String> typePath, boolean proto2) {
        String type = elementType(field, typePath);
        if (field.repeated()) return "::prost::alloc::vec::Vec<" + type + ">";
        if (field.isMessage()) {
            if (isNested(field, fqMessage)) type = "::prost::alloc::boxed::Box<" + type + ">";
            return "::core::option::Option<" + type + ">";
        }
        if (field.explicitOptional() || proto2 && !isRequired(field)) return "::core::option::Option<" + type + ">";
        return type;
    }

    private static boolean isRequired(@Nonnull Field field) {
        return field.psi().getDeclaredLabel() != null && "required".equals(field.psi().getDeclaredLabel().getText());
    }

    /** The type of one value of a field: an enum is its number. */
    @Nonnull
    private String elementType(@Nonnull Field field, @Nonnull List<String> typePath) {
        if (field.builtIn() != null) return scalarType(field.builtIn());
        if (field.symbol() == null) return messagePath(unresolvedName(field.psi().getTypeName()), typePath);
        if (field.isEnum()) return "i32";
        return messagePath(field.symbol().fqName(), typePath);
    }

    @Nonnull
    private String mapType(@Nullable PbTypeName typeName, @Nonnull String scope, @Nonnull List<String> typePath) {
        if (typeName == null) return "()";
        BuiltInType builtIn = typeName.getBuiltInType();
        if (builtIn != null) return scalarType(builtIn);
        ProtoSymbols.Symbol symbol = symbols.resolve(typeName, scope);
        if (symbol == null) return messagePath(unresolvedName(typeName), typePath);
        return symbol.isMessage() ? messagePath(symbol.fqName(), typePath) : "i32";
    }

    /** A type of another file outside the scanned ones, `google.protobuf.Timestamp` for example. */
    @Nonnull
    private static String unresolvedName(@Nonnull PbTypeName typeName) {
        String reference = typeName.getReferenceString();
        return reference.startsWith(".") ? reference : "." + reference;
    }

    @Nonnull
    private static String scalarType(@Nonnull BuiltInType type) {
        return switch (type) {
            case DOUBLE -> "f64";
            case FLOAT -> "f32";
            case INT32, SINT32, SFIXED32 -> "i32";
            case INT64, SINT64, SFIXED64 -> "i64";
            case UINT32, FIXED32 -> "u32";
            case UINT64, FIXED64 -> "u64";
            case BOOL -> "bool";
            case STRING -> "::prost::alloc::string::String";
            case BYTES -> "::prost::alloc::vec::Vec<u8>";
        };
    }

    /**
     * The path of a message or enum from the module at {@code typePath}, like prost-build's {@code resolve_ident}:
     * the common start of the two paths is dropped, each remaining local segment becomes {@code super}.
     */
    @Nonnull
    String messagePath(@Nonnull String fqName, @Nonnull List<String> typePath) {
        String extern = EXTERN_TYPES.get(fqName);
        if (extern != null) return extern;
        if (fqName.startsWith(".google.protobuf.")) {
            return "::prost_types::" + ProstNames.toUpperCamel(fqName.substring(".google.protobuf.".length()));
        }
        List<String> local = new ArrayList<>(packagePath);
        local.addAll(typePath);
        List<String> ident = new ArrayList<>(List.of(fqName.substring(1).split("\\.")));
        String typeName = ident.remove(ident.size() - 1);
        int common = 0;
        while (common < local.size() && common < ident.size() && local.get(common).equals(ident.get(common))) common++;
        StringBuilder path = new StringBuilder();
        for (int i = common; i < local.size(); i++) path.append("super::");
        for (int i = common; i < ident.size(); i++) path.append(ProstNames.toSnake(ident.get(i))).append("::");
        return path.append(ProstNames.toUpperCamel(typeName)).toString();
    }

    /**
     * The accessors prost's {@code Message} derive adds: the enum value of an enum field and its setter, the value
     * or the default of an optional scalar.
     */
    private void accessors(@Nonnull String messageName, @Nonnull List<Field> fields, @Nonnull String fqMessage,
                           @Nonnull List<String> typePath, boolean proto2) {
        StringBuilder methods = new StringBuilder();
        List<Field> withAccessors = new ArrayList<>();
        for (Field field : fields) {
            if (field.oneof() != null || field.isMessage()) continue;
            boolean optional = field.explicitOptional() || proto2 && !isRequired(field);
            if (field.isEnum() || optional) withAccessors.add(field);
        }
        if (withAccessors.isEmpty()) return;
        out.append("impl ").append(ProstNames.toUpperCamel(messageName)).append(" {\n");
        for (Field field : withAccessors) {
            String getter = ProstNames.toSnake(field.name());
            String setterName = ProstNames.toSnake(field.name()).replace("r#", "");
            if (field.isEnum()) {
                String enumPath = messagePath(field.symbol().fqName(), typePath);
                if (field.repeated()) {
                    out.append("    pub fn ").name(getter, field.psi())
                        .append("(&self) -> impl ::core::iter::Iterator<Item = ").append(enumPath).append("> + '_ { loop {} }\n");
                    out.append("    pub fn ").name("push_" + setterName, field.psi())
                        .append("(&mut self, value: ").append(enumPath).append(") {}\n");
                }
                else {
                    out.append("    pub fn ").name(getter, field.psi()).append("(&self) -> ").append(enumPath).append(" { loop {} }\n");
                    out.append("    pub fn ").name("set_" + setterName, field.psi())
                        .append("(&mut self, value: ").append(enumPath).append(") {}\n");
                }
            }
            else if (!field.repeated()) {
                String type = switch (field.builtIn()) {
                    case STRING -> "&str";
                    case BYTES -> "&[u8]";
                    case null -> "()";
                    default -> scalarType(field.builtIn());
                };
                out.append("    pub fn ").name(getter, field.psi()).append("(&self) -> ").append(type).append(" { loop {} }\n");
            }
        }
        out.append(methods).append("}\n");
    }

    // ---- oneofs ----

    private void oneof(@Nonnull PbOneofDefinition oneof, @Nonnull List<Field> fields, @Nonnull String fqMessage,
                       @Nonnull List<String> typePath) {
        String name = oneof.getName();
        if (name == null) return;
        List<Field> members = new ArrayList<>();
        for (Field field : fields) {
            if (field.oneof() == oneof) members.add(field);
        }
        boolean copy = true;
        boolean eq = true;
        for (Field member : members) {
            copy &= canFieldDeriveCopy(member, fqMessage);
            eq &= canFieldDeriveEq(member, fqMessage);
        }
        out.append("    #[derive(Clone, ");
        if (copy) out.append("Copy, ");
        out.append("PartialEq, ");
        if (eq) out.append("Eq, Hash, ");
        out.append("Debug)]\n    pub enum ").name(ProstNames.toUpperCamel(name), oneof).append(" {\n");
        for (Field member : members) {
            String type = elementType(member, typePath);
            if (member.isMessage() && isNested(member, fqMessage)) type = "::prost::alloc::boxed::Box<" + type + ">";
            out.append("        ").name(ProstNames.toUpperCamel(member.name()), member.psi()).append("(").append(type).append("),\n");
        }
        out.append("    }\n");
    }

    // ---- enums ----

    private void enumeration(@Nonnull PbEnumDefinition enumDefinition) {
        String name = enumDefinition.getName();
        if (name == null || enumDefinition.getBody() == null) return;
        String rustName = ProstNames.toUpperCamel(name);
        out.append("#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]\n#[repr(i32)]\npub enum ")
            .name(rustName, enumDefinition).append(" {\n");
        Set<String> numbers = new HashSet<>();
        for (PbEnumValue value : enumDefinition.getBody().getEnumValueList()) {
            String valueName = value.getName();
            String number = value.getNumberValue() != null ? value.getNumberValue().getText() : null;
            // an alias of an earlier value gets no variant of its own
            if (valueName == null || number == null || !numbers.add(number)) continue;
            out.append("    ").name(ProstNames.enumValueName(name, valueName), value).append(" = ").append(number).append(",\n");
        }
        out.append("}\n");
        out.append("impl ").append(rustName).append(" {\n")
            .append("    pub fn as_str_name(&self) -> &'static str { loop {} }\n")
            .append("    pub fn from_str_name(value: &str) -> ::core::option::Option<Self> { loop {} }\n")
            .append("    pub fn is_valid(value: i32) -> bool { loop {} }\n")
            .append("}\n");
        out.append("impl ::core::default::Default for ").append(rustName).append(" { fn default() -> Self { loop {} } }\n");
        out.append("impl ::core::convert::From<").append(rustName).append("> for i32 { fn from(value: ")
            .append(rustName).append(") -> i32 { loop {} } }\n");
        out.append("impl ::core::convert::TryFrom<i32> for ").append(rustName)
            .append(" {\n    type Error = ::prost::UnknownEnumValue;\n")
            .append("    fn try_from(value: i32) -> ::core::result::Result<").append(rustName)
            .append(", ::prost::UnknownEnumValue> { loop {} }\n}\n");
    }

    // ---- derives, as prost-build's context.rs decides them ----

    private boolean canDeriveCopy(@Nonnull String fqMessage) {
        Boolean cached = canCopy.get(fqMessage);
        if (cached != null) return cached;
        canCopy.put(fqMessage, false);
        ProtoSymbols.Symbol symbol = symbols.get(fqMessage);
        boolean result = symbol != null && symbol.message() != null;
        if (result) {
            PbMessageBody body = symbol.message().getBody();
            result = body == null || body.getMapFieldList().isEmpty();
            for (Field field : fields(symbol.message(), fqMessage)) {
                if (!result) break;
                result = canFieldDeriveCopy(field, fqMessage);
            }
        }
        canCopy.put(fqMessage, result);
        return result;
    }

    private boolean canFieldDeriveCopy(@Nonnull Field field, @Nonnull String fqMessage) {
        if (field.repeated()) return false;
        if (field.isMessage()) {
            if (field.symbol() == null || isNested(field, fqMessage)) return false;
            return canDeriveCopy(field.symbol().fqName());
        }
        return field.isEnum() || field.builtIn() != BuiltInType.STRING && field.builtIn() != BuiltInType.BYTES;
    }

    private boolean canDeriveEq(@Nonnull String fqMessage) {
        Boolean cached = canEq.get(fqMessage);
        if (cached != null) return cached;
        canEq.put(fqMessage, false);
        ProtoSymbols.Symbol symbol = symbols.get(fqMessage);
        boolean result = symbol != null && symbol.message() != null;
        if (result) {
            PbMessageBody body = symbol.message().getBody();
            result = body == null || body.getMapFieldList().isEmpty();
            for (Field field : fields(symbol.message(), fqMessage)) {
                if (!result) break;
                result = canFieldDeriveEq(field, fqMessage);
            }
        }
        canEq.put(fqMessage, result);
        return result;
    }

    private boolean canFieldDeriveEq(@Nonnull Field field, @Nonnull String fqMessage) {
        if (field.isMessage()) {
            if (field.repeated() || field.symbol() == null || isNested(field, fqMessage)) return false;
            return canDeriveEq(field.symbol().fqName());
        }
        return field.isEnum() || field.builtIn() != BuiltInType.FLOAT && field.builtIn() != BuiltInType.DOUBLE;
    }

    /**
     * Whether the message type of a field contains {@code fqMessage} through singular message fields - such a field
     * is boxed, a recursive type would have no size.
     */
    private boolean isNested(@Nonnull Field field, @Nonnull String fqMessage) {
        if (field.symbol() == null || !field.symbol().isMessage()) return false;
        return reaches(field.symbol().fqName(), fqMessage, new HashSet<>());
    }

    private boolean reaches(@Nonnull String from, @Nonnull String to, @Nonnull Set<String> visited) {
        if (from.equals(to)) return true;
        if (!visited.add(from)) return false;
        ProtoSymbols.Symbol symbol = symbols.get(from);
        if (symbol == null || symbol.message() == null) return false;
        for (Field field : fields(symbol.message(), from)) {
            if (!field.repeated() && field.symbol() != null && field.symbol().isMessage()
                && reaches(field.symbol().fqName(), to, visited)) {
                return true;
            }
        }
        return false;
    }
}
