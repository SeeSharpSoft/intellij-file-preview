package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.CheckBoxWithColorChooser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class PreviewSettingsEditor implements SearchableConfigurable {
    public static final String PREVIEW_SETTINGS_EDITOR_ID = "Preview.Settings.Editor";

    private JPanel mainPanel;
    private JCheckBox cbProjectViewFocusSupport;
    private JCheckBox cbOpenEditorOnEditPreview;
    private JCheckBox cbPreviewClosedOnTabChange;
    private JCheckBox cbProjectViewOneClickToggle;
    private CheckBoxWithColorChooser cpPreviewTabColor;
    private JTextField txtTitlePattern;

    protected void createUIComponents() {
        cpPreviewTabColor = new CheckBoxWithColorChooser("Tab color   ");
    }

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
        Color previewTabColor = previewSettings.getPreviewTabColor();
        return isModified(cbPreviewClosedOnTabChange, previewSettings.isPreviewClosedOnTabChange()) ||
                isModified(cbProjectViewFocusSupport, previewSettings.isProjectViewFocusSupport()) ||
                isModified(cbOpenEditorOnEditPreview, previewSettings.isOpenEditorOnEditPreview()) ||
                isModified(cbProjectViewOneClickToggle, previewSettings.isProjectViewToggleOneClick()) ||
                !Objects.equals(cpPreviewTabColor.isSelected(), previewTabColor != null) ||
                !Objects.equals(cpPreviewTabColor.getColor(), previewTabColor) ||
                !Objects.equals(txtTitlePattern.getText(), previewSettings.getPreviewTabTitlePattern());
    }

    @Override
    public void reset() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        Color previewTabColor = previewSettings.getPreviewTabColor();
        cbPreviewClosedOnTabChange.setSelected(previewSettings.isPreviewClosedOnTabChange());
        cbProjectViewFocusSupport.setSelected(previewSettings.isProjectViewFocusSupport());
        cbOpenEditorOnEditPreview.setSelected(previewSettings.isOpenEditorOnEditPreview());
        cbProjectViewOneClickToggle.setSelected(previewSettings.isProjectViewToggleOneClick());
        cpPreviewTabColor.setColor(previewTabColor);
        cpPreviewTabColor.setSelected(previewTabColor != null);
        txtTitlePattern.setText(previewSettings.getPreviewTabTitlePattern());
    }

    @Override
    public void apply() throws ConfigurationException {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        Color previewTabColor = cpPreviewTabColor.isSelected() ? cpPreviewTabColor.getColor() : null;
        previewSettings.setPreviewClosedOnTabChange(cbPreviewClosedOnTabChange.isSelected());
        previewSettings.setProjectViewFocusSupport(cbProjectViewFocusSupport.isSelected());
        previewSettings.setOpenEditorOnEditPreview(cbOpenEditorOnEditPreview.isSelected());
        previewSettings.setProjectViewToggleOneClick(cbProjectViewOneClickToggle.isSelected());
        previewSettings.setPreviewTabColor(previewTabColor);
        previewSettings.setPreviewTabTitlePattern(txtTitlePattern.getText());
    }

}
