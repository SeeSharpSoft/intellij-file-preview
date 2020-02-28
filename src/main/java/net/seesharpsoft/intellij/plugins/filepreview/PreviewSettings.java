package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@State(
        name = "PreviewSettings",
        storages = {@Storage(PreviewSettings.PREVIEW_SETTINGS_STORAGE_FILE)}
)
@SuppressWarnings("all")
public final class PreviewSettings implements PersistentStateComponent<PreviewSettings.OptionSet> {

    public static final String PREVIEW_SETTINGS_STORAGE_FILE = "quick-file-preview.xml";

    public enum PreviewBehavior {
        PREVIEW_BY_DEFAULT,
        EXPLICIT_PREVIEW
    }

    static final class OptionSet {
        public boolean CLOSE_PREVIEW_ON_EMPTY_SELECTION = true;
        public boolean CLOSE_PREVIEW_ON_TAB_CHANGE = false;
        public boolean PROJECT_VIEW_FOCUS_SUPPORT = true;
        public boolean OPEN_EDITOR_ON_EDIT_PREVIEW = true;
        public PreviewBehavior PREVIEW_BEHAVIOR = PreviewBehavior.PREVIEW_BY_DEFAULT;
        public boolean PROJECT_VIEW_TOGGLE_ONE_CLICK = true;
        public String PREVIEW_TAB_TITLE_PATTERN = "<<%s>>";
        public String PREVIEW_TAB_COLOR;
        public boolean KEEP_EXPAND_COLLAPSE_STATE = true;
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

    public boolean isPreviewClosedOnTabChange() {
        return getState().CLOSE_PREVIEW_ON_TAB_CHANGE;
    }

    public void setPreviewClosedOnTabChange(boolean previewClosedOnTabChange) {
        getState().CLOSE_PREVIEW_ON_TAB_CHANGE = previewClosedOnTabChange;
    }

    public boolean isProjectViewFocusSupport() {
        return getState().PROJECT_VIEW_FOCUS_SUPPORT;
    }

    public void setProjectViewFocusSupport(boolean projectViewFocusSupport) {
        getState().PROJECT_VIEW_FOCUS_SUPPORT = projectViewFocusSupport;
    }

    public boolean isOpenEditorOnEditPreview() {
        return getState().OPEN_EDITOR_ON_EDIT_PREVIEW;
    }

    public void setOpenEditorOnEditPreview(boolean openEditorOnEditPreview) {
        getState().OPEN_EDITOR_ON_EDIT_PREVIEW = openEditorOnEditPreview;
    }

    public PreviewBehavior getPreviewBehavior() {
        return getState().PREVIEW_BEHAVIOR;
    }

    public void setPreviewBehavior(PreviewBehavior previewBehavior) {
        getState().PREVIEW_BEHAVIOR = previewBehavior;
    }

    public boolean isProjectViewToggleOneClick() {
        return getState().PROJECT_VIEW_TOGGLE_ONE_CLICK;
    }

    public void setProjectViewToggleOneClick(boolean projectViewToggleOneClick) {
        boolean oldValue = getState().PROJECT_VIEW_TOGGLE_ONE_CLICK;
        getState().PROJECT_VIEW_TOGGLE_ONE_CLICK = projectViewToggleOneClick;
        myPropertyChangeSupport.firePropertyChange("ProjectViewToggleOneClick", oldValue, projectViewToggleOneClick);
    }

    public Color getPreviewTabColor() {
        String color = getState().PREVIEW_TAB_COLOR;
        try {
            return color == null || color.isEmpty() ? null : Color.decode(getState().PREVIEW_TAB_COLOR);
        } catch (NumberFormatException exc) {
            return null;
        }
    }

    public void setPreviewTabColor(Color color) {
        getState().PREVIEW_TAB_COLOR = color == null ? "" : "" + color.getRGB();
    }

    public String getPreviewTabTitlePattern() {
        return getState().PREVIEW_TAB_TITLE_PATTERN;
    }

    public void setPreviewTabTitlePattern(String titlePattern) {
        getState().PREVIEW_TAB_TITLE_PATTERN = titlePattern;
    }

    public boolean isKeepExpandCollapseState() {
        return getState().KEEP_EXPAND_COLLAPSE_STATE;
    }

    public void setKeepExpandCollapseState(boolean keepExpandCollapseState) {
        getState().KEEP_EXPAND_COLLAPSE_STATE = keepExpandCollapseState;
        Registry.get("async.project.view.collapse.tree.path.recursively").setValue(!keepExpandCollapseState);
        Registry.get("ide.tree.collapse.recursively").setValue(!keepExpandCollapseState);
    }
}
