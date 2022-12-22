package GUI;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Modification dialog in single peptide part
 * Created by Ken on 7/6/2017.
 */
public class ModificationDialogForNew extends JDialog {

    private JComboBox aAModificationComBox;
    private String[] singleModificationsArray;
    private String selectedModification;
    private String aaName;
    private Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
    private Boolean userInput = false;
    private Boolean modValidation = false;
    private NewDefinedModificationDialog newDefinedModificationDialog;
    /**
     * All modifications map
     */
    private HashMap<String, ArrayList<String>> aASingleModification;
    /**
     * PTMFactory import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Constructor.
     * @param newDefinedModificationDialog Parent class
     * @param aaName aa name
     */
    public ModificationDialogForNew(NewDefinedModificationDialog newDefinedModificationDialog, String aaName, String firstMass){
        super(newDefinedModificationDialog, true);
        this.newDefinedModificationDialog = newDefinedModificationDialog;
        this.setLocation(new Point(newDefinedModificationDialog.getLocation().x+50, newDefinedModificationDialog.getLocation().y+50));
//        this.setLocationRelativeTo(newDefinedModificationDialog);
        this.aaName = aaName;

        getModification();

        ArrayList<String> singleModifications;
        if (Objects.equals(aaName, "N-term")){
            singleModifications = aASingleModification.get("N-terminus");
        } else if (Objects.equals(aaName, "C-term")){
            singleModifications = aASingleModification.get("C-terminus");
        } else {
            singleModifications = aASingleModification.get(aaName);
        }
        Collections.sort(singleModifications);

        String[] orderMods = singleModifications.toArray(new String[0]);
        this.singleModificationsArray = new String[orderMods.length + 1];
        this.singleModificationsArray[0] = firstMass;
        for (int i = 0; i < orderMods.length; i++)
            singleModificationsArray[i+1] = orderMods[i];

        initComponents();
        this.aAModificationComBox.setRenderer(new AlignedListCellRenderer(0));
        this.aAModificationComBox.setSelectedIndex(0);
        this.setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel backgroundJPanel = new JPanel();
        aAModificationComBox = new JComboBox();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Modification of "+ aaName);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(Color.white);

        aAModificationComBox.setModel(new DefaultComboBoxModel(singleModificationsArray));
        aAModificationComBox.addItemListener(this::aAModificationComBoxdMouseClicked);
        aAModificationComBox.setEditable(true);

        backgroundJPanel.add(aAModificationComBox);

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

        selectedModification = String.valueOf(aAModificationComBox.getSelectedItem()).replace(">","&gt;");

        if (aAModificationComBox.getSelectedIndex() == -1){
            if (selectedModification.equals("null") || pattern.matcher(selectedModification).matches()) {
                userInput = true;
                modValidation = true;
                formWindowClosing(null);
            } else {
                modValidation = false;
                JOptionPane.showMessageDialog(null, "Please input modification mass.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            modValidation = true;
            userInput = false;
            formWindowClosing(null);
        }
    }

    /**
     * Get all modifications as map
     * @return HashMap
     */
    private void getModification(){
        HashMap<String, ArrayList< String >> modificationMass = new HashMap<>();

        ArrayList<String> orderedModifications = ptmFactory.getPTMs();

        for (String  modificationName : orderedModifications){

            String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
            String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
            if(modificationMass.containsKey(aminoAcidName)){
                modificationMass.get(aminoAcidName).add(modificationName);
            }else {
                ArrayList< String > singleModi = new ArrayList<>();
                singleModi.add(modificationName);
                modificationMass.put(aminoAcidName, singleModi);
            }
        }

        aASingleModification = modificationMass;
    }

    /**
     * Closes the dialog.
     * @param evt Windows event
     */
    private void formWindowClosing(WindowEvent evt) {

        if (modValidation && !selectedModification.equals("null")) {
            if (!userInput) {
                newDefinedModificationDialog.tempMass = ptmFactory.getPTM(selectedModification).getMass();
            } else {
                newDefinedModificationDialog.tempMass = Double.valueOf(selectedModification);
            }
        }
        this.dispose();
    }
}
