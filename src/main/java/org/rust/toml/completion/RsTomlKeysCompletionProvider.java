/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.toml.Util;
import org.toml.lang.psi.*;

import java.util.Collection;
import java.util.List;

class CargoTomlKeysCompletionProvider extends CompletionProvider<CompletionParameters> {
    private TomlSchema myCachedSchema;

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        if (myCachedSchema == null) {
            myCachedSchema = TomlSchema.parse(parameters.getPosition().getProject(), EXAMPLE_CARGO_TOML);
        }
        TomlSchema schema = myCachedSchema;

        PsiElement pos = parameters.getPosition();
        if (!(pos.getParent() instanceof TomlKeySegment)) return;
        TomlKeySegment key = (TomlKeySegment) pos.getParent();
        TomlKeyValueOwner table = getTopLevelTable(key);
        if (table == null) return;

        PsiElement parent = key.getParent() != null ? key.getParent().getParent() : null;
        Collection<String> variants;
        if (parent instanceof TomlTableHeader) {
            TomlTableHeader header = (TomlTableHeader) parent;
            if (header.getKey() == null || header.getKey().getSegments().isEmpty()) return;
            if (key != header.getKey().getSegments().get(0)) return;
            boolean isArray = table instanceof TomlArrayTable;
            if (!(table instanceof TomlArrayTable) && !(table instanceof TomlTable)) return;
            variants = schema.topLevelKeys(isArray);
        } else if (parent instanceof TomlKeyValue) {
            if (!(table instanceof TomlHeaderOwner)) return;
            TomlHeaderOwner headerOwner = (TomlHeaderOwner) table;
            if (Util.isDependencyListHeader(headerOwner.getHeader())) return;
            String tableName = getHeaderOwnerName(headerOwner);
            if (tableName == null) return;
            variants = schema.keysForTable(tableName);
        } else {
            return;
        }

        for (String variant : variants) {
            result.addElement(LookupElementBuilder.create(variant));
        }
    }

    @Nullable
    private static TomlKeyValueOwner getTopLevelTable(@NotNull TomlKeySegment key) {
        TomlKeyValueOwner table = PsiElementExt.ancestorStrict(key, TomlKeyValueOwner.class);
        if (table == null) return null;
        if (!(table.getParent() instanceof TomlFile)) return null;
        return table;
    }

    @Nullable
    private static String getHeaderOwnerName(@NotNull TomlHeaderOwner owner) {
        TomlKey key = owner.getHeader().getKey();
        if (key == null) return null;
        List<TomlKeySegment> segments = key.getSegments();
        if (segments.isEmpty()) return null;
        return segments.get(0).getName();
    }

    private static final String EXAMPLE_CARGO_TOML = "\n" +
        "[package]\n" +
        "name = \"hello_world\"\n" +
        "version = \"0.1.0\"\n" +
        "authors = [\"you@example.com\"]\n" +
        "build = \"build.rs\"\n" +
        "documentation = \"https://docs.rs/example\"\n" +
        "exclude = [\"build/**/*.o\", \"doc/**/*.html\"]\n" +
        "include = [\"src/**/*\", \"Cargo.toml\"]\n" +
        "publish = false\n" +
        "workspace = \"path/to/workspace/root\"\n" +
        "edition = \"2018\"\n" +
        "rust-version = \"1.56\"\n" +
        "\n" +
        "links = \"...\"\n" +
        "default-run = \"...\"\n" +
        "autobins = false\n" +
        "autoexamples = false\n" +
        "autotests = false\n" +
        "autobenches = false\n" +
        "resolver = \"...\"\n" +
        "\n" +
        "description = \"...\"\n" +
        "homepage = \"...\"\n" +
        "repository = \"...\"\n" +
        "readme = \"...\"\n" +
        "keywords = [\"...\", \"...\"]\n" +
        "categories = [\"...\", \"...\"]\n" +
        "license = \"...\"\n" +
        "license-file = \"...\"\n" +
        "\n" +
        "[badges]\n" +
        "appveyor = { repository = \"...\", branch = \"master\", service = \"github\" }\n" +
        "circle-ci = { repository = \"...\", branch = \"master\" }\n" +
        "gitlab = { repository = \"...\", branch = \"master\" }\n" +
        "travis-ci = { repository = \"...\", branch = \"master\" }\n" +
        "codecov = { repository = \"...\", branch = \"master\", service = \"github\" }\n" +
        "coveralls = { repository = \"...\", branch = \"master\", service = \"github\" }\n" +
        "is-it-maintained-issue-resolution = { repository = \"...\" }\n" +
        "is-it-maintained-open-issues = { repository = \"...\" }\n" +
        "maintenance = { status = \"...\" }\n" +
        "\n" +
        "[profile.release]\n" +
        "opt-level = 3\n" +
        "debug = false\n" +
        "split-debuginfo = \"...\"\n" +
        "strip = \"none\"\n" +
        "rpath = false\n" +
        "lto = false\n" +
        "debug-assertions = false\n" +
        "codegen-units = 1\n" +
        "panic = 'unwind'\n" +
        "incremental = true\n" +
        "overflow-checks = true\n" +
        "\n" +
        "[features]\n" +
        "default = [\"jquery\", \"uglifier\", \"session\"]\n" +
        "\n" +
        "[workspace]\n" +
        "members = [\"path/to/member1\", \"path/to/member2\", \"path/to/member3/*\"]\n" +
        "exclude = [\"path1\", \"path/to/dir2\"]\n" +
        "default-members = [\"path/to/member2\", \"path/to/member3/foo\"]\n" +
        "\n" +
        "[dependencies]\n" +
        "foo = { git = 'https://github.com/example/foo' }\n" +
        "\n" +
        "[dev-dependencies]\n" +
        "tempdir = \"0.3\"\n" +
        "\n" +
        "[build-dependencies]\n" +
        "gcc = \"0.3\"\n" +
        "\n" +
        "[lib]\n" +
        "name = \"foo\"\n" +
        "path = \"src/lib.rs\"\n" +
        "crate-type = [\"dylib\", \"staticlib\", \"cdylib\", \"rlib\"]\n" +
        "test = true\n" +
        "doctest = true\n" +
        "bench = true\n" +
        "doc = true\n" +
        "plugin = false\n" +
        "proc-macro = false\n" +
        "harness = true\n" +
        "edition = \"2018\"\n" +
        "\n" +
        "[[example]]\n" +
        "name = \"foo\"\n" +
        "path = \"src/lib.rs\"\n" +
        "test = true\n" +
        "doctest = true\n" +
        "bench = true\n" +
        "doc = true\n" +
        "plugin = false\n" +
        "harness = true\n" +
        "required-features = [\"postgres\", \"tools\"]\n" +
        "edition = \"2018\"\n" +
        "\n" +
        "[[bin]]\n" +
        "name = \"foo\"\n" +
        "path = \"src/lib.rs\"\n" +
        "test = true\n" +
        "doctest = true\n" +
        "bench = true\n" +
        "doc = true\n" +
        "plugin = false\n" +
        "harness = true\n" +
        "required-features = [\"postgres\", \"tools\"]\n" +
        "edition = \"2018\"\n" +
        "\n" +
        "[[test]]\n" +
        "name = \"foo\"\n" +
        "path = \"src/lib.rs\"\n" +
        "test = true\n" +
        "doctest = true\n" +
        "bench = true\n" +
        "doc = true\n" +
        "plugin = false\n" +
        "harness = true\n" +
        "required-features = [\"postgres\", \"tools\"]\n" +
        "edition = \"2018\"\n" +
        "\n" +
        "[[bench]]\n" +
        "name = \"foo\"\n" +
        "path = \"src/lib.rs\"\n" +
        "test = true\n" +
        "doctest = true\n" +
        "bench = true\n" +
        "doc = true\n" +
        "plugin = false\n" +
        "harness = true\n" +
        "required-features = [\"postgres\", \"tools\"]\n" +
        "edition = \"2018\"\n" +
        "\n" +
        "[patch.crates-io]\n" +
        "foo = { git = 'https://github.com/example/foo' }\n";
}
