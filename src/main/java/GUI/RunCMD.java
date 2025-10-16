package GUI;

import GUI.Export.Export;
import GUI.utils.ResultProcessor;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.cli.*;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static com.compomics.util.experiment.biology.Ion.IonType.PEPTIDE_FRAGMENT_ION;
import static com.compomics.util.experiment.biology.NeutralLoss.H3PO4;

public class RunCMD extends JFrame{

    private JSplitPane spectrumSplitPane;
    private JSplitPane infoSpectrumSplitPane;
    private JPanel secondarySpectrumPlotsJPanel;
    private JPanel spectrumOuterJPanel;
    private JPanel infoJPanel;
    private JPanel spectrumJPanel;
    private JPanel resizeJPanel;

    private File resultsFolder;
    private String outputFolder;
    private ImageType imageType;
    private int height;
    private int width;
    private String unit;
    private int afSize;
    private File logFile;
    private File indexFile;
    private boolean isPeptideKey;
    private boolean isH2O;
    private boolean isNH3;
    private boolean isRP;
    private float peakWidth;

    public PTMFactory ptmFactory = PTMFactory.getInstance();
    private PtmSettings ptmSettings = new PtmSettings();;
    public SearchParameters searchParameters = new SearchParameters();
    public AnnotationSettings annotationSettings = new AnnotationSettings();
    private UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();

    private SpectrumPanel spectrumPanel;

    private HashMap<String, ArrayList<String>> indexesFromFile = new HashMap<>();
    private ResultProcessor resultProcessor;
    private HashMap<String, ScanCollectionDefault> scansFileHashMap = new HashMap<>();

    private int threadNum = 2;
    /**
     * Pattern removing illegal
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");
    /**
     * Get system separator
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");


    public RunCMD(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption("r", "result",true, "Result folder path.");
        options.addOption("k", "queryType", true, "The input data type for parameter -i (protein ID: pro, peptide sequence: pep).");
        options.addOption("i", "queryFile", true, "A file containing peptide sequences or protein IDs. PDV will generate figures for these peptides or proteins.");
        options.addOption("o", "output", true, "Output directory.");
        options.addOption("t", "tolerance", true, "Tolerance for MS/MS fragment ion mass values. Unit is PPM. The default value is 20.");
        options.addOption("c", "intCutoff", true, "The intensity percentile to consider for annotation. Default is 1 (1%), it means that the peaks with intensities >= (3% * max intensity) will be annotated.");
        options.addOption("fh", "figureHeight", true, "Figure height. Default is 400");
        options.addOption("fw", "figureWidth", true, "Figure width. Default is 800");
        options.addOption("fu", "figureUnit", true, "The units in which ‘height’(fh) and ‘width’(fw) are given. Can be cm, mm or px. Default is px");
        options.addOption("ft", "figureType", true, "Figure type. Can be png, pdf or tiff.");
        options.addOption("pw", "peakWidth", true, "Peak width. Default is 1");
        options.addOption("ah", "waterLoss", false, "Whether or not to consider neutral loss of H2O.");
        options.addOption("rp", "precursorRemoval", false, "Whether or not to remove precursor peak.");
        options.addOption("an", "ammoniaLoss", false, "Whether or not to consider neutral loss of NH3.");
        options.addOption("as", "fontSize", true, "Annotation information font size. Default is 12");
        options.addOption("h", false, "Help");
        options.addOption("help", false, "Help");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help") || cmd.hasOption("h") || args.length == 0) {
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(0);
        }

        try {
            runCMD(cmd, options);
        } catch (IOException | FileParsingException | SQLException | InterruptedException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void runCMD(CommandLine commandLine, Options options) throws IOException, FileParsingException, SQLException, InterruptedException, ClassNotFoundException {

        if (commandLine.getOptionValue("r") != null){
            this.resultsFolder = new File(commandLine.getOptionValue("r"));
        } else {
            System.err.println("Missed result file path!");
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(1);
        }

        if (commandLine.getOptionValue("o") != null){
            this.outputFolder = commandLine.getOptionValue("o");
        } else {
            System.err.println("Missed output path!");
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(1);
        }

        if (commandLine.getOptionValue("ft") != null){
            if (commandLine.getOptionValue("ft").equals("png")){
                this.imageType = ImageType.PNG;
            } else if (commandLine.getOptionValue("ft").equals("pdf")){
                this.imageType = ImageType.PDF;
            } else if (commandLine.getOptionValue("ft").equals("tiff")){
                this.imageType = ImageType.TIFF;
            }
        } else {
            System.err.println("Missed output picture type! (png, pdf, tiff)");
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(1);
        }

        if (commandLine.getOptionValue("fh") == null){
            this.height = 400;
        } else {
            this.height = Integer.valueOf(commandLine.getOptionValue("fh"));
        }
        if (commandLine.getOptionValue("fw") == null){
            this.width = 800;
        } else {
            this.width = Integer.valueOf(commandLine.getOptionValue("fw"));
        }
        if (commandLine.getOptionValue("fu") == null){
            this.unit = "px";
        } else {
            this.unit = commandLine.getOptionValue("fu");
        }
        if (commandLine.getOptionValue("as") == null){
            this.afSize = 12;
        } else {
            this.afSize = Integer.valueOf(commandLine.getOptionValue("as"));
        }

        this.logFile = new File(outputFolder+"/log.txt");

        if (commandLine.getOptionValue("i") != null){
            this.indexFile = new File(commandLine.getOptionValue("i"));
        } else {
            System.err.println("Lost selected index file!");
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(1);
        }

        if (commandLine.getOptionValue("k") != null){
            if(commandLine.getOptionValue("k").equals("pro")){
                this.isPeptideKey = false;
            }else if(commandLine.getOptionValue("k").equals("pep")) {
                this.isPeptideKey = true;
            }
        } else {
            System.err.println("Lost selected index file type! (spectrum key: s, peptide sequence: p)");
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(1);
        }

        if (commandLine.getOptionValue("ah") != null){
            this.isH2O = true;
        }
        if (commandLine.getOptionValue("an") != null){
            this.isNH3 = true;
        }
        if (commandLine.getOptionValue("rp") != null){
            this.isRP = true;
        }

        double ionAccurracy;
        if (commandLine.getOptionValue("a") == null){
            ionAccurracy = 20;
        } else {
            ionAccurracy = Double.valueOf(commandLine.getOptionValue("a"));
        }

        Double intensityFilter = 0.03;
        if (commandLine.getOptionValue("c") == null){
            intensityFilter = 0.01;
        } else {
            intensityFilter = Double.valueOf(commandLine.getOptionValue("c")) * 0.01;
        }
        if (commandLine.getOptionValue("pw") == null){
            this.peakWidth = 1f;
        } else {
            this.peakWidth = Float.valueOf(commandLine.getOptionValue("pw"));
        }

        ArrayList<String> modification = ptmFactory.getPTMs();
        PtmSettings ptmSettings = new PtmSettings();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }

        searchParameters.setPtmSettings(ptmSettings);

        searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);

        searchParameters.setFragmentIonAccuracy(ionAccurracy);
        annotationSettings.setIntensityFilter(intensityFilter);
        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        processIndexFile();

        initComponent();
        this.setVisible(true);
        importFile();

        formWindowClosing(null);
    }

    /**
     * Init all GUI components
     */
    private void initComponent(){

        spectrumJPanel = new JPanel();
        spectrumSplitPane = new JSplitPane();
        infoSpectrumSplitPane = new JSplitPane();
        secondarySpectrumPlotsJPanel = new JPanel();
        infoJPanel = new JPanel();
        spectrumOuterJPanel = new JPanel();
        resizeJPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PsmViewer");
        setBackground(new Color(255, 255, 255));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        spectrumSplitPane.setBackground(new Color(255, 255, 255));
        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(100);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        secondarySpectrumPlotsJPanel.setMinimumSize(new Dimension(0, 100));
        secondarySpectrumPlotsJPanel.setOpaque(false);
        secondarySpectrumPlotsJPanel.setLayout(new BoxLayout(secondarySpectrumPlotsJPanel, BoxLayout.LINE_AXIS));
        spectrumSplitPane.setTopComponent(secondarySpectrumPlotsJPanel);

        spectrumOuterJPanel.setBackground(new Color(255, 255, 255));

        spectrumJPanel.setBackground(new Color(255, 255, 255));
        spectrumJPanel.setLayout(new BorderLayout());

        GroupLayout spectrumOuterJPanelLayout = new GroupLayout(spectrumOuterJPanel);
        spectrumOuterJPanel.setLayout(spectrumOuterJPanelLayout);
        spectrumOuterJPanelLayout.setHorizontalGroup(
                spectrumOuterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumOuterJPanelLayout.setVerticalGroup(
                spectrumOuterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumOuterJPanelLayout.createSequentialGroup()
                                .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        spectrumSplitPane.setRightComponent(spectrumOuterJPanel);

        infoJPanel.setOpaque(false);
        infoJPanel.setLayout(new BoxLayout(infoJPanel, BoxLayout.LINE_AXIS));

        infoSpectrumSplitPane.setLeftComponent(infoJPanel);
        infoSpectrumSplitPane.setRightComponent(spectrumSplitPane);
        infoSpectrumSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        infoSpectrumSplitPane.setDividerSize(0);
        infoSpectrumSplitPane.setBorder(null);
        infoSpectrumSplitPane.setBackground(new Color(255, 255, 255));

        resizeJPanel.add(infoSpectrumSplitPane);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(resizeJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(resizeJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Close
     * @param evt WindowEvent
     */
    private void formWindowClosing(WindowEvent evt) {
        final RunCMD finalRef = this;
        SwingUtilities.invokeLater(() -> {

            finalRef.setVisible(false);

            System.exit(0);
        });
    }

    private void processIndexFile() throws IOException {
        String readLine;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(indexFile));
        while ((readLine = bufferedReader.readLine())!=null) {

            String[] keyAndInfo = readLine.split("\t");
            ArrayList<String> addInforList = new ArrayList<>();

            indexesFromFile.put(keyAndInfo[0], addInforList);
        }
    }

    private void importFile() throws IOException, FileParsingException {
        resultProcessor = new ResultProcessor(resultsFolder, null);

        goThroughPSM();
        readRawFiles();

        exportImages();
    }

    private void exportImages() throws IOException, FileParsingException {

        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        FileWriter fileWriter = new FileWriter(logFile);
        fileWriter.write("key\t#foundSpectra\n");

        FormLayout formLayout = new FormLayout(width + unit, height + unit);

        resizeJPanel.setLayout(formLayout);
        resizeJPanel.revalidate();
        resizeJPanel.repaint();

        int resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
        int resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));

        infoSpectrumSplitPane.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);
        infoSpectrumSplitPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));

        for (String key : indexesFromFile.keySet()) {
            int spectraCount = 0;
            ArrayList<String> onePSMData = indexesFromFile.get(key);
            for (String onePSMDataKey : onePSMData) {
                String[] onePSM = onePSMDataKey.trim().split("\t");

                String spectrumTitle = onePSM[1];
                MSnSpectrum currentSpectrum = getSpectrum(spectrumTitle);

                Peptide peptide = new Peptide(onePSM[resultProcessor.peptideSequenceIndex+1],
                        getUtilitiesModifications(onePSM[resultProcessor.assignenModIndex+1],
                                onePSM[resultProcessor.peptideSequenceIndex+1]));

                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0,
                        new Charge(+1, Integer.parseInt(onePSM[resultProcessor.chargeIndex+1])), 0, "*");

                if (currentSpectrum != null) {

                    spectraCount ++;

                    updateSpectrum("1", currentSpectrum, peptideAssumption, resizeJPanelWidth, resizeJPanelHeight);
                    String outputFigurePath = outputFolder + FILE_SEPARATOR + onePSM[0] + "_" +
                            FilePattern.matcher(currentSpectrum.getSpectrumTitle()).replaceAll("") + "_" +
                            peptide.getSequence() + imageType.getExtension();
                    try {
                        Export.exportPic(infoSpectrumSplitPane, infoSpectrumSplitPane.getBounds(), new File(outputFigurePath), imageType);
                    } catch (IOException | TranscoderException e) {
                        e.printStackTrace();
                    }
                }
            }
            fileWriter.write(key + "\t" + spectraCount + "\n");
        }
        fileWriter.close();

    }

    /**
     * Update spectrum panel according to the details
     * @param spectrumKey spectrum key
     * @param mSnSpectrum MSN spectrum
     * @param tempPeptideAssumption peptide assumption
     */
    private void updateSpectrum(String spectrumKey, MSnSpectrum mSnSpectrum, PeptideAssumption tempPeptideAssumption,
                                int resizeJPanelWidth, int resizeJPanelHeight) {
        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
        try {
            if (mSnSpectrum != null) {

                Collection<Peak> peaks = mSnSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {

                } else {

                    boolean newMax = false;

                    double lastMzMaximum = 0;

                    try {
                        lastMzMaximum = mSnSpectrum.getMaxMz()*1.05;
                        newMax = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    double lowerMzZoomRange = 0;
                    double upperMzZoomRange = lastMzMaximum;
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) { // @TODO: sometimes the range is reset when is should not be...
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = mSnSpectrum.getPrecursor();
                    System.out.println("Precursor mz is "+precursor.getMz());
                    MSnSpectrum newMSnSpectrum;
                    if (isRP){
                        HashMap<Double, Peak> peakMap = new HashMap<>();
                        double[] mzValuesAsArray = mSnSpectrum.getMzValuesAsArray();
                        double[] intensityValuesAsArray = mSnSpectrum.getIntensityValuesAsArray();
                        for (int index = 0; index < mzValuesAsArray.length; index ++){
                            double currentMZ = mzValuesAsArray[index];
                            double currentInt = intensityValuesAsArray[index];
                            if (Math.abs(currentMZ - precursor.getMz()) < annotationSettings.getFragmentIonAccuracy()){
                                currentInt = 0;
                                System.out.println(currentMZ);
                            }
                            Peak peak = new Peak(currentMZ, currentInt);
                            peakMap.put(currentMZ, peak);
                        }
                        newMSnSpectrum = new MSnSpectrum(2, precursor, spectrumKey, peakMap, "");
                    } else {
                        newMSnSpectrum = mSnSpectrum;
                    }

                    spectrumPanel = new SpectrumPanel(
                            newMSnSpectrum.getMzValuesAsArray(), newMSnSpectrum.getIntensityValuesNormalizedAsArray(),
                            precursor.getMz(), tempPeptideAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    spectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(peakWidth);
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

                    Peptide currentPeptide = tempPeptideAssumption.getPeptide();

                    SpecificAnnotationSettings specificAnnotationPreferences = annotationSettings.getSpecificAnnotationPreferences(spectrumKey, tempPeptideAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);

                    specificAnnotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                    specificAnnotationPreferences.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.B_ION);

                    specificAnnotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                    specificAnnotationPreferences.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

                    specificAnnotationPreferences.clearNeutralLosses();

                    if (isH2O){
                        specificAnnotationPreferences.addNeutralLoss(NeutralLoss.H2O);
                    }
                    if (isNH3){
                        specificAnnotationPreferences.addNeutralLoss(NeutralLoss.NH3);
                    }
                    specificAnnotationPreferences.addNeutralLoss(NeutralLoss.H3PO4);

                    ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationPreferences, newMSnSpectrum, currentPeptide, null, ptmFactory, true);

                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);

                    spectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    spectrumJPanel.removeAll();
                    spectrumJPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight - 80);
                    spectrumJPanel.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight-80));
                    spectrumJPanel.add(spectrumPanel);
                    spectrumJPanel.revalidate();
                    spectrumJPanel.repaint();

                    Integer forwardIon = PeptideFragmentIon.B_ION;
                    Integer rewindIon = PeptideFragmentIon.Y_ION;

                    String modSequence = currentPeptide.getTaggedModifiedSequence(ptmSettings, false, false, false, false);
                    SequenceFragmentationPanel sequenceFragmentationPanel = new SequenceFragmentationPanel(
                            modSequence, annotations, true, ptmSettings, forwardIon, rewindIon);

                    FontMetrics fm = sequenceFragmentationPanel.getFontMetrics(new Font("Arial", Font.PLAIN, 16));

                    if (fm.stringWidth(modSequence) + 45 > width){
                        sequenceFragmentationPanel.updateFontSize(-2);
                    }

                    sequenceFragmentationPanel.setOpaque(false);
                    sequenceFragmentationPanel.setBackground(Color.WHITE);
                    sequenceFragmentationPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    secondarySpectrumPlotsJPanel.removeAll();
                    secondarySpectrumPlotsJPanel.add(sequenceFragmentationPanel);
                    secondarySpectrumPlotsJPanel.revalidate();
                    secondarySpectrumPlotsJPanel.repaint();

                    spectrumSplitPane.revalidate();
                    spectrumSplitPane.repaint();

                    resizeJPanel.revalidate();
                    resizeJPanel.repaint();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Update error");
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

    private void goThroughPSM() throws IOException {
        for (String expNum : resultProcessor.resultsDict.keySet()) {
            System.out.println("Reading " + expNum);

            File onePSMTable = resultProcessor.resultsDict.get(expNum).get(1);

            if (checkFileOpen(onePSMTable)){
                String line;
                String[] lineSplit;

                BufferedReader bufferedReader = new BufferedReader(new FileReader(onePSMTable));
                line = bufferedReader.readLine();

                while ((line = bufferedReader.readLine()) != null) {
                    lineSplit = line.split("\t");

                    if (isPeptideKey){
                        if (indexesFromFile.containsKey(lineSplit[resultProcessor.peptideSequenceIndex])){
                            indexesFromFile.get(lineSplit[resultProcessor.peptideSequenceIndex]).add(expNum + "\t"+ line);
                        }
                    } else {
                        if (indexesFromFile.containsKey(lineSplit[resultProcessor.proteinIDIndex])){
                            indexesFromFile.get(lineSplit[resultProcessor.proteinIDIndex]).add(expNum + "\t"+ line);
                        }
                    }
                }

                bufferedReader.close();
            }
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
            } else {
                System.err.println("Spectrum file not found: " + spectrumName);
            }

        }

    }

    /**
     * Get the current delta masses map
     * @return Current delta masses map
     */
    private HashMap<Double, String> getCurrentMassDeltas() {
        HashMap<Double, String> currentMassDeltaMap = new HashMap<>();

        ArrayList<String> allModifications = ptmSettings.getAllModifications();
        Collections.sort(allModifications);

        currentMassDeltaMap.put(AminoAcid.A.getMonoisotopicMass(), "A");
        currentMassDeltaMap.put(AminoAcid.C.getMonoisotopicMass(), "C");
        currentMassDeltaMap.put(AminoAcid.D.getMonoisotopicMass(), "D");
        currentMassDeltaMap.put(AminoAcid.E.getMonoisotopicMass(), "E");
        currentMassDeltaMap.put(AminoAcid.F.getMonoisotopicMass(), "F");
        currentMassDeltaMap.put(AminoAcid.G.getMonoisotopicMass(), "G");
        currentMassDeltaMap.put(AminoAcid.H.getMonoisotopicMass(), "H");
        currentMassDeltaMap.put(AminoAcid.I.getMonoisotopicMass(), "I/L");
        currentMassDeltaMap.put(AminoAcid.K.getMonoisotopicMass(), "K");
        currentMassDeltaMap.put(AminoAcid.M.getMonoisotopicMass(), "M");
        currentMassDeltaMap.put(AminoAcid.N.getMonoisotopicMass(), "N");
        currentMassDeltaMap.put(AminoAcid.O.getMonoisotopicMass(), "O");
        currentMassDeltaMap.put(AminoAcid.P.getMonoisotopicMass(), "P");
        currentMassDeltaMap.put(AminoAcid.Q.getMonoisotopicMass(), "Q");
        currentMassDeltaMap.put(AminoAcid.R.getMonoisotopicMass(), "R");
        currentMassDeltaMap.put(AminoAcid.S.getMonoisotopicMass(), "S");
        currentMassDeltaMap.put(AminoAcid.T.getMonoisotopicMass(), "T");
        currentMassDeltaMap.put(AminoAcid.U.getMonoisotopicMass(), "U");
        currentMassDeltaMap.put(AminoAcid.V.getMonoisotopicMass(), "V");
        currentMassDeltaMap.put(AminoAcid.W.getMonoisotopicMass(), "W");
        currentMassDeltaMap.put(AminoAcid.Y.getMonoisotopicMass(), "Y");

        for (String modification : allModifications) {
            PTM ptm = ptmFactory.getPTM(modification);

            if (ptm != null) {

                String shortName = ptm.getShortName();
                AminoAcidPattern aminoAcidPattern = ptm.getPattern();
                double mass = ptm.getMass();

                // Get non-terminus modification
                if (ptm.getType() == PTM.MODAA && aminoAcidPattern != null) {
                    for (Character character : aminoAcidPattern.getAminoAcidsAtTarget()) {
                        if (!currentMassDeltaMap.containsValue(character + "<" + shortName + ">")) {
                            AminoAcid aminoAcid = AminoAcid.getAminoAcid(character);
                            currentMassDeltaMap.put(mass + aminoAcid.getMonoisotopicMass(),
                                    character + "<" + shortName + ">");
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, modification+" PTM not found", "PTM Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Error: PTM not found: " + modification);
            }
        }

        return currentMassDeltaMap;
    }

    private Boolean checkFileOpen(File eachFile){
        if (Files.exists(eachFile.toPath()) && Files.isRegularFile(eachFile.toPath()) && Files.isReadable(eachFile.toPath())){
            return true;
        } else {
            return false;
        }
    }
}
