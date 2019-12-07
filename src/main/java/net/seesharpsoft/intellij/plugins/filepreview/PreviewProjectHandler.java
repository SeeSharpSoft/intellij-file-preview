package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
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

import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;

public class PreviewProjectHandler {

    public static final Key<String> PREVIEW_VIRTUAL_FILE_KEY = Key.create(PreviewProjectHandler.class.getName());

    private Project myProject;
    private AbstractProjectViewPane myProjectViewPane;
    private final KeyListener myTreeKeyListener;

    private final PropertyChangeListener mySettingsPropertyChangeListener = evt -> {
        switch (evt.getPropertyName()) {
            case "ProjectViewToggleOneClick":
                myProjectViewPane.getTree().setToggleClickCount((boolean) evt.getNewValue() ? 1 : 2);
                break;
            default:
                // nothing to do yet
                break;
        }
    };

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        openOrFocusSelectedFile((Component) treeSelectionEvent.getSource());
    };

    private final MouseListener myTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            super.mouseClicked(mouseEvent);

            switch (mouseEvent.getClickCount()) {
                case 1:
                    // one-click behavior is handled by myTreeSelectionListener
                    break;
                case 2:
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                        PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> PreviewUtil.disposePreview(myProject, PreviewUtil.getGotoFile(myProject, selectedFile)));
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
            focusProjectViewTreeIfNeeded();
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            focusProjectViewTreeIfNeeded();
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW) && !PreviewUtil.isPreviewed(event.getNewFile())) {
                PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || !PreviewUtil.isPreviewed(file)) {
                        PreviewUtil.closeAllPreviews(myProject);
                    }
                });
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> {
                if (PreviewUtil.isPreviewed(file) ||
                        (selectedFile != null && selectedFile.equals(file) && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW))) {
                    PreviewUtil.closeOtherPreviews(myProject, file);
                }
            });
        }

        @Override
        public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            PreviewUtil.invokeSafe(myProject, () -> PreviewUtil.disposePreview(myProject, file));
        }
    };

    public static final PreviewProjectHandler createIfPossible(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert !project.isDisposed() : "project should not be disposed";

        PreviewProjectHandler previewProjectHandler = new PreviewProjectHandler();
        if (previewProjectHandler.init(project, messageBusConnection)) {
            return previewProjectHandler;
        }
        return null;
    }

    protected PreviewProjectHandler() {
        myTreeKeyListener = new PreviewKeyListener(myProject);
    }

    protected boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (myProjectViewPane == null) {
            return false;
        }

        myProject = project;

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.addPropertyChangeListener(mySettingsPropertyChangeListener);

        myProjectViewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        myProjectViewPane.getTree().addKeyListener(myTreeKeyListener);
        myProjectViewPane.getTree().addMouseListener(myTreeMouseListener);

        // 'false' required to support TAB key in listener - be aware of side effects...
        myProjectViewPane.getTree().setToggleClickCount(previewSettings.isProjectViewToggleOneClick() ? 1 : 2);

        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);

        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        PreviewUtil.closeAllPreviews(myProject);

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        if (myProjectViewPane != null) {
            myProjectViewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
            myProjectViewPane.getTree().removeKeyListener(myTreeKeyListener);
            myProjectViewPane.getTree().removeMouseListener(myTreeMouseListener);
        }

        myProject = null;
    }

    protected boolean shouldProjectViewTreeFocused() {
        ProjectView projectView = ProjectView.getInstance(myProject);
        return PreviewSettings.getInstance().isProjectViewFocusSupport() && !projectView.isAutoscrollFromSource(projectView.getCurrentViewId());
    }

    protected void focusProjectViewTreeIfNeeded() {
        if (shouldProjectViewTreeFocused()) {
            PreviewUtil.invokeSafe(myProject, () -> myProjectViewPane.getTree().grabFocus());
        }
    }

    protected void focusComponentIfSelectedFileIsNotOpen(final Component component) {
        PreviewUtil.consumeSelectedFile(component, selectedFile -> {
            if (selectedFile == null) {
                return;
            }
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            final VirtualFile gotoFile = PreviewUtil.getGotoFile(myProject, selectedFile);
            if (gotoFile != null && !fileEditorManager.isFileOpen(gotoFile)) {
                component.requestFocus();
            }
        });
    }

    public void openOrFocusSelectedFile(final Component component) {
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
}
