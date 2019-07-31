package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PreviewSettingsEditor implements SearchableConfigurable {
    public static final String PREVIEW_SETTINGS_EDITOR_ID = "Preview.Settings.Editor";

    private JCheckBox cbClosePreviewOnEmptySelection;
    private JPanel mainPanel;

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
        return isModified(cbClosePreviewOnEmptySelection, previewSettings.isPreviewClosedOnEmptySelection());
    }

    @Override
    public void reset() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        cbClosePreviewOnEmptySelection.setSelected(previewSettings.isPreviewClosedOnEmptySelection());
    }

    @Override
    public void apply() throws ConfigurationException {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.setPreviewClosedOnEmptySelection(cbClosePreviewOnEmptySelection.isSelected());
    }
}
