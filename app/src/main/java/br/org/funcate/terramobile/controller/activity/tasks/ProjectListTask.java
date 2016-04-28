package br.org.funcate.terramobile.controller.activity.tasks;

import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.TerraMobileApp;
import br.org.funcate.terramobile.model.domain.Project;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.model.exception.ProjectException;
import br.org.funcate.terramobile.model.service.ProjectsService;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.util.Util;

/**
 * This AsyncTask receives a list of geopackages from the server
 */
public class ProjectListTask extends AsyncTask<String, String, JSONObject> {
    public TerraMobileApp terraMobileApp;

    public ProjectListTask(TerraMobileApp terraMobileApp) {
        this.terraMobileApp = terraMobileApp;
    }

    @Override
    protected void onPreExecute() {
        terraMobileApp.showDefaultLoadingDialog(terraMobileApp.getString(R.string.loading_prj_list));
    }

    protected JSONObject doInBackground(String... url) {
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger(); // Para debugar é preciso colocar um breakpoint nessa linha
        String packagesUrl = url[0];
        JSONObject jsonObject;
        String jsonContent;
        try {
            if (Util.isConnected(terraMobileApp)) {


                HttpParams httpParams = new BasicHttpParams();

                HttpConnectionParams.setConnectionTimeout(httpParams, 5000);

                HttpConnectionParams.setSoTimeout(httpParams, 5000);

                HttpClient httpClient = new DefaultHttpClient(httpParams);

                HttpPost httpPost = new HttpPost(packagesUrl);

                ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                entityBuilder.addPart("projectStatus", new StringBody("0", ContentType.TEXT_PLAIN));

                entityBuilder.addPart("user", new StringBody("userName", ContentType.TEXT_PLAIN));

                entityBuilder.addPart("password", new StringBody("password", ContentType.TEXT_PLAIN));

                httpPost.setEntity(entityBuilder.build());

                HttpResponse response = httpClient.execute(httpPost);

                StatusLine statusLine = response.getStatusLine();

                int statusCode = statusLine.getStatusCode();

                if (statusCode == 200) {

                    StringBuilder stringBuilder = new StringBuilder();

                    HttpEntity httpEntity = response.getEntity();

                    InputStream content = httpEntity.getContent();

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    jsonContent = stringBuilder.toString();

                    jsonObject = new JSONObject(jsonContent);

                    return jsonObject;
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        try {
            terraMobileApp.getProgressDialog().dismiss();

            File appPath = Util.getDirectory(terraMobileApp.getString(R.string.app_workspace_dir));

            ArrayList<Project> aLItems = new ArrayList<Project>();
            ArrayList<File> files = Util.getGeoPackageFiles(appPath, terraMobileApp.getString(R.string.geopackage_extension));
            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    String destinationFilePath = appPath.getPath() + "/" + file.getName();

                    Project project = new Project();
                    project.setName(file.getName());
                    project.setFilePath(destinationFilePath);
                    project.setDownloaded(1);
                    project.setOnTheAppOnly(true);

                    String status = "";
                    String UUID = "";
                    String description = "";

                    try {
                        UUID = ProjectsService.getUUID(terraMobileApp, file.getAbsolutePath());
                        status = ProjectsService.getStatus(terraMobileApp, file.getAbsolutePath());
                        description = ProjectsService.getDescription(terraMobileApp, file.getAbsolutePath());
                    } catch (InvalidAppConfigException e) {
                        e.printStackTrace();
                    } catch (ProjectException e) {
                        e.printStackTrace();
                    }

                    project.setUUID(UUID);

                    if (status.isEmpty()) {
                        project.setStatus(0);
                    } else {
                        project.setStatus(Integer.parseInt(status));
                    }

                    project.setDescription(description);

                    aLItems.add(project);
                }
            }

            if (jsonObject != null) {
                JSONArray packages = jsonObject.getJSONArray("projects");
                for (int cont = 0; cont < packages.length(); cont++) {
                    JSONObject json = (JSONObject) packages.get(cont);
                    String name = json.getString("project_name");
                    String id = json.getString("project_id");
                    int status = json.getInt("project_status");
                    String description = json.getString("project_description");

                    String destinationFilePath = appPath.getPath() + "/" + name;

                    File file = Util.getGeoPackageByName(appPath, terraMobileApp.getString(R.string.geopackage_extension), name);
                    if (file == null) {
                        Project project = new Project();
                        project.setName(name);
                        project.setFilePath(destinationFilePath);
                        project.setDownloaded(0);
                        project.setStatus(status);
                        project.setUUID(id);
                        project.setDescription(description);
                        aLItems.add(project);
                    }
                    //Set the project that is not only on the server
                    for (Project project : aLItems) {
                        if (project.getUUID().equalsIgnoreCase(id)) {
                            project.setOnTheAppOnly(false);
                            break;
                        }
                    }

                }
            }

            if (!aLItems.isEmpty())
                terraMobileApp.getProjectListFragment().setListItems(aLItems);
            else {
                terraMobileApp.getProjectListFragment().dismiss();
                Message.showErrorMessage(terraMobileApp, R.string.error, R.string.projects_not_found);
            }
        } catch (JSONException e) {
            Message.showErrorMessage(terraMobileApp, R.string.error, R.string.connection_failed);
            e.printStackTrace();
        }
    }
}