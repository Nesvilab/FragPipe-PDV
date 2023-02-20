package GUI;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.regex.Pattern;

public class SetLosses extends JDialog{
    private JTextField lossesJTextField = new JTextField("C(0)H(0)O(0)P(0)");
    private NewDefinedModificationDialog newDefinedModificationDialog;

    /**
     * Constructor.
     * @param newDefinedModificationDialog Parent class
     */
    public SetLosses(NewDefinedModificationDialog newDefinedModificationDialog, String lossString){
        super(newDefinedModificationDialog, true);
        this.newDefinedModificationDialog = newDefinedModificationDialog;
        this.setLocation(new Point(newDefinedModificationDialog.getLocation().x+300, newDefinedModificationDialog.getLocation().y+50));
//        this.setLocationRelativeTo(newDefinedModificationDialog);

        if (Objects.equals(lossString, " ")){
            this.lossesJTextField.setText("C(0)H(0)O(0)P(0)");
        } else {
            this.lossesJTextField.setText(lossString);
        }

        initComponents();

        this.setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel backgroundJPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Select Amino Acid");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(Color.white);

        lossesJTextField.setSize(200, 30);
        backgroundJPanel.add(lossesJTextField);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }

    /**
     * Closes the dialog.
     * @param evt Windows event
     */
    private void formWindowClosing(WindowEvent evt) {

        if (Objects.equals(lossesJTextField.getText(), "C(0)H(0)O(0)P(0)")){
            newDefinedModificationDialog.lossesString = " ";
        } else {
            newDefinedModificationDialog.lossesString = lossesJTextField.getText().replaceAll(" ", "");
        }

        this.dispose();
    }

}
