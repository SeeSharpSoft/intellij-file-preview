package net.seesharpsoft.intellij.fileeditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FileEditorManagerExWrapper extends FileEditorManagerEx {

    private final FileEditorManagerEx myDelegate;

    public FileEditorManagerExWrapper(FileEditorManagerEx delegate) {
        myDelegate = delegate;
    }

    @Override
    public JComponent getComponent() {
        return myDelegate.getComponent();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myDelegate.getPreferredFocusedComponent();
    }

    @NotNull
    @Override
    public Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(@NotNull VirtualFile file) {
        return myDelegate.getEditorsWithProviders(file);
    }

    @Nullable
    @Override
    public VirtualFile getFile(@NotNull FileEditor editor) {
        return myDelegate.getFile(editor);
    }

    @Override
    public void updateFilePresentation(@NotNull VirtualFile file) {
        myDelegate.updateFilePresentation(file);
    }

    @Override
    public EditorWindow getCurrentWindow() {
        return myDelegate.getCurrentWindow();
    }

    @NotNull
    @Override
    public Promise<EditorWindow> getActiveWindow() {
        return myDelegate.getActiveWindow();
    }

    @Override
    public void setCurrentWindow(EditorWindow window) {
        myDelegate.setCurrentWindow(window);
    }

    @Override
    public void closeFile(@NotNull VirtualFile file, @NotNull EditorWindow window) {
        myDelegate.closeFile(file, window);
    }

    @Override
    public void unsplitWindow() {
        myDelegate.unsplitWindow();
    }

    @Override
    public void unsplitAllWindow() {
        myDelegate.unsplitAllWindow();
    }

    @Override
    public int getWindowSplitCount() {
        return myDelegate.getWindowSplitCount();
    }

    @Override
    public boolean hasSplitOrUndockedWindows() {
        return myDelegate.hasSplitOrUndockedWindows();
    }

    @NotNull
    @Override
    public EditorWindow[] getWindows() {
        return myDelegate.getWindows();
    }

    @NotNull
    @Override
    public VirtualFile[] getSiblings(@NotNull VirtualFile file) {
        return myDelegate.getSiblings(file);
    }

    @Override
    public void createSplitter(int orientation, @Nullable EditorWindow window) {
        myDelegate.createSplitter(orientation, window);
    }

    @Override
    public void changeSplitterOrientation() {
        myDelegate.changeSplitterOrientation();
    }

    @Override
    public void flipTabs() {
        myDelegate.flipTabs();
    }

    @Override
    public boolean tabsMode() {
        return myDelegate.tabsMode();
    }

    @Override
    public boolean isInSplitter() {
        return myDelegate.isInSplitter();
    }

    @Override
    public boolean hasOpenedFile() {
        return myDelegate.hasOpenedFile();
    }

    @Nullable
    @Override
    public VirtualFile getCurrentFile() {
        return myDelegate.getCurrentFile();
    }

    @Nullable
    @Override
    public FileEditorWithProvider getSelectedEditorWithProvider(@NotNull VirtualFile file) {
        return myDelegate.getSelectedEditorWithProvider(file);
    }

    @Override
    public void closeAllFiles() {
        myDelegate.closeAllFiles();
    }

    @NotNull
    @Override
    public EditorsSplitters getSplitters() {
        return myDelegate.getSplitters();
    }

    @NotNull
    @Override
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@NotNull VirtualFile file, boolean focusEditor, boolean searchForSplitter) {
        return myDelegate.openFileWithProviders(file, focusEditor, searchForSplitter);
    }

    @NotNull
    @Override
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@NotNull VirtualFile file, boolean focusEditor, @NotNull EditorWindow window) {
        return myDelegate.openFileWithProviders(file, focusEditor, window);
    }

    @Override
    public boolean isChanged(@NotNull EditorComposite editor) {
        return myDelegate.isChanged(editor);
    }

    @Override
    public EditorWindow getNextWindow(@NotNull EditorWindow window) {
        return myDelegate.getNextWindow(window);
    }

    @Override
    public EditorWindow getPrevWindow(@NotNull EditorWindow window) {
        return myDelegate.getPrevWindow(window);
    }

    @Override
    public boolean isInsideChange() {
        return myDelegate.isInsideChange();
    }

    @Override
    public EditorsSplitters getSplittersFor(Component c) {
        return myDelegate.getSplittersFor(c);
    }

    @NotNull
    @Override
    public ActionCallback notifyPublisher(@NotNull Runnable runnable) {
        return myDelegate.notifyPublisher(runnable);
    }

    @Override
    public void closeFile(@NotNull VirtualFile file) {
        myDelegate.closeFile(file);
    }

    @Nullable
    @Override
    public Editor openTextEditor(@NotNull OpenFileDescriptor descriptor, boolean focusEditor) {
        return myDelegate.openTextEditor(descriptor, focusEditor);
    }

    @Nullable
    @Override
    public Editor getSelectedTextEditor() {
        return myDelegate.getSelectedTextEditor();
    }

    @Override
    public boolean isFileOpen(@NotNull VirtualFile file) {
        return myDelegate.isFileOpen(file);
    }

    @NotNull
    @Override
    public VirtualFile[] getOpenFiles() {
        return myDelegate.getOpenFiles();
    }

    @NotNull
    @Override
    public VirtualFile[] getSelectedFiles() {
        return myDelegate.getSelectedFiles();
    }

    @NotNull
    @Override
    public FileEditor[] getSelectedEditors() {
        return myDelegate.getSelectedEditors();
    }

    @Nullable
    @Override
    public FileEditor getSelectedEditor(@NotNull VirtualFile file) {
        return myDelegate.getSelectedEditor(file);
    }

    @NotNull
    @Override
    public FileEditor[] getEditors(@NotNull VirtualFile file) {
        return myDelegate.getEditors(file);
    }

    @NotNull
    @Override
    public FileEditor[] getAllEditors(@NotNull VirtualFile file) {
        return myDelegate.getAllEditors(file);
    }

    @NotNull
    @Override
    public FileEditor[] getAllEditors() {
        return myDelegate.getAllEditors();
    }

    @Override
    public void addTopComponent(@NotNull FileEditor editor, @NotNull JComponent component) {
        myDelegate.addTopComponent(editor, component);
    }

    @Override
    public void removeTopComponent(@NotNull FileEditor editor, @NotNull JComponent component) {
        myDelegate.removeTopComponent(editor, component);
    }

    @Override
    public void addBottomComponent(@NotNull FileEditor editor, @NotNull JComponent component) {
        myDelegate.addBottomComponent(editor, component);
    }

    @Override
    public void removeBottomComponent(@NotNull FileEditor editor, @NotNull JComponent component) {
        myDelegate.removeBottomComponent(editor, component);
    }

    @Override
    public void addFileEditorManagerListener(@NotNull FileEditorManagerListener listener) {
        myDelegate.addFileEditorManagerListener(listener);
    }

    @Override
    public void addFileEditorManagerListener(@NotNull FileEditorManagerListener listener, @NotNull Disposable parentDisposable) {
        myDelegate.addFileEditorManagerListener(listener, parentDisposable);
    }

    @Override
    public void removeFileEditorManagerListener(@NotNull FileEditorManagerListener listener) {
        myDelegate.removeFileEditorManagerListener(listener);
    }

    @NotNull
    @Override
    public List<FileEditor> openEditor(@NotNull OpenFileDescriptor descriptor, boolean focusEditor) {
        return myDelegate.openEditor(descriptor, focusEditor);
    }

    @NotNull
    @Override
    public Project getProject() {
        return myDelegate.getProject();
    }

    @Override
    public void setSelectedEditor(@NotNull VirtualFile file, @NotNull String fileEditorProviderId) {
        myDelegate.setSelectedEditor(file, fileEditorProviderId);
    }

    @NotNull
    @Override
    public ActionCallback getReady(@NotNull Object requestor) {
        return myDelegate.getReady(requestor);
    }
}
