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
     * Latest manifest file
     */
    private File latestManiFestFile;
    /**
     * DIA-Umpire indicator
     */
    private Boolean isDIAUmpire = false;
    /**
     * Experiment information
     */
    private ArrayList<String> expInformation = new ArrayList<>();
    /**
     * DDA file dict
     */
    private HashMap<String, ArrayList<String>> ddaSpectrumFiles = new HashMap<>();
    /**
     * DIA file dict
     */
    private HashMap<String, ArrayList<String>> diaSpectrumFiles = new HashMap<>();
    /**
     * Spectrum file types
     */
    private HashMap<String, String> spectrumFileTypes = new HashMap<>();
    /**
     * Parent class
     */
    private GUIMainClass guiMainClass;
    /**
     * Database connection
     */
    private SQLiteConnection sqliteConnection;
    /**
     * Spectrum file lists
     */
    private HashMap<String, String> spectrumFileMap = new HashMap<>();
    /**
     *
     */
    private ArrayList<String> spectrumFileOrder = new ArrayList<>();
    /**
     * Results dict
     */
    private HashMap<String, ArrayList<File>> resultsDict = new HashMap<>();
    /**
     * Index to name
     */
    private HashMap<Integer, String> proteinIndexToName = new HashMap<>();
    /**
     * Index to name
     */
    private HashMap<Integer, String> psmIndexToName = new HashMap<>();
    /**
     *
     */
    private HashMap<String, String> proteinSeqMap = new HashMap<>();
    /**
     *
     */
    private HashMap<String, PredictionEntry> predictionEntryHashMap = new HashMap<>();
    /**
     *
     */
    private Boolean hasPredictionSpectra = false;
    /**
     *
     */
    private ArrayList<String> expNumList = new ArrayList<>();
    /**
     * PSM result file column index
     */
    private int proteinIndex = -1;
    /**
     * Protein result file column index
     */
    private int spectrumIndex = -1, peptideSequenceIndex = -1, chargeIndex = -1, caculatedMZIndex = -1, observedMZIndex = -1, assignenModIndex = -1;
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();;
    /**
     *
     */
    private ArrayList<String> mgfFiles = new ArrayList<>();
    /**
     * Instance to save mzML files
     */
    private HashMap<String, ScanCollectionDefault> scansFileHashMap = new HashMap<>(999999);
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

    /**
     *
     * @param guiMainClass
     * @param resultsFolder
     */
    public ImportData(GUIMainClass guiMainClass, File resultsFolder, int threadsNumber, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.guiMainClass = guiMainClass;
        this.resultsFolder = resultsFolder;
        this.threadsNumber = threadsNumber;
        this.progressDialog = progressDialog;

        try {

            String manifestFile = versionCheck();

            if (manifestFile != null){
                goThroughFolder();

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
                    getTableIndexes();
                    initialDB();
                    sqliteConnection.setPSMScoreNum(psmIndexToName.size());
                    sqliteConnection.setProteinScoreNum(proteinIndexToName.size());
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

    private String versionCheck() {
        ArrayList<String> allManiFile = new ArrayList<>();

        for(File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())) {

//            if (eachFileInMax.getName().endsWith("manifest")) {
//                allManiFile.add(eachFileInMax.getAbsolutePath());
//            } /// For old version test only

            if (eachFileInMax.getName().equals("fragpipe-files.fp-manifest")) {
                allManiFile.add(eachFileInMax.getAbsolutePath());
            }
            if (eachFileInMax.getName().equals("umpire-se.params")){
                isDIAUmpire = true;
            }
        }

        if (allManiFile.size() == 0){
            return null;
        } else {
            allManiFile.sort(Collections.reverseOrder());
            latestManiFestFile = new File(allManiFile.get(0));
            return allManiFile.get(0);
        }
    }

    private Boolean checkFileOpen(File eachFile){
        if (Files.exists(eachFile.toPath()) && Files.isRegularFile(eachFile.toPath()) && Files.isReadable(eachFile.toPath())){
            return true;
        } else {
            return false;
        }
    }

    private void goThroughFolder() throws IOException {
        processManifestFile(latestManiFestFile);

        if (expInformation.contains("inner_defined_empty_exp")){
            expNumList.add("1");

            resultsDict.put("1", new ArrayList<File>() {{
                add(new File(resultsFolder.getAbsolutePath() + "/protein.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/psm.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/peptide.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/protein.fas"));
            }});

            if (new File(resultsFolder.getAbsolutePath() + "/spectraRT.predicted.bin").exists()){
                hasPredictionSpectra = true;
//                DiannSpeclibReader dslr = new DiannSpeclibReader(resultsFolder.getAbsolutePath() +  "/spectraRT.predicted.bin");
//                predictionEntryHashMap = dslr.getPreds();
            }

        } else {
            for(File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())){
                if(expInformation.contains(eachFileInMax.getName())) {

                    String expName = eachFileInMax.getName().replace("-", "_Dash_");

                    expNumList.add(expName);
                    resultsDict.put(expName, new ArrayList<File>() {{
                        add(new File(eachFileInMax.getAbsolutePath() + "/protein.tsv"));
                        add(new File(eachFileInMax.getAbsolutePath() + "/psm.tsv"));
                        add(new File(eachFileInMax.getAbsolutePath() + "/peptide.tsv"));
                        add(new File(eachFileInMax.getAbsolutePath() + "/protein.fas"));
                    }});

                    if (new File(eachFileInMax.getAbsolutePath() + "/spectraRT.predicted.bin").exists()){
                        hasPredictionSpectra = true;
//                        DiannSpeclibReader dslr = new DiannSpeclibReader(eachFileInMax.getAbsolutePath() +  "/spectraRT.predicted.bin");
//
//                        predictionEntryHashMap.putAll(dslr.getPreds());
                    }

                }
            }
        }
    }

    private void processManifestFile(File mainFestFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(mainFestFile));

        String line;
        String expName;
        String[] lineSplit;
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        while ((line = bufferedReader.readLine()) != null) {
            lineSplit = line.split("\t");

            if (lineSplit[0].endsWith(".mgf")){
                String[] fileArr = lineSplit[0].split(pattern);
                spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.mgf")[0], lineSplit[0]);
            } else if (lineSplit[0].endsWith(".mzML")){
                String[] fileArr = lineSplit[0].split(pattern);
                if (lineSplit[0].startsWith("./") || lineSplit[0].startsWith("../")){
                    lineSplit[0] = resultsFolder.getAbsolutePath() + System.getProperty("file.separator") + lineSplit[0].replace("/", System.getProperty("file.separator"));
                } else if (lineSplit[0].startsWith(".\\")|| lineSplit[0].startsWith("..\\")){
                    lineSplit[0] = resultsFolder.getAbsolutePath() + System.getProperty("file.separator") + lineSplit[0].replace("\\", System.getProperty("file.separator"));
                }

                spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.mzML")[0], lineSplit[0]);
            } else if (lineSplit[0].endsWith(".mzml")){
                String[] fileArr = lineSplit[0].split(pattern);
                if (lineSplit[0].startsWith("./")){
                    lineSplit[0] = resultsFolder.getAbsolutePath() + lineSplit[0].replace("./", System.getProperty("file.separator"));
                } else if (lineSplit[0].startsWith(".\\")){
                    lineSplit[0] = resultsFolder.getAbsolutePath() + lineSplit[0].replace(".\\", System.getProperty("file.separator"));
                }

                spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.mzml")[0], lineSplit[0]);
            } else if (lineSplit[0].endsWith(".raw")){
                String[] fileArr = lineSplit[0].split(pattern);
                if (new File(lineSplit[0].replace(".raw", "_uncalibrated.mzml")).exists()){
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.raw")[0], lineSplit[0].replace(".raw", "_uncalibrated.mzml"));
                } else {
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.raw")[0], lineSplit[0].replace(".raw", "_calibrated.mzml"));
                }

            } else if (lineSplit[0].endsWith(".d")){
                String[] fileArr = lineSplit[0].split(pattern);
                if (new File(lineSplit[0].replace(".d", "_uncalibrated.mzml")).exists()){
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0], lineSplit[0].replace(".d", "_uncalibrated.mzml"));
                } else {
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0], lineSplit[0].replace(".d", "_calibrated.mzml"));
                }
            }

            if (Objects.equals(lineSplit[1], "") || Objects.equals(lineSplit[3], "DIA")){
                expName = "inner_defined_empty_exp";
            } else {
                if (Objects.equals(lineSplit[2], "")) {
                    expName = lineSplit[1];
                } else {
                    expName = lineSplit[1] + "_" + lineSplit[2];
                }
            }
            if (expInformation.contains(expName)){
                if (Objects.equals(expName, "inner_defined_empty_exp")){
                    expName = "1";
                }
                if (Objects.equals(lineSplit[3], "DDA")) {
                    ddaSpectrumFiles.get(expName).add(lineSplit[0]);

                } else {
                    if (!lineSplit[0].endsWith(".d")){
                        if (!isDIAUmpire){
                            ddaSpectrumFiles.get(expName).add(lineSplit[0]);
                        } else {
                            String mzmlName = lineSplit[0].split(pattern)[lineSplit[0].split(pattern).length - 1].split("\\.")[0];
                            for (File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())){
                                if (eachFileInMax.getName().startsWith(mzmlName + "_") && eachFileInMax.getName().endsWith("mzML")){
                                    diaSpectrumFiles.get(expName).add(eachFileInMax.getAbsolutePath());
                                    String[] fileArr = eachFileInMax.getAbsolutePath().split(pattern);
                                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.mzML")[0], eachFileInMax.getAbsolutePath());
                                }
                            }
                        }
                    }else {
                        String[] fileArr = lineSplit[0].split(pattern);
                        spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0]+"_centric", lineSplit[0]);
                    }
                }

            } else {
                expInformation.add(expName);
                if (Objects.equals(expName, "inner_defined_empty_exp")){
                    expName = "1";
                }
                ddaSpectrumFiles.put(expName, new ArrayList<>());
                diaSpectrumFiles.put(expName, new ArrayList<>());
                if (Objects.equals(lineSplit[3], "DDA")) {
                    ddaSpectrumFiles.get(expName).add(lineSplit[0]);
                } else {
                    if (!lineSplit[0].endsWith(".d")){
                        if (!isDIAUmpire){
                            ddaSpectrumFiles.get(expName).add(lineSplit[0]);
                        } else {
                            String mzmlName = lineSplit[0].split(pattern)[lineSplit[0].split(pattern).length - 1].split("\\.")[0];
                            for (File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())){
                                if (eachFileInMax.getName().startsWith(mzmlName + "_") && eachFileInMax.getName().endsWith("mzML")){
                                    diaSpectrumFiles.get(expName).add(eachFileInMax.getAbsolutePath());
                                    String[] fileArr = eachFileInMax.getAbsolutePath().split(pattern);
                                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.mzML")[0], eachFileInMax.getAbsolutePath());
                                }
                            }
                        }
                    }else {
                        String[] fileArr = lineSplit[0].split(pattern);
                        spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0]+"_centric", lineSplit[0]);
                    }
                }
            }
        }
        bufferedReader.close();
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
            expNumList = (ArrayList<String>) convertBackByte(rs1.getBytes(2));
            psmIndexToName = (HashMap<Integer, String>) convertBackByte(rs1.getBytes(3));
            proteinIndexToName = (HashMap<Integer, String>) convertBackByte(rs1.getBytes(4));
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

        sqliteConnection.setPSMScoreNum(psmIndexToName.size());
        sqliteConnection.setProteinScoreNum(proteinIndexToName.size());

        guiMainClass.searchButton.setToolTipText("Find items");
        guiMainClass.searchItemTextField.setToolTipText("Find items");

//        processSpectralFilesBack(resultsDict.keySet());
    }

    private void processTable() throws SQLException, IOException, ClassNotFoundException {

        Connection connection = sqliteConnection.getConnection();

        connection.setAutoCommit(false);

        for (String expNum : resultsDict.keySet()){
            File oneProteinTable = resultsDict.get(expNum).get(0);
            File onePSMTable = resultsDict.get(expNum).get(1);
            File onePeptideTable = resultsDict.get(expNum).get(2);

            if (checkFileOpen(oneProteinTable) && checkFileOpen(onePSMTable) && checkFileOpen(onePeptideTable)){
                addData(expNum, oneProteinTable, onePSMTable, onePeptideTable, connection);
            } else {
                progressDialog.setRunFinished();
                if (!checkFileOpen(oneProteinTable)){
                    JOptionPane.showMessageDialog(
                            null, "The protein.tsv are occupied by other programs or unavailable now.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                }
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

    private void addProteinData(String expNum, File oneProteinTable, Connection connection) throws SQLException, IOException {
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = null;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder(" VALUES(?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();
        ArrayList<String> proteinList = new ArrayList<>();

        int countFirst = 0;
        for (Integer index : proteinIndexToName.keySet()){
            countFirst ++;

            String oneName = proteinIndexToName.get(index);
            if (Character.isDigit(oneName.charAt(0))){
                oneName = "Str_" +oneName;
            }

            addQuery.append(", ").append(oneName).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(proteinIndexToName.get(index), 1+countFirst);
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

            protein = lineSplit[proteinIndex]+ ":|" + expNum;
            proteinList.add(protein);

            if (proteinCount == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            preparedStatement.setString(1, protein);

            for (Integer index : proteinIndexToName.keySet()){
                String name = proteinIndexToName.get(index);
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

                proteinList = new ArrayList<>();
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

    private void addData(String expNum, File oneProteinTable, File onePSMTable, File onePeptideTable, Connection connection)
            throws SQLException, IOException, ClassNotFoundException {

//        processSpectralFiles(expNum);

        addProteinData(expNum, oneProteinTable, connection);

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = null;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder(" VALUES(?,?,?,?,?,?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();

        int countFirst = 0;
        for (Integer index : psmIndexToName.keySet()){
            countFirst ++;
            String oneName = psmIndexToName.get(index);
            if (Character.isDigit(oneName.charAt(0))){
                oneName = "Str_" +oneName;
            }

            addQuery.append(", ").append(oneName).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(psmIndexToName.get(index), 6+countFirst);
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
            spectrumNewTitle = lineSplit[spectrumIndex];
            for (String eachItem : spectrumNewTitle.split("\\.")){
                if (isNumeric(eachItem)){
                    spectrumOldTitle = spectrumOldTitle + Integer.valueOf(eachItem) + ".";
                } else {
                    spectrumOldTitle = spectrumOldTitle + eachItem + ".";
                }
            }
            spectrumOldTitle = spectrumOldTitle.substring(0, spectrumOldTitle.length() - 1);
            spectrumFile = spectrumNewTitle.split("\\.")[0];

            chargeValue = Integer.parseInt(lineSplit[chargeIndex]);
            peptideSequence = lineSplit[peptideSequenceIndex];
            calculatedMZ = Double.valueOf(lineSplit[caculatedMZIndex]);
            assignedMod = lineSplit[assignenModIndex];

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

            for (Integer index : psmIndexToName.keySet()){
                String name = psmIndexToName.get(index);
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

        byte[] bos1 = convertByte(expNumList);
        byte[] bos2 = convertByte(psmIndexToName);
        byte[] bos3 = convertByte(proteinIndexToName);
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

    private MSnSpectrum getSpectrum(String spectrumTitle) throws MzMLUnmarshallerException, IOException, FileParsingException {
        MSnSpectrum currentSpectrum = null;

        String spectrumFileName = spectrumTitle.split("\\.")[0];
        if (Objects.equals(spectrumFileTypes.get(spectrumFileName), "mgf")){
            currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName + ".mgf", spectrumTitle);
        } else if (Objects.equals(spectrumFileTypes.get(spectrumFileName), "_calibrated.mgf")){
            currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName + "_calibrated.mgf", spectrumTitle);
        } else if (Objects.equals(spectrumFileTypes.get(spectrumFileName), "_uncalibrated.mgf")){
            currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName + "_uncalibrated.mgf", spectrumTitle);
        } else if (Objects.equals(spectrumFileTypes.get(spectrumFileName), "mzml")){

            ScanCollectionDefault currentNum2scan  = scansFileHashMap.get(spectrumFileName);
            int scanNum = Integer.parseInt(spectrumTitle.split("\\.")[1]);
            IScan iScan = currentNum2scan.getScanByNum(scanNum);
            ISpectrum iSpectrum = iScan.fetchSpectrum();

            Charge charge = new Charge(1, Integer.parseInt(spectrumTitle.split("\\.")[3]));
            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(charge);

            Double precursorInt = 0.0;
            Double precursorMz = 0.0;
            PrecursorInfo precursorInfo = iScan.getPrecursor();
            if (precursorInfo.getIntensity() != null){
                precursorInt = precursorInfo.getIntensity();
            }
            if (precursorInfo.getMzTargetMono() != null){
                precursorMz = precursorInfo.getMzTargetMono();
            } else {
                precursorMz = precursorInfo.getMzTarget();
            }

            Precursor precursor = new Precursor(iScan.getRt(), precursorMz, precursorInt, charges);

            double[] mzs = iSpectrum.getMZs();
            double[] ins = iSpectrum.getIntensities();
            HashMap<Double, Peak> peakHashMap = new HashMap<>();
            for (int i = 0; i<mzs.length; i++){
                Peak peak = new Peak(mzs[i], ins[i]);
                peakHashMap.put(mzs[i], peak);
            }

            currentSpectrum = new MSnSpectrum(2, precursor, spectrumTitle.split("\\.")[1], peakHashMap, spectrumFileName);
        }

        return currentSpectrum;
    }

    private void getTableIndexes() throws IOException {
        File oneProteinTable = resultsDict.get(resultsDict.keySet().toArray()[0]).get(0);
        File onePSMTable = resultsDict.get(resultsDict.keySet().toArray()[0]).get(1);

        BufferedReader pSMBufferedReader = new BufferedReader(new FileReader(onePSMTable));
        String[] pSMHeaders = pSMBufferedReader.readLine().trim().split("\t");

        HashMap<String, Integer> psmColumnNames = new HashMap<>(); // Some results have duplicated column names
        HashMap<String, Integer> proteinColumnNames = new HashMap<>();

        for (int i = 0; i < pSMHeaders.length; i++){

            String header = pSMHeaders[i];

            if (header.equalsIgnoreCase("Spectrum")) {
                spectrumIndex = i;
            } else if (header.equalsIgnoreCase("Peptide")) {
                peptideSequenceIndex = i;
            } else if (header.equalsIgnoreCase("Charge")) {
                chargeIndex = i;
            } else if (header.equalsIgnoreCase("Calculated M/Z")) {
                caculatedMZIndex = i;
            } else if (header.equalsIgnoreCase("Observed M/Z")) {
                String columnName = header.trim().replace(" ", "");
                if (columnName.matches(".*\\d+.*")){

                    columnName = "'" + columnName + "'";
                }
                columnName = columnName.replaceAll("[^a-zA-Z0-9]", "");
                psmIndexToName.put(i, columnName);
                observedMZIndex = i;
            } else if (header.equalsIgnoreCase("Assigned Modifications")) {
                psmIndexToName.put(i, header.trim().replace(" ", ""));
                assignenModIndex = i;
            } else {
                if (!header.equals("NA")){
                    String columnName = header.trim().replace(" ", "");
                    if (columnName.matches(".*\\d+.*")){

                        columnName = "'" + columnName + "'";
                    }
                    columnName = columnName.replaceAll("[^a-zA-Z0-9]", "");
                    if (psmColumnNames.containsKey(columnName)){
                        int colCount = psmColumnNames.get(columnName);
                        columnName = columnName + "_" + ( colCount + 1);
                        psmColumnNames.put(columnName, colCount + 1);
                    } else {
                        psmColumnNames.put(columnName, 0);
                    }
                    psmIndexToName.put(i, columnName);
                }
            }
        }

        BufferedReader proteinBufferedReader = new BufferedReader(new FileReader(oneProteinTable));
        String[] proteinHeaders = proteinBufferedReader.readLine().trim().split("\t");

        for (int i = 0; i < proteinHeaders.length; i++) {

            String header = proteinHeaders[i];

            if (header.equalsIgnoreCase("Protein")) {
                proteinIndex = i;
            } else {
                String columnName = header.trim().replace(" ", "");
                if (columnName.matches(".*\\d+.*")){

                    columnName = "'" + columnName + "'";
                }
                columnName = columnName.replaceAll("[^a-zA-Z0-9]", "");
                if (columnName.equals("Group")){
                    columnName = "ProteinGroup";
                }

                if (!columnName.equals("NA")){
                    if (proteinColumnNames.containsKey(columnName)){
                        int colCount = proteinColumnNames.get(columnName);
                        columnName = columnName + "_" + (colCount + 1);
                        proteinColumnNames.put(columnName, colCount + 1);
                    } else {
                        proteinColumnNames.put(columnName, 0);
                    }
                    proteinIndexToName.put(i, columnName);
                }
            }
        }

        pSMBufferedReader.close();
        proteinBufferedReader.close();

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

        for (String each_exp : resultsDict.keySet()){
            File proteinSeqFile = resultsDict.get(each_exp).get(3);
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

        for (Integer index : proteinIndexToName.keySet()){
            scoreName.add(proteinIndexToName.get(index));
        }
        return scoreName;
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getPSMScoreName(){

        ArrayList<String> scoreName = new ArrayList<>();

        for (Integer index : psmIndexToName.keySet()){
            scoreName.add(psmIndexToName.get(index));
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

    public ArrayList<String> getExpNumList(){return expNumList;}

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

    public HashMap<String, ScanCollectionDefault> getScansFileHashMap() {
        return scansFileHashMap;
    }

    public HashMap<String, String> getSpectrumFileTypes() {
        return spectrumFileTypes;
    }

    public Boolean getHasPredictionSpectra() {
        return hasPredictionSpectra;
    }

    public ArrayList<String> getExpInformation() {
        return expInformation;
    }

    public HashMap<String, PredictionEntry> getPredictionEntryHashMap() {
        return predictionEntryHashMap;
    }

    public ArrayList<String> getSpectrumFileOrder() {
        return spectrumFileOrder;
    }

    public HashMap<String, String> getSpectrumFileMap(){
        return spectrumFileMap;
    }
}
