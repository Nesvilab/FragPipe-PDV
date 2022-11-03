package GUI.utils;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class ProteinTableModel extends DefaultTableModel {

    /**
     * Selected item
     */
    private ArrayList<ArrayList<Object>> selectedItem;
    /**
     * Protein ID list
     */
    private ArrayList<String> proteinIDList;
    /**
     * Decimal format
     */
    private DecimalFormat df = new DecimalFormat("#.0000");
    /**
     * Spectrum key and it's selected boolean
     */
    private HashMap<String, Boolean> proteinKeyToSelected;
    /**
     * Protein info
     */
    private ArrayList<String> proteinScoreName = new ArrayList<>();

    /**
     * Empty constructor
     */
    public ProteinTableModel(){}

    public ProteinTableModel(HashMap<String, Boolean> proteinKeyToSelected, ArrayList<String> proteinScoreName){
        this.proteinKeyToSelected =proteinKeyToSelected;
        this.proteinScoreName = proteinScoreName;
    }

    /**
     * Update the table
     * @param selectedItem Selected item
     * @param proteinIDList Protein ID list
     * @param proteinKeyToSelected Protein key to selected
     */
    public void updateTable(ArrayList<ArrayList<Object>> selectedItem, ArrayList<String> proteinIDList, HashMap<String, Boolean> proteinKeyToSelected){
        this.selectedItem = selectedItem;
        this.proteinIDList = proteinIDList;
        this.proteinKeyToSelected = proteinKeyToSelected;
    }

    @Override
    public int getRowCount() {
        if (proteinIDList != null) {
            return proteinIDList.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        if(proteinScoreName != null) {
            return proteinScoreName.size()+4;
        } else {
            return 4;
        }
    }

    @Override
    public String getColumnName(int column) {

        if(column == 0){
            return "Selected";
        }else if(column == 1){
            return "EXP:Protein";
        } else if (column == 2){
            return "Experiment";
        } else if (column == 3){
            return "Protein";
        }
        for(int index = 0; index < proteinScoreName.size(); index++){
            int newColumn = index + 4;
            if(column == newColumn){
                return proteinScoreName.get(index);
            }
        }

        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            if(row < proteinIDList.size()){
                String proteinID = proteinIDList.get(row);

                ArrayList<Object> oneItem = selectedItem.get(row);

                if(column == 0){
                    return proteinKeyToSelected.getOrDefault(proteinID, false);
                }
                if(column == 1) {
                    return proteinID;
                }
                if(column == 2) {
                    return proteinID.split(":\\|")[1].replace("_Dash_", "-");
                }
                if(column == 3) {
                    return proteinID.split(":\\|")[0];
                }
                for(int index = 0; index < proteinScoreName.size(); index++){
                    int newColumn = index + 4;
                    if(column == newColumn) {
                        Object object = oneItem.get(index);
                        if (object.getClass() == String.class) {
                            return object;
                        } else if (object.getClass() == Integer.class) {
                            return Double.valueOf((Integer) object);
                        } else if (object.getClass() == Double.class) {
                            return object;
                        }
                    }

                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Table not instantiated.\nError: " + e.toString(),
                    "Display Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new IllegalArgumentException("Table not instantiated.");
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();
            }
        }

        return String.class;
    }

}
