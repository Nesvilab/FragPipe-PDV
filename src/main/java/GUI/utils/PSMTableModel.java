package GUI.utils;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.stream.FactoryConfigurationError;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class PSMTableModel extends DefaultTableModel {

    /**
     * Selected item
     */
    private ArrayList<ArrayList<Object>> selectedItem;
    /**
     * Spectrum ID list
     */
    private ArrayList<String> spectrumIDList;
    /**
     * Search parameters
     */
    private SearchParameters searchParameters;
    /**
     * Extra parameters
     */
    private ArrayList<String> scoreName;
    /**
     * Decimal format
     */
    private DecimalFormat df = new DecimalFormat("#.0000");
    /**
     * Spectrum key and it's selected boolean
     */
    private HashMap<String, Boolean> spectrumKeyToSelected;
    /**
     * Mapped spectrum Index
     */
    private ArrayList<String> mappedSpectrumIndex = new ArrayList<>();

    /**
     * Empty constructor
     */
    public PSMTableModel(){}

    public PSMTableModel(HashMap<String, Boolean> spectrumKeyToSelected, ArrayList<String> scoreName, SearchParameters searchParameters){
        this.spectrumKeyToSelected =spectrumKeyToSelected;
        this.scoreName = scoreName;
        this.searchParameters = searchParameters;
    }

    /**
     * Update the table
     * @param selectedItem Selected item
     * @param spectrumIDList Spectrum ID list
     * @param spectrumKeyToSelected Spectrum key to selected
     */
    public void updateTable(ArrayList<ArrayList<Object>> selectedItem, ArrayList<String> spectrumIDList, HashMap<String, Boolean> spectrumKeyToSelected, ArrayList<String> mappedSpectrumIndex){
        this.selectedItem = selectedItem;
        this.spectrumIDList = spectrumIDList;
        this.spectrumKeyToSelected = spectrumKeyToSelected;
        this.mappedSpectrumIndex = mappedSpectrumIndex;
    }

    @Override
    public int getRowCount() {
        if (spectrumIDList != null) {
            return spectrumIDList.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        if(scoreName != null) {
            return scoreName.size()+8;
        } else {
            return 8;
        }
    }

    @Override
    public String getColumnName(int column) {

        if(column == 0){
            return "Selected";
        }else if(column == 1){
            return "Key";
        }else if(column == 2){
            return "Experiment";
        }else if(column == 3){
            return "Title";
        }else if(column == 4){
            return "MappedProtein";
        }else if(column == 5){
            return "Sequence";
        }else if(column == 6){
            return "Charge";
        }else if(column == 7){
            return "m/z";
        }for(int index = 0; index < scoreName.size(); index++){
            int newColumn = index + 8;
            if(column == newColumn){
                return scoreName.get(index);
            }
        }

        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            if(row < spectrumIDList.size()) {
                String spectrumIndex = spectrumIDList.get(row);

                ArrayList<Object> oneItem = selectedItem.get(row);

                SpectrumMatch spectrumMatch = (SpectrumMatch) oneItem.get(0);

                String matchKey = spectrumMatch.getKey();

                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

                String spectrumTitle = matchKey.split("_cus_")[1];

                if(column == 0){
                    return spectrumKeyToSelected.getOrDefault(spectrumIndex, false);
                }
                if(column == 1) {
                    return spectrumIndex;
                }
                if(column == 2) {
                    return spectrumIndex.split(":\\|")[1].replace("_Dash_", "-");
                }
                if(column == 3) {
                    if (spectrumTitle == null) {
                        return "No Title";
                    }
                    return spectrumTitle;
                }
                if(column == 4) {
                    if (mappedSpectrumIndex.contains(spectrumIndex)) {
                        return "MappedProtein";
                    }
                    return " ";
                }
                if(column == 5) {
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        return peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false).replaceAll(" ", "&nbsp;");
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if(column == 6) {
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        return  spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if(column == 7) {
                    Double mz;

                    try{
                        mz = Double.valueOf(df.format(oneItem.get(1)));

                    } catch (Exception e){
                        mz = -0.0;
                    }

                    return mz;
                }
                for(int index = 0; index < scoreName.size(); index++){
                    int newColumn = index + 8;
                    if(column == newColumn){
                        Object object = oneItem.get(index+2);
                        if(object.getClass() == String.class){
                            return object;
                        } else if (object.getClass() == Integer.class){
                            return Double.valueOf((Integer)object);
                        } else if (object.getClass() == Double.class){
                            return object;
                        }
                    }
                }

            }
            return null;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Table not instantiated.\nError: " + e.toString(),
                    "Display Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new IllegalArgumentException("Table not instantiated.");
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex){
            case 0:
                return Boolean.class;
            case 7:
                return Double.class;
            case 6:
                return Integer.class;
            default:
                for (int i = 0; i < getRowCount(); i++) {
                    if (getValueAt(i, columnIndex) != null && !getColumnName(columnIndex).contains("BestLocalization")) {
                        return getValueAt(i, columnIndex).getClass();
                    } else {
                        return String.class;
                    }
                }
        }
        return String.class;
    }
}
