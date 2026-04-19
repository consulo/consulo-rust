/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PreparedAnnotation {
    @NotNull
    private final Severity mySeverity;
    @Nullable
    private final RsErrorCode myErrorCode;
    @InspectionMessage
    @NotNull
    private final String myHeader;
    @NotNull
    private final String myDescription;
    @NotNull
    private final List<QuickFixWithRange> myFixes;
    @Nullable
    private final TextAttributesKey myTextAttributes;

    public PreparedAnnotation(
        @NotNull Severity severity,
        @Nullable RsErrorCode errorCode,
        @InspectionMessage @NotNull String header
    ) {
        this(severity, errorCode, header, "", Collections.emptyList(), null);
    }

    public PreparedAnnotation(
        @NotNull Severity severity,
        @Nullable RsErrorCode errorCode,
        @InspectionMessage @NotNull String header,
        @NotNull String description
    ) {
        this(severity, errorCode, header, description, Collections.emptyList(), null);
    }

    public PreparedAnnotation(
        @NotNull Severity severity,
        @Nullable RsErrorCode errorCode,
        @InspectionMessage @NotNull String header,
        @NotNull String description,
        @NotNull List<QuickFixWithRange> fixes,
        @Nullable TextAttributesKey textAttributes
    ) {
        mySeverity = severity;
        myErrorCode = errorCode;
        myHeader = header;
        myDescription = description;
        myFixes = fixes;
        myTextAttributes = textAttributes;
    }

    @NotNull
    public Severity getSeverity() {
        return mySeverity;
    }

    @Nullable
    public RsErrorCode getErrorCode() {
        return myErrorCode;
    }

    @InspectionMessage
    @NotNull
    public String getHeader() {
        return myHeader;
    }

    @NotNull
    public String getDescription() {
        return myDescription;
    }

    @NotNull
    public List<QuickFixWithRange> getFixes() {
        return myFixes;
    }

    @Nullable
    public TextAttributesKey getTextAttributes() {
        return myTextAttributes;
    }
}
