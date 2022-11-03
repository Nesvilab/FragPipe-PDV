package GUI;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class ClipboardKeyAdapter extends KeyAdapter {

    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    private final JTable table;

    public ClipboardKeyAdapter(JTable table) {
        this.table = table;
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.getKeyCode()==KeyEvent.VK_C) { // Copy
            cancelEditing();
            copyToClipboard();
        }
    }

    private void copyToClipboard() {

        int rowsSelected=table.getSelectedRow();
        int colsSelected=table.getSelectedColumn();
        String element = String.valueOf(table.getValueAt(rowsSelected, colsSelected));
        StringBuilder newElement = new StringBuilder();
        if (colsSelected == 5){
            String[] elementArray = StringUtils.substringsBetween(element, ">", "<");
            for (int co=0; co < elementArray.length; co ++){
                if (co == 0){
                    newElement.append(StringUtils.substringAfter(elementArray[co], "-"));
                } else if (co == elementArray.length-1){
                    newElement.append(StringUtils.substringBefore(elementArray[co], "-"));
                } else {
                    newElement.append(elementArray[co]);
                }
            }
        }

        StringSelection sel  = new StringSelection(newElement.toString());
        CLIPBOARD.setContents(sel, sel);
    }

    private void cancelEditing() {
        if (table.getCellEditor() != null) {
            table.getCellEditor().cancelCellEditing();
        }
    }

}
