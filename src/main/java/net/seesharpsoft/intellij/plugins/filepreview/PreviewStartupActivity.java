package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PreviewStartupActivity implements StartupActivity, DumbAware {

    protected ConcurrentMap<Project, PreviewProjectHandler> myPreviewHandlerMap = new ConcurrentHashMap<>();

    protected void register(Project project, MessageBusConnection connection) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isRegistered(project)) {
                return;
            }
            PreviewProjectHandler projectHandler = PreviewProjectHandler.createIfPossible(project, connection);
            if (projectHandler != null) {
                myPreviewHandlerMap.put(project, projectHandler);
            }
        });
    }

    protected void unregister(Project project, MessageBusConnection connection) {
        PreviewProjectHandler projectHandler = myPreviewHandlerMap.get(project);
        if (projectHandler != null) {
            myPreviewHandlerMap.remove(project);
            projectHandler.dispose();
            connection.disconnect();
            connection.dispose();
        }
    }

    protected boolean isRegistered(Project project) {
        return myPreviewHandlerMap.containsKey(project);
    }

    @Override
    public void runActivity(@NotNull Project activityProject) {
        if (isRegistered(activityProject)) {
            return;
        }

        MessageBusConnection connection = activityProject.getMessageBus().connect();
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowUnregistered(@NotNull String id, @NotNull ToolWindow toolWindow) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    unregister(activityProject, connection);
                }
            }
            @Override
            public void stateChanged() {
                ToolWindow window = ToolWindowManager.getInstance(activityProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
                if (window != null && !isRegistered(activityProject)) {
                    register(activityProject, connection);
                }
            }
        });
        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosingBeforeSave(@NotNull Project project) {
                if (activityProject.equals(project)) {
                    unregister(activityProject, connection);
                }
            }
        });
        // try to register at startup
        register(activityProject, connection);
    }
}
