/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.colorScheme.TextAttributesKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class PreparedAnnotation {
    @Nonnull
    private final Severity mySeverity;
    @Nullable
    private final RsErrorCode myErrorCode;
    
    @Nonnull
    private final String myHeader;
    @Nonnull
    private final String myDescription;
    @Nonnull
    private final List<QuickFixWithRange> myFixes;
    @Nullable
    private final TextAttributesKey myTextAttributes;

    public PreparedAnnotation(
        @Nonnull Severity severity,
        @Nullable RsErrorCode errorCode,
         @Nonnull String header
    ) {
        this(severity, errorCode, header, "", Collections.emptyList(), null);
    }

    public PreparedAnnotation(
        @Nonnull Severity severity,
        @Nullable RsErrorCode errorCode,
         @Nonnull String header,
        @Nonnull String description
    ) {
        this(severity, errorCode, header, description, Collections.emptyList(), null);
    }

    public PreparedAnnotation(
        @Nonnull Severity severity,
        @Nullable RsErrorCode errorCode,
         @Nonnull String header,
        @Nonnull String description,
        @Nonnull List<QuickFixWithRange> fixes,
        @Nullable TextAttributesKey textAttributes
    ) {
        mySeverity = severity;
        myErrorCode = errorCode;
        myHeader = header;
        myDescription = description;
        myFixes = fixes;
        myTextAttributes = textAttributes;
    }

    @Nonnull
    public Severity getSeverity() {
        return mySeverity;
    }

    @Nullable
    public RsErrorCode getErrorCode() {
        return myErrorCode;
    }

    
    @Nonnull
    public String getHeader() {
        return myHeader;
    }

    @Nonnull
    public String getDescription() {
        return myDescription;
    }

    @Nonnull
    public List<QuickFixWithRange> getFixes() {
        return myFixes;
    }

    @Nullable
    public TextAttributesKey getTextAttributes() {
        return myTextAttributes;
    }
}
