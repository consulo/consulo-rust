/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private static String safeText(@NotNull PsiElement element) {
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
        @NotNull
        private final PsiElement myParent;

        protected ChildAddition(@Nullable PsiFile file, @NotNull PsiElement parent) {
            myFile = file;
            myParent = parent;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @NotNull
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

            public Before(@Nullable PsiFile file, @NotNull PsiElement parent, @Nullable PsiElement child) {
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
            @NotNull
            private final PsiElement myChild;

            public After(@Nullable PsiFile file, @NotNull PsiElement parent, @NotNull PsiElement child) {
                super(file, parent);
                myChild = child;
            }

            @NotNull
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
        @NotNull
        private final PsiElement myParent;
        /** Invalid in {@link ChildRemoval.After} */
        @NotNull
        private final PsiElement myChild;

        protected ChildRemoval(@Nullable PsiFile file, @NotNull PsiElement parent, @NotNull PsiElement child) {
            myFile = file;
            myParent = parent;
            myChild = child;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @NotNull
        public PsiElement getParent() {
            return myParent;
        }

        @NotNull
        public PsiElement getChild() {
            return myChild;
        }

        @Override
        public String toString() {
            return "ChildRemoval." + getClass().getSimpleName() + "(file=" + myFile +
                ", parent=`" + myParent.getText() + "`, child=`" + safeText(myChild) + "`)";
        }

        public static class Before extends ChildRemoval {
            public Before(@Nullable PsiFile file, @NotNull PsiElement parent, @NotNull PsiElement child) {
                super(file, parent, child);
            }
        }

        public static class After extends ChildRemoval {
            public After(@Nullable PsiFile file, @NotNull PsiElement parent, @NotNull PsiElement child) {
                super(file, parent, child);
            }
        }
    }

    // ChildReplacement

    public static abstract class ChildReplacement extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @NotNull
        private final PsiElement myParent;
        /** Invalid in {@link ChildReplacement.After} */
        @NotNull
        private final PsiElement myOldChild;

        protected ChildReplacement(@Nullable PsiFile file, @NotNull PsiElement parent, @NotNull PsiElement oldChild) {
            myFile = file;
            myParent = parent;
            myOldChild = oldChild;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @NotNull
        public PsiElement getParent() {
            return myParent;
        }

        @NotNull
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

            public Before(@Nullable PsiFile file, @NotNull PsiElement parent,
                          @NotNull PsiElement oldChild, @Nullable PsiElement newChild) {
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
            @NotNull
            private final PsiElement myNewChild;

            public After(@Nullable PsiFile file, @NotNull PsiElement parent,
                         @NotNull PsiElement oldChild, @NotNull PsiElement newChild) {
                super(file, parent, oldChild);
                myNewChild = newChild;
            }

            @NotNull
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
        @NotNull
        private final PsiElement myOldParent;
        @NotNull
        private final PsiElement myNewParent;
        @NotNull
        private final PsiElement myChild;

        protected ChildMovement(@Nullable PsiFile file, @NotNull PsiElement oldParent,
                                @NotNull PsiElement newParent, @NotNull PsiElement child) {
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

        @NotNull
        public PsiElement getOldParent() {
            return myOldParent;
        }

        @NotNull
        public PsiElement getNewParent() {
            return myNewParent;
        }

        @NotNull
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
            public Before(@Nullable PsiFile file, @NotNull PsiElement oldParent,
                          @NotNull PsiElement newParent, @NotNull PsiElement child) {
                super(file, oldParent, newParent, child);
            }
        }

        public static class After extends ChildMovement {
            public After(@Nullable PsiFile file, @NotNull PsiElement oldParent,
                         @NotNull PsiElement newParent, @NotNull PsiElement child) {
                super(file, oldParent, newParent, child);
            }
        }
    }

    // ChildrenChange

    public static abstract class ChildrenChange extends RsPsiTreeChangeEvent {
        @Nullable
        private final PsiFile myFile;
        @NotNull
        private final PsiElement myParent;
        /**
         * "generic change" event means that "something changed inside an element" and
         * sends before/after all events for concrete PSI changes in the element.
         */
        private final boolean myIsGenericChange;

        protected ChildrenChange(@Nullable PsiFile file, @NotNull PsiElement parent, boolean isGenericChange) {
            myFile = file;
            myParent = parent;
            myIsGenericChange = isGenericChange;
        }

        @Nullable
        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @NotNull
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
            public Before(@Nullable PsiFile file, @NotNull PsiElement parent, boolean isGenericChange) {
                super(file, parent, isGenericChange);
            }
        }

        public static class After extends ChildrenChange {
            public After(@Nullable PsiFile file, @NotNull PsiElement parent, boolean isGenericChange) {
                super(file, parent, isGenericChange);
            }
        }
    }

    // PropertyChange

    public static abstract class PropertyChange extends RsPsiTreeChangeEvent {
        @NotNull
        private final String myPropertyName;
        @Nullable
        private final Object myOldValue;
        @Nullable
        private final Object myNewValue;
        @Nullable
        private final PsiElement myElement;
        @Nullable
        private final PsiElement myChild;

        protected PropertyChange(@NotNull String propertyName, @Nullable Object oldValue,
                                 @Nullable Object newValue, @Nullable PsiElement element,
                                 @Nullable PsiElement child) {
            myPropertyName = propertyName;
            myOldValue = oldValue;
            myNewValue = newValue;
            myElement = element;
            myChild = child;
        }

        @NotNull
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
            public Before(@NotNull String propertyName, @Nullable Object oldValue,
                          @Nullable Object newValue, @Nullable PsiElement element,
                          @Nullable PsiElement child) {
                super(propertyName, oldValue, newValue, element, child);
            }
        }

        public static class After extends PropertyChange {
            public After(@NotNull String propertyName, @Nullable Object oldValue,
                         @Nullable Object newValue, @Nullable PsiElement element,
                         @Nullable PsiElement child) {
                super(propertyName, oldValue, newValue, element, child);
            }
        }
    }
}
