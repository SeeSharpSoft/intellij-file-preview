package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;

public class PreviewProjectHandler {

    public static final Key<String> PREVIEW_VIRTUAL_FILE_KEY = Key.create(PreviewProjectHandler.class.getName());

    private Project myProject;

    private final List<JTree> registeredTrees = new ArrayList<>();

    private final KeyListener myTreeKeyListener;

    private final PropertyChangeListener mySettingsPropertyChangeListener = evt -> {
        switch (evt.getPropertyName()) {
            case "ProjectViewToggleOneClick":
                registeredTrees.forEach(tree -> tree.setToggleClickCount((boolean) evt.getNewValue() ? 1 : 2));
                break;
            default:
                // nothing to do yet
                break;
        }
    };

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        PreviewUtil.consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
            VirtualFile gotoFile = PreviewUtil.getGotoFile(myProject, file);
            PreviewUtil.toggleMarkPreviewHandling(gotoFile, theFile -> openOrFocusSelectedFile((Component) treeSelectionEvent.getSource()), null);
        });
    };

    private final MouseListener myTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            switch (mouseEvent.getClickCount()) {
                case 1:
                    // one-click behavior is handled by myTreeSelectionListener
                    break;
                case 2:
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                        PreviewUtil.consumeSelectedFile(mouseEvent.getComponent(), selectedFile -> PreviewUtil.disposePreview(myProject, PreviewUtil.getGotoFile(myProject, selectedFile)));
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            AbstractProjectViewPane currentProjectViewPane = PreviewUtil.getCurrentProjectViewPane(myProject);
            if (currentProjectViewPane == null) {
                return;
            }
            PreviewUtil.consumeSelectedFile(currentProjectViewPane.getTree(), selectedFile -> {
                if (PreviewUtil.isPreviewed(file) ||
                        (selectedFile != null && selectedFile.equals(file) && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW))) {
                    PreviewUtil.closeOtherPreviews(myProject, file);
                }
            });
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            PreviewUtil.cleanupClosedFile(myProject, file);
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (event.getOldFile() != null) {
                PreviewUtil.unmarkPreviewHandling(event.getOldFile());
            }
            VirtualFile gotoFile = PreviewUtil.getGotoFile(myProject, event.getNewFile());
            PreviewUtil.toggleMarkPreviewHandling(gotoFile);
            AbstractProjectViewPane currentProjectViewPane = PreviewUtil.getCurrentProjectViewPane(myProject);
            if (currentProjectViewPane != null && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW) && !PreviewUtil.isPreviewed(gotoFile)) {
                PreviewUtil.consumeSelectedFile(currentProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || (PreviewUtil.isProjectTreeFocused(myProject) && !PreviewUtil.isPreviewed(file))) {
                        PreviewUtil.closeAllPreviews(myProject);
                    }
                });
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) { }

        @Override
        public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            file.putUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING, null);
            if (PreviewUtil.isPreviewed(file)) {
                if (PreviewUtil.isEditorSelected(myProject, file) && PreviewUtil.isProjectTreeFocused(myProject)) {
                    file.putUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING, true);
                }
                PreviewUtil.invokeSafe(myProject, () -> PreviewUtil.disposePreview(myProject, file, false));
            }
        }
    };

    protected PreviewProjectHandler(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProject = project;
        myTreeKeyListener = new PreviewKeyListener(project);

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        // re-set registry entry
        previewSettings.setKeepExpandCollapseState(previewSettings.isKeepExpandCollapseState());
        previewSettings.addPropertyChangeListener(mySettingsPropertyChangeListener);

        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        PreviewUtil.closeAllPreviews(myProject);

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        unregisterAllTreeHandlers();

        myProject = null;
    }

    public void registerTreeHandlers(@NotNull final JTree tree) {
        if (areTreeHandlersRegistered(tree)) {
            return;
        }
        tree.setToggleClickCount(PreviewSettings.getInstance().isProjectViewToggleOneClick() ? 1 : 2);
        tree.addTreeSelectionListener(myTreeSelectionListener);
        tree.addKeyListener(myTreeKeyListener);
        tree.addMouseListener(myTreeMouseListener);
        registeredTrees.add(tree);
    }

    public void unregisterAllTreeHandlers() {
        for (JTree tree : new ArrayList<>(registeredTrees)) {
            unregisterTreeHandlers(tree);
        }
    }

    public void unregisterTreeHandlers(@NotNull final JTree tree) {
        if (!areTreeHandlersRegistered(tree)) {
            return;
        }

        registeredTrees.remove(tree);
        tree.removeTreeSelectionListener(myTreeSelectionListener);
        tree.removeKeyListener(myTreeKeyListener);
        tree.removeMouseListener(myTreeMouseListener);
    }

    public boolean areTreeHandlersRegistered(@NotNull final JTree tree) {
        return registeredTrees.contains(tree);
    }

    protected void focusComponentIfSelectedFileIsNotOpen(final Component component) {
        PreviewUtil.consumeSelectedFile(component, selectedFile -> {
            if (selectedFile == null) {
                return;
            }
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            final VirtualFile gotoFile = PreviewUtil.getGotoFile(myProject, selectedFile);
            if (PreviewUtil.isProjectTreeFocused(myProject) ||
                    (gotoFile != null && !fileEditorManager.isFileOpen(gotoFile))) {
                component.requestFocus();
            }
        });
    }

    public void openOrFocusSelectedFile(final Component component) {
        if (PreviewUtil.isAutoScrollToSource(myProject)) {
            return;
        }
        // - "Open declaration source in the same tab" is focus based (#29) - ensure that component has focus
        // - "Autoscroll from Source" triggers this function as well when switching tabs (#44) - focus shouldn't change
        focusComponentIfSelectedFileIsNotOpen(component);
        PreviewUtil.invokeSafe(myProject, () -> {
            switch (PreviewSettings.getInstance().getPreviewBehavior()) {
                case PREVIEW_BY_DEFAULT:
                    PreviewUtil.openPreviewOrEditor(myProject, component);
                    break;
                case EXPLICIT_PREVIEW:
                    PreviewUtil.consumeSelectedFile(component, file -> {
                        focusFileEditor(PreviewUtil.getGotoFile(myProject, file), false);
                    });
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
            }
        });
    }

    public void focusFileEditor(VirtualFile file, boolean focusEditor) {
        if (!isValid() || file == null) {
            return;
        }
        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(myProject);
        if (!fileEditorManager.isFileOpen(file)) {
            return;
        }
        PreviewUtil.invokeSafe(myProject, () -> fileEditorManager.openFile(file, focusEditor));
    }

    public boolean isValid() {
        return PreviewUtil.isValid(myProject);
    }

    public Project getProject() {
        return myProject;
    }
}
