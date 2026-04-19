/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class RustcMessage {

    // ---- CargoTopMessage ----

    public static final class CargoTopMessage {
        private static final Logger LOG = Logger.getInstance(CargoTopMessage.class);

        private final RustcDiagnostic message;
        private final String package_id;
        private final String reason;
        private final Target target;

        public CargoTopMessage(RustcDiagnostic message, String package_id, String reason, Target target) {
            this.message = message;
            this.package_id = package_id;
            this.reason = reason;
            this.target = target;
        }

        public RustcDiagnostic getMessage() { return message; }
        public String getPackageId() { return package_id; }
        public String getReason() { return reason; }
        public Target getTarget() { return target; }

        @Nullable
        public static CargoTopMessage fromJson(JsonObject json) {
            JsonPrimitive reasonPrimitive = json.getAsJsonPrimitive("reason");
            if (reasonPrimitive == null || !"compiler-message".equals(reasonPrimitive.getAsString())) {
                return null;
            }
            CargoTopMessage msg;
            try {
                msg = new Gson().fromJson(json, CargoTopMessage.class);
            } catch (JsonSyntaxException e) {
                LOG.warn(e);
                msg = null;
            }
            if (msg == null) {
                throw new IllegalStateException("Failed to parse CargoTopMessage from " + json);
            }
            return msg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CargoTopMessage)) return false;
            CargoTopMessage that = (CargoTopMessage) o;
            return Objects.equals(message, that.message) &&
                Objects.equals(package_id, that.package_id) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, package_id, reason, target);
        }
    }


    public static final class RustcDiagnostic {
        private final List<RustcDiagnostic> children;
        @Nullable
        private final ErrorCode code;
        @NlsSafe
        private final String level;
        @NlsSafe
        private final String message;
        @Nullable
        @NlsSafe
        private final String rendered;
        private final List<RustcSpan> spans;

        public RustcDiagnostic(List<RustcDiagnostic> children, @Nullable ErrorCode code, String level, String message, @Nullable String rendered, List<RustcSpan> spans) {
            this.children = children;
            this.code = code;
            this.level = level;
            this.message = message;
            this.rendered = rendered;
            this.spans = spans;
        }

        public List<RustcDiagnostic> getChildren() { return children; }
        @Nullable public ErrorCode getCode() { return code; }
        public String getLevel() { return level; }
        public String getMessage() { return message; }
        @Nullable public String getRendered() { return rendered; }
        public List<RustcSpan> getSpans() { return spans; }

        @Nullable
        public RustcSpan getMainSpan() {
            RustcSpan validSpan = null;
            for (RustcSpan span : spans) {
                if (span.isValid() && span.isIs_primary()) {
                    validSpan = span;
                    break;
                }
            }
            if (validSpan == null) return null;

            RustcSpan current = validSpan;
            while (current.getExpansion() != null && current.getExpansion().getSpan() != null) {
                current = current.getExpansion().getSpan();
            }

            if (current.isValid() && !current.getFile_name().startsWith("<")) {
                return current;
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RustcDiagnostic)) return false;
            RustcDiagnostic that = (RustcDiagnostic) o;
            return Objects.equals(children, that.children) &&
                Objects.equals(code, that.code) &&
                Objects.equals(level, that.level) &&
                Objects.equals(message, that.message) &&
                Objects.equals(rendered, that.rendered) &&
                Objects.equals(spans, that.spans);
        }

        @Override
        public int hashCode() {
            return Objects.hash(children, code, level, message, rendered, spans);
        }
    }

    // ---- ErrorCode ----

    public static final class ErrorCode {
        private final String code;
        @Nullable
        private final String explanation;

        public ErrorCode(String code, @Nullable String explanation) {
            this.code = code;
            this.explanation = explanation;
        }

        public String getCode() { return code; }
        @Nullable public String getExplanation() { return explanation; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ErrorCode)) return false;
            ErrorCode that = (ErrorCode) o;
            return Objects.equals(code, that.code) && Objects.equals(explanation, that.explanation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, explanation);
        }
    }

    // ---- RustcSpan ----

    public static final class RustcSpan {
        private final String file_name;
        private final int byte_start;
        private final int byte_end;
        private final int line_start;
        private final int line_end;
        private final int column_start;
        private final int column_end;
        private final boolean is_primary;
        private final List<RustcText> text;
        @Nullable private final String label;
        @Nullable private final String suggested_replacement;
        @Nullable private final Applicability suggestion_applicability;
        @Nullable private final Expansion expansion;

        public RustcSpan(
            String file_name, int byte_start, int byte_end,
            int line_start, int line_end, int column_start, int column_end,
            boolean is_primary, List<RustcText> text, @Nullable String label,
            @Nullable String suggested_replacement, @Nullable Applicability suggestion_applicability,
            @Nullable Expansion expansion
        ) {
            this.file_name = file_name;
            this.byte_start = byte_start;
            this.byte_end = byte_end;
            this.line_start = line_start;
            this.line_end = line_end;
            this.column_start = column_start;
            this.column_end = column_end;
            this.is_primary = is_primary;
            this.text = text;
            this.label = label;
            this.suggested_replacement = suggested_replacement;
            this.suggestion_applicability = suggestion_applicability;
            this.expansion = expansion;
        }

        public String getFile_name() { return file_name; }
        public int getByte_start() { return byte_start; }
        public int getByte_end() { return byte_end; }
        public int getLine_start() { return line_start; }
        public int getLine_end() { return line_end; }
        public int getColumn_start() { return column_start; }
        public int getColumn_end() { return column_end; }
        public boolean isIs_primary() { return is_primary; }
        public List<RustcText> getText() { return text; }
        @Nullable public String getLabel() { return label; }
        @Nullable public String getSuggested_replacement() { return suggested_replacement; }
        @Nullable public Applicability getSuggestion_applicability() { return suggestion_applicability; }
        @Nullable public Expansion getExpansion() { return expansion; }

        public boolean isValid() {
            return line_end > line_start || (line_end == line_start && column_end >= column_start);
        }

        @Nullable
        public TextRange toTextRange(Document document) {
            Integer startOffset = toOffset(document, line_start, column_start);
            Integer endOffset = toOffset(document, line_end, column_end);
            if (startOffset != null && endOffset != null && startOffset < endOffset) {
                return new TextRange(startOffset, endOffset);
            }
            return null;
        }

        @Nullable
        public static Integer toOffset(Document document, int line, int column) {
            int adjustedLine = line - 1;
            int adjustedColumn = column - 1;
            if (adjustedLine < 0 || adjustedLine >= document.getLineCount()) return null;
            int offset = document.getLineStartOffset(adjustedLine) + adjustedColumn;
            return offset <= document.getTextLength() ? offset : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RustcSpan)) return false;
            RustcSpan that = (RustcSpan) o;
            return byte_start == that.byte_start && byte_end == that.byte_end &&
                line_start == that.line_start && line_end == that.line_end &&
                column_start == that.column_start && column_end == that.column_end &&
                is_primary == that.is_primary &&
                Objects.equals(file_name, that.file_name) &&
                Objects.equals(text, that.text) &&
                Objects.equals(label, that.label) &&
                Objects.equals(suggested_replacement, that.suggested_replacement) &&
                suggestion_applicability == that.suggestion_applicability &&
                Objects.equals(expansion, that.expansion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file_name, byte_start, byte_end, line_start, line_end,
                column_start, column_end, is_primary, text, label,
                suggested_replacement, suggestion_applicability, expansion);
        }
    }

    // ---- Expansion ----

    public static final class Expansion {
        @Nullable
        private final RustcSpan def_site_span;
        private final String macro_decl_name;
        private final RustcSpan span;

        public Expansion(@Nullable RustcSpan def_site_span, String macro_decl_name, RustcSpan span) {
            this.def_site_span = def_site_span;
            this.macro_decl_name = macro_decl_name;
            this.span = span;
        }

        @Nullable public RustcSpan getDef_site_span() { return def_site_span; }
        public String getMacro_decl_name() { return macro_decl_name; }
        public RustcSpan getSpan() { return span; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Expansion)) return false;
            Expansion that = (Expansion) o;
            return Objects.equals(def_site_span, that.def_site_span) &&
                Objects.equals(macro_decl_name, that.macro_decl_name) &&
                Objects.equals(span, that.span);
        }

        @Override
        public int hashCode() {
            return Objects.hash(def_site_span, macro_decl_name, span);
        }
    }

    // ---- RustcText ----

    public static final class RustcText {
        private final int highlight_end;
        private final int highlight_start;
        @Nullable
        private final String text;

        public RustcText(int highlight_end, int highlight_start, @Nullable String text) {
            this.highlight_end = highlight_end;
            this.highlight_start = highlight_start;
            this.text = text;
        }

        public int getHighlight_end() { return highlight_end; }
        public int getHighlight_start() { return highlight_start; }
        @Nullable public String getText() { return text; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RustcText)) return false;
            RustcText that = (RustcText) o;
            return highlight_end == that.highlight_end &&
                highlight_start == that.highlight_start &&
                Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(highlight_end, highlight_start, text);
        }
    }

    // ---- Target ----

    public static final class Target {
        private final List<String> crate_types;
        private final List<String> kind;
        private final String name;
        private final String src_path;

        public Target(List<String> crate_types, List<String> kind, String name, String src_path) {
            this.crate_types = crate_types;
            this.kind = kind;
            this.name = name;
            this.src_path = src_path;
        }

        public List<String> getCrate_types() { return crate_types; }
        public List<String> getKind() { return kind; }
        public String getName() { return name; }
        public String getSrc_path() { return src_path; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Target)) return false;
            Target that = (Target) o;
            return Objects.equals(crate_types, that.crate_types) &&
                Objects.equals(kind, that.kind) &&
                Objects.equals(name, that.name) &&
                Objects.equals(src_path, that.src_path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(crate_types, kind, name, src_path);
        }
    }

    // ---- Applicability ----

    @SuppressWarnings("unused")
    public enum Applicability {
        @SerializedName("MachineApplicable")
        MACHINE_APPLICABLE,

        @SerializedName("MaybeIncorrect")
        MAYBE_INCORRECT,

        @SerializedName("HasPlaceholders")
        HAS_PLACEHOLDERS,

        @SerializedName("Unspecified")
        UNSPECIFIED
    }

    // ---- CompilerMessage (sealed hierarchy) ----

    public static abstract class CompilerMessage {
        public abstract String getPackageId();
        public abstract CompilerMessage convertPaths(Function<String, String> converter);

        @Nullable
        public static CompilerMessage fromJson(JsonObject json) {
            JsonPrimitive reasonPrimitive = json.getAsJsonPrimitive("reason");
            if (reasonPrimitive == null) return null;
            String reason = reasonPrimitive.getAsString();
            Class<? extends CompilerMessage> cls;
            if (BuildScriptMessage.REASON.equals(reason)) {
                cls = BuildScriptMessage.class;
            } else if (CompilerArtifactMessage.REASON.equals(reason)) {
                cls = CompilerArtifactMessage.class;
            } else {
                return null;
            }
            return new Gson().fromJson(json, cls);
        }
    }

    // ---- BuildScriptMessage ----

    public static final class BuildScriptMessage extends CompilerMessage {
        public static final String REASON = "build-script-executed";

        private final String package_id;
        private final List<String> cfgs;
        private final List<List<String>> env;
        @Nullable
        private final String out_dir;

        public BuildScriptMessage(String package_id, List<String> cfgs, List<List<String>> env, @Nullable String out_dir) {
            this.package_id = package_id;
            this.cfgs = cfgs;
            this.env = env;
            this.out_dir = out_dir;
        }

        @Override
        public String getPackageId() { return package_id; }
        public List<String> getCfgs() { return cfgs; }
        public List<List<String>> getEnv() { return env; }
        @Nullable public String getOutDir() { return out_dir; }

        public BuildScriptMessage withOutDir(@Nullable String outDir) {
            return new BuildScriptMessage(package_id, cfgs, env, outDir);
        }

        @Override
        public CompilerMessage convertPaths(Function<String, String> converter) {
            return new BuildScriptMessage(
                package_id,
                cfgs,
                env,
                out_dir != null ? converter.apply(out_dir) : null
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BuildScriptMessage)) return false;
            BuildScriptMessage that = (BuildScriptMessage) o;
            return Objects.equals(package_id, that.package_id) &&
                Objects.equals(cfgs, that.cfgs) &&
                Objects.equals(env, that.env) &&
                Objects.equals(out_dir, that.out_dir);
        }

        @Override
        public int hashCode() {
            return Objects.hash(package_id, cfgs, env, out_dir);
        }
    }

    // ---- CompilerArtifactMessage ----

    public static final class CompilerArtifactMessage extends CompilerMessage {
        public static final String REASON = "compiler-artifact";

        private final String package_id;
        private final CargoMetadata.Target target;
        private final Profile profile;
        private final List<String> filenames;
        @Nullable
        private final String executable;

        public CompilerArtifactMessage(String package_id, CargoMetadata.Target target, Profile profile, List<String> filenames, @Nullable String executable) {
            this.package_id = package_id;
            this.target = target;
            this.profile = profile;
            this.filenames = filenames;
            this.executable = executable;
        }

        @Override
        public String getPackageId() { return package_id; }
        public CargoMetadata.Target getTarget() { return target; }
        public Profile getProfile() { return profile; }
        public List<String> getFilenames() { return filenames; }
        @Nullable public String getExecutable() { return executable; }

        public List<String> getExecutables() {
            if (executable != null) {
                return List.of(executable);
            } else {
                // .dSYM and .pdb files are binaries, but they should not be used when starting debug session.
                return filenames.stream()
                    .filter(f -> !f.endsWith(".dSYM") && !f.endsWith(".pdb"))
                    .toList();
            }
        }

        @Override
        public CompilerMessage convertPaths(Function<String, String> converter) {
            return new CompilerArtifactMessage(
                package_id,
                target.convertPaths(converter),
                profile,
                filenames.stream().map(converter).toList(),
                executable != null ? converter.apply(executable) : null
            );
        }

        @Nullable
        public static CompilerArtifactMessage fromJson(JsonObject json) {
            if (!REASON.equals(json.getAsJsonPrimitive("reason").getAsString())) {
                return null;
            }
            return new Gson().fromJson(json, CompilerArtifactMessage.class);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CompilerArtifactMessage)) return false;
            CompilerArtifactMessage that = (CompilerArtifactMessage) o;
            return Objects.equals(package_id, that.package_id) &&
                Objects.equals(target, that.target) &&
                Objects.equals(profile, that.profile) &&
                Objects.equals(filenames, that.filenames) &&
                Objects.equals(executable, that.executable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(package_id, target, profile, filenames, executable);
        }
    }

    // ---- Profile ----

    public static final class Profile {
        private final boolean test;

        public Profile(boolean test) {
            this.test = test;
        }

        public boolean isTest() { return test; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Profile)) return false;
            return test == ((Profile) o).test;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(test);
        }
    }

    private RustcMessage() {
        // Utility class, not instantiable
    }
}
