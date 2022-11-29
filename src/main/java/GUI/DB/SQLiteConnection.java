package GUI.DB;

import GUI.GUIMainClass;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.sql.rowset.serial.SerialBlob;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * DB class
 * Created by Ken on 5/25/2017.
 */
public class SQLiteConnection {

    /**
     * Database connection
     */
    private Connection connection;
    /**
     * Score num
     */
    private Integer pSMScoreNum;
    /**
     * Score num
     */
    private Integer proteinScoreNum;
    /**
     *
     */
    private GUIMainClass guiMainClass;

    /**
     * Constructor of DB
     * @param name DB name
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public SQLiteConnection(String name) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + name );
    }

    /**
     * Set additional attributes number
     * @param scoreNum Additional attributes number
     */
    public void setPSMScoreNum(Integer scoreNum){
        this.pSMScoreNum = scoreNum;
    }

    /**
     * Set additional attributes number
     * @param scoreNum Additional attributes number
     */
    public void setProteinScoreNum(Integer scoreNum){
        this.proteinScoreNum = scoreNum;
    }

    /**
     * Get DB connection
     * @return Connection
     */
    public Connection getConnection(){
        return connection;
    }

    public void setParentGUI(GUIMainClass guiMainClass){
        this.guiMainClass = guiMainClass;
    }

    /**
     * Close DB connection
     */
    public void closeConnection(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get one SpectrumMach
     * @param spectrumID Spectrum key
     * @return SpectrumMatch
     * @throws SQLException
     */
    public SpectrumMatch getSpectrumMatch(String spectrumID) throws SQLException {

        String[] itemsForMatch = getWhatYouWant("SpectrumMatch", spectrumID, new String[]{"Sequence", "Charge", "Title", "OldTitle", "AssignedModifications"});

        SpectrumMatch spectrumMatch;
        ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();
        if (itemsForMatch[4] != null && !itemsForMatch[4].equals(" ")) {

            utilitiesModifications = getUtilitiesModifications(itemsForMatch[4], itemsForMatch[0]);
        }

        Peptide peptide = new Peptide(itemsForMatch[0], utilitiesModifications);

        PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, Integer.parseInt(itemsForMatch[1])), 0, "*");

        spectrumMatch = new SpectrumMatch(itemsForMatch[2].split("\\.")[0] + "_cus_" + itemsForMatch[3]);

        spectrumMatch.addHit(0, peptideAssumption, false);

        spectrumMatch.setBestPeptideAssumption(peptideAssumption);

        return spectrumMatch;
    }

    private ArrayList<ModificationMatch> getUtilitiesModifications(String assignedMod, String peptideSequence){

        String modAA;
        Integer position;
        Double modMass;
        String singleModificationName;
        ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

        for (String eachMod : assignedMod.split(",")) {

            if (eachMod.contains(":") || !eachMod.contains("(")) { //15.9949:Oxidation (Oxidation or Hydroxylation)
                //Do nothing
            } else {

                modMass = Double.valueOf(eachMod.substring(eachMod.lastIndexOf("(") + 1, eachMod.lastIndexOf(")")));

                if (eachMod.contains("n") || eachMod.toLowerCase().contains("n-term")) { //n(42.0106; new case N-term(42.0106)
                    modAA = "N-term";
                    position = 1;

                } else if (eachMod.contains("c") || eachMod.toLowerCase().contains("c-term")) { //c(42.0106); new case C-term(42.0106)
                    modAA = "C-term";
                    position = peptideSequence.length();

                } else {
                    modAA = eachMod.substring(eachMod.lastIndexOf("(") - 1, eachMod.lastIndexOf("("));

                    position = Integer.valueOf(eachMod.substring(0, eachMod.lastIndexOf("(") - 1).trim());
                }

                if (checkReporter(modMass, modAA) != null){
                    singleModificationName = checkReporter(modMass, modAA);
                } else {
                    singleModificationName = modMass + " of " + modAA;
                }

                utilitiesModifications.add(new ModificationMatch(singleModificationName, true, position));
            }
        }
        return utilitiesModifications;
    }

    private String checkReporter(Double modMass, String modAA){
        if (Objects.equals(modAA, "N-term")){
            // TMT
            if (Math.abs(modMass - 229.1629) <= 0.1){
                return "TMT 10-plex of peptide N-term";
            }
            // iTraQ
            if (Math.abs(modMass - 144.1) <= 0.1){
                return "iTRAQ 4-plex of peptide N-term";
            }
        }
        return null;
    }

    public String[] getWhatYouWant(String typeName, String idName, String[] columnNames) throws SQLException {

        String[] output = new String[columnNames.length];
        String expNum = idName.split(":\\|")[1];

        Statement statement = connection.createStatement();

        String query1 = "SELECT ";
        query1 += String.join(", ", columnNames);
        query1 += " FROM " + typeName + "_" + expNum + " WHERE PSMIndex = '" + idName + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            for (int i =0; i<columnNames.length; i ++){
                output[i] = rs1.getString(i +1);
            }
        }

        return output;
    }

    /**
     * Get one spectral
     * @param spectrumID Spectrum key
     * @return SpectrumMatch
     * @throws SQLException
     */
    public MSnSpectrum getSpectrum(String spectrumID) throws SQLException {

        String expNum = spectrumID.split(":\\|")[1];

        MSnSpectrum mSnSpectrum = null;
        Statement statement = connection.createStatement();
        String query1 = "SELECT Spectrum FROM SpectrumMatch_" + expNum + " WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            Blob tempBlob;
            byte[] bytes = rs1.getBytes(1);
            tempBlob = new SerialBlob(bytes);
            BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(bis);
                try {
                    mSnSpectrum = (MSnSpectrum) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return mSnSpectrum;
    }

    /**
     * Get one spectral
     * @param spectrumID Spectrum key
     * @return SpectrumMatch
     * @throws SQLException
     */
    public String getSpectrumOldTitle(String spectrumID) throws SQLException {

        String expNum = spectrumID.split(":\\|")[1];

        String spectrumOldTitle = null;
        Statement statement = connection.createStatement();
        String query1 = "SELECT OldTitle FROM SpectrumMatch_" + expNum + " WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            spectrumOldTitle = rs1.getString(1);
        }
        return spectrumOldTitle;
    }

    /**
     * Get one ProteinID
     * @param spectrumID Spectrum key
     * @return ProteinID
     * @throws SQLException
     */
    public String getOneProteinID(String spectrumID) throws SQLException {

        String expNum = spectrumID.split(":\\|")[1];

        String proteinID = null;
        Statement statement = connection.createStatement();
        String query1 = "SELECT Protein FROM SpectrumMatch_" + expNum + " WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            proteinID = rs1.getString(1);
        }
        return proteinID;
    }

    /**
     * Get one ProteinID
     * @param spectrumID Spectrum key
     * @return ProteinID
     * @throws SQLException
     */
    public ArrayList<String> getOneProteinIDForMapped(String spectrumID) throws SQLException {

        ArrayList<String> output = new ArrayList<>();
        String expNum = spectrumID.split(":\\|")[1];

        Statement statement = connection.createStatement();
        String query1 = "SELECT MappedProteins FROM SpectrumMatch_"+ expNum + " WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            output.addAll(Arrays.asList(rs1.getString(1).split(", ")));
        }
        return output;
    }

    /**
     * Get one record
     * @param spectrumID Spectrum ID
     * @return ArrayList
     * @throws SQLException
     */
    public ArrayList<Object> getOneSpectrumItem(String spectrumID) throws SQLException {

        ArrayList<Object> oneItem = new ArrayList<>();

        String expNum = spectrumID.split(":\\|")[1];

        Statement statement = connection.createStatement();
        String query1 = "SELECT * FROM SpectrumMatch_" + expNum + " WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){

            oneItem.add(getSpectrumMatch(spectrumID));
            oneItem.add(rs1.getObject(2));

            for (int i = 0; i<pSMScoreNum; i++){
                oneItem.add(rs1.getObject(7+i));
            }
        }

        return oneItem;
    }

    /**
     * Get one record
     * @param proteinID Spectrum ID
     * @return ArrayList
     * @throws SQLException
     */
    public ArrayList<Object> getOneProteinItem(String proteinID) throws SQLException {

        ArrayList<Object> oneItem = new ArrayList<>();

        String expNum = proteinID.split(":\\|")[1];

        Statement statement = connection.createStatement();
        String query1 = "SELECT * FROM Protein_" + expNum + " WHERE Protein = '" + proteinID + "'";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){

            for (int i = 0; i<proteinScoreNum; i++){
                oneItem.add(rs1.getObject(2+i));
            }
        }

        return oneItem;
    }

    public ArrayList<String> getProteinList(ArrayList<String> expNums) throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();
        Statement statement = connection.createStatement();
        for (String oneExp : expNums){

            String query1 = "SELECT * FROM Protein_" + oneExp;
            ResultSet rs1 = null;
            try {
                rs1 = statement.executeQuery(query1);
                while (rs1.next()){
                    selectedIndexList.add(rs1.getString(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return selectedIndexList;
    }

    public ArrayList<String>[] getSpectrumList(ArrayList<String> proteinIDs, ProgressDialogX progressDialog) throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();
        ArrayList<String> mappedProteins = new ArrayList<>();
        ArrayList<String>[] output = new ArrayList[2];
        Statement statement = connection.createStatement();
        for (String oneProtein : proteinIDs){
            String expNum = oneProtein.split(":\\|")[1];
            oneProtein = oneProtein.split(":\\|")[0];
            ResultSet rs1;

            String query2 = "SELECT PSMList, MappedPSMList FROM Protein_" + expNum + " WHERE Protein = '" + oneProtein + ":|" + expNum + "'";
            rs1 = statement.executeQuery(query2);
            byte[] bytes;
            Blob tempBlob;
            byte[] bytes2;
            Blob tempBlob2;
            while (rs1.next()){
                bytes = rs1.getBytes(1);
                bytes2 = rs1.getBytes(2);
                if (bytes != null) {
                    tempBlob = new SerialBlob(bytes);
                    BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
                    try {
                        ObjectInputStream in = new ObjectInputStream(bis);
                        try {
                            selectedIndexList.addAll((ArrayList) in.readObject());
                        } finally {
                            in.close();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (bytes2 != null) {
                    tempBlob2 = new SerialBlob(bytes2);
                    BufferedInputStream bis = new BufferedInputStream(tempBlob2.getBinaryStream());
                    try {
                        ObjectInputStream in = new ObjectInputStream(bis);
                        try {
                            mappedProteins.addAll((ArrayList) in.readObject());
                        } finally {
                            in.close();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            progressDialog.increasePrimaryProgressCounter();
        }

        output[0] = selectedIndexList;
        output[1] = mappedProteins;
        return output;
    }

    public ArrayList<String>[] getSpectrumListOneProtein(String oneProtein) throws SQLException {
        ArrayList<String> mappedProteins = new ArrayList<>();
        ArrayList<String>[] output = new ArrayList[2];
        ArrayList<String> selectedIndexList = new ArrayList<>();
        Statement statement = connection.createStatement();
        String expNum = oneProtein.split(":\\|")[1];
        oneProtein = oneProtein.split(":\\|")[0];

        String query1 = "SELECT PSMList, MappedPSMList FROM Protein_" + expNum + " WHERE Protein = '" + oneProtein + ":|" + expNum + "'";
        ResultSet rs1;
        rs1 = statement.executeQuery(query1);
        byte[] bytes;
        Blob tempBlob;
        byte[] bytes2;
        Blob tempBlob2;
        while (rs1.next()){
            bytes = rs1.getBytes(1);
            bytes2 = rs1.getBytes(2);
            if (bytes != null){
                tempBlob = new SerialBlob(bytes);
                BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
                try {
                    ObjectInputStream in = new ObjectInputStream(bis);
                    try {
                        selectedIndexList.addAll((ArrayList) in.readObject());
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (bytes2 != null) {
                tempBlob2 = new SerialBlob(bytes2);
                BufferedInputStream bis = new BufferedInputStream(tempBlob2.getBinaryStream());
                try {
                    ObjectInputStream in = new ObjectInputStream(bis);
                    try {
                        mappedProteins.addAll((ArrayList) in.readObject());
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        output[0] = selectedIndexList;
        output[1] = mappedProteins;
        return output;
    }

    /**
     * Get all index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String> getAllIndex() throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT PSMIndex FROM SpectrumMatch";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){
            selectedIndexList.add(rs1.getString(1));
        }

        return selectedIndexList;
    }

    /**
     * Get all searched index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String[]> getSelectedPeptideIndex(String peptide, ArrayList<String> expNumList) throws SQLException {
        ArrayList<String[]> selectedIndexList = new ArrayList<>();
        String[] oneString;

        Statement statement = connection.createStatement();
        for (String oneExp : expNumList) {
            String query1 = "SELECT PSMIndex, Protein FROM SpectrumMatch_"+ oneExp + " WHERE Sequence LIKE '%" + peptide + "%'";
            ResultSet rs1 = statement.executeQuery(query1);

            while (rs1.next()){
                oneString = new String[2];
                oneString[0] = rs1.getString(1);
                oneString[1] = rs1.getString(2) + ":|" + oneExp;
                selectedIndexList.add(oneString);
            }
        }

        return selectedIndexList;
    }

    /**
     * Get all searched index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String[]> getSelectedTitleIndex(String title, ArrayList<String> expNumList) throws SQLException {
        ArrayList<String[]> selectedIndexList = new ArrayList<>();
        String[] oneString;

        Statement statement = connection.createStatement();
        for (String oneExp : expNumList){
            String query1 = "SELECT PSMIndex, Protein FROM SpectrumMatch_"+ oneExp + " WHERE Title LIKE '%" + title + "%'";
            ResultSet rs1 = statement.executeQuery(query1);

            while (rs1.next()){
                oneString = new String[2];
                oneString[0] = rs1.getString(1);
                oneString[1] = rs1.getString(2) + ":|" + oneExp;
                selectedIndexList.add(oneString);
            }
        }

        return selectedIndexList;
    }

    /**
     * Get all searched index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String[]> getSelectedProteinIndex(String title, ArrayList<String> expNumList) throws SQLException {
        ArrayList<String[]> selectedIndexList = new ArrayList<>();
        String[] oneString;
        String oneID;
        byte[] bytes;
        Blob tempBlob;

        Statement statement = connection.createStatement();
        for (String oneExp : expNumList){
            String query1 = "SELECT Protein, PSMList, MappedPSMList FROM Protein_"+ oneExp + " WHERE Protein LIKE '%" + title + "%'";
            ResultSet rs1 = statement.executeQuery(query1);

            while (rs1.next()){
                oneID = rs1.getString(1);

                bytes = rs1.getBytes(2);
                if (bytes != null){
                    tempBlob = new SerialBlob(bytes);
                    BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
                    try {
                        try (ObjectInputStream in = new ObjectInputStream(bis)) {
                            for (String eachPSM : ((ArrayList<String>) in.readObject())){
                                oneString = new String[2];
                                oneString[0] = eachPSM;
                                oneString[1] = oneID;
                                selectedIndexList.add(oneString);
                            }
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return selectedIndexList;
    }
}
