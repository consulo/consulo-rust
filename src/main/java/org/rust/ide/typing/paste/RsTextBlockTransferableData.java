/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;

import java.awt.datatransfer.DataFlavor;

public class RsTextBlockTransferableData implements TextBlockTransferableData {
    private final ImportMap myImportMap;

    public RsTextBlockTransferableData(ImportMap importMap) {
        myImportMap = importMap;
    }

    public ImportMap getImportMap() {
        return myImportMap;
    }

    @Override
    public DataFlavor getFlavor() {
        return RsImportCopyPasteProcessor.dataFlavor;
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
