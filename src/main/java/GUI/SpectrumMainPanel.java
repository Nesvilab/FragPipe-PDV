package GUI;

import GUI.Export.ExportExpectedSizeDialog;
import GUI.Export.ExportMGFDialog;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.spectrum.FragmentIonTable;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import org.apache.commons.lang.math.NumberUtils;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import umich.ms.fileio.filetypes.diann.DiannSpeclibReader;
import umich.ms.fileio.filetypes.diann.PredictionEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import static com.compomics.util.experiment.biology.Ion.IonType.*;
import static com.compomics.util.experiment.biology.NeutralLoss.H3PO4;

/**
 * Spectrum main panel
 * Created by Ken on 9/15/2017.
 */
public class SpectrumMainPanel extends JPanel {

    public JPanel spectrumShowPanel;
    private JPanel mainShowJPanel;
    private JPanel fragmentIonJPanel;
    private JPanel contextJPanel;
    private JScrollPane ionFragmentsJScrollPane;
    private JLayeredPane spectrumJLayeredPane;
    private JLayeredPane mirrorJLayeredPane;
    private JLayeredPane checkPeptideJLayeredPane;
    private JLayeredPane predictionJLayeredPane;
    private JCheckBoxMenuItem defaultAnnotationCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardAIonCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardBIonCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardCIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardXIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardYIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardZIonCheckBoxMenuItem;
    private JCheckBoxMenuItem precursorCheckMenuItem;
    private JCheckBoxMenuItem immoniumIonsCheckMenuItem;
    private JCheckBoxMenuItem relatedIonsCheckMenuItem;
    private JCheckBoxMenuItem reporterIonsCheckMenuItem;
    private JCheckBoxMenuItem glycansCheckMenuItem;
    private JCheckBoxMenuItem glycanZeroCheckMenuItem;
    private JCheckBoxMenuItem glycanOneCheckMenuItem;
    private JCheckBoxMenuItem showPairedETDScanMenuItem;
    private JCheckBoxMenuItem defaultLossCheckBoxMenuItem;
    private JCheckBoxMenuItem showAllPeaksMenuItem;
    private JCheckBoxMenuItem showMatchesPeaksMenuItem;
    private JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private JCheckBoxMenuItem rewindIonsDeNovoCheckBoxMenuItem;
    private JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private JCheckBoxMenuItem showSpectrumJMenuItem;
    private JCheckBoxMenuItem showIonTableJMenuItem;
    private JCheckBoxMenuItem showMirrorJMenuItem;
    private JMenuItem showCheckPeptideJMenuItem;
    private JCheckBoxMenuItem showPredictionJMenuItem;
    private JMenu lossMenu;
    private JMenu chargeMenu;
    private JMenu ionsMenu;
    private JMenu otherMenu;
    private JMenu deNovoMenu;
    private JMenu exportGraphicsMenu;
    private JMenu switchPaneMenu;
    private JMenu checkFileMenu;
    private JMenu lossSplitter;
    private JMenu splitterMenu6;
    private JMenu splitterMenu7;
    private JLabel contentJLabel;
    private JLabel bIonNumJLabel;
    private JLabel yIonNumJLabel;
    private JLabel matchNumJLabel;
    public JMenu settingsMenu;
    public JMenu peptideCheckMenu;

    /**
     * Mirror spectrum panel
     */
    private SpectrumContainer mirrorSpectrumPanel;
    /**
     * Check peptide spectrum panel
     */
    private SpectrumContainer checkPeptideSpectrumPanel;
    /**
     * Predicted spectrum panel
     */
    private SpectrumContainer predictedSpectrumPanel;
    /**
     * Original sequence fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanel;
    /**
     * Mirror sequence fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanelMirror;
    /**
     * Check peptide fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanelCheck;
    /**
     * Predicted sequence fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanelPredicted;
    /**
     * Mirror fragment panel
     */
    private SequenceFragmentationPanel mirrorFragmentPanel;
    /**
     * Check fragment panel
     */
    private SequenceFragmentationPanel checkFragmentPanel;
    /**
     * Predicted fragment panel
     */
    private SequenceFragmentationPanel predictedFragmentPanel;
    /**
     * Boolean show if shoe spectrum selected
     */
    private boolean showSpectrumSelected = false;
    /**
     * Boolean show if ion table selected
     */
    private boolean ionTableSelected = false;
    /**
     * Boolean show if spectrum check selected
     */
    private boolean mirrorSelected = false;
    /**
     * Boolean show if peptide check selected
     */
    private boolean peptideCheckSelected = false;
    /**
     * Boolean show if spectrum predicted selected
     */
    private boolean predictedSelected = false;
    /**
     * Show matched peaks only
     */
    private boolean showMatchedPeaksOnly = false;
    private boolean showPairedETDScan = false;
    /**
     * LastSelectFolder accessed easily
     */
    public LastSelectedFolder lastSelectedFolder;
    /**
     * Utilities user preferences
     */
    public UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    /**
     * SpectrumPanel to paint spectrum import from utilities
     */
    private SpectrumContainer spectrumPanel;
    /**
     * Original peptide sequence
     */
    private String currentPeptideSequence;
    /**
     * SpecificAnnotationSettings
     */
    private SpecificAnnotationSettings specificAnnotationSettings;
    /**
     * Current psmKey selected
     */
    private String selectedPsmKey = "";
    private String pairedScanNum = "";
    private MSnSpectrum pairedSpectrum;
    /**
     * Synthetic peptide spectra file map
     */
    private HashMap<String, File> checkSpectrumFileMaps = new HashMap<>();
    /**
     * Current assumptions
     */
    private SpectrumIdentificationAssumption spectrumIdentificationAssumption;
    /**
     * Original assumptions
     */
    private HashMap<Integer, ModificationMatch> oriModificationMatches = new HashMap<>();

    private MSnSpectrum currentSpectrum;
    /**
     * Maximum mz to rescale spectrumPanel
     */
    private double lastMzMaximum = 0;
    /**
     * Forward ions searched for (a, b or c)
     */
    private Integer forwardIon = PeptideFragmentIon.B_ION;
    /**
     * Reward ions searched for (x, y or z)
     */
    private Integer rewindIon = PeptideFragmentIon.Y_ION;
    /**
     * Forward ions list searched for (a, b or c)
     */
    private ArrayList<Integer> forwardIons = new ArrayList();
    /**
     * Reward ions list searched for (x, y or z)
     */
    private ArrayList<Integer> rewindIons = new ArrayList();
    /**
     * PTMFactory containing all modifications import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Spectrum annotator
     */
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * Spectrum tagSpectrumAnnotator
     */
    private TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();
    /**
     * SearchParameters
     */
    private SearchParameters searchParameters;
    /**
     * ChargeMenus according to the precursor charge state
     */
    private HashMap<Integer, JCheckBoxMenuItem> chargeMenuMap = new HashMap<>();
    /**
     * lossMenus
     */
    private HashMap<NeutralLoss, JCheckBoxMenuItem> lossMenuMap = new HashMap<>();
    public ArrayList<NeutralLoss> newDefinedLosses = new ArrayList<>();
    /**
     * Check peptide map
     */
    public HashMap<String, Peptide> checkPeptideMap = new HashMap<>();
    /**
     * Parent frame
     */
    public GUIMainClass parentFrame;
    /**
     * Show spectrum details or not
     */
    public Boolean showDetails = false;
    /**
     * Intensity details
     */
    public String matchedToAllPeakInt;
    /**
     * Spectrum setAction
     */
    private SetAction spectrumSetAction;
    /**
     * Check spectrum mirror spectrum setAction
     */
    private SetAction mirrorSetAction;
    /**
     * Check peptide mirror spectrum setAction
     */
    private SetAction checkSetAction;
    /**
     * Show prediction mirror spectrum setAction
     */
    private SetAction preSetAction;
    /**
     * If true, de novo function
     */
    private boolean isDenovo;
    /**
     * Glycan menu item selection
     */
    private Integer[] glycanMenuItemSelection = new Integer[]{0,0};

    /**
     * Construtor
     * @param guiMainClass Parent class
     */
    public SpectrumMainPanel(GUIMainClass guiMainClass){
        this.searchParameters = guiMainClass.searchParameters;
        this.annotationSettings = guiMainClass.annotationSettings;
        this.parentFrame = guiMainClass;

//        annotationSettings.setFragmentIonAccuracy(0.05);
        annotationSettings.setIntensityFilter(0.00);

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        loadUserPreferences();

        initComponents();

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
    }

    /**
     * Show details or not
     * @param showDetails Boolean
     */
    public void setShowDetails(Boolean showDetails){
        this.showDetails = showDetails;
    }

    /**
     * Set tolerance
     * @param massAccuracyType Type
     * @param fragmentIonAccuracy Value
     */
    public void setFragmentIonAccuracy(SearchParameters.MassAccuracyType massAccuracyType, Double fragmentIonAccuracy){
        searchParameters.setFragmentAccuracyType(massAccuracyType);
        searchParameters.setFragmentIonAccuracy(fragmentIonAccuracy);
        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        updateSpectrum();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){

        JToolBar allJToolBar = new JToolBar();
        JMenuBar annotationMenuBar = new JMenuBar();
        JPanel allAnnotationMenuPanel = new JPanel();
        ButtonGroup deNovoChargeButtonGroup = new ButtonGroup();
        JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem = new JRadioButtonMenuItem();
        JMenuItem annotationSettingsJMenuItem = new JMenuItem();
        JMenuItem exportSpectrumGraphicsJMenuItem = new JMenuItem();
        JMenuItem exportSpectrumMGFJMenuItem = new JMenuItem();
        JMenuItem checkFileMenuItem = new JMenuItem();
        JMenuItem checkPeptideMenuItem = new JMenuItem();
        JMenuItem changeModificationJMenuItem = new JMenuItem();
        JMenu splitterMenu1 = new JMenu();
        JMenu splitterMenu2 = new JMenu();
        JMenu splitterMenu3 = new JMenu();
        JMenu splitterMenu4 = new JMenu();
        JMenu splitterMenu5 = new JMenu();
        JMenu splitterMenu8 = new JMenu();
        JMenu splitterMenu9 = new JMenu();
        JPopupMenu.Separator jSeparator1 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator2 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator3 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator4 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator5 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator6 = new JPopupMenu.Separator();
        JLabel matchedToAllJLabel = new JLabel();
        JLabel matchIonNumJLabel = new JLabel();
        JLabel matchBIonJLabel = new JLabel();
        JLabel matchYIonJLabel = new JLabel();

        Font menuFont = new Font("Arial", Font.PLAIN, 12);

        spectrumShowPanel = new JPanel();
        mainShowJPanel = new JPanel();
        fragmentIonJPanel = new JPanel();
        contextJPanel = new JPanel();
        ionFragmentsJScrollPane = new JScrollPane();
        ionsMenu = new JMenu();
        chargeMenu = new JMenu();
        lossMenu = new JMenu();
        otherMenu = new JMenu();
        settingsMenu = new JMenu();
        exportGraphicsMenu = new JMenu();
        switchPaneMenu = new JMenu();
        checkFileMenu = new JMenu();
        deNovoMenu = new JMenu();
        peptideCheckMenu = new JMenu();
        lossSplitter = new JMenu();
        splitterMenu6 = new JMenu();
        splitterMenu7 = new JMenu();
        defaultAnnotationCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardAIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardBIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardCIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardXIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardYIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardZIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        precursorCheckMenuItem = new JCheckBoxMenuItem();
        glycansCheckMenuItem = new JCheckBoxMenuItem();
        glycanZeroCheckMenuItem = new JCheckBoxMenuItem();
        glycanOneCheckMenuItem = new JCheckBoxMenuItem();
        showPairedETDScanMenuItem = new JCheckBoxMenuItem();
        immoniumIonsCheckMenuItem = new JCheckBoxMenuItem();
        relatedIonsCheckMenuItem = new JCheckBoxMenuItem();
        reporterIonsCheckMenuItem = new JCheckBoxMenuItem();
        defaultLossCheckBoxMenuItem = new JCheckBoxMenuItem();
        showSpectrumJMenuItem = new JCheckBoxMenuItem();
        showIonTableJMenuItem = new JCheckBoxMenuItem();
        showMirrorJMenuItem = new JCheckBoxMenuItem();
        showCheckPeptideJMenuItem = new JCheckBoxMenuItem();
        showAllPeaksMenuItem = new JCheckBoxMenuItem();
        showMatchesPeaksMenuItem = new JCheckBoxMenuItem();
        forwardIonsDeNovoCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewindIonsDeNovoCheckBoxMenuItem = new JCheckBoxMenuItem();
        deNovoChargeOneJRadioButtonMenuItem = new JRadioButtonMenuItem();
        showPredictionJMenuItem = new JCheckBoxMenuItem();
        spectrumJLayeredPane = new JLayeredPane();
        mirrorJLayeredPane= new JLayeredPane();
        checkPeptideJLayeredPane = new JLayeredPane();
        predictionJLayeredPane = new JLayeredPane();
        contentJLabel = new JLabel();
        bIonNumJLabel = new JLabel();
        yIonNumJLabel = new JLabel();
        matchNumJLabel = new JLabel();
        spectrumShowPanel.setOpaque(false);

        annotationMenuBar.setBorder(BorderFactory.createRaisedBevelBorder());
        annotationMenuBar.setOpaque(false);

        splitterMenu1.setText("|");
        splitterMenu1.setEnabled(false);
        annotationMenuBar.add(splitterMenu1);

        ionsMenu.setText("Ions");
        ionsMenu.setFont(menuFont);
        ionsMenu.setEnabled(false);

        forwardAIonCheckBoxMenuItem.setText("a");
        forwardAIonCheckBoxMenuItem.setFont(menuFont);
        forwardAIonCheckBoxMenuItem.addActionListener(this::forwardAIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardAIonCheckBoxMenuItem);

        forwardBIonCheckBoxMenuItem.setText("b");
        forwardBIonCheckBoxMenuItem.setFont(menuFont);
        forwardBIonCheckBoxMenuItem.addActionListener(this::forwardBIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardBIonCheckBoxMenuItem);

        forwardCIonCheckBoxMenuItem.setText("c");
        forwardCIonCheckBoxMenuItem.setFont(menuFont);
        forwardCIonCheckBoxMenuItem.addActionListener(this::forwardCIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardCIonCheckBoxMenuItem);
        ionsMenu.add(jSeparator1);

        rewardXIonCheckBoxMenuItem.setText("x");
        rewardXIonCheckBoxMenuItem.setFont(menuFont);
        rewardXIonCheckBoxMenuItem.addActionListener(this::rewardXIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardXIonCheckBoxMenuItem);

        rewardYIonCheckBoxMenuItem.setText("y");
        rewardYIonCheckBoxMenuItem.setFont(menuFont);
        rewardYIonCheckBoxMenuItem.addActionListener(this::rewardYIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardYIonCheckBoxMenuItem);

        rewardZIonCheckBoxMenuItem.setText("z");
        rewardZIonCheckBoxMenuItem.setFont(menuFont);
        rewardZIonCheckBoxMenuItem.addActionListener(this::rewardZIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardZIonCheckBoxMenuItem);

        annotationMenuBar.add(ionsMenu);

        splitterMenu2.setText("|");
        splitterMenu2.setEnabled(false);
        annotationMenuBar.add(splitterMenu2);

        otherMenu.setText("Other");
        otherMenu.setFont(menuFont);
        otherMenu.setEnabled(false);

        precursorCheckMenuItem.setSelected(true);
        precursorCheckMenuItem.setText("Precursor");
        precursorCheckMenuItem.setFont(menuFont);
        precursorCheckMenuItem.addActionListener(this::precursorCheckMenuItemAction);
        otherMenu.add(precursorCheckMenuItem);

        immoniumIonsCheckMenuItem.setSelected(true);
        immoniumIonsCheckMenuItem.setText("Immonium");
        immoniumIonsCheckMenuItem.setFont(menuFont);
        immoniumIonsCheckMenuItem.addActionListener(this::immoniumIonsCheckMenuItemAction);
        otherMenu.add(immoniumIonsCheckMenuItem);

        relatedIonsCheckMenuItem.setSelected(true);
        relatedIonsCheckMenuItem.setText("Related");
        relatedIonsCheckMenuItem.setFont(menuFont);
        relatedIonsCheckMenuItem.addActionListener(this::relatedIonsCheckMenuItemAction);
        otherMenu.add(relatedIonsCheckMenuItem);

        reporterIonsCheckMenuItem.setSelected(true);
        reporterIonsCheckMenuItem.setText("Reporter");
        reporterIonsCheckMenuItem.setFont(menuFont);
        reporterIonsCheckMenuItem.addActionListener(this::reporterIonsCheckMenuItemAction);
        otherMenu.add(reporterIonsCheckMenuItem);
        otherMenu.add(jSeparator6);

        glycansCheckMenuItem.setEnabled(true);
        glycansCheckMenuItem.setText("Glycan: B/Y");
        glycansCheckMenuItem.setFont(menuFont);
        glycansCheckMenuItem.addActionListener(this::glyconsCheckMenuItemAction);
        otherMenu.add(glycansCheckMenuItem);

        glycanZeroCheckMenuItem.setEnabled(true);
        glycanZeroCheckMenuItem.setText("Glycan: complete loss");
        glycanZeroCheckMenuItem.setFont(menuFont);
        glycanZeroCheckMenuItem.addActionListener(this::glycanZeroCheckMenuItemAction);
        otherMenu.add(glycanZeroCheckMenuItem);

        glycanOneCheckMenuItem.setEnabled(true);
        glycanOneCheckMenuItem.setText("Glycan: HexNAc remainder");
        glycanOneCheckMenuItem.setFont(menuFont);
        glycanOneCheckMenuItem.addActionListener(this::glycanOneCheckMenuItemAction);
        otherMenu.add(glycanOneCheckMenuItem);

        showPairedETDScanMenuItem.setVisible(false);
        showPairedETDScanMenuItem.setText("Show Paired ETD Scan");
        showPairedETDScanMenuItem.setFont(menuFont);
        showPairedETDScanMenuItem.addActionListener(this::pairedScanNumMenuItemAction);
        otherMenu.add(showPairedETDScanMenuItem);

        annotationMenuBar.add(otherMenu);

        lossSplitter.setText("|");
        lossSplitter.setEnabled(false);
        annotationMenuBar.add(lossSplitter);

        lossMenu.setText("Loss");
        lossMenu.setFont(menuFont);
        lossMenu.setEnabled(false);
        lossMenu.add(jSeparator2);

        defaultLossCheckBoxMenuItem.setText("Default");
        defaultLossCheckBoxMenuItem.setFont(menuFont);
        defaultLossCheckBoxMenuItem.setToolTipText("Adapt losses to sequence and modifications");
        defaultLossCheckBoxMenuItem.addActionListener(this::defaultLossCheckBoxMenuItemAction);
        lossMenu.add(defaultLossCheckBoxMenuItem);

        annotationMenuBar.add(lossMenu);

        splitterMenu9.setText("|");
        splitterMenu9.setEnabled(false);
        annotationMenuBar.add(splitterMenu9);

        chargeMenu.setText("Charge");
        chargeMenu.setFont(menuFont);
        chargeMenu.setEnabled(false);
        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        splitterMenu3.setEnabled(false);
        annotationMenuBar.add(splitterMenu3);

        deNovoMenu.setText("De Novo");
        deNovoMenu.setFont(menuFont);

        forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        forwardIonsDeNovoCheckBoxMenuItem.setFont(menuFont);
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(this::forwardIonsDeNovoCheckBoxMenuItemAction);
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
        rewindIonsDeNovoCheckBoxMenuItem.setFont(menuFont);
        rewindIonsDeNovoCheckBoxMenuItem.addActionListener(this::rewindIonsDeNovoCheckBoxMenuItemAction);
        deNovoMenu.add(rewindIonsDeNovoCheckBoxMenuItem);
        deNovoMenu.add(jSeparator3);

        deNovoChargeButtonGroup.add(deNovoChargeOneJRadioButtonMenuItem);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(true);
        deNovoChargeOneJRadioButtonMenuItem.setText("Single Charge");
        deNovoChargeOneJRadioButtonMenuItem.setFont(menuFont);
        deNovoChargeOneJRadioButtonMenuItem.addActionListener(this::deNovoChargeOneJRadioButtonMenuItemAction);
        deNovoMenu.add(deNovoChargeOneJRadioButtonMenuItem);

        deNovoChargeButtonGroup.add(deNovoChargeTwoJRadioButtonMenuItem);
        deNovoChargeTwoJRadioButtonMenuItem.setText("Double Charge");
        deNovoChargeTwoJRadioButtonMenuItem.setFont(menuFont);
        deNovoChargeTwoJRadioButtonMenuItem.addActionListener(this::deNovoChargeTwoJRadioButtonMenuItemAction);
        deNovoMenu.add(deNovoChargeTwoJRadioButtonMenuItem);

        annotationMenuBar.add(deNovoMenu);

        splitterMenu4.setText("|");
        splitterMenu4.setEnabled(false);
        annotationMenuBar.add(splitterMenu4);

        settingsMenu.setText("Settings");
        settingsMenu.setFont(menuFont);
        settingsMenu.setEnabled(false);

        showAllPeaksMenuItem.setText("Show All Peaks");
        showAllPeaksMenuItem.setFont(menuFont);
        showAllPeaksMenuItem.setToolTipText("Show all peaks or just the annotated peaks");
        showAllPeaksMenuItem.addActionListener(this::showAllPeaksMenuItemAction);
        settingsMenu.add(showAllPeaksMenuItem);

        showMatchesPeaksMenuItem.setText("Show Matched Peaks");
        showMatchesPeaksMenuItem.setFont(menuFont);
        showMatchesPeaksMenuItem.setToolTipText("Only show matched peaks. Removed background peaks.");
        showMatchesPeaksMenuItem.addActionListener(this::setShowMatchesPeaksMenuItem);
        settingsMenu.add(showMatchesPeaksMenuItem);

        settingsMenu.add(jSeparator4);

        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItem.setText("Automatic Annotation");
        defaultAnnotationCheckBoxMenuItem.setFont(menuFont);
        defaultAnnotationCheckBoxMenuItem.setToolTipText("Use automatic annotation");
        defaultAnnotationCheckBoxMenuItem.addActionListener(this::defaultAnnotationCheckBoxMenuItemAction);
        settingsMenu.add(defaultAnnotationCheckBoxMenuItem);

        settingsMenu.add(jSeparator5);

        annotationSettingsJMenuItem.setText("Annotation Setting");
        annotationSettingsJMenuItem.setFont(menuFont);
        annotationSettingsJMenuItem.addActionListener(this::annotationSettingsJMenuItemActionPerformed);
        settingsMenu.add(annotationSettingsJMenuItem);

        annotationMenuBar.add(settingsMenu);

        splitterMenu5.setText("|");
        splitterMenu5.setEnabled(false);
        annotationMenuBar.add(splitterMenu5);

        exportGraphicsMenu.setText("Export");
        exportGraphicsMenu.setFont(menuFont);
        exportGraphicsMenu.setEnabled(false);

        exportSpectrumGraphicsJMenuItem.setText("Spectra");
        exportSpectrumGraphicsJMenuItem.setFont(menuFont);
        exportSpectrumGraphicsJMenuItem.addActionListener(this::exportSpectrumGraphicsJMenuItemActionPerformed);

        exportSpectrumMGFJMenuItem.setText("MGF");
        exportSpectrumMGFJMenuItem.setFont(menuFont);
        exportSpectrumMGFJMenuItem.addActionListener(this::exportSpectrumMGFJMenuItemActionPerformed);

        exportGraphicsMenu.add(exportSpectrumGraphicsJMenuItem);
        exportGraphicsMenu.add(exportSpectrumMGFJMenuItem);

        annotationMenuBar.add(exportGraphicsMenu);

        splitterMenu6.setText("|");
        splitterMenu6.setEnabled(false);
        annotationMenuBar.add(splitterMenu6);

        switchPaneMenu.setText("Tools");
        switchPaneMenu.setFont(menuFont);
        switchPaneMenu.setEnabled(false);

        showSpectrumJMenuItem.setSelected(true);
        showSpectrumJMenuItem.setText("Show spectrum");
        showSpectrumJMenuItem.setFont(menuFont);
        showSpectrumJMenuItem.addActionListener(this::showSpectrumJMenuItemAction);

        switchPaneMenu.add(showSpectrumJMenuItem);

        showIonTableJMenuItem.setText("Ion Table");
        showIonTableJMenuItem.setFont(menuFont);
        showIonTableJMenuItem.addActionListener(this::showIonTableJMenuItemAction);

        switchPaneMenu.add(showIonTableJMenuItem);

        showMirrorJMenuItem.setText("Check spectrum");
        showMirrorJMenuItem.setFont(menuFont);
        showMirrorJMenuItem.addActionListener(this::showMirrorJMenuItemActionPerformed);

        switchPaneMenu.add(showMirrorJMenuItem);

        showCheckPeptideJMenuItem.setText("Check Peptide");
        showCheckPeptideJMenuItem.setFont(menuFont);
        showCheckPeptideJMenuItem.addActionListener(this::showCheckPeptideJMenuItemActionPerformed);

        switchPaneMenu.add(showCheckPeptideJMenuItem);

        showPredictionJMenuItem.setText("Show Predicted");
        showPredictionJMenuItem.setFont(menuFont);
        showPredictionJMenuItem.addActionListener(this::showPredictionJMenuItemActionPerformed);

        switchPaneMenu.add(showPredictionJMenuItem);

        changeModificationJMenuItem.setEnabled(true); // For pre-release
        changeModificationJMenuItem.setText("Change Modifications");
        changeModificationJMenuItem.setFont(menuFont);
        changeModificationJMenuItem.addActionListener(this::changeModificationJMenuItemAction);

        switchPaneMenu.add(changeModificationJMenuItem);

        annotationMenuBar.add(switchPaneMenu);

        splitterMenu7.setText("|");
        splitterMenu7.setEnabled(false);
        splitterMenu7.setVisible(false);
        annotationMenuBar.add(splitterMenu7);

        checkFileMenu.setText("Add");
        checkFileMenu.setFont(menuFont);
        checkFileMenu.setVisible(false);

        checkFileMenuItem.setText("Add Spectrum");
        checkFileMenuItem.setFont(menuFont);

        checkFileMenuItem.addActionListener(this::checkFileMenuItemPerform);

        checkFileMenu.add(checkFileMenuItem);

        annotationMenuBar.add(checkFileMenu);

        peptideCheckMenu.setText("Add");
        peptideCheckMenu.setFont(menuFont);
        peptideCheckMenu.setVisible(false);

        checkPeptideMenuItem.setText("Add Peptide");
        checkPeptideMenuItem.setFont(menuFont);
        checkPeptideMenuItem.addActionListener(this::checkPeptideMenuItemPerform);

        peptideCheckMenu.add(checkPeptideMenuItem);

        annotationMenuBar.add(peptideCheckMenu);

        splitterMenu8.setText("|");
        splitterMenu8.setEnabled(false);
        annotationMenuBar.add(splitterMenu8);

        allJToolBar.setBackground(new Color(255, 255, 255));
        allJToolBar.setBorder(null);
        allJToolBar.setFloatable(false);
        allJToolBar.setRollover(true);
        allJToolBar.setBorderPainted(false);

        allAnnotationMenuPanel.add(annotationMenuBar);

        allAnnotationMenuPanel.setLayout(new BoxLayout(allAnnotationMenuPanel, BoxLayout.LINE_AXIS));
        allJToolBar.add(allAnnotationMenuPanel);

        ionFragmentsJScrollPane.setOpaque(false);

        GroupLayout fragmentIonJPanelLayout = new GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
                fragmentIonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                                .addGroup(fragmentIonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(ionFragmentsJScrollPane, GroupLayout.DEFAULT_SIZE, 1240, 3000)
                                )
                                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
                fragmentIonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup( fragmentIonJPanelLayout.createSequentialGroup()
                                .addComponent(ionFragmentsJScrollPane, 100, 240, 1300)
                        )
        );

        mainShowJPanel.setOpaque(false);
        mainShowJPanel.setBackground(Color.WHITE);

        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );

        matchedToAllJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchBIonJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchYIonJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchIonNumJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        matchedToAllJLabel.setText("Matched Peaks Intensity / Total Intensity: ");
        matchBIonJLabel.setText("b ions: ");
        matchBIonJLabel.setToolTipText("The number of all b ions.");
        matchYIonJLabel.setText("y ions: ");
        matchYIonJLabel.setToolTipText("The number of all y ions.");
        matchIonNumJLabel.setText("by pairs: ");
        matchIonNumJLabel.setToolTipText("The number of all by pairs in same charge state.");

        spectrumShowPanel.setOpaque(false);
        spectrumShowPanel.setBackground(Color.white);

        contextJPanel.setOpaque(false);
        contextJPanel.setBackground(Color.white);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowPanel);
        spectrumShowPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(allJToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(contextJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                        /*.addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(matchedToAllJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(contentJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchBIonJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(bIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchYIonJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(yIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(matchNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))*/
                        .addComponent(mainShowJPanel,100, 1240, Short.MAX_VALUE)
        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(mainShowJPanel, 90, 250, Short.MAX_VALUE)
                                .addGroup(spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(allJToolBar, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(contextJPanel, 25, 25, GroupLayout.PREFERRED_SIZE))

                                /*.addGroup(spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(matchedToAllJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(contentJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchBIonJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(bIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchYIonJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(yIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE))*/)
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(spectrumShowPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(spectrumShowPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
    }

    /**
     * No select of default annotation menu item
     */
    private void noSelectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(false);
    }

    /**
     * ForwardAIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardAIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardBIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardBIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardCIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardCIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardXIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardXIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardYIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardYIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardZIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardZIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * precursorCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void precursorCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * immoniumIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void immoniumIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * reporterIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void reporterIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * glyconsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void glyconsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();

        updateSpectrum();
    }

    /**
     * glycanZeroCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void glycanZeroCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * glycanOneCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void glycanOneCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * glycanOneCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void pairedScanNumMenuItemAction(ActionEvent evt) {
        showPairedETDScan = showPairedETDScanMenuItem.isSelected();
        updateSpectrum();
    }

    /**
     * relatedIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void relatedIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * defaultLossCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void defaultLossCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardIonsDeNovoCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardIonsDeNovoCheckBoxMenuItemAction(ActionEvent evt) {
        if (forwardIonsDeNovoCheckBoxMenuItem.isSelected()){
            isDenovo = true;
        } else {
            isDenovo = false;
        }
        updateSpectrum();
    }

    /**
     * RewindIonsDeNovoCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewindIonsDeNovoCheckBoxMenuItemAction(ActionEvent evt) {
        if (rewindIonsDeNovoCheckBoxMenuItem.isSelected()){
            isDenovo = true;
        } else {
            isDenovo = false;
        }
        updateSpectrum();
    }

    /**
     * DeNovoChargeOneJRadioButtonMenuItemAction
     * @param evt Mouse click event
     */
    private void deNovoChargeOneJRadioButtonMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }

    /**
     * DeNovoChargeTwoJRadioButtonMenuItemAction
     * @param evt Mouse click event
     */
    private void deNovoChargeTwoJRadioButtonMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }

    /**
     * showAllPeaksMenuItemAction
     * @param evt Mouse click event
     */
    private void showAllPeaksMenuItemAction(ActionEvent evt) {
        annotationSettings.setShowAllPeaks(showAllPeaksMenuItem.isSelected());
        updateSpectrum();
    }

    /**
     * showAllPeaksMenuItemAction
     * @param evt Mouse click event
     */
    private void setShowMatchesPeaksMenuItem(ActionEvent evt) {
        showMatchedPeaksOnly = showMatchesPeaksMenuItem.isSelected();
        updateSpectrum();
    }

    /**
     * DefaultAnnotationCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void defaultAnnotationCheckBoxMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }

    /**
     * Open annotation setting dialog
     * @param evt Mouse click event
     */
    private void annotationSettingsJMenuItemActionPerformed(ActionEvent evt) {

        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        new AnnotationSettingsDialog(this, searchParameters);
    }

    /**
     * ExportSpectrumGraphicsJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportSpectrumGraphicsJMenuItemActionPerformed(ActionEvent evt) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mirrorSelected){
            exportMirrorSpectrumAsFigure();
        } else if(peptideCheckSelected){
            exportCheckSpectrumAsFigure();
        } else if (predictedSelected){
            exportPreSpectrumAsFigure();
        }
        else {
            exportSpectrumAsFigure();
        }
    }

    /**
     * ExportSpectrumGraphicsJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportSpectrumMGFJMenuItemActionPerformed(ActionEvent evt) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mirrorSelected){
            //exportMirrorSpectrumAsFigure(); @ Will do, output mirror mgf
            exportSpectrumAsMGF();
        } else if(peptideCheckSelected){
            //exportCheckSpectrumAsFigure();
            exportSpectrumAsMGF();
        } else {
            exportSpectrumAsMGF();
        }
    }

    /**
     * Update the export exacted size dialog
     */
    public void updateExportJDialog(){
        if(mirrorSelected){
            exportMirrorSpectrumAsFigure();
        } else if(peptideCheckSelected){
            exportCheckSpectrumAsFigure();
        } else {
            exportSpectrumAsFigure();
        }
    }

    /**
     * Export current spectrum as MGF
     */
    private void exportSpectrumAsMGF(){
        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
        Peptide currentPeptide = peptideAssumption.getPeptide();
        String modSequence = currentPeptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

        String spectrumTitle = currentSpectrum.getSpectrumTitle();
        MSnSpectrum outputSpectrum = currentSpectrum;
        outputSpectrum.setSpectrumTitle(spectrumTitle + " " + modSequence);

        ExportMGFDialog exportMGFDialog = new ExportMGFDialog(this, spectrumTitle, outputSpectrum.asMgf());

    }

    /**
     * Export normal spectrum
     */
    private void exportSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, spectrumJLayeredPane, sequenceFragmentationPanel, null, spectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), true, false, false, false);

        spectrumSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Export mirror spectrum
     */
    private void exportMirrorSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, mirrorJLayeredPane, sequenceFragmentationPanelMirror, mirrorFragmentPanel, mirrorSpectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), false, true, false, false);

        mirrorSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Export check peptide spectrum
     */
    private void exportCheckSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, checkPeptideJLayeredPane, sequenceFragmentationPanelCheck, checkFragmentPanel, checkPeptideSpectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), false, false, true, false);

        checkSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Export predict spectrum
     */
    private void exportPreSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, predictionJLayeredPane, sequenceFragmentationPanelPredicted, predictedFragmentPanel, predictedSpectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), false, false, false, true);

        preSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Switch pane to normal spectrum
     * @param evt Mosue click event
     */
    public void showSpectrumJMenuItemAction(ActionEvent evt) {
        showSpectrumSelected = true;
        ionTableSelected = false;
        mirrorSelected = false;
        peptideCheckSelected = false;
        predictedSelected = false;

        showSpectrumJMenuItem.setSelected(true);
        showIonTableJMenuItem.setSelected(false);
        showMirrorJMenuItem.setSelected(false);
        showCheckPeptideJMenuItem.setSelected(false);
        showPredictionJMenuItem.setSelected(false);

        contextJPanel.removeAll();
        contextJPanel.repaint();

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();

        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(false);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to fragment table
     * @param evt Mouse click event
     */
    private void showIonTableJMenuItemAction(ActionEvent evt) {
        showSpectrumSelected = false;
        ionTableSelected = true;
        mirrorSelected = false;
        peptideCheckSelected = false;
        predictedSelected = false;

        showSpectrumJMenuItem.setSelected(false);
        showIonTableJMenuItem.setSelected(true);
        showMirrorJMenuItem.setSelected(false);
        showCheckPeptideJMenuItem.setSelected(false);
        showPredictionJMenuItem.setSelected(false);

        contextJPanel.removeAll();
        contextJPanel.repaint();

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(fragmentIonJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(fragmentIonJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(false);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to Mirror spectrum
     * @param evt Mouse click event
     */
    public void showMirrorJMenuItemActionPerformed(ActionEvent evt) {
        showSpectrumSelected = false;
        ionTableSelected = false;
        mirrorSelected = true;
        peptideCheckSelected = false;
        predictedSelected = false;

        showSpectrumJMenuItem.setSelected(false);
        showIonTableJMenuItem.setSelected(false);
        showMirrorJMenuItem.setSelected(true);
        showCheckPeptideJMenuItem.setSelected(false);
        showPredictionJMenuItem.setSelected(false);

        contextJPanel.removeAll();
        contextJPanel.repaint();

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mirrorJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mirrorJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(true);
        checkFileMenu.setVisible(true);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to Mirror spectrum
     * @param evt Mouse click event
     */
    public void showCheckPeptideJMenuItemActionPerformed(ActionEvent evt) {
        showSpectrumSelected = false;
        ionTableSelected = false;
        mirrorSelected = false;
        peptideCheckSelected = true;
        predictedSelected = false;

        showSpectrumJMenuItem.setSelected(false);
        showIonTableJMenuItem.setSelected(false);
        showMirrorJMenuItem.setSelected(false);
        showCheckPeptideJMenuItem.setSelected(true);
        showPredictionJMenuItem.setSelected(false);

        contextJPanel.removeAll();
        contextJPanel.repaint();

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(checkPeptideJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(checkPeptideJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(true);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(true);
    }

    public void showPredictionJMenuItemActionPerformed(ActionEvent evt){
        showSpectrumSelected = false;
        ionTableSelected = false;
        mirrorSelected = false;
        peptideCheckSelected = false;
        predictedSelected = true;

        showSpectrumJMenuItem.setSelected(false);
        showIonTableJMenuItem.setSelected(false);
        showMirrorJMenuItem.setSelected(false);
        showCheckPeptideJMenuItem.setSelected(false);
        showPredictionJMenuItem.setSelected(true);

        exportGraphicsMenu.setVisible(true);

        try {

            if (parentFrame.predictionEntryHashMap.size() == 0) {
                parentFrame.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/loading.gif")));
                parentFrame.loadingJButton.setText("Loading predicted spectra.");

                if (parentFrame.predictedFileName.endsWith("bin")){
                    DiannSpeclibReader dslr = new DiannSpeclibReader(parentFrame.resultsFolder.getAbsolutePath() + "/MSBooster/" + parentFrame.predictedFileName);
                    parentFrame.predictionEntryHashMap = dslr.getPreds();
                } else {
                    parentFrame.predictionEntryHashMap = importPredictedSpectra(parentFrame.resultsFolder.getAbsolutePath() + "/MSBooster/" + parentFrame.predictedFileName);
                }

                parentFrame.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
                parentFrame. loadingJButton.setText("Import done");
//                ProgressDialogX progressDialog = new ProgressDialogX(parentFrame,
//                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
//                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
//                        true);
//                progressDialog.setPrimaryProgressCounterIndeterminate(true);
//                progressDialog.setTitle("First time Loading predicted spectra. Please Wait...");
//
//                new Thread(() -> {
//                    try {
//                        progressDialog.setVisible(true);
//                    } catch (IndexOutOfBoundsException ignored) {
//                    }
//                }, "PredictedBar").start();
//                new Thread("Import_Predicted_Spectra") {
//                    @Override
//                    public void run() {
//                        try {
//                            if (parentFrame.expInformation.contains("inner_defined_empty_exp")) {
//                                DiannSpeclibReader dslr = new DiannSpeclibReader(parentFrame.resultsFolder.getAbsolutePath() + "/spectraRT.predicted.bin");
//                                parentFrame.predictionEntryHashMap = dslr.getPreds();
//                            } else {
//                                for (File eachFileInMax : Objects.requireNonNull(parentFrame.resultsFolder.listFiles())) {
//                                    if (parentFrame.expInformation.contains(eachFileInMax.getName())) {
//                                        if (new File(eachFileInMax.getAbsolutePath() + "/spectraRT.predicted.bin").exists()) {
//                                            DiannSpeclibReader dslr = new DiannSpeclibReader(eachFileInMax.getAbsolutePath() + "/spectraRT.predicted.bin");
//                                            parentFrame.predictionEntryHashMap.putAll(dslr.getPreds());
//                                        }
//                                    }
//                                }
//                            }
//
//                            //updateSpectrum();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            progressDialog.setRunFinished();
//                            JOptionPane.showMessageDialog(
//                                    parentFrame, "Failed to load predicted spectra, please check it.\n" + e.toString(),
//                                    "Loading spectrum file error", JOptionPane.ERROR_MESSAGE);
//                            System.exit(-1);
//                        }
//                        showPredictionJMenuItemActionPerformed(null);
//                        progressDialog.setRunFinished();
//                    }
//                }.start();
            }
            mainShowJPanel.removeAll();
            GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
            mainShowJPanel.setLayout(mainShowJPanelLayout);

            mainShowJPanelLayout.setHorizontalGroup(
                    mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(predictionJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            );
            mainShowJPanelLayout.setVerticalGroup(
                    mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(predictionJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            );

            mainShowJPanel.revalidate();
            mainShowJPanel.repaint();
            splitterMenu7.setVisible(false);
            checkFileMenu.setVisible(false);
            peptideCheckMenu.setVisible(false);

            updateSpectrum();
        } catch (Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null, e.getMessage(),
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
        }
    }

    private HashMap<String, PredictionEntry> importPredictedSpectra(String mgfFile) throws IOException {
        HashMap<String, PredictionEntry> predictionEntryHashMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(mgfFile));
        String line;
        PredictionEntry currentPre = new PredictionEntry();
        String pepKey = "";
        ArrayList<Float> preMzs = new ArrayList<>();
        ArrayList<Float> preInts = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("BEGIN IONS")) {
                currentPre = new PredictionEntry();
                pepKey = "";
                preMzs = new ArrayList<>();
                preInts = new ArrayList<>();
            } else if (line.startsWith("TITLE")) {
                pepKey = line.split("TITLE=")[1];
            } else if (line.startsWith("CHARGE")) {
                pepKey = pepKey + "|" + line.split("CHARGE=")[1];
            } else if (line.startsWith("RT")) {

            } else if (line.startsWith("1/K0")) {

            } else if (line.startsWith("END IONS")) {
                float[] preMz = new float[preMzs.size()];
                float[] preInt = new float[preInts.size()];
                for (int i = 0; i<preMzs.size(); i++){
                    preMz[i] = preMzs.get(i);
                    preInt[i] = preInts.get(i);
                }
                currentPre.setMzs(preMz);
                currentPre.setIntensities(preInt);
                predictionEntryHashMap.put(pepKey, currentPre);
            } else {
                String[] splitLine = line.split("\t");
                preMzs.add(Float.parseFloat(splitLine[0]));
                if (splitLine[1].contains(" ")){// New MSBooster return different mgf file
                    preInts.add(Float.parseFloat(splitLine[1].split(" ")[0]));
                } else{
                    preInts.add(Float.parseFloat(splitLine[1]));
                }

            }
        }
        return predictionEntryHashMap;
    }

    private MSnSpectrum getPredictSpectrum(){
        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
        Peptide currentPeptide = peptideAssumption.getPeptide();
        String modPep = currentPeptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);
        for (String userMod : ptmFactory.getUserModifications()){
            String modMass = userMod.split(" of")[0];
            modPep = modPep.replaceAll("<" + userMod + ">", "[" + modMass + "]");
        }
        modPep = modPep.replaceAll("TMT-", "[229.1629]");
        modPep = modPep.replaceAll("NH2-", "");
        modPep = modPep.replaceAll("-COOH", "");

        String pepKey = modPep + "|" + peptideAssumption.getIdentificationCharge().value;
        if (!parentFrame.predictionEntryHashMap.containsKey(pepKey)){
            pepKey = pepKey.replace("C[57.0214]", "C[57.0215]");
        }

        if (pepKey.contains("-")){ // Add this block to fix N-term Modification bug
            String nTermMod = pepKey.split("-")[0];
            if (NumberUtils.isNumber(nTermMod)) {
                String restStr = pepKey.split("-")[1];
                pepKey = "[" + nTermMod + "]" + restStr;
            }
        }

        if (parentFrame.predictionEntryHashMap.containsKey(pepKey)){

            PredictionEntry currentPre = parentFrame.predictionEntryHashMap.get(pepKey);

            float[] preMzs = currentPre.getMZs();
            float[] preInts = currentPre.getIntensities();

            HashMap<Double, Peak> peakHashMap = new HashMap<>();
            for (int i = 0; i<preMzs.length; i++){
                if (preInts[i] >= 0) { // TODO add a filter for predicted peaks.
                    Peak peak = new Peak(preMzs[i], preInts[i]);
                    peakHashMap.put((double) preMzs[i], peak);
                }
            }
            peakHashMap.put(0.01, new Peak(0.01, 0.01));

            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(peptideAssumption.getIdentificationCharge());
            Precursor precursor = new Precursor(0.0, 0.0, 0.0, charges);

            return new MSnSpectrum(2, precursor, "", peakHashMap, "");

        } else {
            return null;
        }
    }

    private void changeModificationJMenuItemAction(ActionEvent evt){
        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
        Peptide currentPeptide = peptideAssumption.getPeptide();
        if (!parentFrame.newDefinedMods.containsKey(selectedPsmKey)){
            HashMap<Integer, Object[]> oneHash = new HashMap<>();
            for (ModificationMatch modificationMatch : currentPeptide.getModificationMatches()) {
                String name = modificationMatch.getTheoreticPtm();
                Object[] oneMod = new Object[5];
                oneMod[0] = ptmFactory.getPTM(name).getMass();
                oneMod[1] = ptmFactory.getPTM(name).getMass();
                oneMod[4] = 1;
                if (ptmFactory.getPTM(name).getNeutralLosses().size() != 0){
                    oneMod[3] = ptmFactory.getPTM(name).getNeutralLosses().get(0).getComposition().toString();
                } else {
                    oneMod[3] = " ";
                }
                if (name.contains("N-term")){
                    oneHash.put(0, oneMod);
                    oneMod[2] = 0;
                } else if (name.contains("C-term")){
                    oneHash.put(currentPeptide.getSequence().length() + 1, oneMod);
                    oneMod[2] = currentPeptide.getSequence().length() + 1;
                } else {
                    oneHash.put(modificationMatch.getModificationSite(), oneMod);
                    oneMod[2] = modificationMatch.getModificationSite();
                }
            }
            Object[] oneMod = new Object[5];
            oneMod[0] = parentFrame.deltaMass;
            oneMod[1] = parentFrame.deltaMass;
            oneMod[2] = -1;
            oneMod[3] = " ";
            oneMod[4] = -1;
            oneHash.put(-1, oneMod);
            parentFrame.newDefinedMods.put(selectedPsmKey, oneHash);
        }
        SpectrumMatch oldSpectrumMatch = parentFrame.getSpectrumMatch(selectedPsmKey);
        new NewDefinedModificationDialog(this, parentFrame.newDefinedMods.get(selectedPsmKey),
                oldSpectrumMatch.getBestPeptideAssumption().getPeptide(), selectedPsmKey, parentFrame.modChangeGlobalMap, ptmFactory);

        parentFrame.updateSpectrum(currentPeptide.getSequence());
    }

    /**
     * Select synthetic peptide spectrum
     * @param evt Menu click event
     */
    private void checkFileMenuItemPerform(ActionEvent evt) {
        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Mgf File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File checkSpectrumFile = fileChooser.getSelectedFile();

            if(checkSpectrumFileMaps.containsKey(selectedPsmKey)){
                checkSpectrumFileMaps.remove(selectedPsmKey);
            }

            checkSpectrumFileMaps.put(selectedPsmKey,checkSpectrumFile);

            lastSelectedFolder.setLastSelectedFolder(checkSpectrumFile.getParent());

            updateSpectrum();
        }
    }

    /**
     * Select synthetic peptide spectrum
     * @param evt Menu click event
     */
    private void checkPeptideMenuItemPerform(ActionEvent evt) {

        new CheckPeptideJDialog(this, selectedPsmKey, currentPeptideSequence);
    }

    /**
     * Update spectrum
     */
    public void updateSpectrum(){
        spectrumJLayeredPane.removeAll();
        mirrorJLayeredPane.removeAll();
        checkPeptideJLayeredPane.removeAll();
        predictionJLayeredPane.removeAll();

        int maxCharge = 1;
        ArrayList<ModificationMatch> allModifications = new ArrayList<>();
        ArrayList<ModificationMatch> checkPeptideModificationMatches = new ArrayList<>();
        MSnSpectrum spectrumUsed;

        try {
            if (showPairedETDScan){
                spectrumUsed = pairedSpectrum;
            } else {
                spectrumUsed = currentSpectrum;
            }
            if (spectrumUsed != null) {

                Collection<Peak> peaks = spectrumUsed.getPeakList();

                if (peaks == null || peaks.isEmpty()) {

                } else {

                    boolean newMax = false;

                    if (selectedPsmKey != null) {
                        try {
                            MSnSpectrum tempSpectrum = spectrumUsed;
                            if (tempSpectrum.getPeakList() != null) {
                                lastMzMaximum = tempSpectrum.getMaxMz()*1.05;
                                newMax = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    double lowerMzZoomRange = 0;
                    double upperMzZoomRange = lastMzMaximum;
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) {
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = spectrumUsed.getPrecursor();

                    double[] intensitiesAsArray = spectrumUsed.getIntensityValuesNormalizedAsArray();
                    double[] mzAsArray = spectrumUsed.getMzValuesAsArray();

                    specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(selectedPsmKey, spectrumIdentificationAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);
                    updateGlycanSetting();
                    String modSequence;
                    ArrayList<IonMatch> annotations;

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        updateAnnotationSettings();
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        currentPeptideSequence = tagAssumption.getTag().asSequence();
                        modSequence = tagAssumption.getTag().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, spectrumUsed, tagAssumption.getTag());
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        updateAnnotationSettings();
                        updateGlycanSetting();

                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        Peptide currentPeptide = peptideAssumption.getPeptide();
                        currentPeptideSequence = currentPeptide.getSequence();
                        modSequence = currentPeptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        allModifications = currentPeptide.getModificationMatches();
                        annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, spectrumUsed, currentPeptide, null, ptmFactory, true);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }
                    
                    if(showMatchedPeaksOnly){
                        mzAsArray = removeBackground(annotations).get(0);
                        intensitiesAsArray = removeBackground(annotations).get(1);
                    }

                    spectrumPanel = new SpectrumContainer(
                            mzAsArray, intensitiesAsArray,
                            precursor.getMz(), (spectrumIdentificationAssumption.getIdentificationCharge().value) + "+",
                            "", 40, false, showDetails, false, 2, false, isDenovo);

                    spectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    spectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    spectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    spectrumPanel.setGlycanColor(utilitiesUserPreferences.getAddGlycanColor());
                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        spectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        spectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    spectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    DecimalFormat df = new DecimalFormat("#.00");

                    Double allMatchInt = 0.0;
                    Double allPeakInt = 0.0;
                    HashMap<Integer, ArrayList<String>> bIonMap = new HashMap<>();
                    HashMap<Integer, ArrayList<String>> yIonMap = new HashMap<>();
                    ArrayList<String> bIonList = null;
                    ArrayList<String> yIonList = null;
                    ArrayList<Double> mzList = new ArrayList<>();
                    for (IonMatch ionMatch : annotations){
                        String match = ionMatch.getPeakAnnotation();
                        Integer charge = ionMatch.charge;

                        if (match.contains("b")) {

                            if (!match.contains("-")) {

                                if (bIonMap.containsKey(charge)) {
                                    bIonMap.get(charge).add(match.replace("+", ""));
                                } else {
                                    bIonList = new ArrayList<>();
                                    bIonList.add(match.replace("+", ""));
                                    bIonMap.put(charge, bIonList);
                                }
                            } else {

                                String chargeMatch = match.split("-")[0];

                                if (bIonMap.containsKey(charge)) {
                                    bIonMap.get(charge).add(chargeMatch.replace("+", ""));
                                } else {
                                    bIonList = new ArrayList<>();
                                    bIonList.add(chargeMatch.replace("+", ""));
                                    bIonMap.put(charge, bIonList);
                                }

                            }

                        } else if (match.contains("y")){

                            if (!match.contains("-")) {

                                if (yIonMap.containsKey(charge)) {
                                    yIonMap.get(charge).add(match.replace("+", ""));
                                } else {
                                    yIonList = new ArrayList<>();
                                    yIonList.add(match.replace("+", ""));
                                    yIonMap.put(charge, yIonList);
                                }
                            } else {

                                String chargeMatch = match.split("-")[0];

                                if (yIonMap.containsKey(charge)) {
                                    yIonMap.get(charge).add(chargeMatch.replace("+", ""));
                                } else {
                                    yIonList = new ArrayList<>();
                                    yIonList.add(chargeMatch.replace("+", ""));
                                    yIonMap.put(charge, yIonList);
                                }

                            }
                        }

                        if (!mzList.contains(ionMatch.peak.getMz())){
                            allMatchInt += ionMatch.peak.getIntensity();
                            mzList.add(ionMatch.peak.getMz());
                        }
                    }

                    for (Double each : spectrumUsed.getIntensityValuesAsArray()){
                        allPeakInt += each;
                    }

                    matchedToAllPeakInt = allMatchInt.intValue() + "/" + allPeakInt.intValue() + "  " + df.format((allMatchInt/allPeakInt) * 100) + "%";
                    contentJLabel.setText(matchedToAllPeakInt);
                    contentJLabel.setForeground(new Color(15, 22,255));

                    Integer[] nums = getPair(bIonMap, yIonMap, currentPeptideSequence.length());

                    bIonNumJLabel.setText(String.valueOf(nums[0]));
                    bIonNumJLabel.setForeground(new Color(15, 22,255));
                    yIonNumJLabel.setText(String.valueOf(nums[1]));
                    yIonNumJLabel.setForeground(new Color(15, 22,255));
                    matchNumJLabel.setText(String.valueOf(nums[2]));
                    matchNumJLabel.setForeground(new Color(15, 22,255));

                    int currentCharge = spectrumIdentificationAssumption.getIdentificationCharge().value;
                    if (currentCharge > maxCharge) {
                        maxCharge = currentCharge;
                    }

                    if (forwardIons.size() <= 1 && rewindIons.size() <= 1) {
                        if (forwardIons.size() != 0) {
                            forwardIon = forwardIons.get(0);
                        }

                        if (rewindIons.size() != 0) {
                            rewindIon = rewindIons.get(0);
                        }

                        sequenceFragmentationPanel = new SequenceFragmentationPanel(modSequence, annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        sequenceFragmentationPanelMirror = new SequenceFragmentationPanel(
                                modSequence, annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        sequenceFragmentationPanelPredicted = new SequenceFragmentationPanel(
                                modSequence, annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);

                    } else {
                        sequenceFragmentationPanel = new SequenceFragmentationPanel(modSequence, annotations, true, searchParameters.getPtmSettings(), true);
                        sequenceFragmentationPanelMirror = new SequenceFragmentationPanel(
                                modSequence, annotations, true, searchParameters.getPtmSettings(), true);
                        sequenceFragmentationPanelPredicted = new SequenceFragmentationPanel(
                                modSequence, annotations, true, searchParameters.getPtmSettings(), true);
                    }

                    sequenceFragmentationPanel.setOpaque(false);
                    sequenceFragmentationPanel.setBackground(Color.WHITE);

                    if (spectrumPanel != null && spectrumPanel.getParent() == null) {
                        spectrumJLayeredPane.setLayer(spectrumPanel, JLayeredPane.DEFAULT_LAYER);
                        spectrumJLayeredPane.add(spectrumPanel);
                        spectrumPanel.setBounds(0, 75, parentFrame.spectrumShowJPanel.getWidth()-12,parentFrame.spectrumShowJPanel.getHeight()-110);
                    }

                    spectrumJLayeredPane.setLayer(sequenceFragmentationPanel, JLayeredPane.DRAG_LAYER);
                    spectrumJLayeredPane.add(sequenceFragmentationPanel);
                    zoomAction(sequenceFragmentationPanel, modSequence, false);

                    spectrumSetAction = new SetAction(this, spectrumJLayeredPane, sequenceFragmentationPanel, null, spectrumPanel, 0, 0, spectrumShowPanel);

                    // Mirror 
                    mirrorSpectrumPanel = new SpectrumContainer(
                            mzAsArray, intensitiesAsArray,
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false, isDenovo);

                    sequenceFragmentationPanelMirror.setMinimumSize(new Dimension(sequenceFragmentationPanelMirror.getPreferredSize().width, sequenceFragmentationPanelMirror.getHeight()));
                    sequenceFragmentationPanelMirror.setOpaque(false);
                    sequenceFragmentationPanelMirror.setBackground(Color.WHITE);

                    if(checkSpectrumFileMaps.containsKey(selectedPsmKey)){
                        File checkSpectrumFile = checkSpectrumFileMaps.get(selectedPsmKey);
                        SpectrumFactory spectrumFactory1 = SpectrumFactory.getInstance();
                        spectrumFactory1.addSpectra(checkSpectrumFile);
                        MSnSpectrum mirrorSpectrum = (MSnSpectrum) spectrumFactory1.getSpectrum(checkSpectrumFile.getName(), spectrumFactory1.getSpectrumTitle(checkSpectrumFile.getName(),1));
                        Precursor mirrorPrecursor = mirrorSpectrum.getPrecursor();

                        ArrayList<IonMatch> mirroredAnnotations;

                        if (spectrumIdentificationAssumption instanceof TagAssumption) {
                            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                            mirroredAnnotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, mirrorSpectrum, tagAssumption.getTag());
                        } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                            mirroredAnnotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, mirrorSpectrum, peptideAssumption.getPeptide(), null, ptmFactory, true);
                        } else {
                            throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                        }

                        double[] checkedMzAsArray = mirrorSpectrum.getMzValuesAsArray();
                        double[] checkedIntensitiesAsArray = mirrorSpectrum.getIntensityValuesNormalizedAsArray();
                        if(showMatchedPeaksOnly){
                            checkedMzAsArray = removeBackground(mirroredAnnotations).get(0);
                            checkedIntensitiesAsArray = removeBackground(mirroredAnnotations).get(1);
                        }

                        mirrorSpectrumPanel.addMirroredSpectrum(
                                checkedMzAsArray, checkedIntensitiesAsArray, mirrorPrecursor.getMz(),
                                "", "", false, utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor(),
                                utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor());

                        mirrorSpectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(mirroredAnnotations));

                        if (forwardIons.size() <= 1 && rewindIons.size() <= 1) {
                            if (forwardIons.size() != 0) {
                                forwardIon = forwardIons.get(0);
                            }

                            if (rewindIons.size() != 0) {
                                rewindIon = rewindIons.get(0);
                            }

                            mirrorFragmentPanel = new SequenceFragmentationPanel(
                                    modSequence, mirroredAnnotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        } else {
                            mirrorFragmentPanel = new SequenceFragmentationPanel(
                                    modSequence, mirroredAnnotations, true, searchParameters.getPtmSettings(), true);
                        }
                        mirrorFragmentPanel.setMinimumSize(new Dimension(mirrorFragmentPanel.getPreferredSize().width, mirrorFragmentPanel.getHeight()));
                        mirrorFragmentPanel.setOpaque(false);
                        mirrorFragmentPanel.setBackground(Color.WHITE);

                        mirrorJLayeredPane.setLayer(mirrorFragmentPanel, JLayeredPane.DRAG_LAYER);
                        mirrorJLayeredPane.add(mirrorFragmentPanel);
                        zoomAction(mirrorFragmentPanel, modSequence, true);
                    }

                    mirrorSpectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    mirrorSpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    mirrorSpectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    mirrorSpectrumPanel.setBorder(null);
                    mirrorSpectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    mirrorSpectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    mirrorSpectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    mirrorSpectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    mirrorSpectrumPanel.setGlycanColor(utilitiesUserPreferences.getAddGlycanColor());
                    mirrorSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    mirrorSpectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    mirrorSpectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    mirrorSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);
                    mirrorSpectrumPanel.setMaxPadding(70);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        mirrorSpectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        mirrorSpectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    // Defensive check to prevent IllegalArgumentException
                    if (mirrorSpectrumPanel != null && mirrorSpectrumPanel.getParent() == null) {
                        mirrorJLayeredPane.setLayer(mirrorSpectrumPanel, JLayeredPane.DEFAULT_LAYER);
                        mirrorJLayeredPane.add(mirrorSpectrumPanel);
                        mirrorSpectrumPanel.setBounds(0,75,parentFrame.spectrumShowJPanel.getWidth()-12,parentFrame.spectrumShowJPanel.getHeight()-110);
                    }

                    mirrorJLayeredPane.setLayer(sequenceFragmentationPanelMirror, JLayeredPane.DRAG_LAYER);
                    mirrorJLayeredPane.add(sequenceFragmentationPanelMirror);
                    zoomAction(sequenceFragmentationPanelMirror, modSequence, false);

                    mirrorSetAction = new SetAction(this, mirrorJLayeredPane, sequenceFragmentationPanelMirror, mirrorFragmentPanel, mirrorSpectrumPanel, 0, 0, spectrumShowPanel);

                    // Show checked peptide
                    checkPeptideSpectrumPanel = new SpectrumContainer(
                            mzAsArray, intensitiesAsArray,
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false, isDenovo);

                    sequenceFragmentationPanelCheck = new SequenceFragmentationPanel(
                            modSequence,
                            annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                    sequenceFragmentationPanelCheck.setMinimumSize(new Dimension(sequenceFragmentationPanelCheck.getPreferredSize().width, sequenceFragmentationPanelCheck.getHeight()));
                    sequenceFragmentationPanelCheck.setOpaque(false);
                    sequenceFragmentationPanelCheck.setBackground(Color.WHITE);

                    if(checkPeptideMap.containsKey(selectedPsmKey)){
                        Peptide peptide = checkPeptideMap.get(selectedPsmKey);

                        ArrayList<ModificationMatch> checkModifications = peptide.getModificationMatches();

                        checkPeptideModificationMatches.addAll(checkModifications);

                        String checkModSequence = peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        ArrayList<IonMatch> checkAnnotations =  peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, spectrumUsed, peptide, null, ptmFactory, null);

                        double[] checkedMzAsArray = mzAsArray;
                        double[] checkedIntensitiesAsArray = intensitiesAsArray;
                        if(showMatchedPeaksOnly){
                            checkedMzAsArray = removeBackground(checkAnnotations).get(0);
                            checkedIntensitiesAsArray = removeBackground(checkAnnotations).get(1);
                        }

                        checkPeptideSpectrumPanel.addMirroredSpectrum(checkedMzAsArray, checkedIntensitiesAsArray, precursor.getMz(),
                                "", "", false, utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor(),
                                utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor());

                        checkPeptideSpectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(checkAnnotations));

                        if (forwardIons.size() <= 1 && rewindIons.size() <= 1) {
                            if (forwardIons.size() != 0) {
                                forwardIon = forwardIons.get(0);
                            }

                            if (rewindIons.size() != 0) {
                                rewindIon = rewindIons.get(0);
                            }

                            checkFragmentPanel = new SequenceFragmentationPanel(
                                    checkModSequence, checkAnnotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        } else {
                            checkFragmentPanel = new SequenceFragmentationPanel(
                                    checkModSequence, checkAnnotations, true, searchParameters.getPtmSettings(), true);
                        }
                        checkFragmentPanel.setMinimumSize(new Dimension(checkFragmentPanel.getPreferredSize().width, checkFragmentPanel.getHeight()));
                        checkFragmentPanel.setOpaque(false);
                        checkFragmentPanel.setBackground(Color.WHITE);

                        checkPeptideJLayeredPane.setLayer(checkFragmentPanel, JLayeredPane.DRAG_LAYER);
                        checkPeptideJLayeredPane.add(checkFragmentPanel);
                        zoomAction(checkFragmentPanel, checkModSequence, true);
                        checkAnnotations = null;
                    }

                    checkPeptideSpectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    checkPeptideSpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    checkPeptideSpectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    checkPeptideSpectrumPanel.setBorder(null);
                    checkPeptideSpectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    checkPeptideSpectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    checkPeptideSpectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    checkPeptideSpectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    checkPeptideSpectrumPanel.setGlycanColor(utilitiesUserPreferences.getAddGlycanColor());
                    checkPeptideSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    checkPeptideSpectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    checkPeptideSpectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    checkPeptideSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);
                    checkPeptideSpectrumPanel.setMaxPadding(70);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        checkPeptideSpectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        checkPeptideSpectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    if (checkPeptideSpectrumPanel != null && checkPeptideSpectrumPanel.getParent() == null) {
                        checkPeptideJLayeredPane.setLayer(checkPeptideSpectrumPanel, JLayeredPane.DEFAULT_LAYER);
                        checkPeptideJLayeredPane.add(checkPeptideSpectrumPanel);
                        checkPeptideSpectrumPanel.setBounds(0,75,parentFrame.spectrumShowJPanel.getWidth()-12,parentFrame.spectrumShowJPanel.getHeight()-110);
                    }

                    checkPeptideJLayeredPane.setLayer(sequenceFragmentationPanelCheck, JLayeredPane.DRAG_LAYER);
                    checkPeptideJLayeredPane.add(sequenceFragmentationPanelCheck);
                    zoomAction(sequenceFragmentationPanelCheck, modSequence, false);

                    checkSetAction = new SetAction(this, checkPeptideJLayeredPane, sequenceFragmentationPanelCheck, checkFragmentPanel, checkPeptideSpectrumPanel, 0, 0, spectrumShowPanel);

                    // Show prediction panel
                    predictedSpectrumPanel = new SpectrumContainer(
                            mzAsArray, intensitiesAsArray,
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false, isDenovo);

                    sequenceFragmentationPanelPredicted.setMinimumSize(new Dimension(sequenceFragmentationPanelPredicted.getPreferredSize().width, sequenceFragmentationPanelPredicted.getHeight()));
                    sequenceFragmentationPanelPredicted.setOpaque(false);
                    sequenceFragmentationPanelPredicted.setBackground(Color.WHITE);

                    if (predictedSelected & getPredictSpectrum() != null) {
                        contextJPanel.removeAll();
                        JLabel nameJLabel = new JLabel("Unweighted Spectral Entropy:");
                        nameJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

                        JLabel valueJLabel = new JLabel(parentFrame.getOneItemFromSQL("SpectrumMatch", "SpectralSim", selectedPsmKey));
                        valueJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

                        GroupLayout contextJPanelLayout = new GroupLayout(contextJPanel);
                        contextJPanel.setLayout(contextJPanelLayout);
                        contextJPanelLayout.setHorizontalGroup(
                                contextJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(nameJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(valueJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        );

                        contextJPanelLayout.setVerticalGroup(
                                contextJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(contextJPanelLayout.createSequentialGroup()
                                                .addComponent(nameJLabel, 12, 25, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(valueJLabel, 12, 25, GroupLayout.PREFERRED_SIZE))
                        );
                    }

                    if(getPredictSpectrum() != null){

                        MSnSpectrum preSpectrum = getPredictSpectrum();
                        Precursor prePrecursor = preSpectrum.getPrecursor();
                        predictedSpectrumPanel.addMirroredSpectrum(
                                preSpectrum.getMzValuesAsArray(), preSpectrum.getIntensityValuesNormalizedAsArray(), prePrecursor.getMz(),
                                "", "", false, utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor(),
                                utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor());

                        ArrayList<IonMatch> predictedAnnotations;

                        if (spectrumIdentificationAssumption instanceof TagAssumption) {
                            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                            predictedAnnotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, preSpectrum, tagAssumption.getTag());
                        } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                            predictedAnnotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, preSpectrum, peptideAssumption.getPeptide(), null, ptmFactory, true);
                        } else {
                            throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                        }

                        predictedSpectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(predictedAnnotations));

                        if (forwardIons.size() <= 1 && rewindIons.size() <= 1) {
                            if (forwardIons.size() != 0) {
                                forwardIon = forwardIons.get(0);
                            }

                            if (rewindIons.size() != 0) {
                                rewindIon = rewindIons.get(0);
                            }

                            predictedFragmentPanel = new SequenceFragmentationPanel(
                                    modSequence, predictedAnnotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        } else {
                            predictedFragmentPanel = new SequenceFragmentationPanel(
                                    modSequence, predictedAnnotations, true, searchParameters.getPtmSettings(), true);
                        }
                        predictedFragmentPanel.setMinimumSize(new Dimension(predictedFragmentPanel.getPreferredSize().width, predictedFragmentPanel.getHeight()));
                        predictedFragmentPanel.setOpaque(false);
                        predictedFragmentPanel.setBackground(Color.WHITE);

                        predictionJLayeredPane.setLayer(predictedFragmentPanel, JLayeredPane.DRAG_LAYER);
                        predictionJLayeredPane.add(predictedFragmentPanel);

                        zoomAction(predictedFragmentPanel, modSequence, true);
                    }

                    predictedSpectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    predictedSpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    predictedSpectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    predictedSpectrumPanel.setBorder(null);
                    predictedSpectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    predictedSpectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    predictedSpectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    predictedSpectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    predictedSpectrumPanel.setGlycanColor(utilitiesUserPreferences.getAddGlycanColor());
                    predictedSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    predictedSpectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    predictedSpectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    predictedSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);
                    predictedSpectrumPanel.setMaxPadding(70);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        predictedSpectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        predictedSpectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    // Defensive check to prevent IllegalArgumentException
                    if (predictedSpectrumPanel != null && predictedSpectrumPanel.getParent() == null) {
                        predictionJLayeredPane.setLayer(predictedSpectrumPanel, JLayeredPane.DEFAULT_LAYER);
                        predictionJLayeredPane.add(predictedSpectrumPanel);
                        predictedSpectrumPanel.setBounds(0,75,parentFrame.spectrumShowJPanel.getWidth()-12,parentFrame.spectrumShowJPanel.getHeight()-110);
                    }

                    predictionJLayeredPane.setLayer(sequenceFragmentationPanelPredicted, JLayeredPane.DRAG_LAYER);
                    predictionJLayeredPane.add(sequenceFragmentationPanelPredicted);
                    zoomAction(sequenceFragmentationPanelPredicted, modSequence, false);

                    preSetAction = new SetAction(this, predictionJLayeredPane, sequenceFragmentationPanelPredicted, predictedFragmentPanel, predictedSpectrumPanel, 0, 0, spectrumShowPanel);

                    ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<>();
                    allAnnotations.add(annotations);

                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        ionFragmentsJScrollPane.setViewportView(new FragmentIonTable(peptideAssumption.getPeptide(), allAnnotations, specificAnnotationSettings.getFragmentIonTypes(),
                                specificAnnotationSettings.getNeutralLossesMap(),
                                specificAnnotationSettings.getSelectedCharges().contains(1),
                                specificAnnotationSettings.getSelectedCharges().contains(2)));
                    }

                    spectrumShowPanel.revalidate();
                    spectrumShowPanel.repaint();

                    updateAnnotationMenus(maxCharge, allModifications, checkPeptideModificationMatches);

                    annotations = null;
                    allModifications = null;
                    bIonMap = null;
                    yIonMap = null;
                    bIonList = null;
                    yIonList = null;
                    allAnnotations = null;
                }
            }
            //System.gc();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when Update spectrum.", "update error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private ArrayList<double[]> removeBackground(ArrayList<IonMatch> annotations){
        double[] mzAsArray = new double[annotations.size() +1];
        double[] intensitiesAsArray = new double[annotations.size()+1];

        double highestInt = 0.0;
        mzAsArray[0] = 0.0;
        intensitiesAsArray[0] = 0.0;
        for (int i = 1; i < annotations.size()+1; i++){
            mzAsArray[i] = annotations.get(i - 1).peak.mz;
            intensitiesAsArray[i] = annotations.get(i - 1).peak.intensity;
            if (annotations.get(i - 1).peak.intensity > highestInt){
                highestInt = annotations.get(i - 1).peak.intensity;
            }
        }
        for (int i = 0; i < intensitiesAsArray.length; i++){
            intensitiesAsArray[i] = intensitiesAsArray[i] * 100/highestInt;
        }

        ArrayList<double[]> output = new ArrayList<>();
        output.add(mzAsArray);
        output.add(intensitiesAsArray);
        return output;
    }

    /**
     * Zoom in out
     * @param sequenceFragmentationPanel Sequence fragment panel
     * @param modSequence Peptide Sequence
     * @param isDown Down or not
     */
    private void zoomAction(SequenceFragmentationPanel sequenceFragmentationPanel, String modSequence, Boolean isDown){

        JPanel fragmentJPanel = new JPanel();
        fragmentJPanel.setOpaque(false);

        int fontHeight = sequenceFragmentationPanel.getFontMetrics(sequenceFragmentationPanel.getFont()).getHeight() + 3;
        int length = 0;

        String[] firstSplit = modSequence.split("<");

        for (String firstOne : firstSplit){
            String[] secondSplit = firstOne.split(">");
            if (secondSplit.length == 1){
                length += secondSplit[0].length();
            } else {
                length += secondSplit[1].length();
            }
        }

        final int peptideLength = length;

        if (!isDown){
            sequenceFragmentationPanel.setBounds(40,10, peptideLength*fontHeight*2 ,fontHeight * 8);
        } else {
            sequenceFragmentationPanel.setBounds(40,spectrumShowPanel.getHeight() -fontHeight * 7, peptideLength*fontHeight*2 ,fontHeight * 8);
        }

        sequenceFragmentationPanel.addMouseWheelListener(e -> {
            if(e.getWheelRotation()==1){
                sequenceFragmentationPanel.updateFontSize(-1);

                sequenceFragmentationPanel.revalidate();
                sequenceFragmentationPanel.repaint();
            }
            if(e.getWheelRotation()==-1){
                sequenceFragmentationPanel.updateFontSize(1);

                sequenceFragmentationPanel.revalidate();
                sequenceFragmentationPanel.repaint();
            }
        });
    }

    /**
     * Get by pairs
     * @param bIonMap B ion map
     * @param yIonMap Y ion map
     * @param length Peptide length
     * @return All details
     */
    private Integer[] getPair(HashMap<Integer, ArrayList<String>> bIonMap, HashMap<Integer, ArrayList<String>> yIonMap, Integer length){
        Integer[] nums = new Integer[3];
        Integer bIonNum = 0;
        Integer yIonNum = 0;
        Integer pairNum = 0;
        ArrayList<String> bIonList;
        ArrayList<String> yIonList;
        ArrayList<Integer> bIonLength = new ArrayList<>();
        ArrayList<Integer> yIonLength = new ArrayList<>();

        for (Integer eachCharge : bIonMap.keySet()){
            bIonList = bIonMap.get(eachCharge);

            bIonNum += bIonList.size();

            for (String eachMatch : bIonList){

                int ionLength = Integer.valueOf(eachMatch.substring(1, eachMatch.length()));

                if (!bIonLength.contains(ionLength)){
                    bIonLength.add(ionLength);
                }
            }
        }
        for (Integer eachCharge : yIonMap.keySet()){
            yIonList = yIonMap.get(eachCharge);

            yIonNum += yIonList.size();

            for (String eachYMatch : yIonList){

                int ionLength = length - Integer.valueOf(eachYMatch.substring(1,eachYMatch.length()));

                if (!yIonLength.contains(ionLength)) {
                    yIonLength.add(ionLength);
                }
            }
        }

        for (Integer bIon : bIonLength){
            if (yIonLength.contains(bIon)){
                pairNum ++;
            }
        }

        nums[0] = bIonNum;
        nums[1] = yIonNum;
        nums[2] = pairNum;

        return nums;
    }

    /**
     * Update the spectrum
     * @param spectrumIdentificationAssumption SpectrumIdentificationAssumption
     * @param spectrum MSNSpectrum
     * @param selectedPsmKey Spectrum key
     */
    public void updateSpectrum(SpectrumIdentificationAssumption spectrumIdentificationAssumption, MSnSpectrum spectrum, String selectedPsmKey,
                               String pairedScanNum, MSnSpectrum pairedSpectrum){

        ionsMenu.setEnabled(true);
        otherMenu.setEnabled(true);
        lossMenu.setEnabled(true);
        chargeMenu.setEnabled(true);
        settingsMenu.setEnabled(true);
        exportGraphicsMenu.setEnabled(true);
        switchPaneMenu.setVisible(true);
        switchPaneMenu.setEnabled(true);
        showPairedETDScanMenuItem.setSelected(false);
        showPairedETDScan = false;

        this.spectrumIdentificationAssumption = spectrumIdentificationAssumption;
        this.currentSpectrum = spectrum;
        this.selectedPsmKey = selectedPsmKey;
        this.pairedScanNum = pairedScanNum;
        this.pairedSpectrum = pairedSpectrum;

        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
        Peptide currentPeptide = peptideAssumption.getPeptide();
        if (currentPeptide.getModificationMatches() != null){
            for (ModificationMatch modificationMatch :currentPeptide.getModificationMatches()){
                oriModificationMatches.put(modificationMatch.getModificationSite(), modificationMatch);
            }
        }

        if (!parentFrame.hasPredictionSpectra){
            showPredictionJMenuItem.setEnabled(false);
        }

        updateSpectrum();
    }

    private void updateGlycanSetting(){

        ArrayList<ModificationMatch> newModificationMatches = new ArrayList<>();
        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
        Peptide currentPeptide = peptideAssumption.getPeptide();

        for (ModificationMatch modificationMatch :currentPeptide.getModificationMatches()){
//            System.out.println(modificationMatch.getTheoreticPtm());
            if ((modificationMatch.getTheoreticPtm().split(" of ")[1]).equals("N")
                    && (modificationMatch.getTheoreticPtm().equals("203.079 of N") || modificationMatch.getTheoreticPtm().equals("0.0 of N")
                    || Double.parseDouble(modificationMatch.getTheoreticPtm().split(" of")[0]) > 500)){
                if (Arrays.equals(glycanMenuItemSelection, new Integer[]{0, 0})){
                    newModificationMatches.add(oriModificationMatches.get(modificationMatch.getModificationSite()));
                } else if (glycanMenuItemSelection[0] == 1){
                    newModificationMatches.add(new ModificationMatch("0.0 of N", true, modificationMatch.getModificationSite()));
                } else if (glycanMenuItemSelection[1] == 1){
                    newModificationMatches.add(new ModificationMatch("203.079 of N", true, modificationMatch.getModificationSite()));
                }

            } else if ( ((modificationMatch.getTheoreticPtm().split(" of ")[1]).equals("S") ||
                    (modificationMatch.getTheoreticPtm().split(" of ")[1]).equals("T") ||
                    (modificationMatch.getTheoreticPtm().split(" of ")[1]).equals("ST"))
                    && (modificationMatch.getTheoreticPtm().equals("0.0 of ST")
                    || Double.parseDouble(modificationMatch.getTheoreticPtm().split(" of")[0]) > 100)){
                if (Arrays.equals(glycanMenuItemSelection, new Integer[]{0, 0})){
                    newModificationMatches.add(oriModificationMatches.get(modificationMatch.getModificationSite()));
                } else if (glycanMenuItemSelection[0] == 1){
                    newModificationMatches.add(new ModificationMatch("0.0 of ST", true, modificationMatch.getModificationSite()));
                } else if (glycanMenuItemSelection[1] == 1){
                    newModificationMatches.add(oriModificationMatches.get(modificationMatch.getModificationSite()));
                }

            } else {
                newModificationMatches.add(modificationMatch);
            }
        }

        currentPeptide.setModificationMatches(newModificationMatches);
        spectrumIdentificationAssumption = new PeptideAssumption(currentPeptide, peptideAssumption.getIdentificationCharge());
    }

    /**
     * Update teh search parameter
     * @param searchParameters Parameter
     */
    public void updateSearchParameters(SearchParameters searchParameters){
        this.searchParameters = searchParameters;

        annotationSettings.setPreferencesFromSearchParameters(searchParameters);
    }

    /**
     * Get the current delta masses map
     * @return Current delta masses map
     */
    private HashMap<Double, String> getCurrentMassDeltas() {
        HashMap<Double, String> currentMassDeltaMap = new HashMap<>();

        PtmSettings ptmSettings = searchParameters.getPtmSettings();
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

                if (ptm.getType() == PTM.MODAA && aminoAcidPattern != null) {
                    for (Character character : aminoAcidPattern.getAminoAcidsAtTarget()) {
                        if (!currentMassDeltaMap.containsValue(character + "<" + shortName + ">")) {
                            AminoAcid aminoAcid = AminoAcid.getAminoAcid(character);
                            double aminoAcidMass = aminoAcid.getMonoisotopicMass();
                            currentMassDeltaMap.put(mass + aminoAcidMass,
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

    /**
     * Update the annotation menu bar
     * @param precursorCharge Precursor charges
     * @param modificationMatches Modification matches list
     */
    private void updateAnnotationMenus(int precursorCharge, ArrayList<ModificationMatch> modificationMatches, ArrayList<ModificationMatch> checkPeptideModificationMatches) {

        forwardAIonCheckBoxMenuItem.setSelected(false);
        forwardBIonCheckBoxMenuItem.setSelected(false);
        forwardCIonCheckBoxMenuItem.setSelected(false);
        rewardXIonCheckBoxMenuItem.setSelected(false);
        rewardYIonCheckBoxMenuItem.setSelected(false);
        rewardZIonCheckBoxMenuItem.setSelected(false);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(false);
        precursorCheckMenuItem.setSelected(false);
        immoniumIonsCheckMenuItem.setSelected(false);
        relatedIonsCheckMenuItem.setSelected(false);
        reporterIonsCheckMenuItem.setSelected(false);
        glycansCheckMenuItem.setSelected(false);
        glycanZeroCheckMenuItem.setSelected(false);
        glycanZeroCheckMenuItem.setEnabled(false);
        glycanOneCheckMenuItem.setSelected(false);
        glycanOneCheckMenuItem.setEnabled(false);
        showPairedETDScanMenuItem.setVisible(false);

        for (JCheckBoxMenuItem lossMenuItem : lossMenuMap.values()) {
            lossMenu.remove(lossMenuItem);
        }
        lossMenu.setVisible(true);
        lossSplitter.setVisible(true);
        lossMenuMap.clear();

        HashMap<String, NeutralLoss> neutralLossHashMap = new HashMap<>();

        for (NeutralLoss neutralLoss : IonFactory.getInstance().getDefaultNeutralLosses()) {
            neutralLossHashMap.put(neutralLoss.name, neutralLoss);
        }

        for (ModificationMatch modificationMatch : modificationMatches) {

            NeutralLoss neutralLoss = getPhosphyNeutralLoss(modificationMatch);

            if (neutralLoss != null){
                neutralLossHashMap.put(neutralLoss.name, neutralLoss);
            }
        }

        for (ModificationMatch modificationMatch : checkPeptideModificationMatches) {

            NeutralLoss neutralLoss = getPhosphyNeutralLoss(modificationMatch);

            if (neutralLoss != null){
                neutralLossHashMap.put(neutralLoss.name, neutralLoss);
            }
        }

        if (newDefinedLosses.size() != 0){
            neutralLossHashMap.put(newDefinedLosses.get(0).name, newDefinedLosses.get(0));
        }

        ArrayList<String> neutralLossNameList = new ArrayList<>(neutralLossHashMap.keySet());
        Collections.sort(neutralLossNameList);

        if (neutralLossHashMap.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {

            ArrayList<String> currentNeutralLosses;
            boolean neutralLossesAuto;
            if (specificAnnotationSettings != null) {
                currentNeutralLosses = specificAnnotationSettings.getNeutralLossesMap().getAccountedNeutralLosses();
                neutralLossesAuto = specificAnnotationSettings.isNeutralLossesAuto();
            } else {
                ArrayList<NeutralLoss> annotationNeutralLosses = annotationSettings.getNeutralLosses();
                if (annotationNeutralLosses != null) {
                    currentNeutralLosses = new ArrayList<>(annotationNeutralLosses.size());
                    for (NeutralLoss neutralLoss : annotationNeutralLosses) {
                        currentNeutralLosses.add(neutralLoss.name);
                    }
                } else {
                    currentNeutralLosses = new ArrayList<>(0);
                }
                neutralLossesAuto = true;
            }

            for (int i = 0; i < neutralLossNameList.size(); i++) {

                String neutralLossName = neutralLossNameList.get(i);
                NeutralLoss neutralLoss = neutralLossHashMap.get(neutralLossName);

                boolean selected = false;
                for (String specificNeutralLossName : currentNeutralLosses) {
                    NeutralLoss specificNeutralLoss = NeutralLoss.getNeutralLoss(specificNeutralLossName);
                    if (neutralLoss.isSameAs(specificNeutralLoss)) {
                        selected = true;
                        break;
                    }
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(neutralLossName);
                lossMenuItem.setSelected(selected);
                lossMenuItem.setEnabled(!neutralLossesAuto);
                lossMenuItem.addActionListener(evt -> updateSpectrum());
                lossMenuMap.put(neutralLossHashMap.get(neutralLossName), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
            defaultLossCheckBoxMenuItem.setSelected(neutralLossesAuto);
        }

        chargeMenuMap.clear();
        chargeMenu.removeAll();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        ArrayList<Integer> selectedCharges;
        if (specificAnnotationSettings != null) {
            selectedCharges = specificAnnotationSettings.getSelectedCharges();
        } else {
            selectedCharges = new ArrayList<>();
            selectedCharges.add(1);
        }

        for (Integer charge = 1; charge <= precursorCharge; charge++) {

            final JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            chargeMenuItem.setSelected(selectedCharges.contains(charge));
            chargeMenuItem.addActionListener(evt -> {
                noSelectDefaultAnnotationMenuItem();
                updateSpectrumAnnotations();
            });

            chargeMenuMap.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        HashMap<Ion.IonType, HashSet<Integer>> ionTypes;
        if (specificAnnotationSettings != null) {
            ionTypes = specificAnnotationSettings.getIonTypes();
        } else {
            ionTypes = annotationSettings.getIonTypes();
        }

        for (Ion.IonType ionType : ionTypes.keySet()) {
            if (null != ionType) {
                switch (ionType) {
                    case IMMONIUM_ION:
                        immoniumIonsCheckMenuItem.setSelected(true);
                        break;
                    case RELATED_ION:
                        relatedIonsCheckMenuItem.setSelected(true);
                        break;
                    case PRECURSOR_ION:
                        precursorCheckMenuItem.setSelected(true);
                        break;
                    case REPORTER_ION:
                        reporterIonsCheckMenuItem.setSelected(true);
                        break;
                    case GLYCAN:
                        glycansCheckMenuItem.setSelected(true);

                    case TAG_FRAGMENT_ION:
                        for (int subtype : ionTypes.get(ionType)) {
                            switch (subtype) {
                                case TagFragmentIon.A_ION:
                                    forwardAIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.B_ION:
                                    forwardBIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.C_ION:
                                    forwardCIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.X_ION:
                                    rewardXIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Y_ION:
                                    rewardYIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Z_ION:
                                    rewardZIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        if (Arrays.equals(glycanMenuItemSelection, new Integer[]{0, 0})){
            glycanZeroCheckMenuItem.setEnabled(true);
            glycanOneCheckMenuItem.setEnabled(true);
        } else if (glycanMenuItemSelection[0] == 1){
            glycanZeroCheckMenuItem.setEnabled(true);
            glycanZeroCheckMenuItem.setSelected(true);
        } else if (glycanMenuItemSelection[1] == 1){
            glycanOneCheckMenuItem.setEnabled(true);
            glycanOneCheckMenuItem.setSelected(true);
        }

        if (showSpectrumSelected){
            showSpectrumJMenuItem.setSelected(true);
            showIonTableJMenuItem.setSelected(false);
            showMirrorJMenuItem.setSelected(false);
            showCheckPeptideJMenuItem.setSelected(false);
            showPredictionJMenuItem.setSelected(false);
        } else if (ionTableSelected){
            showSpectrumJMenuItem.setSelected(false);
            showIonTableJMenuItem.setSelected(true);
            showMirrorJMenuItem.setSelected(false);
            showCheckPeptideJMenuItem.setSelected(false);
            showPredictionJMenuItem.setSelected(false);
        } else if (mirrorSelected){
            showSpectrumJMenuItem.setSelected(false);
            showIonTableJMenuItem.setSelected(false);
            showMirrorJMenuItem.setSelected(true);
            showCheckPeptideJMenuItem.setSelected(false);
            showPredictionJMenuItem.setSelected(false);
        } else if (peptideCheckSelected){
            showSpectrumJMenuItem.setSelected(false);
            showIonTableJMenuItem.setSelected(false);
            showMirrorJMenuItem.setSelected(false);
            showCheckPeptideJMenuItem.setSelected(true);
            showPredictionJMenuItem.setSelected(false);
        } else if (predictedSelected){
            showSpectrumJMenuItem.setSelected(false);
            showIonTableJMenuItem.setSelected(false);
            showMirrorJMenuItem.setSelected(false);
            showCheckPeptideJMenuItem.setSelected(false);
            showPredictionJMenuItem.setSelected(true);
        }

        showAllPeaksMenuItem.setSelected(annotationSettings.showAllPeaks());
        showMatchesPeaksMenuItem.setSelected(showMatchedPeaksOnly);
        if (pairedScanNum != null){
            showPairedETDScanMenuItem.setVisible(true);
        }
    }

    private NeutralLoss getPhosphyNeutralLoss(ModificationMatch modificationMatch){

        String name = modificationMatch.getTheoreticPtm();
        String aa = name.split("of ")[1];

        if (aa.equals("T") || aa.equals("S")){
            double mass = ptmFactory.getPTM(name).getMass();

            if (mass < 80.01 && mass > 79.9){
                return H3PO4;
            }
        }
        return null;

    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    private void updateAnnotationSettings() {

        forwardIons = new ArrayList();
        rewindIons = new ArrayList();
        glycanMenuItemSelection = new Integer[]{0,0};

        if (!defaultAnnotationCheckBoxMenuItem.isSelected()) {

            specificAnnotationSettings.clearIonTypes();
            if (forwardAIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.A_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
                this.forwardIons.add(0);
            }
            if (forwardBIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                this.forwardIons.add(1);
            }
            if (forwardCIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.C_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
                this.forwardIons.add(2);
            }
            if (rewardXIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.X_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
                this.rewindIons.add(3);
            }
            if (rewardYIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                this.rewindIons.add(4);
            }
            if (rewardZIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Z_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
                this.rewindIons.add(5);
            }
            if (precursorCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(PRECURSOR_ION);
            }
            if (immoniumIonsCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(IMMONIUM_ION);
            }
            if (relatedIonsCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(RELATED_ION);
            }
            if (reporterIonsCheckMenuItem.isSelected()) {
                ArrayList<Integer> reporterIons = new ArrayList<>(IonFactory.getReporterIons(searchParameters.getPtmSettings()));
                for (int subtype : reporterIons) {
                    specificAnnotationSettings.addIonType(REPORTER_ION, subtype);
                }
            }

            if (glycansCheckMenuItem.isSelected()){
                specificAnnotationSettings.addIonType(GLYCAN);
            }

            if (glycanZeroCheckMenuItem.isSelected()){
                glycanMenuItemSelection[0] = 1;
            }
            if (glycanOneCheckMenuItem.isSelected()){
                glycanMenuItemSelection[1] = 1;
            }

            if (!defaultLossCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.setNeutralLossesAuto(false);
                specificAnnotationSettings.clearNeutralLosses();
                for (NeutralLoss neutralLoss : lossMenuMap.keySet()) {
                    if (lossMenuMap.get(neutralLoss).isSelected()) {
                        specificAnnotationSettings.addNeutralLoss(neutralLoss);
                    }
                }
            } else {
                specificAnnotationSettings.clearNeutralLosses();
            }

            specificAnnotationSettings.clearCharges();
            for (int charge : chargeMenuMap.keySet()) {
                if (chargeMenuMap.get(charge).isSelected()) {
                    specificAnnotationSettings.addSelectedCharge(charge);
                }
            }

        } else {
            specificAnnotationSettings.clearNeutralLosses();
            selectDefaultAnnotationMenuItem();
        }

        annotationSettings.setShowAllPeaks(showAllPeaksMenuItem.isSelected());
        showMatchedPeaksOnly = showMatchesPeaksMenuItem.isSelected();
        annotationSettings.setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
        annotationSettings.setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

        if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
            annotationSettings.setDeNovoCharge(1);
        } else {
            annotationSettings.setDeNovoCharge(2);
        }
    }

    /**
     * Update annotation settings
     */
    public void updateSpectrumAnnotations() {
        updateSpectrum();
    }

    /**
     * SelectDefaultAnnotationMenuItem
     */
    private void selectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
    }

    /**
     * load userPreference
     */
    public void loadUserPreferences() {
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
            lastSelectedFolder = new LastSelectedFolder("user.home");
            lastSelectedFolder = utilitiesUserPreferences.getLastSelectedFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
        }
    }

    /**
     * Set annotation setting
     * @param annotationSettings AnnotationSetting
     */
    public void setAnnotationSettings(AnnotationSettings annotationSettings) {
        this.annotationSettings = annotationSettings;
    }

    /**
     * Get annotation settings
     * @return AnnotationSettings
     */
    public AnnotationSettings getAnnotationSettings() {
        return annotationSettings;
    }

    /**
     * Set utilitiesUserPreferences
     */
    public void setUtilitiesUserPreferences(UtilitiesUserPreferences utilitiesUserPreferences){
        this.utilitiesUserPreferences = utilitiesUserPreferences;
    }

    /**
     * Get spectrum main panel all parameters
     * @return Object[]
     */
    public Object[] getParameters() {

        Object[] allParameters = new Object[6];
        allParameters[0] = forwardIon;
        allParameters[1] = rewindIon;
        allParameters[2] = peptideSpectrumAnnotator;
        allParameters[3] = specificAnnotationSettings;
        allParameters[4] = getCurrentMassDeltas();
        allParameters[5] = searchParameters.getPtmSettings();

        return allParameters;
    }
}
