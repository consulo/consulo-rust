/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;

/**
 * Represents a statically-describable scope that can be used to bound the lifetime/region for values.
 */
public abstract class Scope {
    /** Returns an element associated with this scope. */
    @NotNull
    public abstract RsElement getElement();

    public static class Node extends Scope {
        private final RsElement myElement;

        public Node(@NotNull RsElement element) {
            myElement = element;
        }

        @NotNull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(myElement, node.myElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement);
        }

        @Override
        public String toString() {
            return "Node(" + myElement + ")";
        }
    }

    /** Scope of the call-site for a function or closure (outlives the arguments as well as the body). */
    public static class CallSite extends Scope {
        private final RsElement myElement;

        public CallSite(@NotNull RsElement element) {
            myElement = element;
        }

        @NotNull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CallSite that = (CallSite) o;
            return Objects.equals(myElement, that.myElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement);
        }
    }

    /** Scope of arguments passed to a function or closure (they outlive its body). */
    public static class Arguments extends Scope {
        private final RsElement myElement;

        public Arguments(@NotNull RsElement element) {
            myElement = element;
        }

        @NotNull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Arguments that = (Arguments) o;
            return Objects.equals(myElement, that.myElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement);
        }
    }

    /** Scope of destructors for temporaries of node-id. */
    public static class Destruction extends Scope {
        private final RsElement myElement;

        public Destruction(@NotNull RsElement element) {
            myElement = element;
        }

        @NotNull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Destruction that = (Destruction) o;
            return Objects.equals(myElement, that.myElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement);
        }
    }

    /**
     * Scope of the condition and then block of an if expression.
     * Used for variables introduced in an if-let expression.
     */
    public static class IfThen extends Scope {
        private final RsBlock myElement;

        public IfThen(@NotNull RsBlock element) {
            myElement = element;
        }

        @NotNull
        @Override
        public RsBlock getElement() {
            return myElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IfThen that = (IfThen) o;
            return Objects.equals(myElement, that.myElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement);
        }
    }

    /** Scope following a `let id = expr;` binding in a block. */
    public static class Remainder extends Scope {
        private final RsBlock myElement;
        private final RsLetDecl myLetDecl;

        public Remainder(@NotNull RsBlock element, @NotNull RsLetDecl letDecl) {
            myElement = element;
            myLetDecl = letDecl;
        }

        @NotNull
        @Override
        public RsBlock getElement() {
            return myElement;
        }

        @NotNull
        public RsLetDecl getLetDecl() {
            return myLetDecl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Remainder that = (Remainder) o;
            return Objects.equals(myElement, that.myElement) && Objects.equals(myLetDecl, that.myLetDecl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElement, myLetDecl);
        }
    }

    /** Returns the span of this Scope. */
    @NotNull
    public TextRange getSpan() {
        TextRange span = getElement().getTextRange();
        if (this instanceof Remainder) {
            Remainder remainder = (Remainder) this;
            TextRange letSpan = remainder.getLetDecl().getTextRange();
            if (span.getStartOffset() <= letSpan.getStartOffset() && letSpan.getEndOffset() <= span.getEndOffset()) {
                return new TextRange(letSpan.getEndOffset(), span.getEndOffset());
            }
        }
        return span;
    }
}
