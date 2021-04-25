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

    private JCheckBox cbClosePreviewOnEmptySelection;
    private JPanel mainPanel;
    private JComboBox sbPreviewBehavior;
    private JCheckBox cbOpenEditorOnEditPreview;
    private JCheckBox cbPreviewClosedOnTabChange;
    private JCheckBox cbProjectViewOneClickToggle;
    private CheckBoxWithColorChooser cpPreviewTabColor;
    private JTextField txtTitlePattern;
    private JCheckBox cbKeepExpandCollapseState;
    private JTextField txtFileSizeLimit;
    private JCheckBox cbPreviewOnlyKnownFileTypes;

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
        return isModified(cbClosePreviewOnEmptySelection, previewSettings.isPreviewClosedOnEmptySelection()) ||
                isModified(cbPreviewClosedOnTabChange, previewSettings.isPreviewClosedOnTabChange()) ||
                isModified(cbOpenEditorOnEditPreview, previewSettings.isOpenEditorOnEditPreview()) ||
                !Objects.equals(sbPreviewBehavior.getSelectedIndex(), previewSettings.getPreviewBehavior().ordinal()) ||
                isModified(cbProjectViewOneClickToggle, previewSettings.isProjectViewToggleOneClick()) ||
                !Objects.equals(cpPreviewTabColor.isSelected(), previewTabColor != null) ||
                !Objects.equals(cpPreviewTabColor.getColor(), previewTabColor) ||
                !Objects.equals(txtTitlePattern.getText(), previewSettings.getPreviewTabTitlePattern()) ||
                isModified(cbKeepExpandCollapseState, previewSettings.isKeepExpandCollapseState()) ||
                getFileSizeLimitKB() != previewSettings.getFileSizeLimitKB() ||
                isModified(cbPreviewOnlyKnownFileTypes, previewSettings.isPreviewOnlyKnownFileTypes());
    }

    @Override
    public void reset() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        Color previewTabColor = previewSettings.getPreviewTabColor();
        cbClosePreviewOnEmptySelection.setSelected(previewSettings.isPreviewClosedOnEmptySelection());
        cbPreviewClosedOnTabChange.setSelected(previewSettings.isPreviewClosedOnTabChange());
        cbOpenEditorOnEditPreview.setSelected(previewSettings.isOpenEditorOnEditPreview());
        sbPreviewBehavior.setSelectedIndex(previewSettings.getPreviewBehavior().ordinal());
        cbProjectViewOneClickToggle.setSelected(previewSettings.isProjectViewToggleOneClick());
        cpPreviewTabColor.setColor(previewTabColor);
        cpPreviewTabColor.setSelected(previewTabColor != null);
        txtTitlePattern.setText(previewSettings.getPreviewTabTitlePattern());
        cbKeepExpandCollapseState.setSelected(previewSettings.isKeepExpandCollapseState());
        txtFileSizeLimit.setText(previewSettings.getFileSizeLimitKB().toString());
        cbPreviewOnlyKnownFileTypes.setSelected(previewSettings.isPreviewOnlyKnownFileTypes());
    }

    @Override
    public void apply() throws ConfigurationException {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        Color previewTabColor = cpPreviewTabColor.isSelected() ? cpPreviewTabColor.getColor() : null;
        previewSettings.setPreviewClosedOnEmptySelection(cbClosePreviewOnEmptySelection.isSelected());
        previewSettings.setPreviewClosedOnTabChange(cbPreviewClosedOnTabChange.isSelected());
        previewSettings.setOpenEditorOnEditPreview(cbOpenEditorOnEditPreview.isSelected());
        previewSettings.setPreviewBehavior(PreviewSettings.PreviewBehavior.values()[sbPreviewBehavior.getSelectedIndex()]);
        previewSettings.setProjectViewToggleOneClick(cbProjectViewOneClickToggle.isSelected());
        previewSettings.setPreviewTabColor(previewTabColor);
        previewSettings.setPreviewTabTitlePattern(txtTitlePattern.getText());
        previewSettings.setKeepExpandCollapseState(cbKeepExpandCollapseState.isSelected());
        previewSettings.setFileSizeLimitKB(getFileSizeLimitKB());
        previewSettings.setPreviewOnlyKnownFileTypes(cbPreviewOnlyKnownFileTypes.isSelected());
    }

    private int getFileSizeLimitKB() {
        return Integer.parseInt(txtFileSizeLimit.getText());
    }

}
