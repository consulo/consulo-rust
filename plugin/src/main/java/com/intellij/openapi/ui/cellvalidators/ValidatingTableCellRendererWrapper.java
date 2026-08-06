package com.intellij.openapi.ui.cellvalidators;
import javax.swing.table.TableCellRenderer;
/** IntelliJ-compat stub. */
public class ValidatingTableCellRendererWrapper implements TableCellRenderer {
    public ValidatingTableCellRendererWrapper(TableCellRenderer delegate) {}
    public ValidatingTableCellRendererWrapper withCellValidator(Object validator) { return this; }
    @Override public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v, boolean s, boolean f, int r, int c) { return null; }
}
