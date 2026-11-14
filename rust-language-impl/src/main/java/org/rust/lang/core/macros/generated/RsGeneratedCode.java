/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.generated;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import consulo.util.dataholder.Key;

/**
 * Rust code a {@link RsGeneratedIncludeProvider} generates, with the source element each generated name stands for
 * and the source files it was generated from.
 */
public final class RsGeneratedCode {
    /** On the file the code is parsed into: where its generated names navigate to. */
    public static final Key<RsGeneratedCode> KEY = Key.create("RS_GENERATED_CODE");

    @Nonnull
    private final String text;
    /** Start offset of a generated name in {@link #text} to the source element it navigates to. */
    @Nonnull
    private final TreeMap<Integer, SmartPsiElementPointer<PsiElement>> targets;
    @Nonnull
    private final List<PsiFile> sources;

    private RsGeneratedCode(@Nonnull String text,
                            @Nonnull TreeMap<Integer, SmartPsiElementPointer<PsiElement>> targets,
                            @Nonnull List<PsiFile> sources) {
        this.text = text;
        this.targets = targets;
        this.sources = sources;
    }

    @Nonnull
    public String getText() {
        return text;
    }

    /** The source files the code was generated from: it is out of date once any of them changes. */
    @Nonnull
    public List<PsiFile> getSources() {
        return sources;
    }

    /** The source element of the generated name that starts at {@code offset}, if it is still valid. */
    @Nullable
    public PsiElement findTarget(int offset) {
        SmartPsiElementPointer<PsiElement> pointer = targets.get(offset);
        PsiElement element = pointer != null ? pointer.getElement() : null;
        return element != null && element.isValid() ? element : null;
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final StringBuilder text = new StringBuilder();
        private final TreeMap<Integer, SmartPsiElementPointer<PsiElement>> targets = new TreeMap<>();
        private final Set<PsiFile> sources = new LinkedHashSet<>();

        private Builder() {
        }

        @Nonnull
        public Builder append(@Nonnull CharSequence code) {
            text.append(code);
            return this;
        }

        /** Appends a generated name that navigates to {@code target}. */
        @Nonnull
        public Builder name(@Nonnull String name, @Nullable PsiElement target) {
            if (target != null) {
                targets.put(text.length(), SmartPointerManager.getInstance(target.getProject()).createSmartPsiElementPointer(target));
            }
            text.append(name);
            return this;
        }

        @Nonnull
        public Builder source(@Nonnull PsiFile file) {
            sources.add(file);
            return this;
        }

        @Nonnull
        public RsGeneratedCode build() {
            return new RsGeneratedCode(text.toString(), targets, Collections.unmodifiableList(new ArrayList<>(sources)));
        }
    }
}
