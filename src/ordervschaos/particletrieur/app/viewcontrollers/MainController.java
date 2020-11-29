/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ordervschaos.particletrieur.app.viewcontrollers;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import ordervschaos.particletrieur.app.App;
import ordervschaos.particletrieur.app.FxmlLocation;
import ordervschaos.particletrieur.app.controls.SymbolLabel;
import ordervschaos.particletrieur.app.viewmanagers.UndoManager;
import ordervschaos.particletrieur.app.viewmanagers.commands.UndoableCommand;
import ordervschaos.particletrieur.app.viewmodels.*;
import ordervschaos.particletrieur.app.models.Supervisor;
import ordervschaos.particletrieur.app.AbstractController;
import ordervschaos.particletrieur.app.viewcontrollers.label.LabelListViewController;
import ordervschaos.particletrieur.app.AbstractDialogController;
import ordervschaos.particletrieur.app.viewcontrollers.tag.TagListViewController;
import ordervschaos.particletrieur.app.controls.BasicDialogs;
import ordervschaos.particletrieur.app.models.project.Particle;
import ordervschaos.particletrieur.app.models.project.Project;
import ordervschaos.particletrieur.app.viewmodels.SelectionViewModel;
import com.google.inject.Inject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
//import org.bytedeco.javacpp.Loader;
//import org.bytedeco.opencv.opencv_java;
import org.tensorflow.TensorFlow;

import java.util.stream.Collectors;


/**
 * @author Ross Marchant <ross.g.marchant@gmail.com>
 */
@FxmlLocation("views/Main.fxml")
public class MainController extends AbstractController implements Initializable  {

    public static MainController instance;
    public StackPane rootDialog;
    public VBox rootVBox;
    @FXML CheckBox checkBoxAutoAdvance;
    @FXML CheckBox checkBoxAutoValidate;


    public Pane getRootPane() { return root; }
    public Window getWindow() { return root.getScene().getWindow(); }

    @FXML
    Menu menuRecent;
    @FXML
    Label labelServer;
    @FXML
    Label labelFolderWatch;
    @FXML
    MenuItem menuItemFolderWatch;
    @FXML
    MenuItem menuItemExceptionLog;
    @FXML
    MenuItem menuItemServer;
    @FXML
    MenuItem menuItemUndo;
    @FXML
    SymbolLabel symbolLabelWorking;
    //Display (?)
    @FXML
    StackPane root;
    @FXML
    AnchorPane rootMain;
    @FXML
    StackPane rootLoading;
    @FXML
    GridPane gridPaneMain;

    //Menu bar
    @FXML
    MenuBar mainMenuBar;
    @FXML
    MenuItem menuItemCalculateCNNVector;
    @FXML
    MenuItem menuItemRemoveByClass;
    @FXML
    TextField textFieldUser;

    //Menu buttons
    @FXML
    Button buttonNew;
    @FXML
    Button buttonOpen;
    @FXML
    Button buttonSave;
    @FXML
    Button buttonExport;
    @FXML
    Button buttonFolderWatch;
    @FXML
    Button buttonServer;

    //Main Screen
    @FXML
    SplitPane splitPaneMain;
    @FXML
    ClassificationPaneController classificationPaneController;
    @FXML
    ParticleListViewController particleListViewController;
    @FXML
    ProcessingPaneController processingPaneController;
    @FXML
    SimilarityViewController similarityViewController;
    @FXML
    Tab tabMorphology;
    @FXML
    Tab tabSimilar;

    @Inject
    Supervisor supervisor;
    @Inject
    MainViewModel mainViewModel;
    @Inject
    SelectionViewModel selectionViewModel;
    @Inject
    ParticlesViewModel particlesViewModel;
    @Inject
    LabelsViewModel labelsViewModel;
    @Inject
    CNNVectorViewModel cnnVectorViewModel;
    @Inject
    ExportViewModel exportViewModel;
    @Inject
    PredictionViewModel predictionViewModel;
    @Inject
    ProjectRepositoryViewModel projectRepositoryViewModel;
    @Inject
    FindDuplicatesViewModel findDuplicatesViewModel;
    @Inject
    FolderWatchViewModel folderWatchViewModel;
    @Inject
    UndoManager undoManager;
    @Inject
    StatisticsChartsViewModel statisticsChartsViewModel;

    //Helpers
    double splitPaneMainCurrentDivision;
    int startupMode = 0;
    Object startupParams = null;

    RotateTransition rt;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        instance = this;

//        mainMenuBar.setUseSystemMenuBar(true);



        //Init OpenCV
        nu.pattern.OpenCV.loadShared();
//        Loader.load(opencv_java.class);
//        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);

        //Tensorflow is already initialised
        System.out.println(String.format("Tensorflow version: %s", TensorFlow.version()));
        setupBindings();

        rt = new RotateTransition(Duration.millis(3000), symbolLabelWorking);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(Animation.INDEFINITE);
        symbolLabelWorking.setCache(true);
        symbolLabelWorking.setCacheHint(CacheHint.SPEED);
        updateRecentMenu();

        menuRecent.setOnShowing(p -> {
            updateRecentMenu();
        });
    }

    private void updateRecentMenu() {
        List<String> recents = App.getPrefs().getRecentProjects();
        menuRecent.getItems().clear();
        for(String recent : recents) {
            if (!recent.equals("")) {
                MenuItem item = new MenuItem(recent);
                item.setOnAction(event -> {
                    projectRepositoryViewModel.openProject(recent);
                });
                menuRecent.getItems().add(item);
            }
        }
        if (menuRecent.getItems().size() == 0) {
            MenuItem noItems = new MenuItem("No recent projects");
            noItems.setDisable(true);
            menuRecent.getItems().add(noItems);
        }
        else {
            menuRecent.getItems().add(new SeparatorMenuItem());
            MenuItem clearAll = new MenuItem("Clear recent projects");
            clearAll.setOnAction(event -> {
                App.getPrefs().setRecentProjects(new ArrayList<>());
            });
            menuRecent.getItems().add(clearAll);
        }
    }

    /**
     * Called on startup to select the first action to perform
     * @param mode
     */
    public void setStartupMode(int mode) {
        startupMode = mode;
    }

    public void setStartupParams(Object params) {
        startupParams = params;
    }

    public void startup() {
        switch (startupMode) {
            case 0:
                projectRepositoryViewModel.newProject();
                break;

            case 1:
                projectRepositoryViewModel.newProjectFromExisting();
                break;

            case 2:
                projectRepositoryViewModel.openProject();
                break;

            case 3:
                projectRepositoryViewModel.openProject((String) startupParams);
                break;
        }
    }

    /**
     * Check if the project needs saving, if so display the save dialog, if not clean up and exit
     * @param stage
     */
    public void setupStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (!projectRepositoryViewModel.showIfProjectNeedsSave("Choose whether to save before exiting")) {
                event.consume();
            } else {
                //Make sure the server and folder watch services have stopped
                supervisor.classificationServer.stop();
                supervisor.folderWatch.stop();
                //Save preferences
//                App.getPrefs().setUsername(supervisor.getUsername());
                App.getPrefs().save();
                App.getExecutorService().shutdown();
            }
        });
        this.stage.setTitle("Particle Trieur " + App.VERSION);
    }

    /**
     * Setup up the bindings between the Main form and instance values of the controller
     */
    public void setupBindings() {

        //Window Title
        supervisor.project.fileProperty().addListener((observable, oldValue, newValue) -> {
            this.stage.setTitle("Particle Trieur " + App.VERSION + " - "  + newValue);
        });

        //Username
        textFieldUser.textProperty().bindBidirectional(supervisor.usernameProperty());

        //Validation
        checkBoxAutoAdvance.selectedProperty().bindBidirectional(labelsViewModel.autoAdvanceProperty());
        checkBoxAutoValidate.selectedProperty().bindBidirectional(labelsViewModel.autoValidateProperty());

        //Events
        mainViewModel.expandListRequested.addListener(val -> expandList());
        mainViewModel.addImageRequested.addListener(val -> {
            handleAddWithOptions(null);
        });
        mainViewModel.removeImageRequested.addListener(val -> {
            handleRemove(null);
        });

        //Disable the similarity view when not shown - prevents unnecessary calculation of similarity values
        //TODO combine with knn prediction?
        tabSimilar.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            System.out.println("tab similar selected? " + newValue);
            similarityViewController.isEnabledProperty.set(newValue);
        });

        //Disable the main window and add a spinner if there is an operation in progress
        mainViewModel.operationInProgressProperty().addListener((obs, oldv, newv) -> {
            //System.out.println(newv);
//            rootMain.setDisable(newv);
            rootLoading.setVisible(newv);
            if (newv) {
                rootMain.setEffect(new GaussianBlur(10));
                rt.play();
            }
            else {
                rootMain.setEffect(null);
                rt.stop();
            }
        });

        //Particle changed
        //TODO check for missing file when project is loaded
        //Maybe make into another view / manager
//        selectionViewModel.currentParticleProperty().addListener((obs, oldv, newv) -> {
//            if (newv != null) {
//                if (newv.getFile() != null && newv.getFile().exists()) {
//
//                }
//                else {
////                    BasicDialogs.ShowError("File missing",
////                            "The image file could not be found.\n\nThe locatation was supposed to be "
////                                    + newv.getFilename()
////                                    + ".\nThe project XML file might have been moved, or the image deleted.");
//                }
//            }
//        });

        //TODO add back in these indicators!
        //Server?
        supervisor.classificationServer.isRunningProperty().addListener((obs, oldv, newv) -> {
            if (newv) {
                labelServer.setVisible(true);
            } else {
                labelServer.setVisible(false);
            }
        });
        labelServer.setVisible(false);

        //Network?
        supervisor.network.enabledProperty().addListener((obs, oldv, newv) -> {
            if (newv == true) {

            }
            else {

            }
        });

        //Folder watch?
        supervisor.folderWatch.enabledProperty().addListener((obs, oldv, newv) -> {
            //System.out.println("folder watch " + newv);
            if (newv) {
                menuItemFolderWatch.setText("Stop folder watch");
                labelFolderWatch.setVisible(true);
            } else {
                menuItemFolderWatch.setText("Start folder watch");
                labelFolderWatch.setVisible(false);
            }
        });
        labelFolderWatch.setVisible(false);

        //Change colour of the save button depending on if the project has changed
        supervisor.project.isDirtyProperty().addListener((obs,oldv,newv) -> {
            //if (buttonSave.isDisabled()) return;
            buttonSave.getStyleClass().remove("red-button");
            if (newv) {
                buttonSave.getStyleClass().add("red-button");
            }
        });

        //Undo stack
        undoManager.stackUpdated.addListener(value -> {
            if (value > 0) {
                UndoableCommand command = undoManager.commands.peek();
                menuItemUndo.setDisable(false);
                menuItemUndo.setText("Undo " + command.name);
            }
            else {
                menuItemUndo.setText("Undo");
                menuItemUndo.setDisable(true);
            }
        });

        supervisor.exceptionMonitor.errorCountProperty().addListener((observable, oldValue, newValue) -> {
            menuItemExceptionLog.setText(String.format("Exception Log (%d)", newValue));
        });
    }

    /**
     * Setup the keyboard accelerators
     */
    public void setupAccelerators() {
        Scene scene = root.getScene();
        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent ke) -> {
            //if (ke.getCode() == KeyCode.LEFT) { previousForam(); ke.consume(); }
            // if (ke.getCode() == KeyCode.RIGHT) { nextForam(); ke.consume(); }
            if (ke.getCode() == KeyCode.UP) {
                particleListViewController.selectPrevious();
                ke.consume();
            } else if (ke.getCode() == KeyCode.DOWN) {
                particleListViewController.selectNext();
                ke.consume();
            }
            else if (ke.getCode() == KeyCode.DIGIT1 || ke.getCode() == KeyCode.NUMPAD1) {
                setClassFromKeyPress(0);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT2 || ke.getCode() == KeyCode.NUMPAD2) {
                setClassFromKeyPress(1);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT3 || ke.getCode() == KeyCode.NUMPAD3) {
                setClassFromKeyPress(2);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT4 || ke.getCode() == KeyCode.NUMPAD4) {
                setClassFromKeyPress(3);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT5 || ke.getCode() == KeyCode.NUMPAD5) {
                setClassFromKeyPress(4);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT6 || ke.getCode() == KeyCode.NUMPAD6) {
                setClassFromKeyPress(5);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT7 || ke.getCode() == KeyCode.NUMPAD7) {
                setClassFromKeyPress(6);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT8 || ke.getCode() == KeyCode.NUMPAD8) {
                setClassFromKeyPress(7);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT9 || ke.getCode() == KeyCode.NUMPAD9) {
                setClassFromKeyPress(8);
                ke.consume();
            } else if (ke.getCode() == KeyCode.DIGIT0 || ke.getCode() == KeyCode.NUMPAD0) {
                setClassFromKeyPress(9);
                ke.consume();
            }
        });
    }

    public void setClassFromKeyPress(int index) {
        if (index < classificationPaneController.taxonCodes.size()) {
            labelsViewModel.setLabel(classificationPaneController.taxonCodes.get(index), 1.0, true);
        }
    }

    /**
     * Start / stop server event
     * @param event
     */
    @FXML
    private void handleServer(ActionEvent event) {
        if (supervisor.classificationServer.getIsRunning()) {
            supervisor.classificationServer.stop();
        } else {
            try {
                supervisor.classificationServer.start(3333);
            } catch (IOException e) {
                BasicDialogs.ShowException("Error starting server", e);
            }
        }
    }


    /**
     * Add particles
     * @param event
     */
    @FXML
    private void handleAddWithOptions(ActionEvent event) {
        particlesViewModel.addParticles();
    }

    /**
     * Remove particles
     * @param event
     */
    @FXML
    private void handleRemove(ActionEvent event) {
        particlesViewModel.removeParticles(selectionViewModel.getCurrentParticles());
    }

    /**
     * Tag duplicates by feature vector
     * @param event
     */
    @FXML
    private void handleTagDuplicatesByCNNVector(ActionEvent event) {
        if (selectionViewModel.getCurrentParticles().size() > 1) {
            findDuplicatesViewModel.tagDuplicatesUsingCNNVector(selectionViewModel.getCurrentParticles(), true);
        } else {
            findDuplicatesViewModel.tagDuplicatesUsingCNNVector(selectionViewModel.getCurrentParticles(), false);
        }
    }

    /**
     * Tag duplicates by file hash
     * @param event
     */
    @FXML
    private void handleTagDuplicatesUsingFileHash(ActionEvent event) {
        if (selectionViewModel.getCurrentParticles().size() > 1) {
            findDuplicatesViewModel.tagDuplicatesUsingFilename(selectionViewModel.getCurrentParticles(), true);
        } else {
            findDuplicatesViewModel.tagDuplicatesUsingFilename(selectionViewModel.getCurrentParticles(), false);
        }
    }

    /**
     * Tag duplicates by file hash
     * @param event
     */
    @FXML
    private void handleTagMissing(ActionEvent event) {
        findDuplicatesViewModel.tagMissing(supervisor.project.getParticles());
    }

    /**
     * Expand / contract the forams list
     */
    //TODO remove as not needed
    private void expandList() {
        if (splitPaneMain.getDividerPositions()[0] > 0.9) {
            splitPaneMain.setDividerPositions(splitPaneMainCurrentDivision);
            particleListViewController.setExpanded(false);
        } else {
            splitPaneMainCurrentDivision = splitPaneMain.getDividerPositions()[0];
            splitPaneMain.setDividerPositions(1.0);
            particleListViewController.setExpanded(true);
        }
    }

    /**
     * Edit selected forams
     * @param event
     */
    @FXML
    private void handleEditForam(ActionEvent event) {
        particlesViewModel.editParticlesMetadata();
    }

    /**
     * Open a project
     * @param event
     */
    @FXML
    private void handleOpenProject(ActionEvent event) {
        projectRepositoryViewModel.openProject();
    }

    /**
     * Create new project with settings from previous
     * @param event
     */
    @FXML
    private void handleNewFromExistingProject(ActionEvent event) {
        projectRepositoryViewModel.newProjectFromExisting();
    }

    /**
     * Create new project
     * @param event
     */
    @FXML
    private void handleNewProject(ActionEvent event) {
        projectRepositoryViewModel.newProject();
    }

    /**
     * Save current project
     * @param event
     */
    @FXML
    private void handleSaveProject(ActionEvent event) {
        projectRepositoryViewModel.saveProject();
    }

    @FXML
    private void handleShowFolder(ActionEvent event) {
        projectRepositoryViewModel.showProject();
    }



    /**
     * Save project as a new file
     * @param event
     */
    @FXML
    private void handleSaveAsProject(ActionEvent event) {
        projectRepositoryViewModel.saveAsProject();
    }

    @FXML
    private void handleStartSegmenter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("other/segmenter/Segmenter.fxml"));
            Parent rooty = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(rooty);
            stage.setScene(scene);
            stage.initOwner(root.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.centerOnScreen();
            stage.showAndWait();
        } catch (IOException ex) {
            BasicDialogs.ShowException("The segmenter program could not be started.", ex);
        }
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        try {
            AbstractDialogController.create(AboutViewController.class).showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFolderWatch(ActionEvent event) {
        folderWatchViewModel.toggleFolderWatch();
    }

    @FXML
    private void handleExport(ActionEvent event) {
        exportViewModel.exportProjectImages();
    }

    @FXML
    private void handleExportMorphologyToCSV(ActionEvent event) {
        exportViewModel.exportProjectData();
    }

    @FXML
    private void handleExportAbundanceToCSV(ActionEvent event) {
        exportViewModel.exportAbundance();
    }

    @FXML
    private void handleExportSampleCountsToCSV(ActionEvent event) {
        exportViewModel.exportSampleCounts();
    }

    @FXML
    private void handleImportFromProject(ActionEvent event) {
        projectRepositoryViewModel.importFromProject();
    }

    @FXML
    private void handleAddLabel(ActionEvent event) {
        try {
            LabelListViewController controller = AbstractController.create(LabelListViewController.class);
            controller.showAndWait(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTag(ActionEvent event) {
        try {
            TagListViewController controller = AbstractController.create(TagListViewController.class);
            controller.showAndWait(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePredictUsingCNN(ActionEvent event) {
        predictionViewModel.predictUsingCNN(selectionViewModel.getCurrentParticles(), supervisor.project.processingInfo.getProcessBeforeClassification());
    }

    @FXML
    private void handlePredictUsingCNNAll(ActionEvent event) {
        predictionViewModel.predictUsingCNN(supervisor.project.getParticles(), supervisor.project.processingInfo.getProcessBeforeClassification());
    }

    //TODO move into viewmodel
    @FXML
    private void handlePredictUsingCNNUnlabeled(ActionEvent event) {
        List<Particle> unlabeledParticles = supervisor.project.getParticles()
                .stream()
                .filter(foram -> foram.classification.get().equalsIgnoreCase(Project.UNLABELED_CODE))
                .collect(Collectors.toList());
        predictionViewModel.predictUsingCNN(unlabeledParticles, supervisor.project.processingInfo.getProcessBeforeClassification());
    }

    @FXML
    private void handlePredictUsingKNN(ActionEvent event) {
        predictionViewModel.predictUsingkNN(selectionViewModel.getCurrentParticles());
    }

    @FXML
    private void handlePredictUsingKNNAll(ActionEvent event) {
        predictionViewModel.predictUsingkNN(supervisor.project.getParticles());
    }

    //TODO move into viewmodel
    @FXML
    private void handlePredictUsingKNNUnlabeled(ActionEvent event) {
        List<Particle> unlabeledParticles = supervisor.project.getParticles()
                .stream()
                .filter(foram -> foram.classification.get().equalsIgnoreCase(Project.UNLABELED_CODE))
                .collect(Collectors.toList());
        predictionViewModel.predictUsingkNN(unlabeledParticles);
    }

    @FXML
    private void handleFlowcamSegmenter(ActionEvent event) {
        try {
            AbstractDialogController.create(FlowcamSegmenterViewController.class).showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCalculateCNNVectors(ActionEvent event) {
        cnnVectorViewModel.calculateVectors();
    }

    @FXML
    private void handleStatsLabelCount(ActionEvent event) {
        statisticsChartsViewModel.showLabelCounts();
    }

    @FXML
    private void handleStatsSampleCount(ActionEvent event) {
        statisticsChartsViewModel.showSampleCounts();
    }

    @FXML
    private void handleStatsIndex1Frequency(ActionEvent event) {
        statisticsChartsViewModel.showRelativeAbundance(1);
    }

    @FXML
    private void handleStatsIndex2Frequency(ActionEvent event) {
        statisticsChartsViewModel.showRelativeAbundance(2);
    }

    @FXML
    private void handleStatsSampleFrequency(ActionEvent event) {
        statisticsChartsViewModel.showRelativeAbundanceWithCoreID();
    }

    @FXML
    private void handleUndo(ActionEvent event) {
        undoManager.undo();
    }

    @FXML
    private void handleTrain(ActionEvent event) {
        mainViewModel.cnnTrainingViewController.showAndWait();
    }

    @FXML
    private void handleShowExceptionLog(ActionEvent event) {
        BasicDialogs.ShowExceptionLog(supervisor.exceptionMonitor.getLog());
    }

    @FXML
    private void handleStartStopServer(ActionEvent event) {
        if (!supervisor.classificationServer.getIsRunning()) {
            try {
                supervisor.classificationServer.start(5555);
                if (supervisor.classificationServer.getIsRunning()) {
                    BasicDialogs.ShowInfo("Server", "Server started on http://localhost:5555");
                    menuItemServer.setText("Stop server");
                }
                else {
                    BasicDialogs.ShowError("Server", "Server could not be started");
                }
            } catch (IOException e) {
                BasicDialogs.ShowException("Error starting server", e);
            }
        }
        else {
            supervisor.classificationServer.stop();
            if (supervisor.classificationServer.getIsRunning()) {
                BasicDialogs.ShowError("Server", "Server could not be stopped");
            }
            else {
                BasicDialogs.ShowInfo("Server", "Server stopped");
                menuItemServer.setText("Start server");
            }
        }
    }
}
