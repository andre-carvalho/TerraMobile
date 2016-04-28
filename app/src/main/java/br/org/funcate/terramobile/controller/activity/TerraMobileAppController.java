package br.org.funcate.terramobile.controller.activity;

import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.apache.http.impl.cookie.IgnoreSpecFactory;
import org.opengis.geometry.BoundingBox;

import java.io.File;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.model.db.ApplicationDatabase;
import br.org.funcate.terramobile.model.db.DatabaseFactory;
import br.org.funcate.terramobile.model.db.dao.ProjectDAO;
import br.org.funcate.terramobile.model.domain.Project;
import br.org.funcate.terramobile.model.domain.Setting;
import br.org.funcate.terramobile.model.exception.DAOException;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.model.exception.ProjectException;
import br.org.funcate.terramobile.model.exception.SettingsException;
import br.org.funcate.terramobile.model.service.LayersService;
import br.org.funcate.terramobile.model.service.ProjectsService;
import br.org.funcate.terramobile.model.service.SettingsService;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.util.Util;

/**
 * Created by bogo on 31/07/15.
 */
public class TerraMobileAppController {

    private TerraMobileApp terraMobileApp;
    private MenuMapController menuMapController;
    private GPSOverlayController gpsOverlayController;
    private TreeViewController treeViewController;
    private MarkerInfoWindowController markerInfoWindowController;

    private FeatureInfoPanelController featureInfoPanelController;

    private Project currentProject;

    public TerraMobileAppController(TerraMobileApp terraMobileApp) throws InvalidAppConfigException {
        this.terraMobileApp = terraMobileApp;
        this.menuMapController = new MenuMapController(terraMobileApp, this);
        this.gpsOverlayController = new GPSOverlayController(terraMobileApp);
        this.markerInfoWindowController = new MarkerInfoWindowController(terraMobileApp);
        this.featureInfoPanelController = new FeatureInfoPanelController(terraMobileApp);
        treeViewController = new TreeViewController(this.terraMobileApp, this);
    }

    public String getServerURL()
    {
        return getSettingValue("terramobile_url");
    }

    public String getUsername()
    {
        return getSettingValue("username");
    }

    public String getPassword()
    {
        return getSettingValue("password");
    }

    private String getCurrentProjectPath()
    {
        return getSettingValue("current_project");
    }

    private String getSettingValue(String key)
    {
        try {

            Setting setting = SettingsService.get(terraMobileApp, key, ApplicationDatabase.DATABASE_NAME);
            new IgnoreSpecFactory();
            if(setting!=null)
            {
                return setting.getValue();
            }

        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        } catch (SettingsException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }
        return null;
    }

    public MenuMapController getMenuMapController() {
        return menuMapController;
    }

    public void setMenuMapController(MenuMapController menuMapController) {
        this.menuMapController = menuMapController;
    }

    public MapFragment getMapFragment() {
        FragmentManager fm = this.terraMobileApp.getSupportFragmentManager();
        MapFragment fragment = (MapFragment)fm.findFragmentById(R.id.content_frame);
        return fragment;
    }

    public GPSOverlayController getGpsOverlayController() {
        return this.gpsOverlayController;
    }

    public TreeViewController getTreeViewController() {
        return treeViewController;
    }

    public void setTreeViewController(TreeViewController treeViewController) {
        this.treeViewController = treeViewController;
    }

    public boolean setCurrentProject(Project project) throws InvalidAppConfigException {

        if(project==null)
        {
            clearCurrentProject();
            return true;
        }

        DatabaseFactory.getDatabase(terraMobileApp, project.getFilePath());

        

        // remove GPS Overlay of the map
        boolean hasGPSEnabledOnMap = getGpsOverlayController().isOverlayAdded();
        if(hasGPSEnabledOnMap) getGpsOverlayController().removeGPSTrackerLayer();

        // remove all infoWindow
        markerInfoWindowController.closeAllInfoWindows();

        this.currentProject = project;

        getTreeViewController().refreshTreeView();

        terraMobileApp.invalidateOptionsMenu();

        try {
            Setting currentProjectSet = new Setting("current_project", project.getName());

            SettingsService.update(terraMobileApp, currentProjectSet, ApplicationDatabase.DATABASE_NAME);

            getMenuMapController().removeAllLayers(true);

            SettingsService.initProjectSettings(terraMobileApp, project);

            BoundingBox bb = ProjectsService.getProjectDefaultBoundingBox(terraMobileApp, project.getFilePath());

            if(bb==null)
            {
                //if bb == null include all layers bounding box
                bb = LayersService.getLayersMaxExtent(getTreeViewController().getAllLayers());
            }

            if(bb!=null)
            {
                getMenuMapController().panTo(bb);
            }


            if(hasGPSEnabledOnMap) getGpsOverlayController().addGPSTrackerLayer();

            getTreeViewController().enableInitialLayers();

        } catch (SettingsException e)
        {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
            clearCurrentProject();
            return false;
        } catch (ProjectException e)
        {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
            clearCurrentProject();
            return false;
        } catch (Exception e)
        {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, R.string.invalid_project);
            clearCurrentProject();
            return false;
        }
        return true;
    }

    private void clearCurrentProject()
    {
        try {
            this.currentProject = null;

            Setting currentProjectSet = new Setting("current_project", null);

            SettingsService.update(terraMobileApp, currentProjectSet, ApplicationDatabase.DATABASE_NAME);

            getTreeViewController().refreshTreeView();

            terraMobileApp.invalidateOptionsMenu();

        } catch (SettingsException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }
    }
    public void initMain()
    {
        try {

            SettingsService.initApplicationSettings(terraMobileApp);

        } catch (InvalidAppConfigException e) {

            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());

        } catch (SettingsException e) {

            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }

        try{
            getTreeViewController().initTreeView();
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }
    }


    public void loadCurrentProject() throws InvalidAppConfigException, DAOException {

        File directory = Util.getDirectory(terraMobileApp.getResources().getString(R.string.app_workspace_dir));

        String currentProjectPath = getCurrentProjectPath();

        String ext = terraMobileApp.getString(R.string.geopackage_extension);
        if(currentProjectPath != null) {
            ProjectDAO projectDAO = new ProjectDAO(DatabaseFactory.getDatabase(terraMobileApp, ApplicationDatabase.DATABASE_NAME));
            File currentProjectFile = Util.getGeoPackageByName(directory, ext, currentProjectPath);
            Project currentProject = projectDAO.getByName(currentProjectPath);
            if(currentProjectFile != null) {
                if(currentProject == null) {
                    Project project = new Project();
                    project.setId(null);
                    project.setName(currentProjectPath);
                    project.setFilePath(currentProjectFile.getPath());
                    projectDAO.insert(project);

                    currentProject = projectDAO.getByName(currentProjectPath);
                }
                else
                {
                    setCurrentProject(currentProject);
                }
            }
            else{
                if(currentProject != null){
                    if(projectDAO.remove(currentProject.getId())){
                        Log.i("Remove project", "Project removed");
                    }
                    else
                        Log.e("Remove project","Couldn't remove the project");
                }
            }
        }
    }

    public Project getCurrentProject()
    {
        return currentProject;
    }

    public MarkerInfoWindowController getMarkerInfoWindowController() {
        return markerInfoWindowController;
    }

    public void setMarkerInfoWindowController(MarkerInfoWindowController markerInfoWindowController) {
        this.markerInfoWindowController = markerInfoWindowController;
    }


    public FeatureInfoPanelController getFeatureInfoPanelController() {
        return featureInfoPanelController;
    }

    public void setFeatureInfoPanelController(FeatureInfoPanelController featureInfoPanelController) {
        this.featureInfoPanelController = featureInfoPanelController;
    }

    /**
     * When MapView is initialized this method is called. Use this method to trigger any initialization feature
     */
    public void onMapViewInitialized()
    {
        getMapFragment().configureMapView();

        try {

            menuMapController.getTerraMobileAppController().loadCurrentProject();

        } catch (InvalidAppConfigException e) {

            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());

        } catch (DAOException e) {

            Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
        }

    }
}
