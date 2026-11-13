/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.localize.LocalizeValue;

/**
 * The bundled Rust live templates: loops and iteration, printing, tests and common items.
 */
@ExtensionImpl
public class RsLiveTemplateContributor implements LiveTemplateContributor {
    @Override
    public String groupId() {
        return "rust";
    }

    @Override
    public LocalizeValue groupName() {
        return LocalizeValue.localizeTODO("Rust");
    }

    @Override
    public void contribute(Factory factory) {
        // iterations
        try (Builder builder = factory.newBuilder("rustLoop", "loop", "loop {\n    $END$\n}", LocalizeValue.localizeTODO("infinite loop"))) {
            builder.withReformat();
            builder.withContext(RsExpressionContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustWhile", "while", "while $CONDITION$ {\n    $END$\n}", LocalizeValue.localizeTODO("while loop"))) {
            builder.withReformat();
            builder.withVariable("CONDITION", "", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustWhilel", "whilel", "while let $PATTERN$ = $EXPRESSION$ {\n    $END$\n}", LocalizeValue.localizeTODO("while let ... loop"))) {
            builder.withReformat();
            builder.withVariable("EXPRESSION", "", "", true);
            builder.withVariable("PATTERN", "", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustWhilels", "whilels", "while let Some($VAR$) = $ITERABLE$ {\n    $END$\n}", LocalizeValue.localizeTODO("while let Some(...) loop"))) {
            builder.withReformat();
            builder.withVariable("ITERABLE", "", "", true);
            builder.withVariable("VAR", "rustCollectionElementName(ITERABLE)", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustIter", "iter", "for $VAR$ in $ITERABLE$ {\n    $END$\n}", LocalizeValue.localizeTODO("iterate (for ... in ...)"))) {
            builder.withReformat();
            builder.withVariable("ITERABLE", "", "", true);
            builder.withVariable("VAR", "rustCollectionElementName(ITERABLE)", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustFor", "for", "for $VAR$ in $ITERABLE$ {\n    $END$\n}", LocalizeValue.localizeTODO("iterate (for ... in ...)"))) {
            builder.withReformat();
            builder.withVariable("ITERABLE", "", "", true);
            builder.withVariable("VAR", "rustCollectionElementName(ITERABLE)", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustFori", "fori", "for $INDEX$ in $START$..$STOP$ {\n    $END$\n}", LocalizeValue.localizeTODO("iterate over range (for ... in range)"))) {
            builder.withReformat();
            builder.withVariable("START", "", "\"0\"", true);
            builder.withVariable("STOP", "", "", true);
            builder.withVariable("INDEX", "rustSuggestIndexName()", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }

        // output
        try (Builder builder = factory.newBuilder("rustP", "p", "println!(\"$END$\");", LocalizeValue.localizeTODO("println!"))) {
            builder.withReformat();
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustPr", "pr", "print!(\"$END$\");", LocalizeValue.localizeTODO("print!"))) {
            builder.withReformat();
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustPd", "pd", "eprintln!(\"$EXPRESSION_COPY$ = {:?}\", $EXPRESSION$);", LocalizeValue.localizeTODO("debug eprintln! value"))) {
            builder.withReformat();
            builder.withVariable("EXPRESSION", "", "", true);
            builder.withVariable("EXPRESSION_COPY", "EXPRESSION", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustPpd", "ppd", "eprintln!(\"$EXPRESSION_COPY$ = {:#?}\", $EXPRESSION$);", LocalizeValue.localizeTODO("debug pretty eprintln! value"))) {
            builder.withReformat();
            builder.withVariable("EXPRESSION", "", "", true);
            builder.withVariable("EXPRESSION_COPY", "EXPRESSION", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustFmt", "fmt", "format!(\"$END$\");", LocalizeValue.localizeTODO("format!"))) {
            builder.withReformat();
            builder.withContext(RsExpressionContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustWrt", "wrt", "write!($BUFFER$, \"$END$\");", LocalizeValue.localizeTODO("write! to buffer"))) {
            builder.withReformat();
            builder.withVariable("BUFFER", "", "\"buffer\"", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustWrtn", "wrtn", "writeln!($BUFFER$, \"$END$\");", LocalizeValue.localizeTODO("writeln! to buffer"))) {
            builder.withReformat();
            builder.withVariable("BUFFER", "", "\"buffer\"", true);
            builder.withContext(RsStatementContextType.class, true);
        }

        // test
        try (Builder builder = factory.newBuilder("rustA", "a", "assert!($END$);", LocalizeValue.localizeTODO("assert"))) {
            builder.withReformat();
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustAe", "ae", "assert_eq!($LEFT$, $RIGHT$);", LocalizeValue.localizeTODO("assert equals"))) {
            builder.withReformat();
            builder.withVariable("LEFT", "", "", true);
            builder.withVariable("RIGHT", "", "", true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustTfn", "tfn", "#[test]\nfn $NAME$() {\n    $END$\n}", LocalizeValue.localizeTODO("test function"))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(RsExpressionContextType.class, false);
            builder.withContext(RsItemContextType.class, false);
            builder.withContext(RsModContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustBfn", "bfn", "#[bench]\nfn $NAME$(b: &mut test::Bencher) {\n    b.iter(||);$END$\n}", LocalizeValue.localizeTODO("bench function"))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(RsExpressionContextType.class, false);
            builder.withContext(RsItemContextType.class, false);
            builder.withContext(RsModContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustTmod", "tmod", "#[cfg(test)]\nmod tests {\n    use super::*;\n    \n    $END$\n}", LocalizeValue.localizeTODO("test module"))) {
            builder.withContext(RsExpressionContextType.class, false);
            builder.withContext(RsItemContextType.class, false);
            builder.withContext(RsModContextType.class, true);
        }

        // other
        try (Builder builder = factory.newBuilder("rustF", "f", "$IDENT$: $TYPE$,", LocalizeValue.localizeTODO("field"))) {
            builder.withReformat();
            builder.withVariable("IDENT", "", "\"foo\"", true);
            builder.withVariable("TYPE", "", "\"u32\"", true);
            builder.withContext(RsStructContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustPf", "pf", "pub $IDENT$: $TYPE$,", LocalizeValue.localizeTODO("pub field"))) {
            builder.withReformat();
            builder.withVariable("IDENT", "", "\"foo\"", true);
            builder.withVariable("TYPE", "", "\"u32\"", true);
            builder.withContext(RsStructContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustMain", "main", "fn main() {\n    $END$\n}", LocalizeValue.localizeTODO("main() function declaration"))) {
            builder.withReformat();
            builder.withContext(RsModContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustMacro", "macro", "macro_rules! $NAME$ {\n    ($MATCHER$) => {$TRANSCRIBER$};$END$\n}", LocalizeValue.localizeTODO("macro_rules! declaration"))) {
            builder.withReformat();
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("MATCHER", "", "", true);
            builder.withVariable("TRANSCRIBER", "", "", true);
            builder.withContext(RsModContextType.class, true);
            builder.withContext(RsStatementContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustIfl", "ifl", "if let $PATTERN$ = $EXPRESSION$ {\n    $END$\n}", LocalizeValue.localizeTODO("if let ... statement"))) {
            builder.withReformat();
            builder.withVariable("EXPRESSION", "", "", true);
            builder.withVariable("PATTERN", "", "", true);
            builder.withContext(RsExpressionContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustIfls", "ifls", "if let Some($VAR$) = $EXPRESSION$ {\n    $END$\n}", LocalizeValue.localizeTODO("if let Some(...) statement"))) {
            builder.withReformat();
            builder.withVariable("EXPRESSION", "", "", true);
            builder.withVariable("VAR", "", "", true);
            builder.withContext(RsExpressionContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustClosure", "closure", "|$PARAM$| $PARAM_COPY$", LocalizeValue.localizeTODO("closure"))) {
            builder.withReformat();
            builder.withVariable("PARAM", "", "\"x\"", true);
            builder.withVariable("PARAM_COPY", "PARAM", "", true);
            builder.withContext(RsExpressionContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustImpl", "impl", "impl $NAME$ {\n    $END$\n}", LocalizeValue.localizeTODO("impl Foo {} declaration"))) {
            builder.withReformat();
            builder.withVariable("NAME", "", "", true);
            builder.withContext(RsModContextType.class, true);
        }
        try (Builder builder = factory.newBuilder("rustImplt", "implt", "impl $NAME$ for $TRAIT_NAME$ {\n    $END$\n}", LocalizeValue.localizeTODO("impl Foo for Bar {} declaration"))) {
            builder.withReformat();
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("TRAIT_NAME", "", "", true);
            builder.withContext(RsModContextType.class, true);
        }
    }
}
