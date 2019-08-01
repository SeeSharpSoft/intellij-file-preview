package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class PreviewSettingsEditor implements SearchableConfigurable {
    public static final String PREVIEW_SETTINGS_EDITOR_ID = "Preview.Settings.Editor";

    private JCheckBox cbClosePreviewOnEmptySelection;
    private JPanel mainPanel;
    private JCheckBox cbKeyListenerEnabled;
    private JCheckBox cbProjectViewFocusSupport;
    private JComboBox sbPreviewBehavior;

    @NotNull
    @Override
    public String getId() {
        return PREVIEW_SETTINGS_EDITOR_ID;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Quick File Preview";
    }

    @Override
    public String getHelpTopic() {
        return "Edit 'Quick File Preview' Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        return isModified(cbClosePreviewOnEmptySelection, previewSettings.isPreviewClosedOnEmptySelection()) ||
                isModified(cbKeyListenerEnabled, previewSettings.isQuickNavigationKeyListenerEnabled()) ||
                isModified(cbProjectViewFocusSupport, previewSettings.isProjectViewFocusSupport()) ||
                !Objects.equals(sbPreviewBehavior.getSelectedIndex(), previewSettings.getPreviewBehavior().ordinal());
    }

    @Override
    public void reset() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        cbClosePreviewOnEmptySelection.setSelected(previewSettings.isPreviewClosedOnEmptySelection());
        cbKeyListenerEnabled.setSelected(previewSettings.isQuickNavigationKeyListenerEnabled());
        cbProjectViewFocusSupport.setSelected(previewSettings.isProjectViewFocusSupport());
        sbPreviewBehavior.setSelectedIndex(previewSettings.getPreviewBehavior().ordinal());
    }

    @Override
    public void apply() throws ConfigurationException {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.setPreviewClosedOnEmptySelection(cbClosePreviewOnEmptySelection.isSelected());
        previewSettings.setQuickNavigationKeyListenerEnabled(cbKeyListenerEnabled.isSelected());
        previewSettings.setProjectViewFocusSupport(cbProjectViewFocusSupport.isSelected());
        previewSettings.setPreviewBehavior(PreviewSettings.PreviewBehavior.values()[sbPreviewBehavior.getSelectedIndex()]);
    }
}
