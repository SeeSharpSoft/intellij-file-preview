package net.seesharpsoft.intellij.plugins.filepreview.viewhandler;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import net.seesharpsoft.intellij.plugins.filepreview.PreviewProjectHandler;
import net.seesharpsoft.intellij.plugins.filepreview.PreviewUtil;
import net.seesharpsoft.intellij.plugins.filepreview.PreviewViewHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class ToolWindowHandlerBase implements PreviewViewHandler {

    private final Map<PreviewProjectHandler, ProjectToolWindowHandler.ContentManagerListener> registeredContentManagerListeners;

    public ToolWindowHandlerBase() {
        registeredContentManagerListeners = new HashMap<>();
    }

    protected abstract String getToolWindowId();

    protected abstract ContentManagerListener createContentManagerListener(PreviewProjectHandler previewProjectHandler);

    @Override
    public void register(PreviewProjectHandler previewProjectHandler) {
        if (registeredContentManagerListeners.containsKey(previewProjectHandler)) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(previewProjectHandler.getProject()).getToolWindow(getToolWindowId());
        if (toolWindow == null) {
            return;
        }

        ContentManager cm = toolWindow.getContentManager();
        assert cm != null : "ContentManager required";
        ProjectToolWindowHandler.ContentManagerListener cml = createContentManagerListener(previewProjectHandler);
        cml.registerCurrentTree();
        cm.addContentManagerListener(cml);
        registeredContentManagerListeners.put(previewProjectHandler, cml);
    }

    @Override
    public void unregister(PreviewProjectHandler previewProjectHandler) {
        if (!registeredContentManagerListeners.containsKey(previewProjectHandler)) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(previewProjectHandler.getProject()).getToolWindow(getToolWindowId());
        if (toolWindow == null) {
            return;
        }

        ContentManager cm = toolWindow.getContentManager();
        ProjectToolWindowHandler.ContentManagerListener cml = registeredContentManagerListeners.get(previewProjectHandler);
        if (cm != null) {
            cm.removeContentManagerListener(cml);
        }
        cml.unregisterCurrentTree();
        registeredContentManagerListeners.remove(previewProjectHandler);
    }

    public abstract static class ContentManagerListener extends ContentManagerAdapter {
        protected final PreviewProjectHandler myPreviewProjectHandler;

        public ContentManagerListener(PreviewProjectHandler previewProjectHandler) {
            myPreviewProjectHandler = previewProjectHandler;
        }

        @Override
        public void selectionChanged(@NotNull ContentManagerEvent event) {
            if (event.getOperation() == ContentManagerEvent.ContentOperation.remove) {
                PreviewUtil.closeAllPreviews(myPreviewProjectHandler.getProject());
                unregisterCurrentTree();
            }
            if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                PreviewUtil.invokeSafe(myPreviewProjectHandler.getProject(), () -> {
                    registerCurrentTree();
                });
            }
        }

        public abstract void unregisterCurrentTree();
        public abstract void registerCurrentTree();
    }
}
