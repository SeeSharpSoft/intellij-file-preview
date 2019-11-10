package net.seesharpsoft.intellij.plugins.filepreview.viewhandler;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import net.seesharpsoft.intellij.plugins.filepreview.PreviewProjectHandler;

public class ProjectToolWindowHandler extends ToolWindowHandlerBase {

    @Override
    protected String getToolWindowId() {
        return ToolWindowId.PROJECT_VIEW;
    }

    @Override
    protected ContentManagerListener createContentManagerListener(PreviewProjectHandler previewProjectHandler) {
        return new ProjectContentManagerListener(previewProjectHandler);
    }

    public static class ProjectContentManagerListener extends ContentManagerListener {

        public ProjectContentManagerListener(PreviewProjectHandler previewProjectHandler) {
            super(previewProjectHandler);
        }

        protected AbstractProjectViewPane getCurrentViewPane() {
            Project project = myPreviewProjectHandler.getProject();
            ProjectView projectView = ProjectView.getInstance(project);
            return projectView.getCurrentProjectViewPane();
        }

        public void unregisterCurrentTree() {
            AbstractProjectViewPane viewPane = getCurrentViewPane();
            if (viewPane != null) {
                myPreviewProjectHandler.unregisterTreeHandlers(viewPane.getTree());
            }
        }

        public void registerCurrentTree() {
            AbstractProjectViewPane viewPane = getCurrentViewPane();
            if (viewPane != null) {
                myPreviewProjectHandler.registerTreeHandlers(viewPane.getTree());
            }
        }
    }
}
