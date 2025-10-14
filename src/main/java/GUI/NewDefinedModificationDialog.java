package GUI;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import it.unimi.dsi.fastutil.doubles.Double2LongArrayMap;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class NewDefinedModificationDialog extends JDialog {

    private JTable modificationJTable;

    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Modification HashMap
     */
    private HashMap<Integer, Object[]> onePeptideModificationHash;
    private ArrayList<ModificationMatch> modificationMatches;
    /**
     *
     */
    private HashMap<String, Object[]> modChangeGlobalMap = new HashMap<>();
    private PTMFactory ptmFactory;
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
    public String lossesString = "";

    /**
     *
     * @param spectrumMainPanel
     * @param onePeptideModificationHash
     * @param peptide
     */

    public NewDefinedModificationDialog(SpectrumMainPanel spectrumMainPanel, HashMap<Integer, Object[]> onePeptideModificationHash,
                                        Peptide peptide, String spectrumKey, HashMap<String, Object[]> modChangeGlobalMap,
                                        PTMFactory ptmFactory){
        super(spectrumMainPanel.parentFrame, true);
        this.spectrumMainPanel = spectrumMainPanel;
        this.onePeptideModificationHash = onePeptideModificationHash;
        this.modChangeGlobalMap = modChangeGlobalMap;
        this.peptideSeq = peptide.getSequence();
        this.modificationMatches = peptide.getModificationMatches();
        this.spectrumKey = spectrumKey;
        this.ptmFactory = ptmFactory;

        initComponents();

        modificationJTable.setModel(new ModificationTableModel());
        modificationJTable.getColumn("Type").setMaxWidth(40);
        modificationJTable.getColumn("Ori Index").setMaxWidth(40);
        modificationJTable.getColumn("AA").setMaxWidth(30);
        modificationJTable.getColumn("Recover").setMaxWidth(60);

        modificationJTable.getColumn("Global").setMinWidth(0);
        modificationJTable.getColumn("Global").setMaxWidth(0);

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
        modificationJTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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

//        for (int row=0; row < modificationJTable.getRowCount(); row++){
//            int index = (int) modificationJTable.getValueAt(row, 1);
//            onePeptideModificationHash.get(index)[1] = modificationJTable.getValueAt(row, 5);
//        }

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

        if (row != -1 && column == 9 ){
            tempMass = (Double) modificationJTable.getValueAt(row, 4);
            onePeptideModificationHash.get(index)[1] = tempMass;
            onePeptideModificationHash.get(index)[2] = index;
            onePeptideModificationHash.get(index)[3] = modificationJTable.getValueAt(row, 6);

            if ((Boolean)modificationJTable.getValueAt(row, 8)){
                modChangeGlobalMap.get(oriMass + " of " + aa)[1] = oriMass;
            }

        } else if (row != -1 && column == 5){

            new ModificationDialogForNew(this, aa, modificationJTable.getValueAt(row, 5).toString());
            onePeptideModificationHash.get(index)[1] = tempMass;

            if ((Boolean)modificationJTable.getValueAt(row, 8)){
                if (Objects.equals(modificationJTable.getValueAt(row, 7).toString(), " ")){
                    modChangeGlobalMap.get(oriMass + " of " + aa)[1] = tempMass;
                } else {
                    modChangeGlobalMap.get(oriMass + " of " + aa)[1] = tempMass + "_" + modificationJTable.getValueAt(row, 7);
                }
            }

        } else if (row == onePeptideModificationHash.size()-1 && (column == 1 || column == 2)){

            new AASelectionForDeltaModDialog(this, peptideSeq, selectedPosAAForDelta);

            onePeptideModificationHash.get(index)[2] = Integer.valueOf(selectedPosAAForDelta.split(" ")[0]);
            tempMass = (Double) modificationJTable.getValueAt(row, 5);
            onePeptideModificationHash.get(index)[1] = tempMass;

        } else if (row != onePeptideModificationHash.size()-1 && row != -1 && column == 8){
            if (!modChangeGlobalMap.containsKey(oriMass + " of " + aa)){
                Object[] values = new Object[2];
                values[0] = true;
                if (Objects.equals(onePeptideModificationHash.get(index)[3], " ")){
                    values[1] = onePeptideModificationHash.get(index)[1];
                } else {
                    values[1] = onePeptideModificationHash.get(index)[1] + "_" + onePeptideModificationHash.get(index)[3];
                }

                modChangeGlobalMap.put(oriMass + " of " + aa, values);
            } else {
                Boolean isSelected = (Boolean) modChangeGlobalMap.get(oriMass + " of " + aa)[0];
                if (isSelected){
                    modChangeGlobalMap.get(oriMass + " of " + aa)[0] = false;
                } else {
                    modChangeGlobalMap.get(oriMass + " of " + aa)[0] = true;
                }
                if (Objects.equals(onePeptideModificationHash.get(index)[3], " ")){
                    modChangeGlobalMap.get(oriMass + " of " + aa)[1] = onePeptideModificationHash.get(index)[1];
                } else {
                    modChangeGlobalMap.get(oriMass + " of " + aa)[1] = onePeptideModificationHash.get(index)[1] + "_" + onePeptideModificationHash.get(index)[3];
                }
            }

        } else if (row != onePeptideModificationHash.size()-1 && (column == 7)){
            new SetLosses(this, (String) onePeptideModificationHash.get(index)[3]);
            onePeptideModificationHash.get(index)[3] = lossesString;
        }

        modificationJTable.repaint();
        modificationJTable.updateUI();
    }

    private void modificationJTableEdit(KeyEvent evt){
        System.out.println("Release key");
        System.out.println();
        int row = modificationJTable.getSelectedRow();
        int column = modificationJTable.getSelectedColumn();
        int index = (int) modificationJTable.getValueAt(row, 1);

        System.out.println(modificationJTable.getValueAt(row, 6));

        if (row != onePeptideModificationHash.size()-1 && (column == 6)){

            onePeptideModificationHash.get(index)[3] =  modificationJTable.getValueAt(row, 6);;
        }
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
            return 10;
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
                    return "Original Loss";
                case 7:
                    return "Mod Loss";
                case 8:
                    return "Global";
                case 9:
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
                if ((Integer) onePeptideModificationHash.get(index)[2] == -1){
                    currentAA = "";
                } else {
                    currentAA = String.valueOf(peptideSeq.charAt((Integer) onePeptideModificationHash.get(index)[2]-1));
                }

            } else if (index == 0) {
                currentAA = "N-term";
            } else if (index == peptideSeq.length() +1 ){
                currentAA = "C-term";
            } else {
                currentAA = String.valueOf(peptideSeq.charAt(index-1));
            }

            double oldMass = 0.0;
            String oldLoss = " ";
            for (ModificationMatch modificationMatch : modificationMatches) {
                String name = modificationMatch.getTheoreticPtm();

                if (currentAA.equals("N-term") && name.contains("N-term")){
                    oldMass = ptmFactory.getPTM(name).getMass();
                    if (ptmFactory.getPTM(name).getNeutralLosses().size() != 0){
                        oldLoss = ptmFactory.getPTM(name).getNeutralLosses().get(0).getComposition().toString();
                    }
                } else if (currentAA.equals("C-term") && name.contains("C-term")) {
                    oldMass = ptmFactory.getPTM(name).getMass();
                    if (ptmFactory.getPTM(name).getNeutralLosses().size() != 0){
                        oldLoss = ptmFactory.getPTM(name).getNeutralLosses().get(0).getComposition().toString();
                    }
                } else if (modificationMatch.getModificationSite() == (int)onePeptideModificationHash.get(index)[2]){
                    oldMass = ptmFactory.getPTM(name).getMass();
                    if (ptmFactory.getPTM(name).getNeutralLosses().size() != 0){
                        oldLoss = ptmFactory.getPTM(name).getNeutralLosses().get(0).getComposition().toString();
                    }
                }
            }
            if (index == -1) {
                oldMass = (double) onePeptideModificationHash.get(index)[0];
                oldLoss = " ";
            }
            String globalMapKey = oldMass + " of " + currentAA;
            boolean hasGlobal = checkGlobal(globalMapKey);

            switch (column) {
                case 0:
                    if ((int) onePeptideModificationHash.get(index)[4] == 1){
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
                    return oldMass;

                case 5:
                    if (hasGlobal){
                        if (String.valueOf(modChangeGlobalMap.get(globalMapKey)[1]).contains("_")){
                            onePeptideModificationHash.get(index)[1] = Double.parseDouble(((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[0]);
                            return Double.valueOf(((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[0]);
                        } else {
                            return modChangeGlobalMap.get(globalMapKey)[1];
                        }

                    } else {
                        return onePeptideModificationHash.get(index)[1];
                    }
                case 6:
                    return oldLoss;
                case 7:
                    if (hasGlobal){
                        if (String.valueOf(modChangeGlobalMap.get(globalMapKey)[1]).contains("_")){
                            onePeptideModificationHash.get(index)[3] = (((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[1]);
                            return ((String) modChangeGlobalMap.get(globalMapKey)[1]).split("_")[1];
                        } else {
                            return " ";
                        }
                    } else {
                        return onePeptideModificationHash.get(index)[3];
                    }
                case 8:
                    return hasGlobal;

                case 9:
                    return "Re-Set";
                default:
                    return "";
            }

        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 1){
                return Integer.class;
            } else if(columnIndex == 8){
                return Boolean.class;
            } else {
                return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;

        }

        private Boolean checkGlobal(String globalMapKey){
            boolean hasGlobal = false;
            if (modChangeGlobalMap.containsKey(globalMapKey)){
                hasGlobal = (Boolean) modChangeGlobalMap.get(globalMapKey)[0];
            }
            return hasGlobal;
        }
    }

}
