package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PreviewStartupActivity implements StartupActivity, DumbAware {

    Map<Project, PreviewProjectHandler> myPreviewHandlerMap = new HashMap<>();

    protected void register(Project project, MessageBusConnection connection) {
        if (myPreviewHandlerMap.containsKey(project)) {
            return;
        }
        PreviewProjectHandler projectHandler = PreviewProjectHandler.createIfPossible(project, connection);
        if (projectHandler != null) {
            myPreviewHandlerMap.put(project, projectHandler);
        }
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

    @Override
    public void runActivity(@NotNull Project activityProject) {
        if (myPreviewHandlerMap.containsKey(activityProject)) {
            return;
        }

        MessageBusConnection connection = activityProject.getMessageBus().connect();
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowRegistered(@NotNull String id) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    register(activityProject, connection);
                }
            }
            @Override
            public void toolWindowUnregistered(@NotNull String id, @NotNull ToolWindow toolWindow) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    unregister(activityProject, connection);
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
