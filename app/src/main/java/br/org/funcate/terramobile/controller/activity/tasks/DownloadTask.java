package br.org.funcate.terramobile.controller.activity.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.TerraMobileApp;
import br.org.funcate.terramobile.model.db.ApplicationDatabase;
import br.org.funcate.terramobile.model.db.DatabaseFactory;
import br.org.funcate.terramobile.model.db.dao.ProjectDAO;
import br.org.funcate.terramobile.model.domain.Project;
import br.org.funcate.terramobile.model.exception.DAOException;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.util.Util;

/**
 * This AsyncTask downloads a geopackage from the server
 */
public class DownloadTask extends AsyncTask<String, String, Boolean> {
    private String unzipDestinationFilePath;
    private String downloadDestinationFilePath;
    private ArrayList<String> mFiles;

    private TerraMobileApp terraMobileApp;

    private File destinationFile;

    private String projectFileName;

    private String projectUUID;

    private int projectStatus;

    private boolean error;

    private int errorMsg;

    public DownloadTask(String downloadDestinationFilePath, String unzipDestinationFilePath, String projectFileName, String projectUUID, int projectStatus, TerraMobileApp terraMobileApp) {
        this.terraMobileApp = terraMobileApp;
        this.unzipDestinationFilePath = unzipDestinationFilePath;
        this.downloadDestinationFilePath = downloadDestinationFilePath;
        this.projectFileName = projectFileName;
        this.projectUUID = projectUUID;
        this.projectStatus = projectStatus;
        mFiles = new ArrayList<String>();
        errorMsg=R.string.download_failed;
    }

    @Override
    protected void onPreExecute() {
        terraMobileApp.showDownloadProgressDialog(terraMobileApp.getString(R.string.downloading));
    }

    protected Boolean doInBackground(String... urlToDownload) {
        if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger(); // Para debugar é preciso colocar um breakpoint nessa linha

        if (urlToDownload[0].isEmpty()) {
            Log.e("URL missing", "Variable urlToDownload[0] is empty");
            return false;
        }

        if (downloadDestinationFilePath.isEmpty()) {
            Log.e("Path missing", "Variable downloadDestinationFilePath is empty");
            return false;
        }
        destinationFile = new File(downloadDestinationFilePath);
        try {
            if (!destinationFile.exists()) {
                if (!destinationFile.createNewFile()) {
                    return null;
                }
            }else {
                if(destinationFile.delete()) {
                    if(!destinationFile.createNewFile())
                        return null;
                }
                else return null;
            }

            HttpParams httpParams = new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);

            HttpConnectionParams.setSoTimeout(httpParams, 5000);

            HttpClient httpClient = new DefaultHttpClient(httpParams);

            HttpPost httpPost = new HttpPost(urlToDownload[0]);

            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            entityBuilder.addPart("projectStatus", new StringBody(Integer.toString(projectStatus), ContentType.TEXT_PLAIN));

            entityBuilder.addPart("projectId", new StringBody(projectUUID, ContentType.TEXT_PLAIN));

            entityBuilder.addPart("projectName", new StringBody(projectFileName, ContentType.TEXT_PLAIN));

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


                long totalSize = httpEntity.getContentLength();
                if(totalSize == -1){
                    this.cancel(true);
                    error = true;
                    return false;
                }

                if(content == null) {
                    this.cancel(true);
                    error = true;
                    return false;
                }

                InputStream inputStream = new BufferedInputStream(content);
                OutputStream fileOutput = new FileOutputStream(destinationFile);

                byte buffer[] = new byte[1024];

                int bufferLength;
                long total = 0;
                while ((bufferLength = inputStream.read(buffer)) != -1) {
                    if(isCancelled()) {
                        fileOutput.flush();
                        fileOutput.close();
                        inputStream.close();
                        return false;
                    }
                    total += bufferLength;
                    publishProgress("" + (int) ((total * 100) / totalSize), terraMobileApp.getString(R.string.downloading));
                    fileOutput.write(buffer, 0, bufferLength);
                }
                fileOutput.flush();
                fileOutput.close();

                String ext = terraMobileApp.getString(R.string.geopackage_extension);

                if(downloadDestinationFilePath.endsWith(ext))
                    mFiles.add(downloadDestinationFilePath.substring(downloadDestinationFilePath.lastIndexOf(File.separatorChar)+1, downloadDestinationFilePath.length()));
                else
                {
                    Log.e("Wrong file extension", "The requested GeoPackage hasn't the expected '.gpkg' extension.");
                    errorMsg = R.string.invalid_project;
                    error = true;
                    return false;
                }


                unzipDestinationFilePath += "/" +projectFileName;

                if(destinationFile.exists()) {
                    publishProgress("99", terraMobileApp.getString(R.string.copying_file));
                    Util.copyFile(downloadDestinationFilePath, unzipDestinationFilePath);
                    destinationFile.delete();
                }


                return true;

            }
            else
            {
                this.cancel(true);
                error = true;
                if(destinationFile.exists())
                    destinationFile.delete();
                return false;
            }

        }catch (IOException e) {
            e.printStackTrace();
            this.cancel(true);
            error = true;
            if(destinationFile.exists())
                destinationFile.delete();
            return false;
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            this.cancel(true);
            if(destinationFile.exists())
                destinationFile.delete();
            error = true;
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        try {
            if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger(); // Para debugar é preciso colocar um breakpoint nessa linha

              //  terraMobileApp.getTerraMobileAppController().getTreeViewController().refreshTreeView();

            if(mFiles.size()!=0)
            {
                String projectName = mFiles.get(0);// The project is the last not_downloaded geopackage file.

                ProjectDAO projectDAO = new ProjectDAO(DatabaseFactory.getDatabase(terraMobileApp, ApplicationDatabase.DATABASE_NAME));

                Project project = new Project();
                project.setId(null);
                project.setName(projectName);
                project.setFilePath(unzipDestinationFilePath);
                project.setDownloaded(1);
                projectDAO.insert(project);

                terraMobileApp.getTerraMobileAppController().setCurrentProject(projectDAO.getByName(projectName));
            }

            if(terraMobileApp.getProgressDialog() != null && terraMobileApp.getProgressDialog().isShowing()) {
                if (aBoolean) {
                    terraMobileApp.getProgressDialog().dismiss();
                    terraMobileApp.getProjectListFragment().dismiss();
                    Message.showSuccessMessage(terraMobileApp, R.string.success, R.string.download_success);
                } else {
                    terraMobileApp.getProgressDialog().dismiss();
                    Message.showErrorMessage(terraMobileApp, R.string.error, errorMsg);
                }
            }
            else {
                Message.showErrorMessage(terraMobileApp, R.string.error, errorMsg);
            }
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        } catch (DAOException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }

    }

    @Override
    protected void onProgressUpdate(String... values) {
        if(terraMobileApp.getProgressDialog() != null && terraMobileApp.getProgressDialog().isShowing()) {
            terraMobileApp.getProgressDialog().setProgress(Integer.parseInt(values[0]));
            terraMobileApp.getProgressDialog().setMessage(values[1]);
        }
    }

    /**
     * Called when the cancel button of the ProgressDialog is touched
     * @param aBoolean
     */
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        if(destinationFile.exists()) {
            if(destinationFile.delete())
                if(terraMobileApp.getProgressDialog().isShowing())
                    terraMobileApp.getProgressDialog().dismiss();
                if(error)
                    Message.showErrorMessage(terraMobileApp, R.string.error, R.string.download_failed);
                else
                    Message.showSuccessMessage(terraMobileApp, R.string.success, R.string.download_cancelled);
        }
    }
}