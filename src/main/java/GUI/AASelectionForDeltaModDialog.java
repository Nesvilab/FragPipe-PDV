package GUI;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.regex.Pattern;

public class AASelectionForDeltaModDialog extends JDialog{
    private JComboBox aAComBox;
    private String selectedAA;
    private NewDefinedModificationDialog newDefinedModificationDialog;
    private String[] aaCellList;

    /**
     * Constructor.
     * @param newDefinedModificationDialog Parent class
     * @param pepSeq aa name
     */
    public AASelectionForDeltaModDialog(NewDefinedModificationDialog newDefinedModificationDialog, String pepSeq, String selectedPosAAForDelta){
        super(newDefinedModificationDialog, true);
        this.newDefinedModificationDialog = newDefinedModificationDialog;
        this.setLocation(new Point(newDefinedModificationDialog.getLocation().x+50, newDefinedModificationDialog.getLocation().y+50));
//        this.setLocationRelativeTo(newDefinedModificationDialog);

        getAA(pepSeq);

        initComponents();
        this.aAComBox.setRenderer(new AlignedListCellRenderer(0));
        if (Objects.equals(selectedPosAAForDelta, "-1 ")){
            this.aAComBox.setSelectedIndex(0);
        } else {
            for (int index = 0; index<pepSeq.length(); index ++){
                if (Objects.equals(aaCellList[index], selectedPosAAForDelta)){
                    this.aAComBox.setSelectedIndex(index);
                }
            }
        }

        this.setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel backgroundJPanel = new JPanel();
        aAComBox = new JComboBox();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Select Amino Acid");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(Color.white);

        aAComBox.setModel(new DefaultComboBoxModel(aaCellList));
        aAComBox.addItemListener(this::aAModificationComBoxdMouseClicked);
        aAComBox.setEditable(true);

        backgroundJPanel.add(aAComBox);

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
     * Modification selections
     * @param evt Item event
     */
    private void aAModificationComBoxdMouseClicked(ItemEvent evt) {

        selectedAA = String.valueOf(aAComBox.getSelectedItem());

        formWindowClosing(null);

    }

    /**
     * Get all modifications as map
     * @return HashMap
     */
    private void getAA(String pepSeq){
        aaCellList = new String[pepSeq.length() + 1];
        aaCellList[0] = "None";
        for (int index=0; index < pepSeq.length(); index ++){
            aaCellList[index + 1] = (index + 1 + " " + pepSeq.charAt(index));
        }
    }

    /**
     * Closes the dialog.
     * @param evt Windows event
     */
    private void formWindowClosing(WindowEvent evt) {

        if (selectedAA != null){
            if (selectedAA.equals("None")){
                newDefinedModificationDialog.selectedPosAAForDelta = "-1 ";
            }else {
                newDefinedModificationDialog.selectedPosAAForDelta = selectedAA;
            }
        }

        this.dispose();
    }

}
