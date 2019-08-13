package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import net.seesharpsoft.intellij.editor.EditorSnapshot;
import net.seesharpsoft.intellij.editor.EditorSnapshotFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;

public class PreviewProjectHandler {

    private Project myProject;
    private PreviewVirtualFile myPreviewFile;
    private AbstractProjectViewPane myProjectViewPane;
    private final AnAction myReopenClosedTabAction;
    private final KeyListener myKeyListener;

    private final DataContext myComponentDataContext = dataId -> {
        if (!PlatformDataKeys.CONTEXT_COMPONENT.getName().equals(dataId)) {
            throw new UnsupportedOperationException(dataId);
        }
        return FileEditorManagerEx.getInstanceEx(myProject).getSplitters();
    };

    private final PropertyChangeListener mySettingsPropertyChangeListener = evt -> {
        switch (evt.getPropertyName()) {
            case "ProjectViewToggleOneClick":
                myProjectViewPane.getTree().setToggleClickCount((boolean)evt.getNewValue() ? 1 : 2);
                break;
            case "QuickNavigationKeyListenerEnabled":
                myProjectViewPane.getTree().setFocusTraversalKeysEnabled(!(boolean)evt.getNewValue());
                break;
            default:
                // nothing to do yet
                break;
        }
    };

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        switch (PreviewSettings.getInstance().getPreviewBehavior()) {
            case PREVIEW_BY_DEFAULT:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    openPreviewOrEditor(file);
                });
                break;
            case EXPLICIT_PREVIEW:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    focusFileEditor(file, false);
                });
                break;
            default:
                throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (PreviewSettings.getInstance().isProjectViewFocusSupport() && file instanceof PreviewVirtualFile) {
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (file instanceof PreviewVirtualFile) {
                onAfterPreviewFileClosed((PreviewVirtualFile) file);
            }

            if (PreviewSettings.getInstance().isProjectViewFocusSupport()) {
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW) && !(event.getNewProvider() instanceof PreviewEditorProvider)) {
                consumeSelectedFile(myProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || !isCurrentlyPreviewed(file)) {
                        closePreview();
                    }
                });
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (!(file instanceof PreviewVirtualFile) || !file.equals(myPreviewFile)) {
                consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> {
                    if ((selectedFile != null && selectedFile.equals(file) && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW)) ||
                            ((file instanceof PreviewVirtualFile) && !isCurrentlyPreviewed(file)) ||
                            (!(file instanceof PreviewVirtualFile) && isCurrentlyPreviewed(file))) {
                        closePreview();
                    }

                    if (file instanceof PreviewVirtualFile) {
                        onBeforePreviewFileOpened((PreviewVirtualFile) file);
                    }
                });
            }
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

    public PreviewProjectHandler() {
        myKeyListener = new PreviewKeyListener(this);
        myReopenClosedTabAction = ActionManager.getInstance().getAction("ReopenClosedTab");
    }

    public boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (myProjectViewPane == null) {
            return false;
        }

        myProject = project;

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.addPropertyChangeListener(mySettingsPropertyChangeListener);

        myProjectViewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        myProjectViewPane.getTree().addKeyListener(myKeyListener);
        // 'false' required to support TAB key in listener - be aware of side effects...
        myProjectViewPane.getTree().setFocusTraversalKeysEnabled(!previewSettings.isQuickNavigationKeyListenerEnabled());
        myProjectViewPane.getTree().setToggleClickCount(previewSettings.isProjectViewToggleOneClick() ? 1 : 2);

        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);

        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        closePreview();

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        if (myProjectViewPane != null) {
            myProjectViewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
            myProjectViewPane.getTree().removeKeyListener(myKeyListener);
        }

        myProject = null;
    }

    public void openPreviewOrEditor(VirtualFile file) {
        if (!isValid() || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                closePreview();
            }
            return;
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final VirtualFile fileToOpen = !fileEditorManager.isFileOpen(file) ? createAndSetPreviewFile(file) : file;
        if (fileToOpen != null) {
            invokeSafe(() -> fileEditorManager.openFile(fileToOpen, false));
        }
    }

    public boolean isCurrentlyPreviewed(VirtualFile file) {
        if (!isValid() || file == null || myPreviewFile == null) {
            return false;
        }
        return myPreviewFile.equals(file) || myPreviewFile.getSource().equals(file);
    }

    public void openFileEditor(final VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        invokeSafe(() -> openFileEditorWithPreviewSnapshot(file));
    }

    private void openFileEditorWithPreviewSnapshot(final VirtualFile file) {
        final VirtualFile fileToOpen = file instanceof PreviewVirtualFile ? ((PreviewVirtualFile) file).getSource() : file;
        final List<EditorSnapshot> editorSnapshots = getEditorsSnapshots(file);
        if (isCurrentlyPreviewed(file)) {
            closePreview();
        }
        invokeSafe(() -> openFileEditorWithPreviewSnapshot(fileToOpen, editorSnapshots));
    }

    private void openFileEditorWithPreviewSnapshot(final VirtualFile file, final List<EditorSnapshot> editorSnapshots) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final FileEditor[] targetFileEditors = fileEditorManager.openFile(file, true);
        for (FileEditor fileEditor : targetFileEditors) {
            EditorSnapshot editorSnapshot = editorSnapshots.stream()
                    .filter(snapshot -> snapshot.accepts(fileEditor))
                    .findFirst().orElse(null);
            if (editorSnapshot != null) {
                editorSnapshot.apply(fileEditor);
            }
        }
    }

    @NotNull
    private List<EditorSnapshot> getEditorsSnapshots(final VirtualFile file) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final FileEditor[] sourceFileEditors = fileEditorManager.getEditors(file);
        final List<EditorSnapshot> editorSnapshots = new ArrayList<>();
        for (FileEditor fileEditor : sourceFileEditors) {
            EditorSnapshot editorSnapshot = EditorSnapshotFactory.getInstance().create(fileEditor);
            if (editorSnapshot != null) {
                editorSnapshots.add(editorSnapshot);
            }
        }
        return editorSnapshots;
    }

    public void consumeSelectedFile(final Component tree, Consumer<VirtualFile> consumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        getReady(dataContext).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            DataContext context = DataManager.getInstance().getDataContext(tree);
            final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
            consumer.accept(file);
        }));
    }

    private ActionCallback getReady(DataContext context) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(this) : ActionCallback.DONE;
    }

    public void closeCurrentFileEditor() {
        if (!isValid()) {
            return;
        }

        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        closeFileEditor(fileEditorManager.getCurrentFile());
    }

    public void closeFileEditor(VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            closePreview();
        } else {
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
            invokeSafe(() -> fileEditorManager.closeFile(file, false, true));
        }
    }

    public void closePreview() {
        if (!isValid() || myPreviewFile == null) {
            return;
        }

        final PreviewVirtualFile closingPreviewFile = myPreviewFile;
        myPreviewFile = null;
        disposePreviewFile(closingPreviewFile);
        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        invokeSafeAndWait(() -> fileEditorManager.closeFile(closingPreviewFile, false, true));
    }

    public void focusFileEditor(VirtualFile file, boolean focusEditor) {
        if (!isValid() || file == null) {
            return;
        }
        final VirtualFile fileToFocus = myPreviewFile != null && myPreviewFile.getSource().equals(file) ? myPreviewFile : file;
        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        if (fileToFocus == null || !fileEditorManager.isFileOpen(fileToFocus)) {
            return;
        }
        invokeSafe(() -> fileEditorManager.openFile(fileToFocus, focusEditor));
    }

    public VirtualFile createAndSetPreviewFile(VirtualFile file) {
        if (!isValid()) {
            return null;
        }

        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            return myPreviewFile;
        }
        return new PreviewVirtualFile(file);
    }

    public void invokeSafeAndWait(Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (isValid()) {
                runnable.run();
            }
        });
    }

    public void invokeSafe(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isValid()) {
                runnable.run();
            }
        });
    }

    public boolean isValid() {
        return myProject != null && !myProject.isDisposed();
    }

    protected void disposePreviewFile(PreviewVirtualFile previewFile) {
        PreviewFileListener listener = previewFile.getUserData(PreviewFileListener.PREVIEW_FILE_LISTENER);

        if (listener == null) {
            return;
        }

        FileDocumentManager.getInstance().getDocument(previewFile.getSource()).removeDocumentListener(listener);
    }

    protected void onBeforePreviewFileOpened(PreviewVirtualFile previewFile) {
        myPreviewFile = previewFile;
        if (!PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            return;
        }

        invokeSafe(() -> {
            Document document = FileDocumentManager.getInstance().getDocument(previewFile.getSource());
            if (document != null) {
                PreviewFileListener listener = new PreviewFileListener(this, previewFile);
                previewFile.putUserData(PreviewFileListener.PREVIEW_FILE_LISTENER, listener);
                document.addDocumentListener(listener);
            }
        });
    }

    protected void onAfterPreviewFileClosed(PreviewVirtualFile previewFile) {
        // Preview files are not supposed to be part of the "closed tabs" stack - therefore remove them by executing the action
        // => a preview can not be restored, but it is removed from stack
        if (myReopenClosedTabAction != null && previewFile != null) {
            invokeSafe(() -> myReopenClosedTabAction.actionPerformed(AnActionEvent.createFromDataContext("Editor", null, myComponentDataContext)));
        }
    }
}
