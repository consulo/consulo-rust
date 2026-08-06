package com.intellij.codeInsight.editorActions;
import java.awt.datatransfer.DataFlavor;
public interface TextBlockTransferableData {
    DataFlavor getFlavor();
    int getOffsetCount();
    int getOffsets(int[] offsets, int index);
    int setOffsets(int[] offsets, int index);
}
