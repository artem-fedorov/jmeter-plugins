package com.blazemeter.api;

import com.blazemeter.api.explorer.Test;
import com.blazemeter.api.explorer.base.HttpBaseEntity;
import com.blazemeter.api.explorer.Account;
import com.blazemeter.api.explorer.Project;
import com.blazemeter.api.explorer.User;
import com.blazemeter.api.explorer.Workspace;
import com.blazemeter.jmeter.StatusNotifierCallback;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class BlazemeterAPIClient extends HttpBaseEntity {


    private Test test;
    private BlazemeterReport report;

    public BlazemeterAPIClient(StatusNotifierCallback notifier, String address, String dataAddress, BlazemeterReport report) {
        super(notifier, address, dataAddress, report.getToken(), report.isAnonymousTest());
        this.report = report;
        prepare();
    }

    public String startOnline() throws IOException {
        if (isAnonymousTest()) {
            notifier.notifyAbout("No BlazeMeter API key provided, will upload anonymously");
            test = new Test(this);
            return test.startAnonymousExternal();
        } else {
            test.startExternal();
            return (report.isShareTest()) ? test.getMaster().makeReportPublic() :
                    (address + "/app/#/masters/" + test.getMaster().getId());
        }
    }

    public void sendOnlineData(JSONObject data) throws IOException {
        test.getSession().sendData(data);
    }

    public void endOnline() throws IOException {
        if (isAnonymousTest()) {
            test.getSession().stopAnonymous();
        } else {
            test.getSession().stop();
        }
        test = null;
    }

    private void prepare() {
        User user = new User(this);
        try {
            user.ping();
        } catch (IOException e) {
            notifier.notifyAbout("Cannot reach online results storage, maybe the address/token is wrong");
            log.error("Cannot reach online results storage, maybe the address/token is wrong");
            return;
        }

        if (!isAnonymousTest()) {
            try {
                prepareClient(user);
            } catch (IOException e) {
                log.error("Cannot prepare client for sending report", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void prepareClient(User user) throws IOException {
        List<Account> accounts = user.getAccounts();
        Workspace workspace = findWorkspace(accounts);
        if (workspace != null) {
            Project project = findProject(workspace);
            test = findTest(project);
        }
    }

    private Test findTest(Project project) throws IOException {
        String testNameOrId = report.getTitle();
        if (testNameOrId == null || testNameOrId.isEmpty()) {
            testNameOrId = Test.DEFAULT_TEST;
            log.warn("Empty test title. Will be used '" + Test.DEFAULT_TEST + "' as test title");
        }

        final List<Test> tests = project.getTests();
        for (Test test : tests) {
            if (testNameOrId.equals(test.getId()) || testNameOrId.equals(test.getName())) {
                return test;
            }
        }

        notifier.notifyAbout("Creating a test '" + testNameOrId + "' in '" + project.getName() +"' project (id:" + project.getId() + ")");
        return project.createTest(testNameOrId);
    }

    private Project findProject(Workspace workspace) throws IOException {
        String projectNameOrId = report.getProject();
        if (projectNameOrId == null || projectNameOrId.isEmpty()) {
            projectNameOrId = Project.DEFAULT_PROJECT;
            log.warn("Empty project name. Will be used '" + Project.DEFAULT_PROJECT + "' as project name");
        }

        final List<Project> projects = workspace.getProjects();
        for (Project project : projects) {
            if (projectNameOrId.equals(project.getId()) || projectNameOrId.equals(project.getName())) {
                return project;
            }
        }

        notifier.notifyAbout("Creating a project '" + projectNameOrId + "' in '" + workspace.getName() +"' workspace (id:" + workspace.getId() + ")");
        return workspace.createProject(projectNameOrId);
    }

    private Workspace findWorkspace(List<Account> accounts) throws IOException {
        String workspaceNameOrId = report.getWorkspace();
        if (workspaceNameOrId == null || workspaceNameOrId.isEmpty()) {
            workspaceNameOrId = Workspace.DEFAULT_WORKSPACE;
            log.warn("Empty workspace name. Will be used '" + Workspace.DEFAULT_WORKSPACE + "' as workspace name");
        }

        for (Account account : accounts) {
            final List<Workspace> workspaces = account.getWorkspaces();
            for (Workspace workspace : workspaces) {
                if (workspaceNameOrId.equals(workspace.getId()) || workspaceNameOrId.equals(workspace.getName())) {
                    return workspace;
                }
            }
        }

        for (Account account : accounts) {
            Workspace wsp = account.createWorkspace(workspaceNameOrId);
            if (wsp != null) {
                return wsp;
            }
        }

        log.error("Cannot find workspace or create it");
        notifier.notifyAbout("Cannot find workspace or create it");
        return null;
    }

    public BlazemeterReport getReport() {
        return report;
    }
}

