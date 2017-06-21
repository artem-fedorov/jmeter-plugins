package com.blazemeter.api.explorer;

import com.blazemeter.api.entity.BlazemeterReport;
import com.blazemeter.jmeter.StatusNotifierCallback;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Account extends BaseEntity {

    public Account(StatusNotifierCallback notifier, String address, String dataAddress, BlazemeterReport report, String id, String name) {
        super(notifier, address, dataAddress, report, id, name);
    }

    public List<Workspace> getWorkspaces() throws IOException {
        String uri = address + String.format("/api/v4/workspaces?accountId=%s&enabled=true&limit=100", getId());
        JSONObject response = queryObject(createGet(uri), 200);
        return extractWorkspaces(response.getJSONArray("result"));
    }

    private List<Workspace> extractWorkspaces(JSONArray result) {
        List<Workspace> workspaces = new ArrayList<>();

        for (Object obj : result) {
            workspaces.add(convertToWorkspace((JSONObject) obj));
        }

        return workspaces;
    }

    private Workspace convertToWorkspace(JSONObject obj) {
        return new Workspace(notifier, address, dataAddress, report, obj.getString("id"), obj.getString("name"));
    }

    public Workspace createWorkspace(String workspace) {
        // TODO:
        return null;
    }
}
