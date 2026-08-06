/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;

public abstract class RsPsiTreeChangeEvent {
    /**
     * Event can relate to changes in a file system, e.g. file creation/deletion/movement.
     * In this case the property is set to null.
     */
    @Nullable
    public PsiFile getFile() {
        return null;
    }

    @Nonnull
    private static String safeText(@Nonnull PsiElement element) {
        try {
            return element.getText();
        } catch (Exception ignored) {
            return "<exception>";
        }
    }

    // ChildAddition

    public static abstract class ChildAddition extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @Nonnull
        private final PsiElement myParent;

        protected ChildAddition(@Nullable PsiFile file, @Nonnull PsiElement parent) {
            myFile = file;
            myParent = parent;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Nonnull
        public PsiElement getParent() {
            return myParent;
        }

        @Nullable
        public abstract PsiElement getChild();

        @Override
        public String toString() {
            PsiElement child = getChild();
            return "ChildAddition." + getClass().getSimpleName() + "(file=" + myFile +
                ", parent=`" + myParent.getText() + "`, child=`" + (child != null ? child.getText() : null) + "`)";
        }

        public static class Before extends ChildAddition {
            @Nullable
            private final PsiElement myChild;

            public Before(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nullable PsiElement child) {
                super(file, parent);
                myChild = child;
            }

            @Nullable
            @Override
            public PsiElement getChild() {
                return myChild;
            }
        }

        public static class After extends ChildAddition {
            @Nonnull
            private final PsiElement myChild;

            public After(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nonnull PsiElement child) {
                super(file, parent);
                myChild = child;
            }

            @Nonnull
            @Override
            public PsiElement getChild() {
                return myChild;
            }
        }
    }

    // ChildRemoval

    public static abstract class ChildRemoval extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @Nonnull
        private final PsiElement myParent;
        /** Invalid in {@link ChildRemoval.After} */
        @Nonnull
        private final PsiElement myChild;

        protected ChildRemoval(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nonnull PsiElement child) {
            myFile = file;
            myParent = parent;
            myChild = child;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Nonnull
        public PsiElement getParent() {
            return myParent;
        }

        @Nonnull
        public PsiElement getChild() {
            return myChild;
        }

        @Override
        public String toString() {
            return "ChildRemoval." + getClass().getSimpleName() + "(file=" + myFile +
                ", parent=`" + myParent.getText() + "`, child=`" + safeText(myChild) + "`)";
        }

        public static class Before extends ChildRemoval {
            public Before(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nonnull PsiElement child) {
                super(file, parent, child);
            }
        }

        public static class After extends ChildRemoval {
            public After(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nonnull PsiElement child) {
                super(file, parent, child);
            }
        }
    }

    // ChildReplacement

    public static abstract class ChildReplacement extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @Nonnull
        private final PsiElement myParent;
        /** Invalid in {@link ChildReplacement.After} */
        @Nonnull
        private final PsiElement myOldChild;

        protected ChildReplacement(@Nullable PsiFile file, @Nonnull PsiElement parent, @Nonnull PsiElement oldChild) {
            myFile = file;
            myParent = parent;
            myOldChild = oldChild;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Nonnull
        public PsiElement getParent() {
            return myParent;
        }

        @Nonnull
        public PsiElement getOldChild() {
            return myOldChild;
        }

        @Nullable
        public abstract PsiElement getNewChild();

        @Override
        public String toString() {
            PsiElement newChild = getNewChild();
            return "ChildReplacement." + getClass().getSimpleName() + "(file=" + myFile +
                ", parent=`" + myParent.getText() + "`, oldChild=`" + safeText(myOldChild) +
                "`, newChild=`" + (newChild != null ? newChild.getText() : null) + "`)";
        }

        public static class Before extends ChildReplacement {
            @Nullable
            private final PsiElement myNewChild;

            public Before(@Nullable PsiFile file, @Nonnull PsiElement parent,
                          @Nonnull PsiElement oldChild, @Nullable PsiElement newChild) {
                super(file, parent, oldChild);
                myNewChild = newChild;
            }

            @Nullable
            @Override
            public PsiElement getNewChild() {
                return myNewChild;
            }
        }

        public static class After extends ChildReplacement {
            @Nonnull
            private final PsiElement myNewChild;

            public After(@Nullable PsiFile file, @Nonnull PsiElement parent,
                         @Nonnull PsiElement oldChild, @Nonnull PsiElement newChild) {
                super(file, parent, oldChild);
                myNewChild = newChild;
            }

            @Nonnull
            @Override
            public PsiElement getNewChild() {
                return myNewChild;
            }
        }
    }

    // ChildMovement

    public static abstract class ChildMovement extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @Nonnull
        private final PsiElement myOldParent;
        @Nonnull
        private final PsiElement myNewParent;
        @Nonnull
        private final PsiElement myChild;

        protected ChildMovement(@Nullable PsiFile file, @Nonnull PsiElement oldParent,
                                @Nonnull PsiElement newParent, @Nonnull PsiElement child) {
            myFile = file;
            myOldParent = oldParent;
            myNewParent = newParent;
            myChild = child;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Nonnull
        public PsiElement getOldParent() {
            return myOldParent;
        }

        @Nonnull
        public PsiElement getNewParent() {
            return myNewParent;
        }

        @Nonnull
        public PsiElement getChild() {
            return myChild;
        }

        @Override
        public String toString() {
            return "ChildMovement." + getClass().getSimpleName() + "(file=" + myFile +
                ", oldParent=`" + myOldParent.getText() + "`, newParent=`" + myNewParent.getText() +
                "`, child=`" + myChild.getText() + "`)";
        }

        public static class Before extends ChildMovement {
            public Before(@Nullable PsiFile file, @Nonnull PsiElement oldParent,
                          @Nonnull PsiElement newParent, @Nonnull PsiElement child) {
                super(file, oldParent, newParent, child);
            }
        }

        public static class After extends ChildMovement {
            public After(@Nullable PsiFile file, @Nonnull PsiElement oldParent,
                         @Nonnull PsiElement newParent, @Nonnull PsiElement child) {
                super(file, oldParent, newParent, child);
            }
        }
    }

    // ChildrenChange

    public static abstract class ChildrenChange extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @Nonnull
        private final PsiElement myParent;
        /**
         * "generic change" event means that "something changed inside an element" and
         * sends before/after all events for concrete PSI changes in the element.
         */
        private final boolean myIsGenericChange;

        protected ChildrenChange(@Nullable PsiFile file, @Nonnull PsiElement parent, boolean isGenericChange) {
            myFile = file;
            myParent = parent;
            myIsGenericChange = isGenericChange;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Nonnull
        public PsiElement getParent() {
            return myParent;
        }

        public boolean isGenericChange() {
            return myIsGenericChange;
        }

        @Override
        public String toString() {
            return "ChildrenChange." + getClass().getSimpleName() + "(file=" + myFile +
                ", parent=`" + myParent.getText() + "`, isGenericChange=" + myIsGenericChange + ")";
        }

        public static class Before extends ChildrenChange {
            public Before(@Nullable PsiFile file, @Nonnull PsiElement parent, boolean isGenericChange) {
                super(file, parent, isGenericChange);
            }
        }

        public static class After extends ChildrenChange {
            public After(@Nullable PsiFile file, @Nonnull PsiElement parent, boolean isGenericChange) {
                super(file, parent, isGenericChange);
            }
        }
    }

    // PropertyChange

    public static abstract class PropertyChange extends RsPsiTreeChangeEvent {
        @Nonnull
        private final String myPropertyName;
        @Nullable
        private final Object myOldValue;
        @Nullable
        private final Object myNewValue;
        @Nullable
        private final PsiElement myElement;
        @Nullable
        private final PsiElement myChild;

        protected PropertyChange(@Nonnull String propertyName, @Nullable Object oldValue,
                                 @Nullable Object newValue, @Nullable PsiElement element,
                                 @Nullable PsiElement child) {
            myPropertyName = propertyName;
            myOldValue = oldValue;
            myNewValue = newValue;
            myElement = element;
            myChild = child;
        }

        @Nonnull
        public String getPropertyName() {
            return myPropertyName;
        }

        @Nullable
        public Object getOldValue() {
            return myOldValue;
        }

        @Nullable
        public Object getNewValue() {
            return myNewValue;
        }

        @Nullable
        public PsiElement getElement() {
            return myElement;
        }

        @Nullable
        public PsiElement getChild() {
            return myChild;
        }

        @Override
        public String toString() {
            Object oldVal = myOldValue instanceof Object[] ? Arrays.toString((Object[]) myOldValue) : myOldValue;
            Object newVal = myNewValue instanceof Object[] ? Arrays.toString((Object[]) myNewValue) : myNewValue;
            return "PropertyChange." + getClass().getSimpleName() + "(propertyName='" + myPropertyName +
                "', oldValue=" + oldVal + ", newValue=" + newVal + ", element=" + myElement +
                ", child=" + myChild + ")";
        }

        public static class Before extends PropertyChange {
            public Before(@Nonnull String propertyName, @Nullable Object oldValue,
                          @Nullable Object newValue, @Nullable PsiElement element,
                          @Nullable PsiElement child) {
                super(propertyName, oldValue, newValue, element, child);
            }
        }

        public static class After extends PropertyChange {
            public After(@Nonnull String propertyName, @Nullable Object oldValue,
                         @Nullable Object newValue, @Nullable PsiElement element,
                         @Nullable PsiElement child) {
                super(propertyName, oldValue, newValue, element, child);
            }
        }
    }
}
