package GUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import GUI.Export.ExportBatchDialog;
import GUI.Export.RealTimeExportJDialog;
import GUI.utils.ExperimentTableModel;
import GUI.utils.ImportData;
import GUI.utils.PSMTableModel;
import GUI.utils.ProteinTableModel;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.*;
import GUI.DB.SQLiteConnection;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.diann.PredictionEntry;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

public class GUIMainClass extends JFrame {

    public JButton searchButton;
    public JButton loadingJButton;
    public JButton settingColorJButton;
    public JTextField pSMPageNumJTextField;
    public JTextField proteinPageNumJTextField;
    public JTextField searchItemTextField;
    private JButton psmUpSortJButton;
    private JButton psmDownSortJButton;
    private JButton proteinUpSortJButton;
    private JButton proteinDownSortJButton;
    private JButton pSMNextJButton;
    private JButton pSMUpJButton;
    private JButton proteinNextJButton;
    private JButton proteinUpJButton;
//    private JButton openSearchFileJButton;
    private JButton showAllSelectedProteinsJButton;
    private JButton psmColumnSelectionJButton;
    private JButton proteinColumnSelectionJButton;
    private JTable experimentsJTable;
    private JTable proteinsJTable;
    private JTable spectrumJTable;
    private JPanel psmsJPanel;
    private JPanel experimentsJPanel;
    private JPanel proteinsJPanel;
    private JPanel coverageJPanel;
    public JPanel spectrumShowJPanel;
    private JPanel backgroundPanel;
    private JPanel searchTextOrButtonJPanel;
    private JPanel coveragePaintPanel;
    private JScrollPane coverageScrollPane;
    private JTextField pSMPageSelectNumJTextField;
    private JTextField proteinPageSelectNumJTextField;
    private JCheckBox pSMAllSelectedJCheckBox;
    private JCheckBox proteinAllSelectedJCheckBox;
    public JComboBox psmSortColumnJCombox;
    public JComboBox proteinSortColumnJCombox;
    private JComboBox searchTypeComboBox;
//    private String[] searchType = new String[]{"Peptide (String)","Spectrum (String)", "Peptide (File)", "Spectrum (File)", "Protein (String)", "Protein (File)"};
    private String[] searchType = new String[]{"Peptide (String)","Spectrum (String)", "Protein (String)"};

    /**
     * SpectrumTable tooltips list
     */
    private ArrayList<String> experimentJTableToolTips;
    /**
     * SpectrumTable tooltips list
     */
    private ArrayList<String> spectrumJTableToolTips;
    /**
     * SpectrumTable tooltips list
     */
    private ArrayList<String> proteinsJTableToolTips;
    /**
     * Experiment table model
     */
    private ExperimentTableModel experimentTableModel;
    /**
     * Protein table model
     */
    private ProteinTableModel proteinTableModel;
    /**
     * PSM table model
     */
    private PSMTableModel psmTableModel;
    /**
     * SearchParameters
     */
    public SearchParameters searchParameters = new SearchParameters();
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    /**
     * ExceptionHandler import from utilities
     */
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/wenbostar/PDV");
    /**
     * Current psmKey selected
     */
    private String selectedPsmKey;
    /**
     * Selected page spectrum index
     */
    private ArrayList<String> selectPageSpectrumIndex = new ArrayList<>();
    /**
     * Selected page protein index
     */
    private ArrayList<String> selectPageProteinIndex = new ArrayList<>();
    /**
     * Current page psm key to selected
     */
    private HashMap<String, Boolean> spectrumKeyToSelected = new HashMap<>();
    /**
     * Current page protein key to selected
     */
    private HashMap<String, Boolean> proteinKeyToSelected = new HashMap<>();
    /**
     * Current page experiment key to selected
     */
    private HashMap<String, Boolean> experimentKeyToSelected = new HashMap<>();
    /**
     * Whole page selections
     */
    private ArrayList<Integer> pSMPageToSelected = new ArrayList<>();
    /**
     * All selections
     */
    private ArrayList<String> pSMAllSelections = new ArrayList<>();
    /**
     * All selections
     */
    private ArrayList<String> proteinAllSelections = new ArrayList<>();
    /**
     * Current proteins selections
     */
    private ArrayList<String> proteinCurrentSelections = new ArrayList<>();
    /**
     * All selections
     */
    private ArrayList<String> expAllSelections = new ArrayList<>();
    /**
     * Current exp All selections
     */
    private ArrayList<String> currentExpAllSelections = new ArrayList<>();
    /**
     * PTMFactory containing all modifications import from utilities
     */
    public PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Find Type
     */
    private String findType = "Peptide (String)";
    /**
     * Current identification assumption
     */
    private SpectrumIdentificationAssumption spectrumIdentificationAssumption = null;
    /**
     * Spectrum main Panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Database connection
     */
    private SQLiteConnection sqliteConnection;
    /**
     * Spectrum match
     */
    private SpectrumMatch spectrumMatch;
    /**
     * Database absolute path
     */
    public String databasePath;
    /**
     * Export all spectrum or not
     */
    private Boolean exportAll = false;
    /**
     * Export selected spectrum or not
     */
    private Boolean exportSelection = false;
    /**
     *
     */
    private Boolean nonProteinSearchMode = false;
    /**
     * All spectrum Index
     */
    public ArrayList<ArrayList<String>> allSpectrumIndex = new ArrayList<>();
    /**
     * Mapped spectrum Index
     */
    private ArrayList<String> mappedSpectrumIndex = new ArrayList<>();
    /**
     * All protein Index
     */
    public ArrayList<ArrayList<String>> allProteinIndex = new ArrayList<>();
    /**
     * Current page default 1
     */
    public int selectedPSMPageNum = 1;
    /**
     * Current page default 1
     */
    public int selectedProteinPageNum = 1;
    /**
     * All score names from Identification file
     */
    private ArrayList<String> psmScoreName = new ArrayList<>();
    /**
     * All score names from Identification file
     */
    private ArrayList<String> proteinScoreName = new ArrayList<>();
    /**
     * Selected column map
     */
    public HashMap<String, Boolean> pSMColumnToSelected;
    /**
     * Selected column map
     */
    public HashMap<String, Boolean> proteinColumnToSelected;
    /**
     * Select index file
     */
    private File selectIndexFile;
    /**
     * Results folder
     */
    private File resultsFolder;
    /**
     * Threads number
     */
    private int threadsNumber = 1;
    /**
     *
     */
    private HashMap<String, String> proteinSeqMap;
    /**
     *
     */
    private ArrayList<String> expNumList = new ArrayList<>();
    /**
     *
     */
    private HashMap<String, Integer[]> experimentInfo = new HashMap<>();
    /**
     *
     */
    private HashMap<String, String> spectrumFileTypes = new HashMap<>();
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();;
    /**
     * Instance to save mzML files
     */
    private HashMap<String, ScanCollectionDefault> scansFileHashMap = new HashMap<>();
    /**
     * Spectrum file lists
     */
    private HashMap<String, String> spectrumFileMap = new HashMap<>();
    /**
     *
     */
    private ArrayList<String> spectrumFileOrder = new ArrayList<>();
    /**
     *
     */
    private ArrayList<String> finishedSpectrumFiles = new ArrayList<>();
    /**
     *
     */
    private Thread readFactoryThread;
    /**
     *
     */
    private ArrayList<String> mgfFiles = new ArrayList<>();
    /**
     *
     */
    public HashMap<String, PredictionEntry> predictionEntryHashMap = new HashMap<>();
    /**
     *
     */
    public HashMap<String, HashMap<Integer, Double[]>> newDefinedMods = new HashMap<>();
    /**
     *
     */
    private HashMap<String, SpectrumMatch> newDefinedModsMatch = new HashMap<>();
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * LastSelectFolder accessed easily
     */
    public static String lastSelectedFolder = "";
    /**
     * Get system separator
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");


    public static void main(String[] args) {
        LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        UIDefaults defaults = lookAndFeel.getDefaults();
        defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));

        InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
        try {
            new GUIMainClass(args[0], args[1]);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null, e.getMessage(),
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public GUIMainClass(String resultsPath, String threadsNumber){

        resultsFolder = new File(resultsPath);
        this.threadsNumber = Integer.parseInt(threadsNumber);

        spectrumMainPanel = new SpectrumMainPanel(this);

        initParameters();

        initComponents();
        setVisible(true);

        importData();

    }

    private void initParameters(){

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

        searchParameters.setFragmentIonAccuracy(20.0);

        annotationSettings.setPreferencesFromSearchParameters(searchParameters);
        annotationSettings.setFragmentIonAccuracy(20);

    }

    /**
     * Set spectrum table tooltips
     */
    private void setUpTableHeaderToolTips() {
        spectrumJTableToolTips = new ArrayList<>();
        spectrumJTableToolTips.add("Select it and output");
        spectrumJTableToolTips.add("Key of spectrum in Identification File");
        spectrumJTableToolTips.add("Experiment");
        spectrumJTableToolTips.add("Spectrum Title in Spectrum File");
        spectrumJTableToolTips.add("MappedProtein");
        spectrumJTableToolTips.add("Peptide Sequence");
        spectrumJTableToolTips.add("Precursor Charge");
        spectrumJTableToolTips.add("Precursor m/z");
        spectrumJTableToolTips.addAll(psmScoreName);

        experimentJTableToolTips = new ArrayList<>();
        experimentJTableToolTips.add("Select it and output");
        experimentJTableToolTips.add("Experiment Name");
        experimentJTableToolTips.add("Number of proteins identified");
        experimentJTableToolTips.add("Number of PSMs identified");
        experimentJTableToolTips.add("Number of peptides identified");
        experimentJTableToolTips.add("Number of modified peptides identified");

        proteinsJTableToolTips = new ArrayList<>();
        proteinsJTableToolTips.add("Select it and output");
        proteinsJTableToolTips.add("Protein ID");
        proteinsJTableToolTips.add("Experiment");
        proteinsJTableToolTips.add("Protein");
        proteinsJTableToolTips.addAll(proteinScoreName);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JSplitPane allJSplitPane = new JSplitPane();
        JSplitPane experimentCoverageJSplitPane = new JSplitPane();
        JSplitPane experimentCoverageProteinJSplitPane = new JSplitPane();
        JSplitPane psmSpectrumJSplitPane = new JSplitPane();
        JScrollPane experimentsScrollPane = new JScrollPane();
        JScrollPane proteinsScrollPane = new JScrollPane();
        JScrollPane psmsScrollPane = new JScrollPane();
        JMenuBar menuBar = new JMenuBar();
        JButton backJButton = new JButton();
        JButton exportSelectedJButton = new JButton();
        JMenu exportJMenu = new JMenu();
        JMenu fileJMenu = new JMenu();
        JPanel mainJPanel = new JPanel();
        JPanel settingJPanel = new JPanel();
        JPanel loadingJPanel = new JPanel();
        JPanel searchJPanel = new JPanel();
        JLabel psmAllSelectedJLabel = new JLabel();
        JLabel proteinAllSelectedJLabel = new JLabel();
        JLabel powerPDV = new JLabel("@Powered by PDV");

        JMenuItem exportAllMenuItem = new JMenuItem();
        JMenuItem exportSelectedJMenuItem = new JMenuItem();

        settingColorJButton = new JButton();
        psmUpSortJButton = new JButton();
        psmDownSortJButton = new JButton();
        proteinUpSortJButton = new JButton();
        proteinDownSortJButton = new JButton();
        pSMAllSelectedJCheckBox = new JCheckBox();
        proteinAllSelectedJCheckBox = new JCheckBox();
//        openSearchFileJButton = new JButton();
        loadingJButton = new JButton();
        psmColumnSelectionJButton = new JButton();
        proteinColumnSelectionJButton = new JButton();
        showAllSelectedProteinsJButton = new JButton();
        psmSortColumnJCombox = new JComboBox();
        proteinSortColumnJCombox = new JComboBox();
        pSMNextJButton = new JButton();
        pSMUpJButton = new JButton();
        pSMPageNumJTextField = new JTextField();
        pSMPageSelectNumJTextField = new JTextField();
        proteinNextJButton = new JButton();
        proteinUpJButton = new JButton();
        proteinPageNumJTextField = new JTextField();
        proteinPageSelectNumJTextField = new JTextField();
        searchItemTextField = new JTextField();
        searchButton = new JButton();
        searchTypeComboBox = new JComboBox();
        experimentsJPanel = new JPanel();
        proteinsJPanel = new JPanel();
        coverageJPanel = new JPanel();
        psmsJPanel = new JPanel();
        spectrumShowJPanel = new JPanel();
        backgroundPanel = new JPanel();
        searchTextOrButtonJPanel = new JPanel();
        coveragePaintPanel = new JPanel();
        coverageScrollPane = new JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("FragPipe-PDV");
        setBackground(new java.awt.Color(255, 255, 255));
        //setPreferredSize(Toolkit. getDefaultToolkit(). getScreenSize());
        setPreferredSize(new java.awt.Dimension(Toolkit. getDefaultToolkit(). getScreenSize().width, Toolkit. getDefaultToolkit(). getScreenSize().height-30));
        setMinimumSize(new java.awt.Dimension(760, 500));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        experimentsJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return experimentJTableToolTips.get(realIndex);}

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                Component component = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    component.setBackground(Color.white);
                }else{
                    component.setBackground(new Color(164, 233, 255));
                }
                if(isRowSelected(row)){
                    component.setBackground(new Color(20,20,40));
                }
                if(String.valueOf(getValueAt(row, column)).contains(" Rank:"+"&nbsp<html>"+1)){
                    component.setBackground(new Color(255, 116, 135));
                }
                return component;
            }
        };

        experimentsJTable.setRowHeight(20);
        experimentsJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        experimentsJTable.getTableHeader().setFont(new Font("Dialog", 1, 13));
        experimentsJTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        proteinsJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return proteinsJTableToolTips.get(realIndex);}

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                Component component = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    component.setBackground(Color.white);
                }else{
                    component.setBackground(new Color(164, 233, 255));
                }
                if(isRowSelected(row)){
                    component.setBackground(new Color(20,20,40));
                }
                if (component instanceof JComponent) {
                    JComponent jc = (JComponent) component;
                    if (getValueAt(row, column) != null){
                        jc.setToolTipText(getValueAt(row, column).toString());
                    }
                }

                return component;
            }
        };

        proteinsJTable.setRowHeight(20);
        proteinsJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        proteinsJTable.getTableHeader().setFont(new Font("Dialog", 1, 13));

        spectrumJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return spectrumJTableToolTips.get(realIndex);}

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                Component component = super.prepareRenderer(renderer, row, column);

                String status = (String)getValueAt(row, 4);
                if ("MappedProtein".equals(status)) {
                    component.setBackground(Color.LIGHT_GRAY);
                } else {
                    component.setBackground(Color.white);
//                    if (row % 2 == 0) {
//
//                    }else{
//                        component.setBackground(Color.white);
//                        //component.setBackground(new Color(164, 233, 255));
//                    }
                    if(isRowSelected(row)){
                        component.setBackground(new Color(20,20,40));
                    }
                }

                if (component instanceof JComponent) {
                    JComponent jc = (JComponent) component;
                    if (getValueAt(row, column) != null){
                        jc.setToolTipText(getValueAt(row, column).toString());
                    }
                }

                return component;
            }
        };

        spectrumJTable.setRowHeight(20);
        spectrumJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        spectrumJTable.getTableHeader().setFont(new Font("Dialog", 1, 13));

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        //mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));
        menuBar.setBackground(new java.awt.Color(255, 255, 255));

//        fileJMenu.setMnemonic('F');
//        fileJMenu.setText("File");
//
//        idenInforMenuItem.setMnemonic('I');
//        idenInforMenuItem.setText("Identification Details");
//        idenInforMenuItem.addActionListener(this::idenInforMenuItemActionPerformed);
//
//        fileJMenu.add(idenInforMenuItem);
//
//        fileJMenu.add(jSeparator2);
//
//        exitJMenuItem.setMnemonic('E');
//        exitJMenuItem.setText("Exit");
//        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);
//
//        fileJMenu.add(exitJMenuItem);

        menuBar.add(fileJMenu);

        exportJMenu.setMnemonic('x');
        exportJMenu.setText("Export");

        exportAllMenuItem.setMnemonic('A');
        exportAllMenuItem.setText("Export All Spectra");
        exportAllMenuItem.addActionListener(this::exportAllMenuItemActionPerformed);
        exportJMenu.add(exportAllMenuItem);

//        exportSelectedJMenuItem.setMnemonic('S');
//        exportSelectedJMenuItem.setText("Export Selected Spectra");
//        exportSelectedJMenuItem.addActionListener(this::exportSelectedJMenuItemActionPerformed);
//        exportJMenu.add(exportSelectedJMenuItem);

        menuBar.add(exportJMenu);

        searchJPanel.setOpaque(false);

        searchTypeComboBox.setModel(new DefaultComboBoxModel(this.searchType));
        searchTypeComboBox.addItemListener(this::searchTypeComboBoxMouseClicked);

        searchItemTextField.setEditable(true);
        searchItemTextField.setHorizontalAlignment(SwingConstants.CENTER);
        searchItemTextField.setToolTipText("Data read unfinished!");

//        openSearchFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
//        openSearchFileJButton.setBorder(null);
//        openSearchFileJButton.setBorderPainted(false);
//        openSearchFileJButton.setContentAreaFilled(false);
//        openSearchFileJButton.addActionListener(this::openSearchFileJButtonActionPerformed);

//        searchButton.setEnabled(false);
        searchButton.setBackground(Color.BLACK);
        searchButton.setFont(searchButton.getFont().deriveFont(searchButton.getFont().getStyle() | Font.BOLD));
        searchButton.setForeground(Color.BLACK);
        searchButton.setIcon(new ImageIcon(getClass().getResource("/icons/search.png")));
        searchButton.setBorder(null);
        searchButton.setBorderPainted(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setToolTipText("Data read unfinished!");
        searchButton.addActionListener(this::searchButtonActionPerformed);

        backJButton.setBackground(Color.BLACK);
        backJButton.setFont(backJButton.getFont().deriveFont(backJButton.getFont().getStyle() | Font.BOLD));
        backJButton.setForeground(Color.BLACK);
        backJButton.setIcon(new ImageIcon(getClass().getResource("/icons/back.png")));
        backJButton.setBorder(null);
        backJButton.setBorderPainted(false);
        backJButton.setContentAreaFilled(false);
        backJButton.setToolTipText("Refresh");
        backJButton.addActionListener(this::backJButtonActionPerformed);

        searchTextOrButtonJPanel.setOpaque(false);

        GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
        searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

        searchTextOrButtonJPanelLayout.setHorizontalGroup(
                searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
        );

        searchTextOrButtonJPanelLayout.setVerticalGroup(
                searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(searchItemTextField, 10, 20, 20)
        );

        searchTextOrButtonJPanel.add(searchItemTextField);

        GroupLayout searchJPanelLayout = new GroupLayout(searchJPanel);
        searchJPanel.setLayout(searchJPanelLayout);
        searchJPanelLayout.setHorizontalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox,150,150,GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchTextOrButtonJPanel,150, 250, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchButton,GroupLayout.PREFERRED_SIZE,50,GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        searchJPanelLayout.setVerticalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox, 10, 20, 20)
                                .addComponent(searchTextOrButtonJPanel, 10, 20, 20)
                                .addComponent(searchButton, 10, 25, 35))
        );

        JPanel blankJPanel = new JPanel();

        blankJPanel.setOpaque(false);
        GroupLayout blankJPanelLayout = new GroupLayout(blankJPanel);
        blankJPanel.setLayout(blankJPanelLayout);

        blankJPanelLayout.setHorizontalGroup(
                blankJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(100,800,Short.MAX_VALUE)
        );

        blankJPanelLayout.setVerticalGroup(
                blankJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

        );

        //setJMenuBar(menuBar);

        settingColorJButton.setIcon(new ImageIcon(getClass().getResource("/icons/color.png")));
        settingColorJButton.setBorder(null);
        settingColorJButton.setBorderPainted(false);
        settingColorJButton.setContentAreaFilled(false);
        settingColorJButton.setToolTipText("Set PTM colors");
        settingColorJButton.addActionListener(this::settingColorJButtonActionPerform);

        settingJPanel.setBackground(new Color(255, 255, 255));
        settingJPanel.setMinimumSize(new Dimension(20, 0));
        settingJPanel.setOpaque(false);

        settingJPanel.setLayout(new BoxLayout(settingJPanel, BoxLayout.X_AXIS));

        //settingJPanel.add(settingColorJButton);

        settingJPanel.add(blankJPanel);
        settingJPanel.add(searchJPanel);

        loadingJPanel.setBackground(new Color(217, 248, 255));
        loadingJPanel.setMinimumSize(new Dimension(20, 0));
        loadingJPanel.setOpaque(false);

        loadingJPanel.setLayout(new BoxLayout(loadingJPanel, BoxLayout.X_AXIS));

        JPanel blankJPanel2 = new JPanel();

        blankJPanel2.setOpaque(false);
        GroupLayout blankJPanelLayout2 = new GroupLayout(blankJPanel2);
        blankJPanel2.setLayout(blankJPanelLayout2);

        blankJPanelLayout2.setHorizontalGroup(
                blankJPanelLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(100,800,Short.MAX_VALUE)
        );

        blankJPanelLayout2.setVerticalGroup(
                blankJPanelLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)

        );

        loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/loading.gif")));
        loadingJButton.setBorder(null);
        loadingJButton.setBorderPainted(false);
        loadingJButton.setContentAreaFilled(false);
        loadingJButton.setEnabled(false);
        loadingJButton.setText("Result importing");

        powerPDV.setFont(new Font("Arial", Font.PLAIN, 11));
        powerPDV.setBackground(Color.GRAY);

        loadingJPanel.add(powerPDV);
        loadingJPanel.add(blankJPanel2);
        loadingJPanel.add(loadingJButton);

        allJSplitPane.setBorder(null);
        allJSplitPane.setDividerLocation(370);
        allJSplitPane.setDividerSize(5);
        allJSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        allJSplitPane.setResizeWeight(0.5);
        allJSplitPane.setOpaque(false);
        allJSplitPane.setContinuousLayout(true);

        allJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> spectrumMainPanel.updateSpectrum());

        // Initiate experiments table
        experimentsScrollPane.setBackground(Color.WHITE);
        experimentsScrollPane.setViewportView(experimentsJTable);
        experimentsScrollPane.setOpaque(false);
        experimentsJTable.getAccessibleContext().setAccessibleName("experimentsJTable");
        experimentsJTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                experimentsJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                experimentsJTableMouseReleased(evt);
            }
        });
        experimentsJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                //experimentsJTableKeyReleased(evt);
            }
        });


        experimentsJPanel.setOpaque(false);
        TitledBorder expTitledBorder = BorderFactory.createTitledBorder("Experiments" + " \t ");
        expTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        experimentsJPanel.setBorder(expTitledBorder);

        GroupLayout expsLayeredPanelLayout = new GroupLayout(experimentsJPanel);
        experimentsJPanel.setLayout(expsLayeredPanelLayout);
        expsLayeredPanelLayout.setHorizontalGroup(
                expsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(expsLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(experimentsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        expsLayeredPanelLayout.setVerticalGroup(
                expsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(expsLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(experimentsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        //Initial protein coverage:
        coveragePaintPanel.setBackground(Color.white);
        coverageScrollPane.setBackground(Color.white);
        coverageScrollPane.setViewportView(coveragePaintPanel);
        coverageScrollPane.setOpaque(false);

        coverageJPanel.setBackground(Color.white);
        coverageJPanel.setOpaque(false);
        TitledBorder coverageTitledBorder = BorderFactory.createTitledBorder("Coverage" + " \t ");
        coverageTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        coverageJPanel.setBorder(coverageTitledBorder);

        GroupLayout coverageLayeredPanelLayout = new GroupLayout(coverageJPanel);
        coverageJPanel.setLayout(coverageLayeredPanelLayout);
        coverageLayeredPanelLayout.setHorizontalGroup(
                coverageLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(coverageLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(coverageScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        coverageLayeredPanelLayout.setVerticalGroup(
                coverageLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(coverageLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(coverageScrollPane, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))
        );

        experimentCoverageJSplitPane.setBorder(null);
        experimentCoverageJSplitPane.setDividerLocation(100);
        experimentCoverageJSplitPane.setDividerSize(5);
        experimentCoverageJSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        experimentCoverageJSplitPane.setResizeWeight(0.5);
        experimentCoverageJSplitPane.setOpaque(false);
        experimentCoverageJSplitPane.setContinuousLayout(true);
        experimentCoverageJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> updateCoverage(proteinCurrentSelections));

        experimentCoverageJSplitPane.setTopComponent(experimentsJPanel);
        experimentCoverageJSplitPane.setBottomComponent(coverageJPanel);

        // Initiate proteinsJTable
        proteinAllSelectedJLabel.setText("All proteins");
        proteinAllSelectedJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        proteinAllSelectedJLabel.setToolTipText("Select all proteins in this experiment.");
        proteinAllSelectedJCheckBox.setToolTipText("Select all proteins in this experiment.");
        proteinAllSelectedJCheckBox.setSelected(false);
        proteinAllSelectedJCheckBox.setOpaque(false);
        proteinAllSelectedJCheckBox.setBackground(Color.white);
        proteinAllSelectedJCheckBox.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent evt){
                proteinAllSelectedJCheckBoxMouseClicked(evt);
            }
        });

        proteinPageNumJTextField.setEditable(false);
        proteinPageNumJTextField.setOpaque(false);
        proteinPageNumJTextField.setBackground(Color.white);
        proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum)+"/"+String.valueOf(allProteinIndex.size()));
        proteinPageNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);

        proteinPageSelectNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        proteinPageSelectNumJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                proteinPageSelectNumJTextFieldKeyReleased(evt);
            }
        });

        proteinUpJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_back.png")));
        proteinUpJButton.setBorder(null);
        proteinUpJButton.setBorderPainted(false);
        proteinUpJButton.setContentAreaFilled(false);
        proteinUpJButton.addActionListener(this::proteinUpJButtonActionPerformed);

        proteinNextJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_forward.png")));
        proteinNextJButton.setBorder(null);
        proteinNextJButton.setBorderPainted(false);
        proteinNextJButton.setContentAreaFilled(false);
        proteinNextJButton.addActionListener(this::proteinNextJButtonActionPerformed);

        showAllSelectedProteinsJButton.setText("Show all selected");
        showAllSelectedProteinsJButton.addActionListener(this::showAllSelectedProteinsJButtonActionPerformed);
        showAllSelectedProteinsJButton.setBackground(Color.WHITE);

        Image img = new ImageIcon(getClass().getResource("/icons/ms.png")).getImage();
        Image newimg = img.getScaledInstance( 15, 15,  java.awt.Image.SCALE_SMOOTH ) ;
        proteinColumnSelectionJButton.setIcon(new ImageIcon(newimg));
        proteinColumnSelectionJButton.setBorder(null);
        proteinColumnSelectionJButton.setBorderPainted(false);
        proteinColumnSelectionJButton.setContentAreaFilled(false);
        proteinColumnSelectionJButton.addActionListener(this::proteinColumnSelectionJButtonActionPerformed);

        proteinSortColumnJCombox.setModel(new DefaultComboBoxModel(new String[]{}));
        proteinSortColumnJCombox.setMaximumSize(new Dimension(100, 20));
        proteinSortColumnJCombox.setBackground(Color.WHITE);

        proteinUpSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/upSort.png")));
        proteinUpSortJButton.setBorder(null);
        proteinUpSortJButton.setBorderPainted(false);
        proteinUpSortJButton.setContentAreaFilled(false);
        proteinUpSortJButton.setToolTipText("Sort results");
        proteinUpSortJButton.addActionListener(this::proteinUpSortJButtonActionPerform);

        proteinDownSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/downSort.png")));
        proteinDownSortJButton.setBorder(null);
        proteinDownSortJButton.setBorderPainted(false);
        proteinDownSortJButton.setContentAreaFilled(false);
        proteinDownSortJButton.setToolTipText("Sort results");
        proteinDownSortJButton.addActionListener(this::proteinDownSortJButtonActionPerform);

        proteinsScrollPane.setBackground(Color.WHITE);
        proteinsScrollPane.setViewportView(proteinsJTable);
        proteinsScrollPane.setOpaque(false);
        proteinsJTable.getAccessibleContext().setAccessibleName("proteinJTable");
        proteinsJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        proteinsJTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                proteinJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                proteinJTableMouseReleased(evt);
            }
        });
        proteinsJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                proteinJTableKeyReleased(evt);
            }
        });

        proteinsJPanel.setOpaque(false);
        TitledBorder proteinTitledBorder = BorderFactory.createTitledBorder("Proteins" + " \t ");
        proteinTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        proteinsJPanel.setBorder(proteinTitledBorder);

        GroupLayout proteinsLayeredPanelLayout = new GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsLayeredPanelLayout);
        proteinsLayeredPanelLayout.setHorizontalGroup(
                proteinsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                                .addComponent(proteinsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                                .addComponent(showAllSelectedProteinsJButton)
                                .addGap(100,1200,2000)
                                .addComponent(proteinSortColumnJCombox)
                                .addComponent(proteinUpSortJButton)
                                .addComponent(proteinDownSortJButton)
                                .addComponent(proteinColumnSelectionJButton))
                        .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                                .addGap(10, 15, 15)
                                .addComponent(proteinAllSelectedJCheckBox)
                                .addComponent(proteinAllSelectedJLabel)
                                .addGap(100,1200,2000)
                                .addComponent(proteinPageSelectNumJTextField,50, 50, 50)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(proteinUpJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proteinNextJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addComponent(proteinPageNumJTextField,50, 50, 70))
        );
        proteinsLayeredPanelLayout.setVerticalGroup(
                proteinsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                                .addGroup(proteinsLayeredPanelLayout.createParallelGroup()
                                        .addComponent(showAllSelectedProteinsJButton)
                                        .addComponent(proteinSortColumnJCombox)
                                        .addComponent(proteinUpSortJButton)
                                        .addComponent(proteinDownSortJButton)
                                        .addComponent(proteinColumnSelectionJButton))
                                .addComponent(proteinsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(proteinsLayeredPanelLayout.createParallelGroup()
                                        .addComponent(proteinAllSelectedJCheckBox)
                                        .addComponent(proteinAllSelectedJLabel, 10, 20, 20)
                                        .addComponent(proteinPageSelectNumJTextField, 10, 20, 20)
                                        .addComponent(proteinUpJButton, 10, 20, 20)
                                        .addComponent(proteinNextJButton, 10, 20, 20)
                                        .addComponent(proteinPageNumJTextField, 10, 20, 20)))
        );

        experimentCoverageProteinJSplitPane.setBorder(null);
        experimentCoverageProteinJSplitPane.setDividerLocation(600);
        experimentCoverageProteinJSplitPane.setDividerSize(5);
        experimentCoverageProteinJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        experimentCoverageProteinJSplitPane.setResizeWeight(0.5);
        experimentCoverageProteinJSplitPane.setOpaque(false);
        experimentCoverageProteinJSplitPane.setContinuousLayout(true);

        experimentCoverageProteinJSplitPane.setLeftComponent(experimentCoverageJSplitPane);
        experimentCoverageProteinJSplitPane.setRightComponent(proteinsJPanel);
        experimentCoverageProteinJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> updateCoverage(proteinCurrentSelections));

        // Initiate spectrumJTable

        spectrumJTable.setModel(new PSMTableModel());
        spectrumJTable.setOpaque(false);
        spectrumJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spectrumJTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                spectrumJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                spectrumJTableMouseReleased(evt);
            }
        });
        spectrumJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                spectrumJTableKeyReleased(evt);
            }
        });
        spectrumJTable.addKeyListener(new ClipboardKeyAdapter(spectrumJTable));

        psmsScrollPane.setBackground(Color.WHITE);
        psmsScrollPane.setViewportView(spectrumJTable);
        psmsScrollPane.setOpaque(false);
        spectrumJTable.getAccessibleContext().setAccessibleName("spectrumJTable");

        psmsJPanel.setOpaque(false);
        TitledBorder titledBorder = BorderFactory.createTitledBorder("PSMs" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        psmsJPanel.setBorder(titledBorder);

        exportSelectedJButton.setIcon(new ImageIcon(getClass().getResource("/icons/export.png")));
        exportSelectedJButton.setToolTipText("Export selected PSMs.");
        exportSelectedJButton.setBorder(null);
        exportSelectedJButton.setBorderPainted(false);
        exportSelectedJButton.setContentAreaFilled(false);
        exportSelectedJButton.addActionListener(this::exportSelectedJButtonActionPerformed);

        psmAllSelectedJLabel.setText("Whole page");
        psmAllSelectedJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        psmAllSelectedJLabel.setToolTipText("Select all spectrum in this page");
        pSMAllSelectedJCheckBox.setToolTipText("Select all spectrum in this page");
        pSMAllSelectedJCheckBox.setSelected(false);
        pSMAllSelectedJCheckBox.setOpaque(false);
        pSMAllSelectedJCheckBox.setBackground(Color.white);
        pSMAllSelectedJCheckBox.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent evt){
                pSMAllSelectedJCheckBoxMouseClicked(evt);
            }
        });

        pSMPageNumJTextField.setEditable(false);
        pSMPageNumJTextField.setOpaque(false);
        pSMPageNumJTextField.setBackground(Color.white);
        pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum)+"/"+String.valueOf(allSpectrumIndex.size()));
        pSMPageNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);

        pSMPageSelectNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        pSMPageSelectNumJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                pSMPageSelectNumJTextFieldKeyReleased(evt);
            }
        });

        pSMUpJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_back.png")));
        pSMUpJButton.setBorder(null);
        pSMUpJButton.setBorderPainted(false);
        pSMUpJButton.setContentAreaFilled(false);
        buttonCheck();
        pSMUpJButton.addActionListener(this::pSMUpJButtonActionPerformed);

        pSMNextJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_forward.png")));
        pSMNextJButton.setBorder(null);
        pSMNextJButton.setBorderPainted(false);
        pSMNextJButton.setContentAreaFilled(false);
        buttonCheck();
        pSMNextJButton.addActionListener(this::pSMNextJButtonActionPerformed);

        psmColumnSelectionJButton.setIcon(new ImageIcon(newimg));
        psmColumnSelectionJButton.setBorder(null);
        psmColumnSelectionJButton.setBorderPainted(false);
        psmColumnSelectionJButton.setContentAreaFilled(false);
        psmColumnSelectionJButton.addActionListener(this::psmColumnSelectionJButtonActionPerformed);

        psmSortColumnJCombox.setModel(new DefaultComboBoxModel(new String[]{}));
        psmSortColumnJCombox.setMaximumSize(new Dimension(100, 20));
        psmSortColumnJCombox.setBackground(Color.WHITE);

        psmUpSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/upSort.png")));
        psmUpSortJButton.setBorder(null);
        psmUpSortJButton.setBorderPainted(false);
        psmUpSortJButton.setContentAreaFilled(false);
        psmUpSortJButton.setToolTipText("Sort results");
        psmUpSortJButton.addActionListener(this::psmUpSortJButtonActionPerform);

        psmDownSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/downSort.png")));
        psmDownSortJButton.setBorder(null);
        psmDownSortJButton.setBorderPainted(false);
        psmDownSortJButton.setContentAreaFilled(false);
        psmDownSortJButton.setToolTipText("Sort results");
        psmDownSortJButton.addActionListener(this::psmDownSortJButtonActionPerform);

        GroupLayout psmsLayeredPanelLayout = new GroupLayout(psmsJPanel);
        psmsJPanel.setLayout(psmsLayeredPanelLayout);
        psmsLayeredPanelLayout.setHorizontalGroup(
                psmsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(psmsLayeredPanelLayout.createSequentialGroup()
                                .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(psmsLayeredPanelLayout.createSequentialGroup()
                                .addComponent(exportSelectedJButton)
                                .addGap(100,1200,2000)
                                .addComponent(psmSortColumnJCombox)
                                .addComponent(psmUpSortJButton)
                                .addComponent(psmDownSortJButton)
                                .addComponent(psmColumnSelectionJButton))
                        .addGroup(psmsLayeredPanelLayout.createSequentialGroup()
                                .addGap(10, 15, 15)
                                .addComponent(pSMAllSelectedJCheckBox)
                                .addComponent(psmAllSelectedJLabel)
                                .addGap(100,1200,2000)
                                .addComponent(pSMPageSelectNumJTextField,50, 50, 50)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(pSMUpJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pSMNextJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addComponent(pSMPageNumJTextField,50, 50, 70))
        );
        psmsLayeredPanelLayout.setVerticalGroup(
                psmsLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(psmsLayeredPanelLayout.createSequentialGroup()
                                .addGroup(psmsLayeredPanelLayout.createParallelGroup()
                                        .addComponent(exportSelectedJButton)
                                        .addComponent(psmSortColumnJCombox)
                                        .addComponent(psmUpSortJButton)
                                        .addComponent(psmDownSortJButton)
                                        .addComponent(psmColumnSelectionJButton))
                                .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(psmsLayeredPanelLayout.createParallelGroup()
                                        .addComponent(pSMAllSelectedJCheckBox)
                                        .addComponent(psmAllSelectedJLabel)
                                        .addComponent(pSMPageSelectNumJTextField, 10, 20, 20)
                                        .addComponent(pSMUpJButton, 10, 20, 20)
                                        .addComponent(pSMNextJButton, 10, 20, 20)
                                        .addComponent(pSMPageNumJTextField, 10, 20, 20)))
        );

        spectrumShowJPanel.setOpaque(false);
        spectrumMainPanel.setOpaque(false);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowJPanel);
        spectrumShowJPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumMainPanel,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(spectrumMainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        psmSpectrumJSplitPane.setBorder(null);
        psmSpectrumJSplitPane.setDividerLocation(600);
        psmSpectrumJSplitPane.setDividerSize(5);
        psmSpectrumJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        psmSpectrumJSplitPane.setResizeWeight(0.5);
        psmSpectrumJSplitPane.setOpaque(false);
        psmSpectrumJSplitPane.setContinuousLayout(true);

        psmSpectrumJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> spectrumMainPanel.updateSpectrum());

        psmSpectrumJSplitPane.setLeftComponent(spectrumShowJPanel);
        psmSpectrumJSplitPane.setRightComponent(psmsJPanel);

        allJSplitPane.setTopComponent(experimentCoverageProteinJSplitPane);
        allJSplitPane.setBottomComponent(psmSpectrumJSplitPane);

        GroupLayout overviewJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
                overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(overviewJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(settingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(loadingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
                overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(overviewJPanelLayout.createSequentialGroup()
                                .addComponent(settingJPanel, 25, 25, 25)
                                .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(loadingJPanel, 17, 17, 17)
                                .addGap(2,3,5))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
            //UIManager.setLookAndFeel(motif);
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);

        pack();
    }

    private HashMap<String, Object[]>[] updatePeptideCoverage(){
        HashMap<String, Object[]>[] objectsArray = new HashMap[2];
        HashMap<String, Object[]> proteinPeptideObjects = new HashMap<>();
        HashMap<String, Object[]> proteinMappedPeptideObjects = new HashMap<>();
        String proteinID = "";

        selectPageSpectrumIndex = allSpectrumIndex.get(selectedPSMPageNum - 1);
        for (String spectrumIndex : selectPageSpectrumIndex) {
            try {
                SpectrumMatch oneSpectrumMatch = sqliteConnection.getSpectrumMatch(spectrumIndex);
                Peptide peptide = oneSpectrumMatch.getBestPeptideAssumption().getPeptide();
                if (mappedSpectrumIndex.contains(spectrumIndex)){
                    for (String oneProteinID : sqliteConnection.getOneProteinIDForMapped(spectrumIndex)){
                        if (proteinSeqMap.containsKey(oneProteinID)) {
                            Object[] oneMap = mapPeptide(peptide, proteinSeqMap.get(oneProteinID));
                            if (proteinMappedPeptideObjects.containsKey(oneProteinID)) {
                                ((ArrayList<Object>) proteinMappedPeptideObjects.get(oneProteinID)[0]).addAll((Collection<?>) oneMap[0]);
                                ((HashMap<Object, Object>) proteinMappedPeptideObjects.get(oneProteinID)[1]).putAll((Map<?, ?>) oneMap[1]);
                            } else {
                                proteinMappedPeptideObjects.put(oneProteinID, oneMap);
                            }
                        }
                    }
                } else {
                    proteinID = sqliteConnection.getOneProteinID(spectrumIndex);
                    Object[] oneMap = mapPeptide(peptide, proteinSeqMap.get(proteinID));
                    if (proteinPeptideObjects.containsKey(proteinID)){
                        ((ArrayList<Object>) proteinPeptideObjects.get(proteinID)[0]).addAll((Collection<?>) oneMap[0]);
                        ((HashMap<Object, Object>) proteinPeptideObjects.get(proteinID)[1]).putAll((Map<?, ?>) oneMap[1]);
                    } else {
                        proteinPeptideObjects.put(proteinID, oneMap);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        objectsArray[0] = proteinPeptideObjects;
        objectsArray[1] = proteinMappedPeptideObjects;
        return objectsArray;
    }

    /**
     * Close the program when click exist
     * @param evt Window event
     */
    private void formWindowClosing(WindowEvent evt) {
        close();
    }

    /**
     * Close dialog
     */
    public void close() {
        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to save visualization data?\nIt will improve import speed when you open the project next time.",
                "Close PDV",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {
            closePDV(true);
        } else if (value == JOptionPane.NO_OPTION) {
            closePDV(false);
        } else {
            // Nothing
        }
    }

    /**
     * Close the program
     */
    private void closePDV(Boolean save) {

        exceptionHandler.setIgnoreExceptions(true);

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        final GUIMainClass finalRef = this;

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(() -> {

            try {

                if (!progressDialog.isRunCanceled()) {
                    TempFilesManager.deleteTempFolders();
                }

            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            } finally {
                progressDialog.setRunFinished();

                finalRef.setVisible(false);

                if (!save){
                    clearData();
                }
                psmsJPanel.removeAll();
                spectrumShowJPanel.removeAll();
                this.revalidate();
                this.repaint();

                System.exit(0);
            }
        });
    }

    /**
     * Clear all data before existing and restart
     */
    private void clearData() {

        if(sqliteConnection != null){
            sqliteConnection.closeConnection();
            File dbFile = new File(databasePath);
            if (dbFile.isFile() && dbFile.exists()) {
                dbFile.delete();
            }
        }
        for (String eachFile : mgfFiles){
            if (new File(eachFile + ".cui").exists()){
                new File(eachFile + ".cui").delete();
            }
        }

        spectrumFactory.clearFactory();
        scansFileHashMap.clear();
    }

    private Object[] mapPeptide(Peptide peptide, String proteinSeq){
        ArrayList<Integer> mappedIndex = new ArrayList<>();
        HashMap<Integer, Color> modificationMap = new HashMap<>();

        String peptideSeq = peptide.getSequence();
        int startIndex = proteinSeq.indexOf(peptideSeq);
        for (int i = startIndex; i<startIndex+peptideSeq.length(); i++){
            mappedIndex.add(i);
        }

        for (ModificationMatch modificationMatch : peptide.getModificationMatches()){
            Color currentColor = searchParameters.getPtmSettings().getColor(modificationMatch.getTheoreticPtm());
            modificationMap.put(modificationMatch.getModificationSite() + startIndex, currentColor);
        }

        return new Object[] { mappedIndex, modificationMap};
    }

    private void updateCoverage(ArrayList<String> proteinShownList){
        if (allSpectrumIndex.size()!=0) {

            HashMap<String, Object[]>[] proteinPeptideObjects;

            proteinPeptideObjects = updatePeptideCoverage();

            coveragePaintPanel.removeAll();
            coveragePaintPanel.setOpaque(false);

            int count = 0;
            coveragePaintPanel.setLayout(new BoxLayout(coveragePaintPanel, BoxLayout.PAGE_AXIS));
            for (String proteinID : proteinShownList) {
                proteinID = proteinID.split(":\\|")[0];
                String proteinSeq = proteinSeqMap.get(proteinID);

                JPanel onePanel = new JPanel();
                onePanel.setBackground(Color.white);
                TitledBorder proteinTitledBorder = BorderFactory.createTitledBorder(proteinID + " \t ");
                proteinTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
                onePanel.setBorder(proteinTitledBorder);
                onePanel.add(new CoveragePanel(proteinSeq, proteinPeptideObjects[0].get(proteinID), proteinPeptideObjects[1].get(proteinID), coverageJPanel.getSize().width, coverageJPanel.getSize().height));

                coveragePaintPanel.add(onePanel);
                if (count > 4) {
                    break;
                }
                count++;
            }
            coveragePaintPanel.revalidate();
            coveragePaintPanel.repaint();
            coverageJPanel.repaint();
            proteinPeptideObjects = null;
            //System.gc();
        }
    }

    /**
     * exportAllMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportAllMenuItemActionPerformed(ActionEvent evt){

        exportAll = true;
        exportSelection = false;

        Integer size = 0;
        for (ArrayList<String> each : allSpectrumIndex){
            size += each.size();
        }
        new ExportBatchDialog(this, size);
    }

    /**
     * exportSelectedJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void exportSelectedJButtonActionPerformed(ActionEvent evt){

        exportSelection = true;
        exportAll = false;

        new ExportBatchDialog(this, pSMAllSelections.size());
    }

    /**
     * Search type select clicked
     * @param evt Item event
     */
    private void searchTypeComboBoxMouseClicked(ItemEvent evt){

        searchButton.setEnabled(false);

        if (searchTypeComboBox.getSelectedIndex() == 0) {

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(searchItemTextField, 10, 20, 20)
            );
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            searchButton.setEnabled(true);
            searchItemTextField.setEditable(true);
            findType = "Peptide (String)";
        } else if (searchTypeComboBox.getSelectedIndex() == 1) {

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(searchItemTextField, 10, 20, 20)
            );
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            searchButton.setEnabled(true);
            searchItemTextField.setEditable(true);
            findType = "Spectrum (String)";
        } else if (searchTypeComboBox.getSelectedIndex() == 2) {

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(searchItemTextField, 10, 20, 20)
            );
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            searchButton.setEnabled(true);
            searchItemTextField.setEditable(true);
            findType = "Protein (String)";
        }
//        else if (searchTypeComboBox.getSelectedIndex() == 3) {
//            findType = "Spectrum (File)";
//
//            searchItemTextField.setText("");
//            selectIndexFile = null;
//
//            searchTextOrButtonJPanel.removeAll();
//
//            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
//            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);
//
//            searchTextOrButtonJPanelLayout.setHorizontalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
//                            .addGroup(searchTextOrButtonJPanelLayout.createSequentialGroup()
//                                    .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
//                                    .addComponent(openSearchFileJButton, 50, 50, GroupLayout.PREFERRED_SIZE))
//            );
//
//            searchTextOrButtonJPanelLayout.setVerticalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
//                            .addComponent(openSearchFileJButton, 10, 20, 20)
//                            .addComponent(searchItemTextField, 10, 20, 20)
//            );
//            searchItemTextField.setEditable(false);
//            searchTextOrButtonJPanel.repaint();
//            searchTextOrButtonJPanel.revalidate();
//
//        } else if (searchTypeComboBox.getSelectedIndex() == 2) {
//            findType = "Peptide (File)";
//
//            searchItemTextField.setText("");
//            searchTextOrButtonJPanel.removeAll();
//
//            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
//            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);
//
//            searchTextOrButtonJPanelLayout.setHorizontalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
//                            .addGroup(searchTextOrButtonJPanelLayout.createSequentialGroup()
//                                    .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
//                                    .addComponent(openSearchFileJButton, 50, 50, GroupLayout.PREFERRED_SIZE))
//            );
//
//            searchTextOrButtonJPanelLayout.setVerticalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
//                            .addComponent(openSearchFileJButton, 10, 20, 20)
//                            .addComponent(searchItemTextField, 10, 20, 20)
//            );
//            searchItemTextField.setEditable(false);
//            searchTextOrButtonJPanel.repaint();
//            searchTextOrButtonJPanel.revalidate();
//
//            selectIndexFile = null;
//        }
//            else if (searchTypeComboBox.getSelectedIndex() == 5) {
//            findType = "Protein (File)";
//
//            searchItemTextField.setText("");
//            searchTextOrButtonJPanel.removeAll();
//
//            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
//            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);
//
//            searchTextOrButtonJPanelLayout.setHorizontalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
//                            .addGroup(searchTextOrButtonJPanelLayout.createSequentialGroup()
//                                    .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
//                                    .addComponent(openSearchFileJButton, 50, 50, GroupLayout.PREFERRED_SIZE))
//            );
//
//            searchTextOrButtonJPanelLayout.setVerticalGroup(
//                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
//                            .addComponent(openSearchFileJButton, 10, 20, 20)
//                            .addComponent(searchItemTextField, 10, 20, 20)
//            );
//            searchItemTextField.setEditable(false);
//            searchTextOrButtonJPanel.repaint();
//            searchTextOrButtonJPanel.revalidate();
//
//            selectIndexFile = null;
//        }

    }

    private void openSearchFileJButtonActionPerformed(ActionEvent evt){

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Input File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(false);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            selectIndexFile = fileChooser.getSelectedFile();

        }

        if (selectIndexFile != null){
            searchItemTextField.setText(selectIndexFile.getName());
            lastSelectedFolder = selectIndexFile.getAbsolutePath();
            searchButton.setEnabled(true);
        }

    }

    /**
     * Find button action
     * @param evt Mouse click event
     */
    private void searchButtonActionPerformed(ActionEvent evt){

        ProgressDialogX progressDialogX = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Searching. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "searchProgressDialog").start();

        new Thread("Searching") {
            @Override
            public void run() {

                try {

                    ArrayList<String[]> searchIDs = getFoundResult();

                    if (searchIDs.size() > 0) {

                        updateTable(searchIDs);

                        //selectPageSpectrumIndex = searchIDs;

                        pSMUpJButton.setEnabled(false);
                        pSMNextJButton.setEnabled(false);

                    } else {
                        searchItemTextField.setText("No Match!");
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    progressDialogX.setRunFinished();
                    JOptionPane.showMessageDialog(GUIMainClass.this, JOptionEditorPane.getJOptionEditorPane(
                                    "Failed to Search.<br>"
                                            + "Please search again."),
                            "Search Error", JOptionPane.ERROR_MESSAGE);
                }

                progressDialogX.setRunFinished();
            }
        }.start();
    }

    private ArrayList<String[]> getFoundResult() throws SQLException {

        ArrayList<String[]> searchIDs = new ArrayList<>();

        if (Objects.equals(findType, "Spectrum (String)")) {

            if (!searchItemTextField.getText().equals("")) {
                String findItem = searchItemTextField.getText();
                searchIDs = sqliteConnection.getSelectedTitleIndex(findItem, expNumList);
            }
            nonProteinSearchMode = true;

        } else if(Objects.equals(findType, "Peptide (String)")) {

            if (!searchItemTextField.getText().equals("")) {
                String findItem = searchItemTextField.getText();
                findItem = findItem.replaceAll(" ", "");
                searchIDs = sqliteConnection.getSelectedPeptideIndex(findItem.toUpperCase(), expNumList);
            }
            nonProteinSearchMode = true;

        } else if(Objects.equals(findType, "Protein (String)")) {

            if (!searchItemTextField.getText().equals("")) {
                String findItem = searchItemTextField.getText();
                searchIDs = sqliteConnection.getSelectedProteinIndex(findItem.toUpperCase(), expNumList);
            }
            nonProteinSearchMode = false;
        }
//        else if(Objects.equals(findType, "Spectrum (File)")) {
//            if (!searchItemTextField.getText().equals("") && !searchItemTextField.getText().equals("null")) {
//                try {
//                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectIndexFile));
//
//                    String line;
//
//                    while ((line = bufferedReader.readLine()) != null) {
//                        if (!line.equals("")) {
//                            ArrayList<String[]> idInFile = sqliteConnection.getSelectedTitleIndex(line.trim(), expNumList);
//                            searchIDs.addAll(idInFile);
//                        }
//                    }
//
//                    if (searchIDs.size() == 0){
//                        searchItemTextField.setText("No Match!");
//                    }
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            nonProteinSearchMode = true;
//
//        } else if(Objects.equals(findType, "Peptide (File)")) {
//
//            if (!searchItemTextField.getText().equals("") && !searchItemTextField.getText().equals("null")) {
//                try {
//                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectIndexFile));
//
//                    String line;
//
//                    while ((line = bufferedReader.readLine()) != null) {
//                        if (!line.equals("")) {
//                            ArrayList<String[]> idInFile = sqliteConnection.getSelectedPeptideIndex(line.trim(), expNumList);
//                            searchIDs.addAll(idInFile);
//                        }
//                    }
//
//                    if (searchIDs.size() == 0){
//                        searchItemTextField.setText("No Match!");
//                    }
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            nonProteinSearchMode = true;
//        }  else if(Objects.equals(findType, "Protein (File)")) {
//            if (!searchItemTextField.getText().equals("") && !searchItemTextField.getText().equals("null")) {
//                try {
//                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectIndexFile));
//
//                    String line;
//
//                    while ((line = bufferedReader.readLine()) != null) {
//                        if (!line.equals("")) {
//                            ArrayList<String[]> idInFile = sqliteConnection.getSelectedTitleIndex(line.trim(), expNumList);
//                            searchIDs.addAll(idInFile);
//                        }
//                    }
//
//                    if (searchIDs.size() == 0) {
//                        searchItemTextField.setText("No Match!");
//                    }
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            nonProteinSearchMode = false;
//        }

            return searchIDs;
    }

    /**
     * backJButtonActionPerformed
     * @param evt Mouse click action
     */
    private void backJButtonActionPerformed(ActionEvent evt){
        nonProteinSearchMode = false;
        experimentsJTable.setRowSelectionInterval(0, 0);
        experimentsJTableMouseReleased(null);
        buttonCheck();
    }

    /**
     * pSMPageSelectNumJTextField event
     *  @param evt Key click event
     */
    private void proteinPageSelectNumJTextFieldKeyReleased(KeyEvent evt){

        int keyChar = evt.getKeyChar();
        if (evt.getKeyCode() == 10 || keyChar >= KeyEvent.VK_0 && keyChar <= KeyEvent.VK_9) {
            if (evt.getKeyCode() == 10) {
                if (!proteinPageSelectNumJTextField.getText().equals("")) {
                    if (Integer.parseInt(proteinPageSelectNumJTextField.getText()) > allProteinIndex.size() || Integer.parseInt(proteinPageSelectNumJTextField.getText()) < 1) {
                        proteinPageSelectNumJTextField.setBackground(Color.GRAY);
                    } else {
                        proteinPageSelectNumJTextField.setBackground(Color.WHITE);
                        selectedProteinPageNum = Integer.parseInt(proteinPageSelectNumJTextField.getText());
                        proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum) + "/" + String.valueOf(allProteinIndex.size()));

                        buttonCheck();
                        updateTable();

                        spectrumJTable.requestFocus();
                        spectrumJTable.setRowSelectionInterval(0, 0);
                    }
                }
            }
        } else {
            proteinPageSelectNumJTextField.setText("");
            evt.consume();
        }
    }

    private void proteinColumnSelectionJButtonActionPerformed(ActionEvent evt){
        ArrayList<String> columnName = new ArrayList<>();
        columnName.add("Selected");
        columnName.add("Experiment");
        columnName.add("Protein");
        columnName.addAll(proteinScoreName);

        if(proteinColumnToSelected == null){
            proteinColumnToSelected = new HashMap<>();
            for (String eachColumn: columnName){
                proteinColumnToSelected.put(eachColumn, true);
            }
        }

        new ColumnSelectionDialog(this, proteinsJTable, columnName, true);
    }

    private void showAllSelectedProteinsJButtonActionPerformed(ActionEvent evt){
        proteinCurrentSelections = proteinAllSelections;
        showAllSelectedProteinsJButton.setBackground(Color.RED);
        fetchBigProteins();
    }

    /**
     * UpButtonAction
     * @param evt Mouse click event
     */
    private void proteinUpJButtonActionPerformed(ActionEvent evt){

        selectedProteinPageNum--;
        proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum) + "/" + String.valueOf(allProteinIndex.size()));
        buttonCheck();

        updateTable();

        proteinsJTable.requestFocus();
        proteinsJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * pSMNextJButtonAction
     * @param evt Mouse click event
     */
    private void proteinNextJButtonActionPerformed(ActionEvent evt){

        selectedProteinPageNum++;
        proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum) + "/" + String.valueOf(allProteinIndex.size()));
        buttonCheck();

        updateTable();

        proteinsJTable.requestFocus();
        proteinsJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * Selected whole page
     * @param evt Mouse click event
     */
    private void proteinAllSelectedJCheckBoxMouseClicked(MouseEvent evt) {
        if (proteinAllSelectedJCheckBox.isSelected()) {
            for (ArrayList<String> eachPage : allProteinIndex){
                for (String proteinKey : eachPage) {
                    proteinKeyToSelected.put(proteinKey, true);
                    if (!proteinAllSelections.contains(proteinKey)){
                        proteinAllSelections.add(proteinKey);
                    }

                }
            }

        } else {
            for (ArrayList<String> eachPage : allProteinIndex){
                for (String proteinKey : eachPage) {
                    proteinKeyToSelected.put(proteinKey, false);
                    proteinAllSelections = remove(proteinAllSelections, proteinKey);
                }
            }
        }

        buttonCheck();
        proteinsJTable.revalidate();
        proteinsJTable.repaint();
    }

    private void fetchBigProteins(){
        if (proteinAllSelections.size() == 0){
            String selectedProteinKey = (String) proteinsJTable.getValueAt(0, 1);
            proteinAllSelections.add(selectedProteinKey);
            proteinKeyToSelected.put(selectedProteinKey, true);
        }

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setTitle("Fetching all records for " + proteinAllSelections.size() +" proteins. Please wait...");
        new Thread("GetPSM") {
            @Override
            public void run() {
                progressDialog.setVisible(true);

                progressDialog.setPrimaryProgressCounterIndeterminate(true);
            }
        }.start();

        new Thread("GetPSM") {
            @Override
            public void run() {
                progressDialog.setMaxPrimaryProgressCounter(proteinAllSelections.size());
                try {
                    ArrayList<String>[] oneFetch = sqliteConnection.getSpectrumList(proteinAllSelections, progressDialog);
                    updateAllPSMIndexes(oneFetch);
                    updatePSMTable();
                    updateCoverage(proteinAllSelections);

                } catch (Exception e) {
                    e.printStackTrace();
                    progressDialog.setRunFinished();
                }
                progressDialog.setRunFinished();


            }
        }.start();
    }

    /**
     * pSMPageSelectNumJTextField event
     *  @param evt Key click event
     */
    private void pSMPageSelectNumJTextFieldKeyReleased(KeyEvent evt){

        int keyChar = evt.getKeyChar();
        if (evt.getKeyCode() == 10 || keyChar >= KeyEvent.VK_0 && keyChar <= KeyEvent.VK_9) {
            if (evt.getKeyCode() == 10) {
                if (!pSMPageSelectNumJTextField.getText().equals("")) {
                    if (Integer.parseInt(pSMPageSelectNumJTextField.getText()) > allSpectrumIndex.size() || Integer.parseInt(pSMPageSelectNumJTextField.getText()) < 1) {
                        pSMPageSelectNumJTextField.setBackground(Color.GRAY);
                    } else {
                        pSMPageSelectNumJTextField.setBackground(Color.WHITE);
                        selectedPSMPageNum = Integer.parseInt(pSMPageSelectNumJTextField.getText());
                        pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                        if (pSMPageToSelected.contains(selectedPSMPageNum)){
                            pSMAllSelectedJCheckBox.setSelected(true);
                        } else {
                            pSMAllSelectedJCheckBox.setSelected(false);
                        }

                        buttonCheck();
                        updateTable();

                        spectrumJTable.requestFocus();
                        spectrumJTable.setRowSelectionInterval(0, 0);
                    }
                }
            }
        } else {
            pSMPageSelectNumJTextField.setText("");
            evt.consume();
        }
    }

    private void psmColumnSelectionJButtonActionPerformed(ActionEvent evt){
        ArrayList<String> columnName = new ArrayList<>();
        columnName.add("Selected");
        columnName.add("Experiment");
        columnName.add("Title");
        columnName.add("Sequence");
        columnName.add("Charge");
        columnName.add("m/z");
        columnName.addAll(psmScoreName);

        if(pSMColumnToSelected == null){
            pSMColumnToSelected = new HashMap<>();
            for (String eachColumn: columnName){
                pSMColumnToSelected.put(eachColumn, true);
            }
        }

        new ColumnSelectionDialog(this, spectrumJTable, columnName, false);
    }

    /**
     * UpButtonAction
     * @param evt Mouse click event
     */
    private void pSMUpJButtonActionPerformed(ActionEvent evt){

        selectedPSMPageNum--;
        pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));
        buttonCheck();

        if (pSMPageToSelected.contains(selectedPSMPageNum)){
            pSMAllSelectedJCheckBox.setSelected(true);
        } else {
            pSMAllSelectedJCheckBox.setSelected(false);
        }

        updateTable();

        spectrumJTable.requestFocus();
        spectrumJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * pSMNextJButtonAction
     * @param evt Mouse click event
     */
    private void pSMNextJButtonActionPerformed(ActionEvent evt){

        selectedPSMPageNum++;
        pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));
        buttonCheck();

        if (pSMPageToSelected.contains(selectedPSMPageNum)){
            pSMAllSelectedJCheckBox.setSelected(true);
        } else {
            pSMAllSelectedJCheckBox.setSelected(false);
        }

        updateTable();

        spectrumJTable.requestFocus();
        spectrumJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void experimentsJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update spectrum table according to the select protein id
     * @param evt Mouse click event
     */
    private void experimentsJTableMouseReleased(MouseEvent evt) {

        nonProteinSearchMode = false;
        experimentsJTable.requestFocus();
        int row = experimentsJTable.getSelectedRow();
        int column = experimentsJTable.getSelectedColumn();
        ArrayList<String> newList = new ArrayList<>();
        selectedProteinPageNum = 1;
        String selectedExpKey;

        if (row != -1) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            selectedExpKey = (String) experimentsJTable.getValueAt(row, 1);

            if (column == experimentsJTable.getColumn("Selected").getModelIndex()){
                if (!experimentKeyToSelected.containsKey(selectedExpKey)) {
                    expAllSelections.add(selectedExpKey);
                    experimentKeyToSelected.put(selectedExpKey, true);
                } else {
                    Boolean isSelected = experimentKeyToSelected.get(selectedExpKey);
                    if (isSelected) {
                        experimentKeyToSelected.put(selectedExpKey, false);
                        remove(expAllSelections, selectedExpKey);
                    } else {
                        expAllSelections.add(selectedExpKey);
                        experimentKeyToSelected.put(selectedExpKey, true);
                    }
                }
                currentExpAllSelections = new ArrayList<>();
                currentExpAllSelections.addAll(expAllSelections);
                buttonCheck();
                try {
                    System.out.println(expAllSelections);
                    if (expAllSelections.size() == 0){
                        newList.add(selectedExpKey);
                    } else {
                        newList.addAll(expAllSelections);
                    }
                    updateAllProteinIndexes(sqliteConnection.getProteinList(newList));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                currentExpAllSelections = new ArrayList<>();
                currentExpAllSelections.add(selectedExpKey);
                newList.add(selectedExpKey);
                try {
                    updateAllProteinIndexes(sqliteConnection.getProteinList(newList));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            updateTable();
            //`proteinJTableMouseReleased(null);
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Update spectrum table according to the select protein ID
     * @param evt Key event
     */
    private void experimentsJTableKeyReleased(KeyEvent evt) {

        nonProteinSearchMode = false;
        experimentsJTable.requestFocus();
        expAllSelections = new ArrayList<>();
        selectedProteinPageNum = 1;

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int[] rows = experimentsJTable.getSelectedRows();

            if (rows.length != 0) {
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                for (int oneRow : rows){
                    expAllSelections.add((String) experimentsJTable.getValueAt(oneRow, 0));
                }
                try {
                    updateAllProteinIndexes(sqliteConnection.getProteinList(expAllSelections));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                updateTable();
                proteinJTableMouseReleased(null);
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void proteinJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update spectrum table according to the select protein id
     * @param evt Mouse click event
     */
    private void proteinJTableMouseReleased(MouseEvent evt) {

        proteinsJTable.requestFocus();
        int row = proteinsJTable.getSelectedRow();
        int column = proteinsJTable.getSelectedColumn();
        String selectedProteinKey;
        selectedPSMPageNum = 1;

        if (!nonProteinSearchMode) {
            if (row != -1) {
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                selectedProteinKey = (String) proteinsJTable.getValueAt(row, 1);

                if (column == proteinsJTable.getColumn("Selected").getModelIndex()){
                    if (!proteinKeyToSelected.containsKey(selectedProteinKey)) {
                        proteinAllSelections.add(selectedProteinKey);
                        proteinKeyToSelected.put(selectedProteinKey, true);
                    } else {
                        Boolean isSelected = proteinKeyToSelected.get(selectedProteinKey);
                        if (isSelected) {
                            proteinKeyToSelected.put(selectedProteinKey, false);
                            remove(proteinAllSelections, selectedProteinKey);
                        } else {
                            proteinAllSelections.add(selectedProteinKey);
                            proteinKeyToSelected.put(selectedProteinKey, true);
                        }
                    }
                    buttonCheck();
                }

                try {
                    if (sqliteConnection.getSpectrumListOneProtein(selectedProteinKey) == null){
                        raiseWarningDialog("There is no PSM for this protein.");
                    } else {
                        updateAllPSMIndexes(sqliteConnection.getSpectrumListOneProtein(selectedProteinKey));
                        updatePSMTable();
                        proteinCurrentSelections = new ArrayList<>();
                        proteinCurrentSelections.add(selectedProteinKey);

                        updateCoverage(proteinCurrentSelections);
                        showAllSelectedProteinsJButton.setBackground(Color.WHITE);

                        proteinsJTable.revalidate();
                        proteinsJTable.repaint();
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }


                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
     * Update spectrum table according to the select protein ID
     * @param evt Key event
     */
    private void proteinJTableKeyReleased(KeyEvent evt) {

        proteinsJTable.requestFocus();
        String selectedProteinKey;
        selectedPSMPageNum = 1;

        if (!nonProteinSearchMode) {
            if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                    || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

                final int row = proteinsJTable.getSelectedRow();

                if (row != -1) {
                    this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                    selectedProteinKey = (String) proteinsJTable.getValueAt(row, 1);

                    try {
                        if (sqliteConnection.getSpectrumListOneProtein(selectedProteinKey) == null){
                            raiseWarningDialog("There is no PSM for this protein.");
                        } else {
                            updateAllPSMIndexes(sqliteConnection.getSpectrumListOneProtein(selectedProteinKey));
                            updatePSMTable();

                            proteinCurrentSelections = new ArrayList<>();
                            proteinCurrentSelections.add(selectedProteinKey);

                            updateCoverage(proteinCurrentSelections);
                            showAllSelectedProteinsJButton.setBackground(Color.WHITE);

                            proteinsJTable.revalidate();
                            proteinsJTable.repaint();

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseReleased(MouseEvent evt) {

        spectrumJTable.requestFocus();
        int row = spectrumJTable.getSelectedRow();
        int column = spectrumJTable.getSelectedColumn();


        if (row != -1) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            selectedPsmKey = (String) spectrumJTable.getValueAt(row, 1);

            try {
                if (newDefinedModsMatch.containsKey(selectedPsmKey)){
                    spectrumMatch = newDefinedModsMatch.get(selectedPsmKey);
                } else {
                    spectrumMatch = sqliteConnection.getSpectrumMatch(selectedPsmKey);
                }
                String spectrumTitle =  sqliteConnection.getSpectrumOldTitle(selectedPsmKey);
                checkSpectrumFactory(spectrumTitle.split("\\.")[0]);

                updateSpectrum(getSpectrum(selectedPsmKey), spectrumMatch);
            } catch (SQLException | FileParsingException | MzMLUnmarshallerException | IOException e) {
                e.printStackTrace();
            }

            if (column == spectrumJTable.getColumn("Selected").getModelIndex()) {
                if(!spectrumKeyToSelected.containsKey(selectedPsmKey)){
                    pSMAllSelections.add(selectedPsmKey);
                    spectrumKeyToSelected.put(selectedPsmKey, true);
                } else {
                    Boolean isSelected = spectrumKeyToSelected.get(selectedPsmKey);
                    if(isSelected){
                        spectrumKeyToSelected.put(selectedPsmKey, false);
                        pSMAllSelections = remove(pSMAllSelections, selectedPsmKey);
                    } else {
                        pSMAllSelections.add(selectedPsmKey);
                        spectrumKeyToSelected.put(selectedPsmKey, true);
                    }
                }
            }
            spectrumJTable.revalidate();
            spectrumJTable.repaint();

            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Key event
     */
    private void spectrumJTableKeyReleased(KeyEvent evt) {

        spectrumJTable.requestFocus();

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int row = spectrumJTable.getSelectedRow();

            if (row != -1) {
                selectedPsmKey = (String) spectrumJTable.getValueAt(row, 1);

               try {
                   if (newDefinedModsMatch.containsKey(selectedPsmKey)){
                       spectrumMatch = newDefinedModsMatch.get(selectedPsmKey);
                   } else {
                       spectrumMatch = sqliteConnection.getSpectrumMatch(selectedPsmKey);
                   }
                    updateSpectrum(getSpectrum(selectedPsmKey), spectrumMatch);
                } catch (SQLException | FileParsingException | MzMLUnmarshallerException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Selected whole page
     * @param evt Mouse click event
     */
    private void pSMAllSelectedJCheckBoxMouseClicked(MouseEvent evt) {
        if (pSMAllSelectedJCheckBox.isSelected()) {
            for (String spectrumKey : selectPageSpectrumIndex) {
                spectrumKeyToSelected.put(spectrumKey, true);
                if (!pSMAllSelections.contains(spectrumKey)){
                    pSMAllSelections.add(spectrumKey);
                }

                pSMPageToSelected.add(selectedPSMPageNum);
            }
        } else {

            for (String spectrumKey : selectPageSpectrumIndex) {
                spectrumKeyToSelected.put(spectrumKey, false);
                pSMAllSelections = remove(pSMAllSelections, spectrumKey);

                pSMPageToSelected = remove(pSMPageToSelected, selectedPSMPageNum);
            }
        }
        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     * Next and up button check
     */
    public void buttonCheck() {

        if (selectedPSMPageNum == 1) {
            pSMUpJButton.setEnabled(false);
        } else {
            pSMUpJButton.setEnabled(true);
        }

        if (pSMPageNumJTextField.getText().contains(String.valueOf(allSpectrumIndex.size()) + "/") || pSMPageNumJTextField.getText().split("/")[0].equals(pSMPageNumJTextField.getText().split("/")[1])) {
            pSMNextJButton.setEnabled(false);
        } else {
            pSMNextJButton.setEnabled(true);
        }

        if (selectedProteinPageNum == 1) {
            proteinUpJButton.setEnabled(false);
        } else {
            proteinUpJButton.setEnabled(true);
        }

        if (proteinPageNumJTextField.getText().contains(String.valueOf(allProteinIndex.size()) + "/") || proteinPageNumJTextField.getText().split("/")[0].equals(proteinPageNumJTextField.getText().split("/")[1])) {
            proteinNextJButton.setEnabled(false);
        } else {
            proteinNextJButton.setEnabled(true);
        }
        if (proteinAllSelections.size()!=0){
            showAllSelectedProteinsJButton.setEnabled(true);
            showAllSelectedProteinsJButton.setToolTipText("Selected "+ proteinAllSelections.size() + " proteins.");
        } else {
            showAllSelectedProteinsJButton.setEnabled(false);
            showAllSelectedProteinsJButton.setToolTipText("No proteins selected.");
        }

    }

    public void updatePSMTable(){
        ProgressDialogX progressDialogX = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Moving. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "MovingPro").start();

        new Thread("Moving") {
            @Override
            public void run() {
                try {

                    ArrayList<ArrayList<Object>> selectedPSMItem = new ArrayList<>();
                    spectrumJTable.removeAll();

                    selectPageSpectrumIndex = allSpectrumIndex.get(selectedPSMPageNum - 1);
                    for (String spectrumIndex : selectPageSpectrumIndex) {
                        try {
                            selectedPSMItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                        } catch (Exception e) {
                            progressDialogX.setRunFinished();
                            e.printStackTrace();
                            break;
                        }
                    }
                    psmTableModel.updateTable(selectedPSMItem, selectPageSpectrumIndex, spectrumKeyToSelected, mappedSpectrumIndex);
                    ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
                    spectrumJTable.repaint();

                    spectrumJTable.setRowSelectionInterval(0, 0);
                    spectrumJTableMouseReleased(null);

                    progressDialogX.setRunFinished();

                    pSMPageNumJTextField.setText(selectedPSMPageNum + "/" + allSpectrumIndex.size());
                    buttonCheck();
                    psmsJPanel.repaint();
                } catch (Exception e){
                    e.printStackTrace();
                    progressDialogX.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Update table
     */
    public void updateTable(ArrayList<String[]> searchIDs){

        ArrayList<ArrayList<Object>> selectedPSMItem = new ArrayList<>();
        ArrayList<ArrayList<Object>> selectedProteinItem = new ArrayList<>();
        selectPageSpectrumIndex = new ArrayList<>();
        selectPageProteinIndex = new ArrayList<>();

        proteinsJTable.removeAll();
        spectrumJTable.removeAll();
        String oneProteinID = "";
        for (String[] oneIndexes : searchIDs){
            selectPageSpectrumIndex.add(oneIndexes[0]);
            try {
                if (!oneProteinID.equals(oneIndexes[1])){
                    oneProteinID = oneIndexes[1];
                    selectedProteinItem.add(sqliteConnection.getOneProteinItem(oneIndexes[1]));
                    selectPageProteinIndex.add(oneIndexes[1]);
                }
                selectedPSMItem.add(sqliteConnection.getOneSpectrumItem(oneIndexes[0]));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        proteinTableModel.updateTable(selectedProteinItem, selectPageProteinIndex, proteinKeyToSelected);
        ((DefaultTableModel) proteinsJTable.getModel()).fireTableDataChanged();
        proteinsJTable.repaint();
        proteinsJTable.setRowSelectionInterval(0, 0);
        proteinJTableMouseReleased(null);

        psmTableModel.updateTable(selectedPSMItem, selectPageSpectrumIndex, spectrumKeyToSelected, mappedSpectrumIndex);
        ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
//        spectrumJTable.revalidate();
//        spectrumJTable.repaint();

        //spectrumJTable.setRowSelectionInterval(0, 0);
        spectrumJTableMouseReleased(null);

        selectedPSMPageNum = 1;
        selectedProteinPageNum = 1;

        proteinPageNumJTextField.setText("1/1");
        buttonCheck();

        pSMPageNumJTextField.setText("1/1");
        psmsJPanel.repaint();

    }

    /**
     * Update table
     */
    public void updateTable(){
        ProgressDialogX progressDialogX = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Moving. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "MovingPro").start();

        new Thread("Moving") {
            @Override
            public void run() {
                try {
                    selectPageProteinIndex = allProteinIndex.get(selectedProteinPageNum - 1);

                    ArrayList<ArrayList<Object>> selectedPSMItem = new ArrayList<>();
                    ArrayList<ArrayList<Object>> selectedProteinItem = new ArrayList<>();
                    spectrumJTable.removeAll();
                    proteinsJTable.removeAll();

                    for (String proteinIndex : selectPageProteinIndex) {
                        try {
                            selectedProteinItem.add(sqliteConnection.getOneProteinItem(proteinIndex));
                        } catch (Exception e) {
                            progressDialogX.setRunFinished();
                            e.printStackTrace();
                            break;
                        }
                    }

                    proteinTableModel.updateTable(selectedProteinItem, selectPageProteinIndex, proteinKeyToSelected);
                    ((DefaultTableModel) proteinsJTable.getModel()).fireTableDataChanged();
                    proteinsJTable.repaint();

                    selectPageSpectrumIndex = allSpectrumIndex.get(selectedPSMPageNum - 1);
                    for (String spectrumIndex : selectPageSpectrumIndex) {
                        selectedPSMItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                    }
                    psmTableModel.updateTable(selectedPSMItem, selectPageSpectrumIndex, spectrumKeyToSelected, mappedSpectrumIndex);
                    ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
                    spectrumJTable.repaint();

                    spectrumJTable.setRowSelectionInterval(0, 0);
                    spectrumJTableMouseReleased(null);

                    progressDialogX.setRunFinished();

                    proteinPageNumJTextField.setText(selectedProteinPageNum + "/" + allProteinIndex.size());
                    buttonCheck();

                    pSMPageNumJTextField.setText(selectedPSMPageNum + "/" + allSpectrumIndex.size());
                    psmsJPanel.repaint();
                    selectedPSMItem = null;
                    selectedProteinItem = null;
                    //System.gc();
                }catch (Exception e) {
                    progressDialogX.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * upSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void proteinUpSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("UpSorting"){
            @Override
            public void run() {

                try {
                    int count = 0;
                    ArrayList<String> each = new ArrayList<>();

                    String selectedItem = (String) proteinSortColumnJCombox.getSelectedItem();

                    if (selectedItem.equals("Selected")) {
                        ArrayList<ArrayList<String>> tempProteinIndex = allProteinIndex;
                        allProteinIndex = new ArrayList<>();
                        for (ArrayList<String> oneList : tempProteinIndex) {
                            for (String proteinID : oneList) {
                                if (!proteinKeyToSelected.containsKey(proteinID)) {
                                    each.add(proteinID);
                                    if (count == 1000) {
                                        allProteinIndex.add(each);

                                        each = new ArrayList<>();

                                        count = 0;
                                    }
                                    count++;
                                } else {
                                    if (!proteinKeyToSelected.get(proteinID)) {
                                        each.add(proteinID);
                                        if (count == 1000) {
                                            allProteinIndex.add(each);

                                            each = new ArrayList<>();

                                            count = 0;
                                        }
                                        count++;
                                    }
                                }
                            }
                        }
                        for (String proteinID : proteinKeyToSelected.keySet()) {
                            if (proteinKeyToSelected.get(proteinID)) {
                                each.add(proteinID);
                                if (count == 1000) {
                                    allProteinIndex.add(each);

                                    each = new ArrayList<>();

                                    count = 0;
                                }
                                count++;
                            }
                        }
                        if (count != 0) {
                            allProteinIndex.add(each);
                        }
                    } else {
                        allProteinIndex = new ArrayList<>();
                        Connection connection = sqliteConnection.getConnection();

                        Statement statement = connection.createStatement();
                        StringBuilder multiExpNames = new StringBuilder("SELECT Protein FROM (");

                        if (Character.isDigit(selectedItem.charAt(0))){
                            selectedItem = "Str_" +selectedItem;
                        }

                        for (String eachExp : currentExpAllSelections) {
                            multiExpNames.append("SELECT Protein, ").append(selectedItem).append(" FROM Protein_").append(eachExp).append(" UNION ALL ");
                        }

                        String multiExpNamesString = String.valueOf(multiExpNames);
                        multiExpNamesString = multiExpNamesString.substring(0, multiExpNamesString.length() - 11);
                        multiExpNamesString += " ) as newTable" + " ORDER BY " + selectedItem;

                        ResultSet rs1 = statement.executeQuery(multiExpNamesString);

                        String proteinID;

                        while (rs1.next()) {

                            proteinID = rs1.getString(1);

                            each.add(proteinID);
                            if (count == 1000) {
                                allProteinIndex.add(each);

                                each = new ArrayList<>();

                                count = 0;
                            }
                            count++;

                        }

                        if (count != 0) {
                            allProteinIndex.add(each);
                        }

                    }

                    selectedProteinPageNum = 1;

                    proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum) + "/" + String.valueOf(allProteinIndex.size()));

                    buttonCheck();

                    updateTable();
                    //System.gc();

                    orderProgressDialog.setRunFinished();
                } catch (Exception e){
                    e.printStackTrace();
                    orderProgressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * downSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void proteinDownSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("DownSorting") {
            @Override
            public void run() {

                try{
                    int count = 0;
                    ArrayList<String> each = new ArrayList<>();

                    String selectedItem = (String) proteinSortColumnJCombox.getSelectedItem();

                    if (selectedItem.equals("Selected")){
                        ArrayList<ArrayList<String>> tempProteinIndex = allProteinIndex;
                        allProteinIndex = new ArrayList<>();
                        for (String proteinID : proteinKeyToSelected.keySet()){
                            if (proteinKeyToSelected.get(proteinID)){
                                each.add(proteinID);
                                if(count == 1000){
                                    allProteinIndex.add(each);

                                    each = new ArrayList<>();

                                    count = 0;
                                }
                                count ++;
                            }
                        }
                        for (ArrayList<String> oneList : tempProteinIndex){
                            for (String proteinID : oneList){
                                if (!proteinKeyToSelected.containsKey(proteinID)){
                                    each.add(proteinID);
                                    if(count == 1000){
                                        allProteinIndex.add(each);

                                        each = new ArrayList<>();

                                        count = 0;
                                    }
                                    count ++;
                                } else {
                                    if (!proteinKeyToSelected.get(proteinID)){
                                        each.add(proteinID);
                                        if(count == 1000){
                                            allProteinIndex.add(each);

                                            each = new ArrayList<>();

                                            count = 0;
                                        }
                                        count ++;
                                    }
                                }
                            }
                        }
                        if(count != 0){
                            allProteinIndex.add(each);
                        }
                    } else {
                        allProteinIndex = new ArrayList<>();
                        Connection connection = sqliteConnection.getConnection();

                        Statement statement = connection.createStatement();
                        StringBuilder multiExpNames = new StringBuilder("SELECT Protein FROM (");

                        if (Character.isDigit(selectedItem.charAt(0))){
                            selectedItem = "Str_" +selectedItem;
                        }

                        for (String eachExp : currentExpAllSelections) {
                            multiExpNames.append("SELECT Protein, ").append(selectedItem).append(" FROM Protein_").append(eachExp).append(" UNION ALL ");
                        }

                        String multiExpNamesString = String.valueOf(multiExpNames);
                        multiExpNamesString = multiExpNamesString.substring(0, multiExpNamesString.length() - 11);
                        multiExpNamesString += " ) as newTable" + " ORDER BY " + selectedItem + " DESC ";

                        ResultSet rs1 = statement.executeQuery(multiExpNamesString);

                        String proteinID;

                        while (rs1.next()) {

                            proteinID = rs1.getString(1);

                            each.add(proteinID);
                            if (count == 1000) {
                                allProteinIndex.add(each);

                                each = new ArrayList<>();

                                count = 0;
                            }
                            count++;

                        }

                        if (count != 0) {
                            allProteinIndex.add(each);
                        }

                    }

                    selectedProteinPageNum = 1;

                    proteinPageNumJTextField.setText(String.valueOf(selectedProteinPageNum) + "/" + String.valueOf(allProteinIndex.size()));

                    buttonCheck();

                    updateTable();
                    //System.gc(); Low speed

                    orderProgressDialog.setRunFinished();
                } catch (Exception e) {
                    e.printStackTrace();
                    orderProgressDialog.setRunFinished();
                }
            }
        }.start();
    }

//    private void orderProteins(Boolean desc) throws SQLException {
//        allProteinIndex = new ArrayList<>();
//        ArrayList<String> each = new ArrayList<>();
//        int count = 0;
//        Connection connection = sqliteConnection.getConnection();
//
//        ArrayList<String> sortedProteinKeys = new ArrayList<>();
//        HashMap<String, ArrayList<String>> proteinExp = new HashMap<>();
//
//        Statement statement = connection.createStatement();
//        StringBuilder multiExpNames = new StringBuilder("SELECT Protein FROM (");
//
//        for (String eachExp : currentExpAllSelections) {
//            multiExpNames.append("SELECT Protein ").append(" FROM Protein_").append(eachExp).append(" UNION ALL ");
//        }
//        String multiExpNamesString = String.valueOf(multiExpNames);
//        multiExpNamesString = multiExpNamesString.substring(0, multiExpNamesString.length() - 11);
//        multiExpNamesString += " ) as newTable";
//
//        ResultSet rs1 = statement.executeQuery(multiExpNamesString);
//        String proteinID;
//        String pureProtein;
//        while (rs1.next()) {
//            proteinID = rs1.getString(1);
//            pureProtein = proteinID.split("_")[1];
//            sortedProteinKeys.add(pureProtein);
//            if (proteinExp.containsKey(pureProtein)){
//                proteinExp.get(pureProtein).add(proteinID);
//            } else {
//                ArrayList<String> temp = new ArrayList<>();
//                temp.add(proteinID);
//                proteinExp.put(pureProtein, temp);
//            }
//        }
//
//        if (!desc){
//            Collections.sort(sortedProteinKeys);
//        }else {
//            sortedProteinKeys.sort(Collections.reverseOrder());
//        }
//
//        for (String oneKey : sortedProteinKeys){
//            for (String oneOri : proteinExp.get(oneKey)){
//                each.add(oneOri);
//                if (count == 1000) {
//                    allProteinIndex.add(each);
//
//                    each = new ArrayList<>();
//
//                    count = 0;
//                }
//                count++;
//            }
//        }
//        if (count != 0) {
//            allProteinIndex.add(each);
//        }
//
//        sortedProteinKeys.clear();
//        proteinExp.clear();
//    }

    /**
     * upSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void psmUpSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("UpSorting"){
            @Override
            public void run() {
                try {
                    ArrayList<String> allSingleSpectrumIndex = new ArrayList<>();
                    for (ArrayList<String> oneList : allSpectrumIndex) {
                        allSingleSpectrumIndex.addAll(oneList);
                    }

                    allSpectrumIndex = new ArrayList<>();

                    String selectedItem = (String) psmSortColumnJCombox.getSelectedItem();

                    Connection connection = sqliteConnection.getConnection();

                    Statement statement = connection.createStatement();
                    int count = 0;

                    ArrayList<String> each = new ArrayList<>();

                    StringBuilder multiExpNames = new StringBuilder("SELECT PSMIndex FROM (");

                    if (Character.isDigit(selectedItem.charAt(0))){
                        selectedItem = "Str_" +selectedItem;
                    }

                    for (String eachExp : currentExpAllSelections) {
                        multiExpNames.append("SELECT PSMIndex, ").append(selectedItem).append(" FROM SpectrumMatch_").append(eachExp).append(" UNION ALL ");
                    }
                    String multiExpNamesString = String.valueOf(multiExpNames);
                    multiExpNamesString = multiExpNamesString.substring(0, multiExpNamesString.length() - 11);
                    multiExpNamesString += " ) as newTable" + " ORDER BY " + selectedItem;

                    ResultSet rs1 = statement.executeQuery(multiExpNamesString);

                    String spectrumKey;

                    while (rs1.next()) {

                        spectrumKey = rs1.getString(1);

                        if (allSingleSpectrumIndex.contains(spectrumKey)) {
                            each.add(spectrumKey);
                            if (count == 1000) {
                                allSpectrumIndex.add(each);

                                each = new ArrayList<>();

                                count = 0;
                            }
                            count++;
                        }
                    }

                    if (count != 0) {
                        allSpectrumIndex.add(each);
                    }

                    selectedPSMPageNum = 1;

                    pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                    buttonCheck();

                    updateTable();
                    //System.gc();

                    orderProgressDialog.setRunFinished();
                } catch (Exception e){
                    e.printStackTrace();
                    orderProgressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * downSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void psmDownSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("DownSorting") {
            @Override
            public void run() {
                try {
                    ArrayList<String> allSingleSpectrumIndex = new ArrayList<>();
                    for (ArrayList<String> oneList : allSpectrumIndex){
                        allSingleSpectrumIndex.addAll(oneList);
                    }

                    allSpectrumIndex = new ArrayList<>();

                    String selectedItem = (String) psmSortColumnJCombox.getSelectedItem();

                    Connection connection = sqliteConnection.getConnection();

                    Statement statement = connection.createStatement();
                    int count = 0;

                    ArrayList<String> each = new ArrayList<>();
                    StringBuilder multiExpNames = new StringBuilder("SELECT PSMIndex FROM (");

                    if (Character.isDigit(selectedItem.charAt(0))){
                        selectedItem = "Str_" +selectedItem;
                    }

                    for (String eachExp : currentExpAllSelections) {
                        multiExpNames.append("SELECT PSMIndex, ").append(selectedItem).append(" FROM SpectrumMatch_").append(eachExp).append(" UNION ALL ");
                    }
                    String multiExpNamesString = String.valueOf(multiExpNames);
                    multiExpNamesString = multiExpNamesString.substring(0, multiExpNamesString.length() - 11);
                    multiExpNamesString += " ) as newTable" + " ORDER BY " + selectedItem + " DESC ";

                    ResultSet rs1 = statement.executeQuery(multiExpNamesString);

                    String spectrumKey;

                    while (rs1.next()){

                        spectrumKey = rs1.getString(1);

                        if (allSingleSpectrumIndex.contains(spectrumKey)){
                            each.add(spectrumKey);
                            if(count == 1000){
                                allSpectrumIndex.add(each);

                                each = new ArrayList<>();

                                count = 0;
                            }
                            count ++;
                        }
                    }

                    if(count != 0){
                        allSpectrumIndex.add(each);
                    }

                    selectedPSMPageNum = 1;

                    pSMPageNumJTextField.setText(String.valueOf(selectedPSMPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                    buttonCheck();

                    updateTable();
                    //System.gc();

                    orderProgressDialog.setRunFinished();
                } catch (Exception e) {
                    e.printStackTrace();
                    orderProgressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Setting the modification color
     * @param evt Mouse click event
     */
    private void settingColorJButtonActionPerform(ActionEvent evt){
        new PTMColorDialog(this, ptmFactory, allModifications);
    }

    private void checkSpectrumFactory(String spectrumFileName){

        if (!finishedSpectrumFiles.contains(spectrumFileName)) {
            if (readFactoryThread.isAlive()) {
                readFactoryThread.interrupt();
                updateSpectrumFactory(true, spectrumFileName);
            }
        }
//        ProgressDialogX progressDialog = new ProgressDialogX(this,
//                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
//                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
//                true);
//        progressDialog.setPrimaryProgressCounterIndeterminate(true);
//        progressDialog.setTitle("Load new spectrum file. Please Wait...");
//
//        new Thread(() -> {
//            try {
//                progressDialog.setVisible(true);
//            } catch (IndexOutOfBoundsException ignored) {
//            }
//        }, "ProgressDialog").start();
//        new Thread("DisplayThread") {
//            @Override
//            public void run() {
//
//                if (!finishedSpectrumFiles.contains(spectrumFileName)) {
//                    if (readFactoryThread.isAlive()) {
//                        readFactoryThread.interrupt();
//                        updateSpectrumFactory(true, spectrumFileName);
//                    }
//                }
//
//                progressDialog.setRunFinished();
//            }
//        }.start();

    }

    /**
     * Get current spectrum according to the selected spectrum key
     * @return MSnSpectrum
     */
    public MSnSpectrum getSpectrum(String selectedPsmKey) throws SQLException, MzMLUnmarshallerException, IOException, FileParsingException {

        String spectrumTitle =  sqliteConnection.getSpectrumOldTitle(selectedPsmKey);

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

    public void updateSpectrum(String peptideSequence){
        try {
            ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();
            if (newDefinedMods.containsKey(selectedPsmKey)){
                spectrumMatch.getBestPeptideAssumption().getPeptide().clearModificationMatches();
                for (int eachIndex : newDefinedMods.get(selectedPsmKey).keySet()){
                    ModificationMatch newMod;
                    double oldMass = newDefinedMods.get(selectedPsmKey).get(eachIndex)[0];
                    double newMass = newDefinedMods.get(selectedPsmKey).get(eachIndex)[1];
                    String singleModificationName;
                    String aaName;
                    if (eachIndex == 0){
                        aaName = "N-term";
                        eachIndex = 1;
                    } else if (eachIndex == spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence().length() + 1){
                        aaName = "C-term";
                        eachIndex = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence().length();
                    } else {
                        aaName = String.valueOf(peptideSequence.charAt(eachIndex-1));
                    }
                    if (!Objects.equals(oldMass, newMass)){
                        singleModificationName = checkReporter(newMass, aaName);
                    }
                    else {
                        singleModificationName = checkReporter(oldMass, aaName);
                    }

                    newMod = new ModificationMatch(singleModificationName, true, eachIndex);
                    utilitiesModifications.add(newMod);
                }
                spectrumMatch.getBestPeptideAssumption().getPeptide().setModificationMatches(utilitiesModifications);
                newDefinedModsMatch.put(selectedPsmKey, spectrumMatch);
            } else {
                newDefinedModsMatch.remove(selectedPsmKey);
            }

            updateSpectrum(getSpectrum(selectedPsmKey), spectrumMatch);
        } catch (SQLException | FileParsingException | MzMLUnmarshallerException | IOException e ) {
            e.printStackTrace();
        }
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
        return modMass + " of " + modAA;
    }

    /**
     * Update spectrum
     * @param mSnSpectrum MSN spectrm
     * @param spectrumMatch Spectrum match
     */
    private void updateSpectrum(MSnSpectrum mSnSpectrum, SpectrumMatch spectrumMatch) {

        String modSequence;
        SpectrumIdentificationAssumption spectrumIdentificationAssumption;

        if (spectrumMatch.getBestPeptideAssumption() != null) {
            spectrumIdentificationAssumption = spectrumMatch.getBestPeptideAssumption();
//            modSequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);
            modSequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
        } else if (spectrumMatch.getBestTagAssumption() != null) {
            spectrumIdentificationAssumption = spectrumMatch.getBestTagAssumption();
//            modSequence = spectrumMatch.getBestTagAssumption().getTag().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false);
            modSequence = spectrumMatch.getBestTagAssumption().getTag().getLongestAminoAcidSequence();
        } else {
            throw new IllegalArgumentException("No best assumption found for spectrum " + ".");
        }

        TitledBorder titledBorder = BorderFactory.createTitledBorder(modSequence + " \t ");
        titledBorder.setTitleColor(Color.black);
        if (newDefinedMods.containsKey(selectedPsmKey)){
            for (int eachIndex : newDefinedMods.get(selectedPsmKey).keySet()){
                if (!Objects.equals(newDefinedMods.get(selectedPsmKey).get(eachIndex)[0], newDefinedMods.get(selectedPsmKey).get(eachIndex)[1])){
                    modSequence += "Mod Changed!";
                    titledBorder = BorderFactory.createTitledBorder(modSequence + " \t ");
                    titledBorder.setTitleColor(Color.red);
                    break;
                }
            }
        }

        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        spectrumShowJPanel.setBorder(titledBorder);
        spectrumMainPanel.updateSpectrum(spectrumIdentificationAssumption, mSnSpectrum, String.valueOf(selectedPsmKey));

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
        //System.gc();

    }

    private void importData(){

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {
                ImportData oneImport;
                try {
                    oneImport = new ImportData(GUIMainClass.this, resultsFolder, threadsNumber, progressDialog);
                    sqliteConnection = oneImport.getSqLiteConnection();
                    allModifications = oneImport.getAllModifications();
                    proteinSeqMap = oneImport.getProteinSeqMap();
                    experimentInfo = oneImport.getExperimentInfo();
                    expNumList = oneImport.getExpNumList();
                    spectrumFileOrder = oneImport.getSpectrumFileOrder();
                    spectrumFileMap = oneImport.getSpectrumFileMap();
                    predictionEntryHashMap = oneImport.getPredictionEntryHashMap();

                    psmScoreName = oneImport.getPSMScoreName();
                    proteinScoreName = oneImport.getProteinScoreName();

                    pSMColumnToSelected = new HashMap<>();
                    pSMColumnToSelected.put("Selected", true);
                    pSMColumnToSelected.put("Experiment", true);
                    pSMColumnToSelected.put("Title", true);
                    pSMColumnToSelected.put("Sequence", true);
                    pSMColumnToSelected.put("Charge", true);
                    pSMColumnToSelected.put("m/z", true);
                    for (String eachColumn: psmScoreName){
                        pSMColumnToSelected.put(eachColumn, false);
                    }

                    ArrayList<String> showNames = (ArrayList<String>) Stream.of("SpectrumFile", "Peptide", "ModifiedPeptide", "Retention", "ObservedMass", "ObservedMZ", "CalculatedPeptideMass", "DeltaMass", "Expectation",
                            "Hyperscore", "Nextscore", "PeptideProphetProbability", "Protein", "Gene").collect(Collectors.toList());

                    for (String eachColumn: showNames){
                        if (psmScoreName.contains(eachColumn)){
                            pSMColumnToSelected.put(eachColumn, true);
                        }
                    }

                    proteinColumnToSelected = new HashMap<>();
                    proteinColumnToSelected.put("Selected", true);
                    proteinColumnToSelected.put("Experiment", true);
                    proteinColumnToSelected.put("Protein", true);
                    for (String eachColumn: proteinScoreName){
                        proteinColumnToSelected.put(eachColumn, false);
                    }

                    ArrayList<String> proteinShowNames = (ArrayList<String>) Stream.of("Protein", "Gene", "ProteinDescription", "TotalPeptides", "UniquePeptides", "RazorPeptides",
                            "TotalSpectralCount", "UniqueSpectralCount", "RazorSpectralCount").collect(Collectors.toList());

                    for (String eachColumn: proteinShowNames){
                        if (proteinScoreName.contains(eachColumn)){
                            proteinColumnToSelected.put(eachColumn, true);
                        }
                    }

                    ArrayList<String> proteinSortNames = new ArrayList<String>(){{add("Selected");}};
                    proteinSortNames.add("Protein");
                    proteinSortNames.addAll(proteinScoreName);

                    psmSortColumnJCombox.setModel(new DefaultComboBoxModel(psmScoreName.toArray()));
                    proteinSortColumnJCombox.setModel(new DefaultComboBoxModel(proteinSortNames.toArray()));

                    setUpTableHeaderToolTips();

                    updateSpectrumFactoryFirst(progressDialog);
                    updateSpectrumFactory(false, "");

                    displayResults();

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void updateSpectrumFactory(Boolean addNew, String addOneFile){
        if (addNew){
            String spectralFilePath = spectrumFileMap.get(addOneFile);

            if (Files.exists(new File(spectralFilePath).toPath())) {
                try {
                    readSpectrumFile(spectralFilePath);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(
                        null, "Invalid spectrum file path, please check it.",
                        "Loading spectrum file error", JOptionPane.ERROR_MESSAGE);
            }
            finishedSpectrumFiles.add(addOneFile);
        }

        readFactoryThread = new Thread("ImportSpectrum") {
            @Override
            public void run() {
                for (String eachFileName : spectrumFileOrder){
                    if (!finishedSpectrumFiles.contains(eachFileName)){
                        String spectralFilePath = spectrumFileMap.get(eachFileName);
                        if (Files.exists(new File(spectralFilePath).toPath())) {
                            loadingJButton.setText(eachFileName);
//                            try {
//                                Thread.sleep(10000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                            try {
                                readSpectrumFile(spectralFilePath);
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                    null, "Invalid spectrum file path, please check it.",
                                    "Loading spectrum file error", JOptionPane.ERROR_MESSAGE);
                        }
                        finishedSpectrumFiles.add(eachFileName);
                    }
                }
                loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
                loadingJButton.setText("Import done");

            }
        };
        readFactoryThread.start();
    }

    private void updateSpectrumFactoryFirst(ProgressDialogX progressDialog) throws IOException, ClassNotFoundException {
        String spectralFilePath = spectrumFileMap.get(spectrumFileOrder.get(0));

        if (Files.exists(new File(spectralFilePath).toPath())) {
            readSpectrumFile(spectralFilePath);
        } else {
            JOptionPane.showMessageDialog(
                    null, "Invalid spectrum file path, please check it.",
                    "Loading spectrum file error", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
        }
        finishedSpectrumFiles.add(spectrumFileOrder.get(0));
        progressDialog.setRunFinished();
    }

    private void readSpectrumFile(String spectralFilePath) throws IOException, ClassNotFoundException {
        String spectrumName = new File(spectralFilePath).getName().split("\\.")[0];
        if (!spectrumFileTypes.containsKey(spectrumName)){

            if (spectralFilePath.endsWith("mgf")){
                spectrumFactory.addSpectra(new File(spectralFilePath));
                mgfFiles.add(spectralFilePath);
                spectrumFileTypes.put(spectrumName, "mgf");
            } else if (spectralFilePath.endsWith("raw")){
                if (new File(spectralFilePath.replace(".raw", "_calibrated.mgf")).exists()){
                    addOneMzML(spectrumName, spectralFilePath.replace(".raw", "_calibrated.mzml"));
                } else {
                    addOneMzML(spectrumName, spectralFilePath.replace(".raw", "_uncalibrated.mzml"));
                }

            } else if (spectralFilePath.endsWith(".d")){
                if (new File(spectralFilePath.replace(".d", "_calibrated.mgf")).exists()){
                    addOneMzML(spectrumName, spectralFilePath.replace(".d", "_calibrated.mzml"));
                } else {
                    addOneMzML(spectrumName, spectralFilePath.replace(".d", "_uncalibrated.mzml"));
                }

            } else if (spectralFilePath.toLowerCase().endsWith("mzml")){
                addOneMzML(spectrumName, spectralFilePath);

            } else if (spectralFilePath.toLowerCase().endsWith("mzxml")){
                //Will do
            }
        }
    }

    private void addOneMzML(String spectrumName, String spectralFilePath){
        MZMLFile mzmlFile = new MZMLFile(spectralFilePath);
        mzmlFile.setNumThreadsForParsing(threadsNumber);
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
        spectrumFileTypes.put(spectrumName, "mzml");
    }

    public void displayResults() throws SQLException {
        psmSortColumnJCombox.setEnabled(true);
        psmDownSortJButton.setVisible(true);
        psmUpSortJButton.setVisible(true);

        experimentKeyToSelected.put(expNumList.get(0), true);
        experimentTableModel = new ExperimentTableModel(experimentInfo, expNumList, experimentKeyToSelected);
        proteinTableModel = new ProteinTableModel(proteinKeyToSelected, proteinScoreName);
        psmTableModel = new PSMTableModel(spectrumKeyToSelected, psmScoreName, searchParameters);

        experimentsJTable.setModel(experimentTableModel);
        proteinsJTable.setModel(proteinTableModel);
        spectrumJTable.setModel(psmTableModel);

        experimentsJTable.revalidate();
        experimentsJTable.repaint();

        spectrumJTable.getColumn("Selected").setMinWidth(30);
        spectrumJTable.getColumn("Selected").setMaxWidth(50);
        spectrumJTable.getColumn("Key").setMinWidth(0);
        spectrumJTable.getColumn("Key").setMaxWidth(0);
        spectrumJTable.getColumn("MappedProtein").setMinWidth(0);
        spectrumJTable.getColumn("MappedProtein").setMaxWidth(0);

        proteinsJTable.getColumn("Selected").setMinWidth(30);
        proteinsJTable.getColumn("Selected").setMaxWidth(50);
        proteinsJTable.getColumn("Experiment").setMinWidth(50);
        proteinsJTable.getColumn("Experiment").setMaxWidth(100);
        proteinsJTable.getColumn("Protein").setMinWidth(50);
        proteinsJTable.getColumn("Protein").setMaxWidth(300);
        proteinsJTable.getColumn("ProteinDescription").setMinWidth(50);
        proteinsJTable.getColumn("ProteinDescription").setMaxWidth(500);
        proteinsJTable.getColumn("EXP:Protein").setMinWidth(0);
        proteinsJTable.getColumn("EXP:Protein").setMaxWidth(0);

        expAllSelections.add(expNumList.get(0));
        currentExpAllSelections.add(expNumList.get(0));
        updateAllProteinIndexes(sqliteConnection.getProteinList(expAllSelections));

        String initProtein = allProteinIndex.get(selectedProteinPageNum - 1).get(0);
        //System.out.println(initProtein);
        updateAllPSMIndexes(sqliteConnection.getSpectrumListOneProtein(initProtein));
        proteinCurrentSelections.add(initProtein);

        updateCoverage(proteinCurrentSelections);
        updateTable();
        proteinJTableMouseReleased(null);

        for (String key: pSMColumnToSelected.keySet()){
            if(!pSMColumnToSelected.get(key)){
                spectrumJTable.getColumn(key).setMinWidth(0);
                spectrumJTable.getColumn(key).setMaxWidth(0);
            } else {
                for (int i = 0; i < 2; i++) {
                    spectrumJTable.getColumn(key).setPreferredWidth(70);
                    spectrumJTable.getColumn(key).setMinWidth(20);
                    spectrumJTable.getColumn(key).setMaxWidth(400);
                }
            }
        }
        spectrumJTable.revalidate();
        spectrumJTable.repaint();

        for (String key: proteinColumnToSelected.keySet()){
            if(!proteinColumnToSelected.get(key)){
                proteinsJTable.getColumn(key).setMinWidth(0);
                proteinsJTable.getColumn(key).setMaxWidth(0);
            } else {
                for (int i = 0; i < 2; i++) {
                    proteinsJTable.getColumn(key).setPreferredWidth(70);
                    proteinsJTable.getColumn(key).setMinWidth(20);
                    proteinsJTable.getColumn(key).setMaxWidth(400);
                }
            }
        }
        proteinsJTable.revalidate();
        proteinsJTable.repaint();

    }

    private void updateAllProteinIndexes(ArrayList<String> newProteinID){
        allProteinIndex = new ArrayList<>();
        ArrayList<String> each = new ArrayList<>();
        int count = 0;

        for (String proteinID : newProteinID){
            each.add(proteinID);
            if(count == 1000){
                allProteinIndex.add(each);

                each = new ArrayList<>();

                count = 0;
            }
            count ++;
        }

        if(count != 0){
            allProteinIndex.add(each);
        }
    }

    private void updateAllPSMIndexes(ArrayList<String>[] spectrumKeys){
        allSpectrumIndex = new ArrayList<>();
        mappedSpectrumIndex = spectrumKeys[1];
        ArrayList<String> each = new ArrayList<>();
        int count = 0;

        for (String spectrumKey : spectrumKeys[0]){
            each.add(spectrumKey);
            if(count == 1000){
                allSpectrumIndex.add(each);

                each = new ArrayList<>();

                count = 0;
            }
            count ++;
        }
        for (String spectrumKey : spectrumKeys[1]){
            each.add(spectrumKey);
            if(count == 1000){
                allSpectrumIndex.add(each);

                each = new ArrayList<>();

                count = 0;
            }
            count ++;
        }

        if(count != 0){
            allSpectrumIndex.add(each);
        }
        each = null;
        //System.gc();
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
        this.annotationSettings = spectrumMainPanel.getAnnotationSettings();
        return annotationSettings;
    }

    /**
     * Set search parameters
     * @param searchParameters Search parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;

        spectrumMainPanel.updateSearchParameters(searchParameters);

        backgroundPanel.revalidate();
        backgroundPanel.repaint();
    }

    /**
     * Remove target in list
     * @param list ArrayList<Integer>
     * @param target Integer
     * @return ArrayList<Integer>
     */
    private ArrayList<Integer> remove(ArrayList<Integer> list, Integer target){
        list.removeIf(item -> item.equals(target));

        return list;
    }

    /**
     * Remove target in list
     * @param list ArrayList<String>
     * @param target String
     * @return ArrayList<String>
     */
    private ArrayList<String> remove(ArrayList<String> list, String target){
        list.removeIf(item -> item.equals(target));

        return list;
    }

    /**
     * Export selected spectral
     * @param finalImageType Image type
     * @param outputFolder Output folder
     * @param picHeight Picture height
     * @param picWidth Picture width
     * @param unit Length unit
     */
    public void exportSelectedSpectra(ImageType finalImageType, String outputFolder, Integer picHeight, Integer picWidth, String unit){

        Object[] allParameters = spectrumMainPanel.getParameters();
        UtilitiesUserPreferences utilitiesUserPreferences = spectrumMainPanel.utilitiesUserPreferences;

        RealTimeExportJDialog realTimeExportJDialog = new RealTimeExportJDialog((Integer) allParameters[0], (Integer) allParameters[1], picHeight, picWidth, unit,
                (PeptideSpectrumAnnotator) allParameters[2], (SpecificAnnotationSettings) allParameters[3], this, (HashMap<Double, String>)allParameters[4],
                (PtmSettings) allParameters[5], finalImageType, outputFolder, ptmFactory, utilitiesUserPreferences);

        if (exportAll){
            realTimeExportJDialog.readAllSpectrums(allSpectrumIndex);
        } else if (exportSelection){
            realTimeExportJDialog.readAllSelections(pSMAllSelections);
        } else {
            System.err.println("Exporting wrong");
        }
    }

    /**
     * Get current spectrum according to the page num and spectrum key
     * @param selectedSpectrumKey spectrum key
     * @return MSnSpectrum
     */
    public MSnSpectrum getSpectrumRealTime(String selectedSpectrumKey) {
        try {
            SpectrumMatch selectedMatch = sqliteConnection.getSpectrumMatch(selectedSpectrumKey);
            String spectrumTitle = getOneItemFromSQL("SpectrumMatch", "OldTitle", selectedSpectrumKey);

            if (selectedMatch.getBestPeptideAssumption() != null){
                spectrumIdentificationAssumption = selectedMatch.getBestPeptideAssumption();

            } else {
                throw new IllegalArgumentException("No best assumption found for spectrum " + ".");
            }

            MSnSpectrum getmSNSpectrum = getSpectrum(selectedSpectrumKey);
            getmSNSpectrum.setSpectrumTitle(spectrumTitle);
            return getmSNSpectrum;

        } catch (SQLException | FileParsingException | MzMLUnmarshallerException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get current identification assumption
     * @return SpectrumIdentificationAssumption
     */
    public SpectrumIdentificationAssumption getspectrumIdentificationAssumption(){
        return spectrumIdentificationAssumption;
    }

    /**
     * CatchException
     * @param e Exception
     */
    private void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    private void raiseWarningDialog(String message){
        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        message),
                "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public String getOneItemFromSQL(String typeName, String itemName, String idName){
        String[] itemNames = new String[1];
        itemNames[0] = itemName;
        String fetchResult = "";
        try {
            fetchResult = sqliteConnection.getWhatYouWant(typeName, idName, itemNames)[0];
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fetchResult;
    }

}
