package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class PreviewEditorProvider implements AsyncFileEditorProvider, DumbAware {

    public static final String EDITOR_TYPE_ID = "file-preview-editor";

    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }

    public FileEditorProvider findSourceProvider(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, file);
        for (FileEditorProvider provider : providers) {
            if (provider.accept(project, file)) {
                return provider;
            }
        }
        return null;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        if (!(file instanceof PreviewVirtualFile)) {
            return false;
        }
        VirtualFile actualFile = ((PreviewVirtualFile) file).getSource();
        if (SingleRootFileViewProvider.isTooLargeForIntelligence(actualFile)) {
            return false;
        }
        return findSourceProvider(project, actualFile) != null;
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return createEditorAsync(project, virtualFile).build();
    }

    @Override
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        FileEditorProvider fileEditorProvider = findSourceProvider(project, file);
        return fileEditorProvider.readState(sourceElement, project, file);
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        // intentionally not persisting preview state for now
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        FileEditorProvider fileEditorProvider = editor.getUserData(FileEditorProvider.KEY);
        if (fileEditorProvider != null) {
            fileEditorProvider.disposeEditor(editor);
        } else {
            Disposer.dispose(editor);
        }
    }

    @NotNull
    @Override
    public AsyncFileEditorProvider.Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new AsyncFileEditorProvider.Builder() {
            @Override
            public FileEditor build() {
                VirtualFile actualFile = ((PreviewVirtualFile) virtualFile).getSource();
                FileEditorProvider fileEditorProvider = findSourceProvider(project, actualFile);
                return fileEditorProvider.createEditor(project, actualFile);
            }
        };
    }
}
