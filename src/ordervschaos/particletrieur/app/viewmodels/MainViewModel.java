package ordervschaos.particletrieur.app.viewmodels;

import ordervschaos.particletrieur.app.AbstractDialogController;
import ordervschaos.particletrieur.app.helpers.CSEvent;
import ordervschaos.particletrieur.app.models.Supervisor;
import ordervschaos.particletrieur.app.viewcontrollers.CNNTrainingViewController;
import com.google.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;

public class MainViewModel {

    public CSEvent addImageRequested = new CSEvent();
    public CSEvent removeImageRequested = new CSEvent();
    public CSEvent expandListRequested = new CSEvent();

    public CNNTrainingViewController cnnTrainingViewController;

    private final BooleanProperty operationInProgress = new SimpleBooleanProperty(false);
    public BooleanProperty operationInProgressProperty() { return operationInProgress; }
    public void setOperationInProgress(Boolean value) { operationInProgress.set(value); }
    public Boolean getOperationInProgress() { return operationInProgress.get(); }

    @Inject
    public Supervisor supervisor;

    public MainViewModel() {
        try {
            cnnTrainingViewController = AbstractDialogController.create(CNNTrainingViewController.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
