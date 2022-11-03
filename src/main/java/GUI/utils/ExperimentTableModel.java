package GUI.utils;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashMap;

public class ExperimentTableModel extends DefaultTableModel {

    /**
     *
     */
    private HashMap<String, Integer[]> experimentInfo = new HashMap<>();
    /**
     *
     */
    private ArrayList<String> expNumList = new ArrayList<>();
    /**
     * Experiment key and it's selected boolean
     */
    private HashMap<String, Boolean> experimentKeyToSelected;
    /**
     * Empty constructor
     */
    public ExperimentTableModel(){}

    public ExperimentTableModel(HashMap<String, Integer[]> experimentInfo, ArrayList<String> expNumList, HashMap<String, Boolean> experimentKeyToSelected){
        this.experimentInfo = experimentInfo;
        this.expNumList = expNumList;
        this.experimentKeyToSelected =experimentKeyToSelected;
    }

    @Override
    public int getRowCount() {
        if (experimentInfo != null) {
            return experimentInfo.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {

        if(column == 0){
            return "Selected";
        } else if(column == 1){
            return "Experiment";
        }else if(column == 2){
            return "#Proteins";
        }else if(column == 3){
            return "#PSMs";
        }else if(column == 4){
            return "#Peptides";
        }
        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {
        String name = expNumList.get(row);
        Integer[] oneExp = experimentInfo.get(name);

        switch (column) {
            case 0:
                return experimentKeyToSelected.getOrDefault(name, false);
            case 1:
                return name;
            case 2:
                return oneExp[0];
            case 3:
                return oneExp[1];
            case 4:
                return oneExp[2];
            case 5:
                return oneExp[3];
            default:
                return "";
        }

    }

    @Override
    public Class getColumnClass(int columnIndex) {
        if(columnIndex == 0){
            return Boolean.class;
        } else if(columnIndex == 1){
            return String.class;
        } else {
            return Integer.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }


}
