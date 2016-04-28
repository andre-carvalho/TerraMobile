package br.org.funcate.terramobile.controller.activity.tasks;


import android.os.AsyncTask;

import java.util.ArrayList;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.TerraMobileApp;
import br.org.funcate.terramobile.model.domain.Project;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.model.exception.StyleException;
import br.org.funcate.terramobile.model.exception.TerraMobileException;
import br.org.funcate.terramobile.model.gpkg.objects.GpkgLayer;
import br.org.funcate.terramobile.model.service.AppGeoPackageService;
import br.org.funcate.terramobile.util.Message;

/**
 * Created by andre on 28/03/16.
 *
 * This AsyncTask build a geopackages to upload
 */
public class BuildUploadGPKGTask extends AsyncTask<String, String, String> {
    private TerraMobileApp terraMobileApp;
    private Project project;
    private ArrayList<GpkgLayer> layers;

    public BuildUploadGPKGTask(TerraMobileApp terraMobileApp, Project project, ArrayList<GpkgLayer> layers) {
        this.terraMobileApp = terraMobileApp;
        this.project=project;
        this.layers=layers;
    }

    @Override
    protected void onPreExecute() {
        terraMobileApp.showDefaultLoadingDialog(terraMobileApp.getString(R.string.loading_prj_list));
    }

    protected String doInBackground(String... args) {
        String fileName = null;

        try {
            fileName = AppGeoPackageService.createGeopackageForUpload(terraMobileApp, this.project, this.layers);

        } catch (TerraMobileException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        } catch (StyleException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        }

        return fileName;
    }

    @Override
    protected void onPostExecute(String geoPackageFileName) {

        terraMobileApp.getProgressDialog().dismiss();

        if(geoPackageFileName!=null) {

            final String serverURL = (terraMobileApp).getTerraMobileAppController().getServerURL();
            UploadTask uploadTask = (UploadTask) new UploadTask(geoPackageFileName, terraMobileApp).execute(serverURL + "uploadproject/");
        }
    }
}