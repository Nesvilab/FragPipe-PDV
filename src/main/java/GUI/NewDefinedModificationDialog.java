package GUI;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import it.unimi.dsi.fastutil.doubles.Double2LongArrayMap;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class NewDefinedModificationDialog extends JDialog {

    private JTable modificationJTable;

    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Modification HashMap
     */
    private HashMap<Integer, double[]> onePeptideModificationHash;
    /**
     *
     */
    private HashMap<String, Object[]> modChangeGlobalMap = new HashMap<>();
    /**
     * Peptide Seq
     */
    private String peptideSeq;
    /**
     *
     */
    private  String spectrumKey;
    /**
     *
     */
    public Double tempMass = 0.0;
    /**
     *
     */
    public Double lossMass = 0.0;
    /**
     *
     */
    public String selectedPosAAForDelta = "-1 ";

    /**
     *
     * @param spectrumMainPanel
     * @param onePeptideModificationHash
     * @param peptideSeq
     */


    public NewDefinedModificationDialog(SpectrumMainPanel spectrumMainPanel, HashMap<Integer, double[]> onePeptideModificationHash, String peptideSeq, String spectrumKey, HashMap<String, Object[]> modChangeGlobalMap){
        super(spectrumMainPanel.parentFrame, true);

        this.spectrumMainPanel = spectrumMainPanel;
        this.onePeptideModificationHash = onePeptideModificationHash;
        this.modChangeGlobalMap = modChangeGlobalMap;
        this.peptideSeq = peptideSeq;
        this.spectrumKey = spectrumKey;

        initComponents();

        modificationJTable.setModel(new ModificationTableModel());
        modificationJTable.getColumn("Type").setMaxWidth(40);
        modificationJTable.getColumn("Ori Index").setMaxWidth(40);
        modificationJTable.getColumn("AA").setMaxWidth(30);
        modificationJTable.getColumn("Recover").setMaxWidth(60);

        setLocationRelativeTo(spectrumMainPanel);
        setVisible(true);
    }

    private void initComponents(){
        modificationJTable = new JTable();
        JPanel mainJPanel = new JPanel();
        JScrollPane modificationJScrollPane = new JScrollPane();
        JPanel modificationJPanel = new JPanel();
        JButton okJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(new Dimension(500, 200));

        mainJPanel.setBackground(new Color(250, 250, 250));

        okJButton.setText("OK");
        okJButton.addActionListener(this::okJButtonActionPerformed);

        modificationJPanel.setOpaque(false);

        modificationJScrollPane.setOpaque(false);

        modificationJTable.setRowHeight(20);
        modificationJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        modificationJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));
        modificationJTable.setOpaque(false);
        modificationJTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        modificationJTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                modificationJTableMouseReleased(evt);
            }

            public void mouseExited(MouseEvent evt) {
                modificationJTableMouseExited(evt);
            }

            public void mouseEntered(MouseEvent evt) {
                modificationJTableMouseEntered(evt);
            }
        });

        modificationJScrollPane.setViewportView(modificationJTable);

        GroupLayout modificationJPanelLayout = new GroupLayout(modificationJPanel);
        modificationJPanel.setLayout(modificationJPanelLayout);
        modificationJPanelLayout.setHorizontalGroup(
                modificationJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(modificationJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(modificationJScrollPane, 10, 500, Short.MAX_VALUE)
                                .addContainerGap())
        );
        modificationJPanelLayout.setVerticalGroup(
                modificationJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(modificationJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(modificationJScrollPane, 10, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(modificationJPanel, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                        .addComponent(okJButton))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(modificationJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okJButton)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * okJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void okJButtonActionPerformed(ActionEvent evt){

        for (int row=0; row < modificationJTable.getRowCount(); row++){
            int index = (int) modificationJTable.getValueAt(row, 1);
            onePeptideModificationHash.get(index)[1] = (Double) modificationJTable.getValueAt(row, 5);
        }

        spectrumMainPanel.parentFrame.newDefinedMods.put(spectrumKey, onePeptideModificationHash);

        dispose();
    }

    /**
     * modificationJTableMouseExited
     * @param evt Mouse move event
     */
    private void modificationJTableMouseExited(MouseEvent evt){
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * modificationJTableMouseEntered
     * @param evt Mouse move event
     */
    private void modificationJTableMouseEntered(MouseEvent evt){
        int column = modificationJTable.columnAtPoint(evt.getPoint());
        int row = modificationJTable.rowAtPoint(evt.getPoint());

        if (column == 5 || column == 4 ){
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        if (row == onePeptideModificationHash.size() -1){
            if (column == 1 ){
                this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        } else {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * modificationJTableMouseReleased
     * @param evt Mouse click event
     */
    private void modificationJTableMouseReleased(MouseEvent evt){
        int row = modificationJTable.rowAtPoint(evt.getPoint());
        int column = modificationJTable.columnAtPoint(evt.getPoint());
        int index = (int) modificationJTable.getValueAt(row, 1);
        String aa = modificationJTable.getValueAt(row, 3).toString();
        String oriMass = String.valueOf(modificationJTable.getValueAt(row, 4));

        if (row != -1 && column == 8 ){
            tempMass = (Double) modificationJTable.getValueAt(row, 4);
            onePeptideModificationHash.get(index)[1] = tempMass;
            onePeptideModificationHash.get(index)[2] = index;

            if ((Boolean)modificationJTable.getValueAt(row, 7)){
                modChangeGlobalMap.get(oriMass + " of " + aa)[1] = oriMass + "_0.0";
            }

        } else if (row != -1 && column == 5){

            new ModificationDialogForNew(this, aa, modificationJTable.getValueAt(row, 5).toString());
            onePeptideModificationHash.get(index)[1] = tempMass;

            if ((Boolean)modificationJTable.getValueAt(row, 7)){
                modChangeGlobalMap.get(oriMass + " of " + aa)[1] = tempMass + "_" + modificationJTable.getValueAt(row, 6);
            }


        } else if (row == onePeptideModificationHash.size()-1 && (column == 1 || column == 2)){

            new AASelectionForDeltaModDialog(this, peptideSeq, selectedPosAAForDelta);

            onePeptideModificationHash.get(index)[2] = Double.parseDouble((selectedPosAAForDelta.split(" ")[0]));

        } else if (row != onePeptideModificationHash.size()-1 && row != -1 && column == 7){

            if (!modChangeGlobalMap.containsKey(oriMass + " of " + aa)){
                Object[] values = new Object[2];
                values[0] = true;
                values[1] = modificationJTable.getValueAt(row, 5) + "_" + modificationJTable.getValueAt(row, 6);

                modChangeGlobalMap.put(oriMass + " of " + aa, values);
            } else {
                Boolean isSelected = (Boolean) modChangeGlobalMap.get(oriMass + " of " + aa)[0];
                if (isSelected){
                    modChangeGlobalMap.get(oriMass + " of " + aa)[0] = false;
                } else {
                    modChangeGlobalMap.get(oriMass + " of " + aa)[0] = true;
                }
            }
        }
        if (modChangeGlobalMap.containsKey(oriMass + " of " + aa)){
            modChangeGlobalMap.get(oriMass + " of " + aa)[1] = modificationJTable.getValueAt(row, 5) + "_" + modificationJTable.getValueAt(row, 6);
        }

        modificationJTable.repaint();
        modificationJTable.updateUI();
    }


    /**
     * PTM modification table model
     */
    private class ModificationTableModel extends DefaultTableModel{

        /**
         * Constructor
         */
        public ModificationTableModel(){
        }

        @Override
        public int getRowCount() {
            if(onePeptideModificationHash == null){
                return 0;
            }
            return onePeptideModificationHash.size();
        }

        @Override
        public int getColumnCount() {
            return 9;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Type";
                case 1:
                    return "Ori Index";
                case 2:
                    return "New Index";
                case 3:
                    return "AA";
                case 4:
                    return "Original Mass";
                case 5:
                    return "New Defined";
                case 6:
                    return "Mod Loss";
                case 7:
                    return "Global";
                case 8:
                    return "Recover";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            ArrayList<Integer> sortedKeys = new ArrayList<Integer>(onePeptideModificationHash.keySet());
            Collections.sort(sortedKeys);
            int index = 0;
            if (row == sortedKeys.size()-1){
                index = sortedKeys.get(0);
            } else {
                index = sortedKeys.get(row+1);
            }

            String currentAA = "";
            if (index == -1){
                if (onePeptideModificationHash.get(index)[2] == -1.0){
                    currentAA = "";
                } else {
                    currentAA = String.valueOf(peptideSeq.charAt((int) (onePeptideModificationHash.get(index)[2]-1)));
                }

            } else if (index == 0) {
                currentAA = "N-term";
            } else if (index == peptideSeq.length() +1 ){
                currentAA = "C-term";
            } else {
                currentAA = String.valueOf(peptideSeq.charAt(index-1));
            }

            boolean hasGlobal = false;
            String globalMapKey = onePeptideModificationHash.get(index)[0] + " of " + currentAA;
            if (modChangeGlobalMap.containsKey(globalMapKey)){
                hasGlobal = (Boolean) modChangeGlobalMap.get(globalMapKey)[0];
            }

            switch (column) {
                case 0:
                    if (onePeptideModificationHash.get(index)[4] == 1.0){
                        return "Mod";
                    } else {
                        return "Delta";
                    }
                case 1:
                    return index;
                case 2:
                    return (int)onePeptideModificationHash.get(index)[2];
                case 3:
                    return currentAA;
                case 4:
                    return onePeptideModificationHash.get(index)[0];
                case 5:
                    if (hasGlobal){
                        return Double.valueOf(((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[0]);
                    } else {
                        return onePeptideModificationHash.get(index)[1];
                    }
                case 6:
                    if (hasGlobal){
                        return Double.valueOf(((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[1]);
                    } else {
                        return onePeptideModificationHash.get(index)[3];
                    }
                case 7:
                    return hasGlobal;

                case 8:
                    return "Re-Set";
                default:
                    return "";
            }

        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 1){
                return Integer.class;
            } else if(columnIndex == 7){
                return Boolean.class;
            } else {
                return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;

        }
    }

}
