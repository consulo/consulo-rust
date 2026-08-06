/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import consulo.language.lexer.Lexer;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.language.psi.stub.todo.LexerBasedTodoIndexer;
import org.rust.lang.core.parser.RustParserDefinition;

public class RsTodoIndexer extends LexerBasedTodoIndexer {

    private static final int VERSION = 2;

    @Override
    public int getVersion() {
        return RustParserDefinition.LEXER_VERSION + VERSION;
    }

    @Override
    public Lexer createLexer(OccurrenceConsumer consumer) {
        return new RsFilterLexer(consumer);
    }

    @Override
    public consulo.virtualFileSystem.fileType.FileType getFileType() {
        return org.rust.lang.RsFileType.INSTANCE;
    }
}
