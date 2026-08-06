package com.intellij.openapi.vcs.changes.ui;
import javax.swing.tree.DefaultMutableTreeNode;
/** IntelliJ-compat stub. */
public class ChangesBrowserNode<T> extends DefaultMutableTreeNode {
    public ChangesBrowserNode(T userObject) { super(userObject); }
    @SuppressWarnings("unchecked") public T getUserObject() { return (T) super.getUserObject(); }
    public void render(Object renderer, boolean selected, boolean expanded, boolean hasFocus) {}
    public int getCount() { return 0; }
}
