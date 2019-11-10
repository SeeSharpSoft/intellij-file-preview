package net.seesharpsoft.intellij.plugins.filepreview.viewhandler;

import com.intellij.openapi.wm.ToolWindowId;
import net.seesharpsoft.intellij.plugins.filepreview.PreviewProjectHandler;

public class VcsToolWindowHandler extends ToolWindowHandlerBase {


    @Override
    protected String getToolWindowId() {
        return ToolWindowId.VCS;
    }

    @Override
    protected ContentManagerListener createContentManagerListener(PreviewProjectHandler previewProjectHandler) {
        return new VcsContentManagerListener(previewProjectHandler);
    }

    public static class VcsContentManagerListener extends ContentManagerListener {

        public VcsContentManagerListener(PreviewProjectHandler previewProjectHandler) {
            super(previewProjectHandler);
        }


        public void unregisterCurrentTree() {
//            ChangesViewManager.getInstance(myPreviewProjectHandler.getProject())
        }

        public void registerCurrentTree() {

        }
    }
}
