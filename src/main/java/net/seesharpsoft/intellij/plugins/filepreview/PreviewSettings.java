package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@State(
        name = "PreviewSettings",
        storages = {@Storage(PreviewSettings.PREVIEW_SETTINGS_STORAGE_FILE)}
)
@SuppressWarnings("all")
public final class PreviewSettings  implements PersistentStateComponent<PreviewSettings.OptionSet> {

    public static final String PREVIEW_SETTINGS_STORAGE_FILE = "QuickFilePreview.xml";

    static final class OptionSet {
        boolean CLOSE_PREVIEW_ON_EMPTY_SELECTION = false;
        boolean QUICK_NAVIGATION_KEY_LISTENER_ENABLED = false;
    }

    private OptionSet myOptions = new OptionSet();
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    private PreviewSettings() {
    }

    public static PreviewSettings getInstance() {
        return ApplicationManager.getApplication().isDisposed() ? new PreviewSettings() : ServiceManager.getService(PreviewSettings.class);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public OptionSet getState() {
        return this.myOptions;
    }

    @Override
    public void loadState(@NotNull OptionSet state) {
        this.myOptions = state;
    }

    /*********** Settings section **********/

    public boolean isPreviewClosedOnEmptySelection() {
        return getState().CLOSE_PREVIEW_ON_EMPTY_SELECTION;
    }
    public void setPreviewClosedOnEmptySelection(boolean previewClosedOnEmptySelection) {
        getState().CLOSE_PREVIEW_ON_EMPTY_SELECTION = previewClosedOnEmptySelection;
    }

    public boolean isQuickNavigationKeyListenerEnabled() {
        return getState().QUICK_NAVIGATION_KEY_LISTENER_ENABLED;
    }
    public void setQuickNavigationKeyListenerEnabled(boolean quickNavigationKeyListenerEnabled) {
        getState().QUICK_NAVIGATION_KEY_LISTENER_ENABLED = quickNavigationKeyListenerEnabled;
    }
}
