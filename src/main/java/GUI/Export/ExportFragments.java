package GUI.Export;

import GUI.GUIMainClass;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.bruker.BrukerTdfFileBase;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static com.compomics.util.experiment.biology.Ion.IonType.PEPTIDE_FRAGMENT_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.TAG_FRAGMENT_ION;

public class ExportFragments {


    private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Index to name
     */
    private HashMap<Integer, String> proteinIndexToName = new HashMap<>();
    /**
     * Index to name
     */
    private HashMap<Integer, String> psmIndexToName = new HashMap<>();
    private int proteinIndex = -1;
    /**
     * Protein result file column index
     */
    private int spectrumIndex = -1, peptideSequenceIndex = -1, chargeIndex = -1, caculatedMZIndex = -1, observedMZIndex = -1, assignenModIndex = -1;
    private Boolean hasPairedScanNum = false;
    private File resultsFolder;
    private File latestManiFestFile;
    private ArrayList<String> expInformation = new ArrayList<>();
    private ArrayList<String> expNumList = new ArrayList<>();
    private HashMap<String, ArrayList<File>> resultsDict = new HashMap<>();
    private HashMap<String, String> spectrumFileMap = new HashMap<>();
    private boolean isDIAUmpire = false;
    private boolean hasPredictionSpectra = false;
    private String predictedFileName = "";
    private boolean useDiaNNPrediction = true;
    private int threadNum = 1;
    private HashMap<String, ArrayList<String>> ddaSpectrumFiles = new HashMap<>();
    private HashMap<String, ArrayList<String>> diaSpectrumFiles = new HashMap<>();
    private HashMap<String, ScanCollectionDefault> scansFileHashMap = new HashMap<>();

    private HashMap<String, ArrayList<String[]>> psmDataHashMap = new HashMap<>();
    private HashMap<String, ArrayList<IonMatch>[]> ionMatchesHashMap = new HashMap<>();
    private GUIMainClass parentFrame;

    public ExportFragments(File resultsFolder, AnnotationSettings annotationSettings, int threadNum, GUIMainClass parentFrame) throws IOException {
        this.resultsFolder = resultsFolder;
        this.annotationSettings = annotationSettings;
        this.threadNum = threadNum;
        this.parentFrame = parentFrame;

        annotationSettings.setIntensityFilter(0.00);
        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

        importData();
    }

    private void importData() throws IOException {
        String manifestFile = versionCheck();

        ProgressDialogX progressDialog = new ProgressDialogX(parentFrame,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Exporting. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        if (manifestFile != null) {

            goThroughFolder();
            getTableIndexes();
            progressDialog.setMaxPrimaryProgressCounter(spectrumFileMap.size() + expNumList.size()*3);
            new Thread("Export") {
                @Override
                public void run() {
                    try {
                        readRawFiles(progressDialog);
                        goThroughPSM(progressDialog);
                        getAnnotations(progressDialog);
                        writeAnnotations(progressDialog);
                    } catch (IOException | FileParsingException | SQLException | InterruptedException |
                             ClassNotFoundException | ExecutionException e) {
                        progressDialog.setRunCanceled();
                        System.exit(1);
                        throw new RuntimeException(e);
                    }
                    progressDialog.setRunFinished();
                    System.exit(0);
                }
            }.start();

        }

    }

    private void readRawFiles(ProgressDialogX progressDialog){
        for (String spectrumName : spectrumFileMap.keySet()){
            progressDialog.setSecondaryProgressText("Reading " + spectrumName + " file");
            if (progressDialog.isRunCanceled()){
                break;
            }
            File eachFile = new File(spectrumFileMap.get(spectrumName));
            if (eachFile.exists()) {
                if (eachFile.getName().endsWith(".mzML")) {
                    MZMLFile mzmlFile = new MZMLFile(spectrumFileMap.get(spectrumName));
                    mzmlFile.setNumThreadsForParsing(10);
                    ScanCollectionDefault scans = new ScanCollectionDefault();
                    scans.setDefaultStorageStrategy(StorageStrategy.SOFT);
                    scans.isAutoloadSpectra(true);
                    scans.setDataSource(mzmlFile);
                    try {
                        scans.loadData(LCMSDataSubset.STRUCTURE_ONLY);
                        scansFileHashMap.put(spectrumName, scans);
                    } catch (FileParsingException e) {
                        e.printStackTrace();
                    }
                }
            }
            progressDialog.increasePrimaryProgressCounter();

        }

    }

    private MSnSpectrum getSpectrum(String spectrumTitle) throws FileParsingException {
        String spectrumFileName = spectrumTitle.split("\\.")[0];
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

        return new MSnSpectrum(2, precursor, spectrumTitle.split("\\.")[1], peakHashMap, spectrumFileName);
    }

    private void goThroughPSM(ProgressDialogX progressDialog) throws IOException, FileParsingException, SQLException, InterruptedException, ClassNotFoundException {
        for (String expNum : resultsDict.keySet()) {
            progressDialog.setSecondaryProgressText("Reading " + expNum + " PSM file");
            if (progressDialog.isRunCanceled()){
                break;
            }
            File oneProteinTable = resultsDict.get(expNum).get(0);
            File onePSMTable = resultsDict.get(expNum).get(1);
            File onePeptideTable = resultsDict.get(expNum).get(2);
            String onePSMTableWithMatch = resultsDict.get(expNum).get(1).getAbsolutePath().replace("psm.tsv", "psm_with_match.tsv");
            ArrayList<String[]> onePSMData = new ArrayList<>();
//            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(onePSMTableWithMatch));

            if (checkFileOpen(onePSMTable)){
                String line;
                String[] lineSplit;
                String spectrumTitle;
                MSnSpectrum currentSpectrum;
                ArrayList<IonMatch> annotations;
                Peptide peptide;

                BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
                line = bufferedReader.readLine();
                int columnNum = line.split("\t").length;
//                bufferedWriter.write(line.stripTrailing() + "\tions\tion_mz\tion_int\n");

                while ((line = bufferedReader.readLine()) != null) {
                    lineSplit = line.split("\t");
                    onePSMData.add(lineSplit);
//                    spectrumTitle = lineSplit[0];
//
//                    currentSpectrum = getSpectrum(spectrumTitle);
//
//                    peptide = new Peptide(lineSplit[peptideSequenceIndex], getUtilitiesModifications(lineSplit[assignenModIndex], lineSplit[peptideSequenceIndex]));
//
//                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, Integer.parseInt(lineSplit[chargeIndex])), 0, "*");
//
//                    SpecificAnnotationSettings specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(spectrumTitle, peptideAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);
//
//                    annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, currentSpectrum, peptide, null, ptmFactory, true);

//                    bufferedWriter.write(line.stripTrailing());
//
//                    ArrayList<String> ionsNames = new ArrayList<>();
//                    ArrayList<Double> ionsMz = new ArrayList<>();
//                    ArrayList<Double> ionsInt = new ArrayList<>();
//                    for (IonMatch ionMatch : annotations){
//                        ionsNames.add(ionMatch.getPeakAnnotation());
//                        ionsMz.add(ionMatch.peak.mz);
//                        ionsInt.add(ionMatch.peak.intensity);
//                    }
//                    if (lineSplit.length != columnNum){
//                        bufferedWriter.write("\t\t");
//                    }
//                    bufferedWriter.write("\t" + ionsNames + "\t" + ionsMz + "\t" + ionsInt + "\n");

                }
                psmDataHashMap.put(expNum, onePSMData);
//                bufferedWriter.close();
                bufferedReader.close();
                progressDialog.increasePrimaryProgressCounter();
            }
        }
    }

    private void writeAnnotations(ProgressDialogX progressDialog){
        DecimalFormat df = new DecimalFormat("#.####");
        for (String expNum : psmDataHashMap.keySet()){
            progressDialog.setSecondaryProgressText("Writing " + expNum + " PSM file");
            if (progressDialog.isRunCanceled()){
                break;
            }
            ArrayList<String[]> onePSMData = psmDataHashMap.get(expNum);
            ArrayList<IonMatch>[] ionMatches = ionMatchesHashMap.get(expNum);
            File onePSMTable = resultsDict.get(expNum).get(1);
            File onePSMTableWithMatch = new File(onePSMTable.getAbsolutePath().replace("psm.tsv", "psm_with_match.tsv"));
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(onePSMTableWithMatch));
                BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
                String line = bufferedReader.readLine();
                int columnNum = line.split("\t").length;
                bufferedWriter.write(line.stripTrailing() + "\tions\tion_mz\tion_int\n");
                bufferedReader.close();

                String[] lineSplit;
                for (int i = 0; i < onePSMData.size(); i++){
                    lineSplit = onePSMData.get(i);
                    bufferedWriter.write(String.join("\t", lineSplit));

                    ArrayList<String> ionsNames = new ArrayList<>();
                    ArrayList<String> ionsMz = new ArrayList<>();
                    ArrayList<Double> ionsInt = new ArrayList<>();
                    for (IonMatch ionMatch : ionMatches[i]){
                        ionsNames.add(ionMatch.getPeakAnnotation());
                        ionsMz.add(df.format(ionMatch.peak.mz));
                        ionsInt.add(ionMatch.peak.intensity);
                    }
                    if (lineSplit.length != columnNum){
                        bufferedWriter.write("\t\t");
                    }
                    bufferedWriter.write("\t" + ionsNames + "\t" + ionsMz + "\t" + ionsInt + "\n");
                }
                bufferedWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressDialog.increasePrimaryProgressCounter();
        }

    }

    private void getAnnotations(ProgressDialogX progressDialog) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (String expNum : psmDataHashMap.keySet()){
            progressDialog.setSecondaryProgressText("Annotating " + expNum + " PSM file");
            if (progressDialog.isRunCanceled()){
                break;
            }
            ArrayList<String[]> onePSMData = psmDataHashMap.get(expNum);
            ArrayList<IonMatch>[] ionMatches = new ArrayList[onePSMData.size()];
            ArrayList<ArrayList<Integer>> psmIndexMulti = splitIntoSublists(onePSMData.size(), threadNum);
            final ArrayList<Future<?>> res1 = new ArrayList<>();
            for (int i = 0; i < threadNum; i++) {
                ArrayList<Integer> oneIndexList = psmIndexMulti.get(i);
                res1.add(executorService.submit(getOneAnnotation(oneIndexList, onePSMData, ionMatches)));
            }
            for (Future<?> future : res1) {
                future.get();
            }
            ionMatchesHashMap.put(expNum, ionMatches);
            progressDialog.increasePrimaryProgressCounter();
        }
    }

    private Runnable getOneAnnotation(ArrayList<Integer> oneIndexList, ArrayList<String[]> onePSMData, ArrayList<IonMatch>[] ionMatches){
        return () -> {
            try {
                for (int psmIndexCount : oneIndexList) {
                    String[] onePSM = onePSMData.get(psmIndexCount);
                    String spectrumTitle = onePSM[0];
                    MSnSpectrum currentSpectrum = getSpectrum(spectrumTitle);

                    Peptide peptide = new Peptide(onePSM[peptideSequenceIndex], getUtilitiesModifications(onePSM[assignenModIndex], onePSM[peptideSequenceIndex]));

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, Integer.parseInt(onePSM[chargeIndex])), 0, "*");

                    SpecificAnnotationSettings specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(spectrumTitle, peptideAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);

                    ArrayList<IonMatch> annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, currentSpectrum, peptide, null, ptmFactory, true);

                    ionMatches[psmIndexCount] = annotations;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (FileParsingException e) {
                throw new RuntimeException(e);
            }
        };
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

    private static ArrayList<ArrayList<Integer>> splitIntoSublists(Integer listSize, int numberOfSublists) {
        ArrayList<ArrayList<Integer>> sublists = new ArrayList<>();
        int sublistSize = listSize / numberOfSublists;
        int remainingElements = listSize % numberOfSublists;

        for (int i = 0; i < numberOfSublists; i++) {
            int start = i * sublistSize;
            int end = (i + 1) * sublistSize;
            if (i == numberOfSublists - 1) {
                end += remainingElements; // Add remaining elements to the last sublist
            }
            ArrayList<Integer> sublist = new ArrayList<>();
            for (int j = start; j < end; j++) {
                sublist.add(j);
            }

            sublists.add(sublist);
        }

        return sublists;
    }

    private Boolean checkFileOpen(File eachFile){
        if (Files.exists(eachFile.toPath()) && Files.isRegularFile(eachFile.toPath()) && Files.isReadable(eachFile.toPath())){
            return true;
        } else {
            return false;
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

    private void goThroughFolder() throws IOException {
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
                } else {
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
//                if (lineSplit[0].contains("spectraModel")){
//                    if (!lineSplit[1].contains("DIA-NN")){
//                        useDiaNNPrediction = false;
//                    }
//                }
            }


        } else {
            hasPredictionSpectra = false;
        }
    }

}
