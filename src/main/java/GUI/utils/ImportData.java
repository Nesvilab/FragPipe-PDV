package GUI.utils;

import GUI.DB.SQLiteConnection;
import GUI.GUIMainClass;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.diann.DiannSpeclibReader;
import umich.ms.fileio.filetypes.diann.PredictionEntry;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import javax.sql.rowset.serial.SerialBlob;
import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNumeric;

public class ImportData {

    /**
     * Results folder
     */
    private File resultsFolder;
    /**
     * Threads number
     */
    private int threadsNumber = 1;

    /**
     * Parent class
     */
    private GUIMainClass guiMainClass;
    /**
     * Database connection
     */
    private SQLiteConnection sqliteConnection;
    /**
     *
     */
    private ArrayList<String> spectrumFileOrder = new ArrayList<>();


    /**
     *
     */
    private HashMap<String, String> proteinSeqMap = new HashMap<>();
    /**
     *
     */
    private HashMap<String, PredictionEntry> predictionEntryHashMap = new HashMap<>();
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * All modifications name in result file
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     *
     */
    private HashMap<String, Integer[]> experimentInfo = new HashMap<>();
    /**
     * Pattern
     */
    private Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
    public boolean runWOProtein = false;

    private ResultProcessor resultProcessor;

    /**
     *
     * @param guiMainClass
     * @param resultsFolder
     */
    public ImportData(GUIMainClass guiMainClass, File resultsFolder, int threadsNumber, ProgressDialogX progressDialog) {
        this.guiMainClass = guiMainClass;
        this.resultsFolder = resultsFolder;
        this.threadsNumber = threadsNumber;
        this.progressDialog = progressDialog;

        this.resultProcessor = new ResultProcessor(resultsFolder,  progressDialog);

        try {

            if (resultProcessor.manifestFile != null){

                if (new File(resultsFolder.getAbsolutePath() + "/FP-PDV.db").exists()){
                    try {
                        importExistDB(resultsFolder.getAbsolutePath() + "/FP-PDV.db");
                    } catch (Exception e){
                        JOptionPane.showMessageDialog(
                                null, e.getMessage(),
                                "Loading DB file error", JOptionPane.ERROR_MESSAGE);
                        progressDialog.setRunFinished();
                    }

                } else {
                    getProteinSeq();
                    initialDB();
                    sqliteConnection.setPSMScoreNum(resultProcessor.psmIndexToName.size());
                    sqliteConnection.setProteinScoreNum(resultProcessor.proteinIndexToName.size());
                    processTable();
                    //cleanData();
                }
            } else {

                JOptionPane.showMessageDialog(
                        null, "There is no valid manifest file in your result folder.",
                        "Result files error", JOptionPane.ERROR_MESSAGE);
                progressDialog.setRunFinished();

            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null, e.getMessage(),
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
            e.printStackTrace();
        }

    }


    private Boolean checkFileOpen(File eachFile){
        if (Files.exists(eachFile.toPath()) && Files.isRegularFile(eachFile.toPath()) && Files.isReadable(eachFile.toPath())){
            return true;
        } else {
            return false;
        }
    }

    private void initialDB() throws SQLException, ClassNotFoundException {
        String dbName = resultsFolder.getAbsolutePath() + "/FP-PDV.db";
        guiMainClass.databasePath = dbName;

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqliteConnection = new SQLiteConnection(dbName);
        sqliteConnection.setParentGUI(guiMainClass);
    }

    private void importExistDB(String dbName) throws SQLException, ClassNotFoundException, IOException {

        guiMainClass.databasePath = dbName;

        sqliteConnection = new SQLiteConnection(dbName);
        sqliteConnection.setParentGUI(guiMainClass);
        Connection connection = sqliteConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs1;
        String query1 = "SELECT * FROM ExperimentInfo";
        rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            resultProcessor.expNumList = (ArrayList<String>) convertBackByte(rs1.getBytes(2));
            resultProcessor.psmIndexToName = (HashMap<Integer, String>) convertBackByte(rs1.getBytes(3));
            resultProcessor.proteinIndexToName = (HashMap<Integer, String>) convertBackByte(rs1.getBytes(4));
            proteinSeqMap = (HashMap<String, String>) convertBackByte(rs1.getBytes(5));
            experimentInfo = (HashMap<String, Integer[]>) convertBackByte(rs1.getBytes(6));
            allModifications = (ArrayList<String>) convertBackByte(rs1.getBytes(7));
            spectrumFileOrder = (ArrayList<String>) convertBackByte(rs1.getBytes(8));
        }

        for (String singleModificationName : allModifications){
            Double modMass = Double.valueOf(singleModificationName.split(" of ")[0]);
            String modAA = singleModificationName.split(" of ")[1];
            ArrayList<String> residues = new ArrayList<>();

            if (modAA.equalsIgnoreCase("n-term")) {
                residues.add(modAA);
                PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modMass, residues);
                ptm.setShortName(String.valueOf(modMass));
                ptmFactory.addUserPTM(ptm);
            } else if (modAA.equalsIgnoreCase("c-term")) {
                residues.add(modAA);
                PTM ptm = new PTM(PTM.MODCP, singleModificationName, modMass, residues);
                ptm.setShortName(String.valueOf(modMass));
                ptmFactory.addUserPTM(ptm);
            } else {
                residues.add(modAA);
                PTM ptm = new PTM(PTM.MODAA, singleModificationName, modMass, residues);
                ptm.setShortName(String.valueOf(modMass));
                if (modAA.equals("T") || modAA.equals("S")){
                    if (modMass < 80.01 && modMass > 79.9){
                        ptm.addNeutralLoss(NeutralLoss.H3PO4);
                    }
                }
                ptmFactory.addUserPTM(ptm);
            }
        }

        if (resultProcessor.psmIndexToName.containsValue("PairedScanNum")){
            resultProcessor.hasPairedScanNum = true;
        }

        sqliteConnection.setPSMScoreNum(resultProcessor.psmIndexToName.size());
        sqliteConnection.setProteinScoreNum(resultProcessor.proteinIndexToName.size());

        guiMainClass.searchButton.setToolTipText("Find items");
        guiMainClass.searchItemTextField.setToolTipText("Find items");

        for (String oneProtein : proteinSeqMap.keySet()){
            if (proteinSeqMap.get(oneProtein).equals("AA")){
                runWOProtein = true;
                break;
            }
        }

//        processSpectralFilesBack(resultsDict.keySet());
    }

    private void processTable() throws SQLException, IOException, ClassNotFoundException {

        Connection connection = sqliteConnection.getConnection();

        connection.setAutoCommit(false);

        for (String expNum : resultProcessor.resultsDict.keySet()){
            File oneProteinTable = resultProcessor.resultsDict.get(expNum).get(0);
            File onePSMTable = resultProcessor.resultsDict.get(expNum).get(1);
            File onePeptideTable = resultProcessor.resultsDict.get(expNum).get(2);

            runWOProtein = false;
            if (!checkFileOpen(oneProteinTable)){
                JOptionPane.showMessageDialog(
                        null, "The protein.tsv are occupied by other programs or unavailable now.\n" +
                                "Get protein data from psm.tsv instead.",
                        "No available protein.tsv file", JOptionPane.WARNING_MESSAGE);
                runWOProtein = true;
            }

            if (checkFileOpen(onePSMTable) && checkFileOpen(onePeptideTable)){
                addData(expNum, oneProteinTable, onePSMTable, onePeptideTable, connection, runWOProtein);
            } else {
                progressDialog.setRunFinished();

                if (!checkFileOpen(onePSMTable)){
                    JOptionPane.showMessageDialog(
                            null, "The psm.tsv are occupied by other programs or unavailable now.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                }
                if (!checkFileOpen(onePeptideTable)){
                    JOptionPane.showMessageDialog(
                            null, "The peptide.tsv are occupied by other programs or unavailable now.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                }
            }

        }
        addExpData(connection);

        guiMainClass.searchButton.setToolTipText("Find items");
        guiMainClass.searchItemTextField.setToolTipText("Find items");
    }

    private void processExpData(String expNum, File onePeptideTable) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(onePeptideTable));

        String line;
        String[] lineSplit;

        int allCount = 0;
        int modCount = 0;
        int modIndex = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineSplit = line.split("\t");

            if (allCount == 0){
                for (int i = 0; i < lineSplit.length; i++) {

                    String header = lineSplit[i];

                    if (header.equalsIgnoreCase("Assigned Modifications")) {
                        modIndex = i;
                    }
                }
            } else {
                if (lineSplit[modIndex] != null | lineSplit[modIndex] != ""){
                    modCount ++;
                }
            }
            allCount ++;
        }
        experimentInfo.get(expNum)[2] = allCount - 1;
        experimentInfo.get(expNum)[3] = modCount;
    }

    private void addProteinDataWOProtein(String expNum, File onePSMTable, Connection connection) throws SQLException, IOException {
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = null;

        String addValuesQuery = " VALUES(?" + ",?,?)";

        String tableName = "Protein_" + expNum;
        String matchTableQuery = "CREATE TABLE " + tableName + " (Protein Char" + ", PSMList OBJECT(50), MappedPSMList OBJECT(50)" + ", PRIMARY KEY(Protein))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(guiMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "An error occurred while creating table Protein in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table Protein");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO " + tableName + addValuesQuery;

        String line;
        String[] lineSplit;

        String protein;

        int proteinCount = 0;
        int proteinCountRound = 0;

        int psmProteinIndex = -1;
        for (Integer index : resultProcessor.psmIndexToName.keySet()){
            String name = resultProcessor.psmIndexToName.get(index);
            if (name.equalsIgnoreCase("Protein")){
                psmProteinIndex = index;
            }
        }

        Set<String> uniqueProteins = new HashSet<>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
        bufferedReader.readLine();

        while ((line = bufferedReader.readLine()) != null) {
            lineSplit = line.split("\t");

            protein = lineSplit[psmProteinIndex]+ ":|" + expNum;
            uniqueProteins.add(protein);

        }bufferedReader.close();

        for (String oneProtein : uniqueProteins){
            if (proteinCount == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            preparedStatement.setString(1, oneProtein);

            preparedStatement.addBatch();

            proteinCount ++;

            if(proteinCount == 1000){
                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                proteinCount = 0;

                proteinCountRound ++;

            }
            proteinSeqMap.put(oneProtein.split(":\\|")[0], "AA");
        }

        if(proteinCount != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

        }
        Integer[] oneArray = new Integer[4];
        oneArray[0] = proteinCountRound * 1000 + proteinCount;
        experimentInfo.put(expNum, oneArray);
    }

    private void addProteinData(String expNum, File oneProteinTable, Connection connection) throws SQLException, IOException {
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = null;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder(" VALUES(?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();

        int countFirst = 0;
        for (Integer index : resultProcessor.proteinIndexToName.keySet()){
            countFirst ++;

            String oneName = resultProcessor.proteinIndexToName.get(index);
            if (Character.isDigit(oneName.charAt(0))){
                oneName = "Str_" +oneName;
            }

            addQuery.append(", ").append(oneName).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(resultProcessor.proteinIndexToName.get(index), 1+countFirst);
        }
        addQuery.append(", PSMList OBJECT(50), MappedPSMList OBJECT(50)");
        addValuesQuery.append(",?,?)");

        String tableName = "Protein_" + expNum;
        String matchTableQuery = "CREATE TABLE " + tableName + " (Protein Char" + addQuery + ", PRIMARY KEY(Protein))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(guiMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "An error occurred while creating table Protein in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table Protein");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO " + tableName + addValuesQuery;

        String line;
        String[] lineSplit;

        String protein;

        int proteinCount = 0;
        int proteinCountRound = 0;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(oneProteinTable));
        bufferedReader.readLine();

        while ((line = bufferedReader.readLine()) != null) {
            lineSplit = line.split("\t");

            protein = lineSplit[resultProcessor.proteinIndex]+ ":|" + expNum;

            if (proteinCount == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            preparedStatement.setString(1, protein);

            for (Integer index : resultProcessor.proteinIndexToName.keySet()){
                String name = resultProcessor.proteinIndexToName.get(index);
                String value;
                if (index >= lineSplit.length){
                    value = "";
                } else {
                    value = lineSplit[index];
                }
                if (pattern.matcher(value).matches()) {
                    preparedStatement.setDouble(nameToDBIndex.get(name), Double.parseDouble(value));
                } else {
                    preparedStatement.setString(nameToDBIndex.get(name), value);
                }
            }

            preparedStatement.addBatch();

            proteinCount ++;

            if(proteinCount == 1000){
                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                proteinCount = 0;

                proteinCountRound ++;

            }

        }bufferedReader.close();

        if(proteinCount != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

        }
        Integer[] oneArray = new Integer[4];
        oneArray[0] = proteinCountRound * 1000 + proteinCount;
        experimentInfo.put(expNum, oneArray);
    }

    private void addData(String expNum, File oneProteinTable, File onePSMTable, File onePeptideTable, Connection connection,
                         boolean runWOProtein)
            throws SQLException, IOException {

        if (runWOProtein){
            addProteinDataWOProtein(expNum, onePSMTable, connection);
        } else {
            addProteinData(expNum, oneProteinTable, connection);
        }

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = null;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder(" VALUES(?,?,?,?,?,?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();

        int countFirst = 0;
        for (Integer index : resultProcessor.psmIndexToName.keySet()){
            countFirst ++;
            String oneName = resultProcessor.psmIndexToName.get(index);
            if (Character.isDigit(oneName.charAt(0))){
                oneName = "Str_" +oneName;
            }

            addQuery.append(", ").append(oneName).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(resultProcessor.psmIndexToName.get(index), 6+countFirst);
        }
        addValuesQuery.append(")");

        String tableName = "SpectrumMatch_" + expNum;
//        String matchTableQuery = "CREATE TABLE " + tableName + " (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, Match Object, Spectrum Object" + addQuery + ", PRIMARY KEY(PSMIndex))";
        String matchTableQuery = "CREATE TABLE " + tableName + " (PSMIndex INT(10), MZ DOUBLE, Charge INT(10), Title Char, Sequence Char, OldTitle Object" + addQuery + ", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(guiMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "An error occurred while creating table SpectrumMatch in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO " + tableName + addValuesQuery;

        String line;
        String[] lineSplit;
        HashMap<String, ArrayList<String>> proteinIDPSMList = new HashMap<>();
        HashMap<String, ArrayList<String>> mappedProteinIDPSMList = new HashMap<>();

        String spectrumNewTitle;
        String spectrumOldTitle;
        String peptideSequence;
        String spectrumFile;
        String spectrumKey;
        int chargeValue;
        double calculatedMZ;
        String assignedMod;

        int lineCount = 0;
        int count = 0;
        int countRound = 0;

        ObjectOutputStream oos;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
        bufferedReader.readLine();

        while ((line = bufferedReader.readLine()) != null){
            lineSplit = line.split("\t");

            spectrumOldTitle = "";
            spectrumNewTitle = lineSplit[resultProcessor.spectrumIndex];
            for (String eachItem : spectrumNewTitle.split("\\.")){
                if (isNumeric(eachItem)){
                    spectrumOldTitle = spectrumOldTitle + Integer.valueOf(eachItem) + ".";
                } else {
                    spectrumOldTitle = spectrumOldTitle + eachItem + ".";
                }
            }
            spectrumOldTitle = spectrumOldTitle.substring(0, spectrumOldTitle.length() - 1);
            spectrumFile = spectrumNewTitle.split("\\.")[0];

            chargeValue = Integer.parseInt(lineSplit[resultProcessor.chargeIndex]);
            peptideSequence = lineSplit[resultProcessor.peptideSequenceIndex];
            calculatedMZ = Double.valueOf(lineSplit[resultProcessor.caculatedMZIndex]);
            assignedMod = lineSplit[resultProcessor.assignenModIndex];

            if (count == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            if (!spectrumFileOrder.contains(spectrumFile)){
                spectrumFileOrder.add(spectrumFile);
            }

            ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

            if (assignedMod != null && !assignedMod.equals("")) {

                utilitiesModifications = getUtilitiesModifications(assignedMod, peptideSequence);
            }

//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            try {
//                ObjectOutputStream oos = new ObjectOutputStream(bos);
//                try {
//                    oos.writeObject(spectrumMatch);
//                } finally {
//                    oos.close();
//                }
//            } finally {
//                bos.close();
//            }


//            currentSpectrum = getSpectrum(spectrumOldTitle);

//            ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
//            try {
//                ObjectOutputStream oos = new ObjectOutputStream(bos1);
//                try {
//                    oos.writeObject(currentSpectrum);
//                } finally {
//                    oos.close();
//                }
//            } finally {
//                bos1.close();
//            }



//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//
//
//            oos = new ObjectOutputStream(new BufferedOutputStream(bos));

//            byteArray1 = convertByte(spectrumMatch);
//            byteArray2 = convertByte(currentSpectrum);

            spectrumKey = lineCount + ":|" + expNum;
            preparedStatement.setString(1, spectrumKey);
            preparedStatement.setDouble(2, calculatedMZ);
            preparedStatement.setInt(3, chargeValue);
            preparedStatement.setString(4, spectrumNewTitle);
            preparedStatement.setString(5, peptideSequence);
            preparedStatement.setString(6, spectrumOldTitle);
//            preparedStatement.setBytes(6, byteArray1);

//            preparedStatement.setBytes(5, byteArray1);
//            preparedStatement.setBytes(6, byteArray2);

            for (Integer index : resultProcessor.psmIndexToName.keySet()){
                String name = resultProcessor.psmIndexToName.get(index);
                String value;
                if (index >= lineSplit.length){
                    value = "";
                } else {
                    value = lineSplit[index];
                }
                if (value.equals(" ") || value.equals("")){
                    value = " ";
                }
                if (pattern.matcher(value).matches()) {
                    preparedStatement.setDouble(nameToDBIndex.get(name), Double.parseDouble(value));
                } else {
                    preparedStatement.setString(nameToDBIndex.get(name), value);
                }

                if (name.equals("Protein")){
                    if (proteinIDPSMList.containsKey(value)){
                        proteinIDPSMList.get(value).add(lineCount + ":|" + expNum);
                    } else {
                        ArrayList<String> oneList = new ArrayList<>();
                        oneList.add(lineCount + ":|" + expNum);
                        proteinIDPSMList.put(value, oneList);
                    }
                }
                if (name.equals("MappedProteins") && !value.equals(" ")){
                    for (String oneValue : value.split(", ")) {
                        if (mappedProteinIDPSMList.containsKey(oneValue)) {
                            mappedProteinIDPSMList.get(oneValue).add(lineCount + ":|" + expNum);
                        } else {
                            ArrayList<String> oneList = new ArrayList<>();
                            oneList.add(lineCount + ":|" + expNum);
                            mappedProteinIDPSMList.put(oneValue, oneList);
                        }
                    }
                }
            }

            preparedStatement.addBatch();

            count ++;

            if(count == 1000){
                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                count = 0;

                countRound ++;

            }
            lineCount ++;

        }bufferedReader.close();

        if(count != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();
        }

        String updateProteinTable = "UPDATE Protein_"+ expNum + " SET PSMList=? WHERE Protein=?";
        PreparedStatement updateProteinTableState = connection.prepareStatement(updateProteinTable);
        for (String eachPSM : proteinIDPSMList.keySet()){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                oos = new ObjectOutputStream(bos);
                try {
                    oos.writeObject(proteinIDPSMList.get(eachPSM));
                } finally {
                    oos.close();
                }
            } finally {
                bos.close();
            }

            updateProteinTableState.setBytes(1, bos.toByteArray());
            updateProteinTableState.setString(2, eachPSM + ":|" + expNum);
            updateProteinTableState.addBatch();
        }

        updateProteinTableState.executeBatch();
        connection.commit();
        updateProteinTableState.close();

        String updateProteinTable2 = "UPDATE Protein_"+ expNum + " SET MappedPSMList=? WHERE Protein=?";
        PreparedStatement updateProteinTableState2 = connection.prepareStatement(updateProteinTable2);
        for (String eachPSM : mappedProteinIDPSMList.keySet()){
            ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
            try {
                oos = new ObjectOutputStream(bos2);
                try {
                    oos.writeObject(mappedProteinIDPSMList.get(eachPSM));
                } finally {
                    oos.close();
                }
            } finally {
                bos2.close();
            }
            updateProteinTableState2.setBytes(1, bos2.toByteArray());
            updateProteinTableState2.setString(2, eachPSM + ":|" + expNum);
            updateProteinTableState2.addBatch();
        }
        updateProteinTableState2.executeBatch();
        connection.commit();
        updateProteinTableState2.close();

        experimentInfo.get(expNum)[1] = countRound * 1000 + count;
        processExpData(expNum, onePeptideTable);

        ArrayList<String> residues = new ArrayList<>();
        residues.add("N");
        PTM ptm = new PTM(PTM.MODAA, "0.0 of N", 0.0, residues);
        ptm.setShortName("0.0");
        ptmFactory.addUserPTM(ptm);
        ptm = new PTM(PTM.MODAA, "203.079 of N", 406.158, residues);
        ptm.setShortName("203.079");
        ptmFactory.addUserPTM(ptm);
        allModifications.add("203.079 of N");
        allModifications.add("0.0 of N");
    }

    private void addExpData(Connection connection) throws SQLException, IOException {
        Statement statement = connection.createStatement();

        String matchTableQuery = "CREATE TABLE ExperimentInfo (name Char, expNumList OBJECT(50), pSMScore OBJECT(50), proteinScore OBJECT(50), proteinSeq OBJECT(50), experimentInfo OBJECT(50), allModifications OBJECT(50), spectrumNames OBJECT(100))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(guiMainClass, JOptionEditorPane.getJOptionEditorPane(
                            e.getMessage()),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO ExperimentInfo VALUES(?,?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(addDataIntoTable);

        preparedStatement.setString(1, "1");

        byte[] bos1 = convertByte(resultProcessor.expNumList);
        byte[] bos2 = convertByte(resultProcessor.psmIndexToName);
        byte[] bos3 = convertByte(resultProcessor.proteinIndexToName);
        byte[] bos4 = convertByte(proteinSeqMap);
        byte[] bos5 = convertByte(experimentInfo);
        byte[] bos6 = convertByte(allModifications);
        byte[] bos7 = convertByte(spectrumFileOrder);

        preparedStatement.setBytes(2, bos1);
        preparedStatement.setBytes(3, bos2);
        preparedStatement.setBytes(4, bos3);
        preparedStatement.setBytes(5, bos4);
        preparedStatement.setBytes(6, bos5);
        preparedStatement.setBytes(7, bos6);
        preparedStatement.setBytes(8, bos7);

        preparedStatement.execute();
        connection.commit();
        preparedStatement.close();
    }

    private byte[] convertByte(Object input) throws IOException {

//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//
//        FSTObjectOutput out = new FSTObjectOutput(bos);
//
//        out.writeObject(input);
//        out.close();
//
//        return bos.toByteArray();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(input);

        return bos.toByteArray();
    }

    private Object convertBackByte(byte[] input) throws SQLException {
        Object output = null;
        Blob tempBlob = new SerialBlob(input);
        BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
        try {
            ObjectInputStream in = new ObjectInputStream(bis);
            try {
                output = in.readObject();
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
        return output;
    }

    private ArrayList<ModificationMatch> getUtilitiesModifications(String assignedMod, String peptideSequence){

        String modAA;
        Integer position;
        Double modMass;
        String singleModificationName;
        ArrayList<String> residues;
        ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

        for (String eachMod : assignedMod.split(",")) {

            if (eachMod.contains(":") || !eachMod.contains("(")) { //15.9949:Oxidation (Oxidation or Hydroxylation)
                //Do nothing
            } else {

                residues = new ArrayList<>();

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

                    if (!ptmFactory.containsPTM(singleModificationName)) {
                        if (modAA.equalsIgnoreCase("n-term")) {
                            residues.add(modAA);
                            PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modMass, residues);
                            ptm.setShortName(String.valueOf(modMass));
                            ptmFactory.addUserPTM(ptm);
                        } else if (modAA.equalsIgnoreCase("c-term")) {
                            residues.add(modAA);
                            PTM ptm = new PTM(PTM.MODCP, singleModificationName, modMass, residues);
                            ptm.setShortName(String.valueOf(modMass));
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            residues.add(modAA);
                            PTM ptm = new PTM(PTM.MODAA, singleModificationName, modMass, residues);
                            ptm.setShortName(String.valueOf(modMass));
                            if (modAA.equals("T") || modAA.equals("S")){
                                if (modMass < 80.01 && modMass > 79.9){
                                    ptm.addNeutralLoss(NeutralLoss.H3PO4);
                                }
                            }
                            ptmFactory.addUserPTM(ptm);
                        }
                    }

                    if (!allModifications.contains(singleModificationName)) {
                        allModifications.add(singleModificationName);
                    }
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

    private void getProteinSeq() throws IOException {

        for (String each_exp : resultProcessor.resultsDict.keySet()){
            File proteinSeqFile = resultProcessor.resultsDict.get(each_exp).get(3);
            if (!Files.exists(proteinSeqFile.toPath())){
                proteinSeqMap.put("empty", "empty");
                continue;
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(proteinSeqFile));

            String line;
            String currentProtein = "";
            StringBuilder currentProteinSeq = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                // Remove decoy filter
                if (line.startsWith(">")){
                    if (!currentProtein.equals("")){
                        if (!proteinSeqMap.containsKey(currentProtein)){
                            proteinSeqMap.put(currentProtein, currentProteinSeq.toString());
                        }
                    }

                    currentProtein = line.split(" ")[0].substring(1);
                    currentProteinSeq = new StringBuilder();
                } else {
                    currentProteinSeq.append(line);
                }

            }
            bufferedReader.close();
            if (!currentProtein.equals("")){
                if (!proteinSeqMap.containsKey(currentProtein)){
                    proteinSeqMap.put(currentProtein, currentProteinSeq.toString());
                }
            }
        }
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getProteinScoreName(){

        ArrayList<String> scoreName = new ArrayList<>();

        for (Integer index : resultProcessor.proteinIndexToName.keySet()){
            scoreName.add(resultProcessor.proteinIndexToName.get(index));
        }
        return scoreName;
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getPSMScoreName(){

        ArrayList<String> scoreName = new ArrayList<>();

        for (Integer index : resultProcessor.psmIndexToName.keySet()){
            scoreName.add(resultProcessor.psmIndexToName.get(index));
        }
        return scoreName;
    }

    public HashMap<String, String> getProteinSeqMap(){return proteinSeqMap;}

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }

    public ArrayList<String> getExpNumList(){return resultProcessor.expNumList;}

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqliteConnection;
    }

    public HashMap<String, Integer[]> getExperimentInfo() {
        return experimentInfo;
    }

    public Boolean getHasPredictionSpectra() {
        return resultProcessor.hasPredictionSpectra;
    }
    public String getUseDiaNNPrediction() {
        return resultProcessor.predictedFileName;
    }

    public Boolean getHasPairedScanNum() {
        return resultProcessor.hasPairedScanNum;
    }

    public ArrayList<String> getExpInformation() {
        return resultProcessor.expInformation;
    }

    public HashMap<String, PredictionEntry> getPredictionEntryHashMap() {
        return predictionEntryHashMap;
    }

    public ArrayList<String> getSpectrumFileOrder() {
        return spectrumFileOrder;
    }

    public HashMap<String, String> getSpectrumFileMap(){
        return resultProcessor.spectrumFileMap;
    }
}
