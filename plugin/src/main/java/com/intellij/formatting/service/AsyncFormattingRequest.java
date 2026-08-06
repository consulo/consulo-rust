package com.intellij.formatting.service;
import consulo.document.Document;
import consulo.language.codeStyle.FormattingContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import java.util.List;

/** IntelliJ-compat stub. */
public interface AsyncFormattingRequest {
    Project getProject();
    Document getDocument();
    String getDocumentText();
    FormattingContext getContext();
    default String getCommandName() { return "Format"; }
    default String getReformatBeforeCommitCommandName() { return "Format"; }
    default VirtualFile getVirtualFile() { return null; }
    default List<com.intellij.formatting.service.FormattingRangesInfo> getFormattingRanges() { return List.of(); }
    default void onTextReady(String formattedText) {}
    default void onError(String title, String message) {}
}
