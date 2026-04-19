/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class JsonStructParser {

    private JsonStructParser() {
    }

    /**
     * Try to parse the input text as a JSON object.
     * Extract a list of (unique) structs that were encountered in the JSON object.
     */
    public static List<Struct> extractStructsFromJson(String text) {
        String input = text.trim();
        if (input.isEmpty() || input.charAt(0) != '{' || input.charAt(input.length() - 1) != '}') return null;

        try {
            com.fasterxml.jackson.core.JsonFactory factory = new JsonFactoryBuilder()
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build();
            JsonParser parser = factory.createParser(input);
            try {
                StructParserInternal structParser = new StructParserInternal();
                JsonToken token = parser.nextToken();
                if (token != JsonToken.START_OBJECT) return null;

                Struct rootStruct = structParser.parseStruct(parser);
                if (rootStruct == null) return null;

                List<Struct> result = new ArrayList<>(gatherEncounteredStructs(rootStruct));
                Collections.reverse(result);
                return result;
            } finally {
                parser.close();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Finds all unique structs contained in the root struct.
     */
    public static Set<Struct> gatherEncounteredStructs(Struct root) {
        LinkedHashSet<Struct> structs = new LinkedHashSet<>();

        DataTypeVisitor visitor = new DataTypeVisitor() {
            @Override
            public void visitStruct(DataType.StructRef dataType) {
                structs.add(dataType.getStruct());
                super.visitStruct(dataType);
            }
        };

        visitor.visit(new DataType.StructRef(root));
        return structs;
    }

    private static class StructParserInternal {
        private final LinkedHashSet<Struct> myStructMap = new LinkedHashSet<>();

        Struct parseStruct(JsonParser parser) throws IOException {
            if (parser.currentToken() != JsonToken.START_OBJECT) return null;

            LinkedHashMap<String, DataType> fields = new LinkedHashMap<>();
            while (true) {
                Field field = parseField(parser);
                if (field == null) break;
                if (fields.containsKey(field.getName())) continue;
                fields.put(field.getName(), field.getType());
            }

            if (parser.currentToken() != JsonToken.END_OBJECT) return null;

            Struct struct = new Struct(fields);
            registerStruct(struct);
            return struct;
        }

        private void registerStruct(Struct struct) {
            myStructMap.add(struct);
        }

        private Field parseField(JsonParser parser) throws IOException {
            if (parser.nextToken() != JsonToken.FIELD_NAME) return null;
            String name = parser.currentName();
            DataType type = parseDataType(parser);
            return new Field(name, type);
        }

        private DataType parseArray(JsonParser parser) throws IOException {
            if (parser.currentToken() != JsonToken.START_ARRAY) return null;

            LinkedHashSet<DataType> foundDataTypes = new LinkedHashSet<>();
            DataType firstType = parseDataType(parser);

            if (firstType instanceof DataType.Unknown && parser.currentToken() == JsonToken.END_ARRAY) {
                return new DataType.Array(DataType.Unknown.INSTANCE);
            }

            foundDataTypes.add(firstType);
            while (parser.currentToken() != null) {
                DataType dataType = parseDataType(parser);
                if (dataType instanceof DataType.Unknown && parser.currentToken() == JsonToken.END_ARRAY) {
                    break;
                }
                foundDataTypes.add(dataType);
            }

            return new DataType.Array(generateContainedType(foundDataTypes));
        }

        private DataType.StructRef extractStructType(DataType type) {
            if (type instanceof DataType.StructRef) return (DataType.StructRef) type;
            if (type instanceof DataType.Nullable && ((DataType.Nullable) type).getType() instanceof DataType.StructRef) {
                return (DataType.StructRef) ((DataType.Nullable) type).getType();
            }
            return null;
        }

        private Set<DataType> unifyArrayTypes(Set<DataType> types) {
            boolean allArrays = true;
            for (DataType t : types) {
                if (!(t instanceof DataType.Array)) {
                    allArrays = false;
                    break;
                }
            }
            if (!allArrays || types.size() < 2) return types;

            List<DataType.Array> arrayTypes = new ArrayList<>();
            for (DataType t : types) {
                arrayTypes.add((DataType.Array) t);
            }

            Set<DataType> innerTypes = new LinkedHashSet<>();
            for (DataType.Array a : arrayTypes) {
                DataType unwrapped = a.getType().unwrap();
                if (!(unwrapped instanceof DataType.Unknown)) {
                    innerTypes.add(unwrapped);
                }
            }

            if (innerTypes.size() == 1) {
                boolean anyNullable = false;
                for (DataType.Array a : arrayTypes) {
                    if (a.getType() instanceof DataType.Nullable) {
                        anyNullable = true;
                        break;
                    }
                }
                DataType inner = innerTypes.iterator().next();
                Set<DataType> resultInner;
                if (anyNullable) {
                    resultInner = new LinkedHashSet<>();
                    resultInner.add(new DataType.Nullable(inner));
                } else {
                    resultInner = innerTypes;
                }
                Set<DataType> result = new LinkedHashSet<>();
                for (DataType ri : resultInner) {
                    result.add(new DataType.Array(ri));
                }
                return result;
            }
            return types;
        }

        private DataType generateContainedType(Set<DataType> containedTypes) {
            Set<DataType> types = unifyArrayTypes(containedTypes);

            boolean containsNullable = false;
            for (DataType t : types) {
                if (t instanceof DataType.Nullable) {
                    containsNullable = true;
                    break;
                }
            }

            List<DataType> typesWithoutNull = new ArrayList<>();
            for (DataType t : types) {
                if (!(t instanceof DataType.Nullable && ((DataType.Nullable) t).getType() instanceof DataType.Unknown)) {
                    typesWithoutNull.add(t);
                }
            }

            List<DataType.StructRef> structTypes = new ArrayList<>();
            for (DataType t : types) {
                DataType.StructRef sr = extractStructType(t);
                if (sr != null) structTypes.add(sr);
            }

            if (types.size() == 1) {
                return types.iterator().next();
            }
            if (types.size() == 2 && containsNullable) {
                for (DataType t : types) {
                    if (!(t instanceof DataType.Nullable)) {
                        return new DataType.Nullable(t);
                    }
                }
            }
            if (structTypes.size() == typesWithoutNull.size()) {
                DataType type = unifyStructs(structTypes);
                if (containsNullable) {
                    return new DataType.Nullable(type);
                }
                return type;
            }
            if (containsNullable) {
                return new DataType.Nullable(DataType.Unknown.INSTANCE);
            }
            return DataType.Unknown.INSTANCE;
        }

        private DataType unifyStructs(List<DataType.StructRef> structTypes) {
            Map<String, List<DataType>> foundFields = new LinkedHashMap<>();
            for (DataType.StructRef st : structTypes) {
                for (Map.Entry<String, DataType> entry : st.getStruct().getFields().entrySet()) {
                    foundFields.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                }
            }

            for (List<DataType> fieldTypes : foundFields.values()) {
                if (fieldTypes.size() < structTypes.size()) {
                    fieldTypes.add(new DataType.Nullable(fieldTypes.get(0)));
                }
            }

            LinkedHashMap<String, DataType> fields = new LinkedHashMap<>();
            for (Map.Entry<String, List<DataType>> entry : foundFields.entrySet()) {
                fields.put(entry.getKey(), generateContainedType(new LinkedHashSet<>(entry.getValue())));
            }

            Struct struct = new Struct(fields);
            registerStruct(struct);
            return new DataType.StructRef(struct);
        }

        private DataType parseDataType(JsonParser parser) throws IOException {
            JsonToken token = parser.nextToken();
            if (token == null) return DataType.Unknown.INSTANCE;

            switch (token) {
                case START_OBJECT: {
                    Struct struct = parseStruct(parser);
                    if (struct == null) return DataType.Unknown.INSTANCE;
                    return new DataType.StructRef(struct);
                }
                case START_ARRAY: {
                    DataType result = parseArray(parser);
                    return result != null ? result : DataType.Unknown.INSTANCE;
                }
                case VALUE_NULL:
                    return new DataType.Nullable(DataType.Unknown.INSTANCE);
                case VALUE_FALSE:
                case VALUE_TRUE:
                    return DataType.BooleanType.INSTANCE;
                case VALUE_NUMBER_INT:
                    return DataType.IntegerType.INSTANCE;
                case VALUE_NUMBER_FLOAT:
                    return DataType.FloatType.INSTANCE;
                case VALUE_STRING:
                    return DataType.StringType.INSTANCE;
                default:
                    return DataType.Unknown.INSTANCE;
            }
        }
    }
}
