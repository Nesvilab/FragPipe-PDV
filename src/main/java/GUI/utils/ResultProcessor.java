package GUI.utils;


import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

public class ResultProcessor {

    /**
     * Results folder
     */
    public File resultsFolder;
    /**
     * Latest manifest file
     */
    public File latestManiFestFile;
    /**
     * DIA-Umpire indicator
     */
    public Boolean isDIAUmpire = false;
    /**
     * Experiment information
     */
    public ArrayList<String> expInformation = new ArrayList<>();
    /**
     * DDA file dict
     */
    public HashMap<String, ArrayList<String>> ddaSpectrumFiles = new HashMap<>();
    /**
     * DIA file dict
     */
    public HashMap<String, ArrayList<String>> diaSpectrumFiles = new HashMap<>();

    /**
     * Spectrum file lists
     */
    public HashMap<String, String> spectrumFileMap = new HashMap<>();
    /**
     * Results dict
     */
    public HashMap<String, ArrayList<File>> resultsDict = new HashMap<>();

    /**
     *
     */
    public Boolean hasPredictionSpectra = false;
    /**
     *
     */
    public Boolean runSpecLib = false;
    public String predictedFileName;
    public Boolean hasPairedScanNum = false;
    /**
     *
     */
    public ArrayList<String> expNumList = new ArrayList<>();

    public String manifestFile;
    /**
     * Index to name
     */
    public HashMap<Integer, String> proteinIndexToName = new HashMap<>();
    /**
     * Index to name
     */
    public HashMap<Integer, String> psmIndexToName = new HashMap<>();
    /**
     * PSM result file column index
     */
    public int proteinIndex = -1;
    public int proteinIDIndex = -1;
    /**
     * Protein result file column index
     */
    public int spectrumIndex = -1, peptideSequenceIndex = -1, chargeIndex = -1, caculatedMZIndex = -1, observedMZIndex = -1, assignenModIndex = -1;


    public ResultProcessor(File resultsFolder, ProgressDialogX progressDialog) {
        this.resultsFolder = resultsFolder;

        try {

            manifestFile = versionCheck();

            if (manifestFile != null){
                goThroughFolder();
                getTableIndexes();

            } else {
                if (progressDialog != null) {
                    JOptionPane.showMessageDialog(
                            null, "There is no valid manifest file in your result folder.",
                            "Result files error", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                } else {
                    System.err.println("No valid manifest file in your result folder.");
                }

            }

        } catch (Exception e) {
            if (progressDialog != null) {JOptionPane.showMessageDialog(
                    null, e.getMessage(),
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                progressDialog.setRunFinished();
                e.printStackTrace();
            } else {
                System.err.println("Error Parsing File");
            }
        }
    }

    private String versionCheck() {
        ArrayList<String> allManiFile = new ArrayList<>();

        for(File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())) {

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

        String workflowFile = "";

        for(File eachFileInMax : Objects.requireNonNull(resultsFolder.listFiles())){
            if (eachFileInMax.getName().equals("fragpipe.workflow")){
                workflowFile = eachFileInMax.getName();
            }
        }
        if (!workflowFile.equals("")){
            processWorkflow(resultsFolder.getAbsolutePath() + "/" + workflowFile);
        }

        processManifestFile(latestManiFestFile);
        processMsBooster();

        if (expInformation.contains("inner_defined_empty_exp")){
            expNumList.add("1");

            resultsDict.put("1", new ArrayList<File>() {{
                add(new File(resultsFolder.getAbsolutePath() + "/protein.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/psm.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/peptide.tsv"));
                add(new File(resultsFolder.getAbsolutePath() + "/protein.fas"));
            }});

//            if (new File(resultsFolder.getAbsolutePath() + "/spectraRT.predicted.bin").exists()){
//                hasPredictionSpectra = true;
//            }

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
//                    if (new File(eachFileInMax.getAbsolutePath() + "/spectraRT.predicted.bin").exists()){
//                        hasPredictionSpectra = true;
//                    }

                }
            }
        }
    }

    private void processMsBooster() throws IOException {
        if (new File(resultsFolder.getAbsolutePath() + "/msbooster_params.txt").exists()){
            BufferedReader bufferedReader = new BufferedReader(new FileReader(resultsFolder.getAbsolutePath() + "/msbooster_params.txt"));
            String line;
            String[] lineSplit;

            while ((line = bufferedReader.readLine()) != null) {
                lineSplit = line.split("=");
                if (lineSplit[0].contains("useSpectra")){
                    if (lineSplit[1].contains("true")){
                        hasPredictionSpectra = true;
                    }
                }
                if (lineSplit[0].contains("spectraPredFile")){
                    String fileSeparator = "/";
                    if (lineSplit[1].contains("\\")){
                        fileSeparator = "\\\\";
                    }
                    predictedFileName = lineSplit[1].split(fileSeparator)[lineSplit[1].split(fileSeparator).length-1];
                }
            }


        } else {
            hasPredictionSpectra = false;
        }
    }

    private void processWorkflow(String workflowFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(workflowFile));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("speclibgen.run-speclibgen")){
                if (Objects.equals(line.split("run-speclibgen=")[1], "false")){
                    runSpecLib = false;
                } else {
                    runSpecLib = true;
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
                if (new File(lineSplit[0].replace(".raw", "_uncalibrated.mzML")).exists()){
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.raw")[0], lineSplit[0].replace(".raw", "_uncalibrated.mzML"));
                } else {
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.raw")[0], lineSplit[0].replace(".raw", "_calibrated.mzML"));
                }

            } else if (lineSplit[0].endsWith(".d") && (Objects.equals(lineSplit[3], "DDA")|| Objects.equals(lineSplit[3], "DDA+"))){
                String[] fileArr = lineSplit[0].split(pattern);
                if (new File(lineSplit[0].replace(".d", "_uncalibrated.mzML")).exists()){
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0], lineSplit[0].replace(".d", "_uncalibrated.mzML"));
                } else {
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0], lineSplit[0].replace(".d", "_calibrated.mzML"));
                }
            } else if (lineSplit[0].endsWith(".d") && Objects.equals(lineSplit[3], "DIA")){
                String[] fileArr = lineSplit[0].split(pattern);
                if (new File(lineSplit[0].replace(".d", "_diatracer_calibrated.mzML")).exists()){
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0]+"_diatracer", lineSplit[0].replace(".d", "_diatracer_calibrated.mzML"));
                } else {
                    spectrumFileMap.put(fileArr[fileArr.length-1].split("\\.d")[0]+"_diatracer", lineSplit[0].replace(".d", "_diatracer.mzML"));
                }
            }

            if (Objects.equals(lineSplit[1], "") || runSpecLib){
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
                    }
                }
            }
        }
        bufferedReader.close();
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
            } else if (header.equalsIgnoreCase("Paired Scan Num")) {
                hasPairedScanNum = true;
                String columnName = header.trim().replace(" ", "");
                psmIndexToName.put(i, columnName);
            } else {
                if (!header.equals("NA")){
                    String columnName = header.trim().replace(" ", "");
                    if(columnName.equals("ProteinID")){
                        proteinIDIndex = i;
                    }
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

        if (Files.exists(oneProteinTable.toPath())) {
            BufferedReader proteinBufferedReader = new BufferedReader(new FileReader(oneProteinTable));
            String[] proteinHeaders = proteinBufferedReader.readLine().trim().split("\t");

            for (int i = 0; i < proteinHeaders.length; i++) {

                String header = proteinHeaders[i];

                if (header.equalsIgnoreCase("Protein")) {
                    proteinIndex = i;
                }else {
                    String columnName = header.trim().replace(" ", "");
                    if (columnName.matches(".*\\d+.*")) {

                        columnName = "'" + columnName + "'";
                    }
                    columnName = columnName.replaceAll("[^a-zA-Z0-9]", "");
                    if (columnName.equals("Group")) {
                        columnName = "ProteinGroup";
                    }

                    if (!columnName.equals("NA")) {
                        if (proteinColumnNames.containsKey(columnName)) {
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
            proteinBufferedReader.close();
        }

        pSMBufferedReader.close();

    }

}
