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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NewDefinedModificationDialog extends JDialog {

    private JTable modificationJTable;

    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Modification HashMap
     */
    private HashMap<Integer, Double[]> onePeptideModificationHash;
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
     * @param spectrumMainPanel
     * @param onePeptideModificationHash
     * @param peptideSeq
     */


    public NewDefinedModificationDialog(SpectrumMainPanel spectrumMainPanel, HashMap<Integer, Double[]> onePeptideModificationHash, String peptideSeq, String spectrumKey){
        super(spectrumMainPanel.parentFrame, true);

        this.spectrumMainPanel = spectrumMainPanel;
        this.onePeptideModificationHash = onePeptideModificationHash;
        this.peptideSeq = peptideSeq;
        this.spectrumKey = spectrumKey;

        initComponents();

        modificationJTable.setModel(new ModificationTableModel());
        modificationJTable.getColumn("Index").setMaxWidth(40);
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
                                .addComponent(modificationJScrollPane, 10, 300, Short.MAX_VALUE)
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
            int index = (int) modificationJTable.getValueAt(row, 0);
            onePeptideModificationHash.get(index)[1] = (Double) modificationJTable.getValueAt(row, 3);
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

        if (column == 4 || column == 3 ){
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * modificationJTableMouseReleased
     * @param evt Mouse click event
     */
    private void modificationJTableMouseReleased(MouseEvent evt){
        int row = modificationJTable.rowAtPoint(evt.getPoint());
        int column = modificationJTable.columnAtPoint(evt.getPoint());
        int index = (int) modificationJTable.getValueAt(row, 0);

        if (row != -1 && column == 4 ){
            tempMass = (Double) modificationJTable.getValueAt(row, 2);
            onePeptideModificationHash.get(index)[1] = tempMass;
            modificationJTable.setValueAt(tempMass, row, 3);
            modificationJTable.repaint();
            modificationJTable.updateUI();

        } else if (row != -1 && column == 3){
            String aa = modificationJTable.getValueAt(row, 1).toString();

            new ModificationDialogForNew(this, aa);
            onePeptideModificationHash.get(index)[1] = tempMass;

            modificationJTable.setValueAt(tempMass, row, 3);
            modificationJTable.repaint();
            modificationJTable.updateUI();
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
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Index";
                case 1:
                    return "AA";
                case 2:
                    return "Original Mass";
                case 3:
                    return "New Defined";
                case 4:
                    return "Recover";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            ArrayList<Integer> sortedKeys = new ArrayList<Integer>(onePeptideModificationHash.keySet());
            Collections.sort(sortedKeys);
            int index = sortedKeys.get(row);

            switch (column) {
                case 0:
                    return index;
                case 1:
                    if (index == 0) {
                        return "N-term";
                    } else if (index == peptideSeq.length() +1 ){
                        return "C-term";
                    } else {
                        return peptideSeq.charAt(index-1);
                    }
                case 2:
                    return onePeptideModificationHash.get(index)[0];
                case 3:
                    return onePeptideModificationHash.get(index)[1];
                case 4:
                    return "Re-Set";
                default:
                    return "";
            }

        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 0){
                return Integer.class;
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
