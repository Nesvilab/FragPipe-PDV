package GUI.Export;

import GUI.GUIMainClass;
import GUI.utils.ResultProcessor;
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

    private File resultsFolder;
    private int threadNum = 1;
    private HashMap<String, ScanCollectionDefault> scansFileHashMap = new HashMap<>();

    private HashMap<String, ArrayList<String[]>> psmDataHashMap = new HashMap<>();
    private HashMap<String, ArrayList<IonMatch>[]> ionMatchesHashMap = new HashMap<>();
    private ResultProcessor resultProcessor;

    public ExportFragments(File resultsFolder, AnnotationSettings annotationSettings, int threadNum, GUIMainClass parentFrame) throws IOException {
        this.resultsFolder = resultsFolder;
        this.annotationSettings = annotationSettings;
        this.threadNum = threadNum;

        annotationSettings.setIntensityFilter(0.00);
        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

        importData();
    }

    private void importData() throws IOException {

        resultProcessor = new ResultProcessor(resultsFolder, null);

        if (resultProcessor.manifestFile != null) {

            if (resultProcessor.psmIndexToName.containsValue("ionint") && resultProcessor.psmIndexToName.containsValue("ionmz")){
                System.out.println("The file has already been annotated.");
                System.exit(1);
            }

            try {
                readRawFiles();
                goThroughPSM();
                getAnnotations();
                writeAnnotations();
            } catch (IOException | FileParsingException | SQLException | InterruptedException |
                     ClassNotFoundException | ExecutionException e) {
                System.exit(1);
                throw new RuntimeException(e);
            }
            System.exit(0);
        }

    }

    private void readRawFiles(){
        for (String spectrumName : resultProcessor.spectrumFileMap.keySet()){
            System.out.println("Reading mzML: " + spectrumName);
            File eachFile = new File(resultProcessor.spectrumFileMap.get(spectrumName));
            if (eachFile.exists()) {
                if (eachFile.getName().endsWith(".mzML")) {
                    MZMLFile mzmlFile = new MZMLFile(resultProcessor.spectrumFileMap.get(spectrumName));
                    mzmlFile.setNumThreadsForParsing(threadNum);
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

    private void goThroughPSM() throws IOException, FileParsingException, SQLException, InterruptedException, ClassNotFoundException {
        for (String expNum : resultProcessor.resultsDict.keySet()) {
            System.out.println("Reading " + expNum);

            File onePSMTable = resultProcessor.resultsDict.get(expNum).get(1);
            ArrayList<String[]> onePSMData = new ArrayList<>();

            if (checkFileOpen(onePSMTable)){
                String line;
                String[] lineSplit;

                BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
                line = bufferedReader.readLine();

                while ((line = bufferedReader.readLine()) != null) {
                    lineSplit = line.split("\t");
                    onePSMData.add(lineSplit);
                }
                psmDataHashMap.put(expNum, onePSMData);
                bufferedReader.close();
            }
        }
    }

    private void writeAnnotations(){
        DecimalFormat df = new DecimalFormat("#.####");
        DecimalFormat dfInt = new DecimalFormat("#.#");
        for (String expNum : psmDataHashMap.keySet()){
            System.out.println("Writing " + expNum);
            ArrayList<String[]> onePSMData = psmDataHashMap.get(expNum);
            ArrayList<IonMatch>[] ionMatches = ionMatchesHashMap.get(expNum);
            File onePSMTable = resultProcessor.resultsDict.get(expNum).get(1);
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
                    ArrayList<String> ionsInt = new ArrayList<>();
                    for (IonMatch ionMatch : ionMatches[i]){
                        ionsNames.add(ionMatch.getPeakAnnotation());
                        ionsMz.add(df.format(ionMatch.peak.mz));
                        ionsInt.add(dfInt.format(ionMatch.peak.intensity));
                    }
                    if (lineSplit.length != columnNum){
                        bufferedWriter.write("\t\t");
                    }
                    bufferedWriter.write("\t" + ionsNames + "\t" + ionsMz + "\t" + ionsInt + "\n");
                }
                bufferedWriter.close();

                onePSMTable.delete();
                onePSMTableWithMatch.renameTo(onePSMTable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void getAnnotations() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (String expNum : psmDataHashMap.keySet()){
            System.out.println("Annotating " + expNum);

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
        }
    }

    private Runnable getOneAnnotation(ArrayList<Integer> oneIndexList, ArrayList<String[]> onePSMData, ArrayList<IonMatch>[] ionMatches){
        return () -> {
            try {
                for (int psmIndexCount : oneIndexList) {
                    String[] onePSM = onePSMData.get(psmIndexCount);
                    String spectrumTitle = onePSM[0];
                    MSnSpectrum currentSpectrum = getSpectrum(spectrumTitle);

                    Peptide peptide = new Peptide(onePSM[resultProcessor.peptideSequenceIndex], getUtilitiesModifications(onePSM[resultProcessor.assignenModIndex], onePSM[resultProcessor.peptideSequenceIndex]));

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, Integer.parseInt(onePSM[resultProcessor.chargeIndex])), 0, "*");

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

}
