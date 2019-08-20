package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.util.PsiNavigationSupportImpl;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviewPsiNavigationSupport extends PsiNavigationSupportImpl {

    @Nullable
    @Override
    public Navigatable getDescriptor(@NotNull PsiElement element) {
        Navigatable navigatable = super.getDescriptor(element);
        if (navigatable instanceof OpenFileDescriptor) {
            navigatable = new PreviewNavigatable((OpenFileDescriptor) navigatable);
        }
        return navigatable;
    }

    @NotNull
    @Override
    public Navigatable createNavigatable(@NotNull Project project, @NotNull VirtualFile vFile, int offset) {
        Navigatable navigatable = super.createNavigatable(project, vFile, offset);
        if (navigatable instanceof OpenFileDescriptor) {
            navigatable = new PreviewNavigatable((OpenFileDescriptor) navigatable);
        }
        return navigatable;
    }

    private static class PreviewNavigatable implements Navigatable {
        private final OpenFileDescriptor myDelegate;

        PreviewNavigatable(OpenFileDescriptor delegate) {
            myDelegate = delegate;
        }

        @Override
        public void navigate(boolean requestFocus) {
            PreviewUtil.disposePreview(myDelegate.getProject(), myDelegate.getFile());
            myDelegate.navigate(requestFocus);
        }

        @Override
        public boolean canNavigate() {
            return myDelegate.canNavigate();
        }

        @Override
        public boolean canNavigateToSource() {
            return myDelegate.canNavigateToSource();
        }
    }
}
