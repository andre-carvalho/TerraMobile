package br.org.funcate.terramobile.controller.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import br.org.funcate.dynamicforms.FormUtilities;
import br.org.funcate.dynamicforms.FragmentDetailActivity;
import br.org.funcate.dynamicforms.images.ImageUtilities;
import br.org.funcate.dynamicforms.util.LibraryConstants;
import br.org.funcate.jgpkg.exception.QueryException;
import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.model.exception.DAOException;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.model.exception.LowMemoryException;
import br.org.funcate.terramobile.model.exception.SettingsException;
import br.org.funcate.terramobile.model.exception.TerraMobileException;
import br.org.funcate.terramobile.model.gpkg.objects.GpkgLayer;
import br.org.funcate.terramobile.model.osmbonuspack.overlays.SFSEditableMarker;
import br.org.funcate.terramobile.model.service.AppGeoPackageService;
import br.org.funcate.terramobile.model.service.EditableLayerService;
import br.org.funcate.terramobile.model.service.FeatureService;
import br.org.funcate.terramobile.model.service.LayersService;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.util.ResourceHelper;
import br.org.funcate.terramobile.util.Util;

/**
 * Created by Andre Carvalho on 14/08/15.
 */
public class MarkerInfoWindowController {

    private ArrayList<File> temporaryThumbnailImages;
    private ArrayList<File> temporaryDisplayImages;
    private TerraMobileApp terraMobileApp;
    private ProgressBar pgrInfoWindow;
    private ImageButton btnEditMarker;
    // identify the return of the request of the Activity Form
    private static int FORM_RESULT_CODE = 222;

    public MarkerInfoWindowController(TerraMobileApp terraMobileApp) {
        this.terraMobileApp = terraMobileApp;
    }

    public void setProgressBar(ProgressBar progressBar) {
        pgrInfoWindow = progressBar;
    }

    public void setImageBtn(ImageButton imageBtn) {
        btnEditMarker = imageBtn;
    }

    private void hideProgress() {
        // in create mode, does not display the info Window of the new marker
        if(pgrInfoWindow!=null && btnEditMarker!=null) {
            pgrInfoWindow.setVisibility(View.GONE);
            btnEditMarker.setVisibility(View.VISIBLE);
        }
    }

    public void editMarker(Marker marker) {
        Long markerId;
        try {
            markerId = ((SFSEditableMarker) marker).getMarkerId().longValue();
        }catch (TerraMobileException e){
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
            return;
        }catch (InvalidAppConfigException e){
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
            return;
        }
        startActivityForm(markerId);
    }

    public void deleteMarker(Marker marker) throws TerraMobileException {
        GpkgLayer layer=this.terraMobileApp.getTerraMobileAppController().getTreeViewController().getSelectedEditableLayer();
        boolean exec=false;

        try {

            long featureID = ((SFSEditableMarker)marker).getMarkerId();
            SimpleFeature feature=null;
            try {
                feature = AppGeoPackageService.getFeature(layer, featureID);
            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
            } catch (LowMemoryException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
            } catch (TerraMobileException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
            }

            String statusKey;
            String objIdKey;
            try {
                statusKey = ResourceHelper.getStringResource(R.string.point_status_column);
                objIdKey = ResourceHelper.getStringResource(R.string.point_obj_id_column);
                if(feature!=null) {
                    String objIdValue = (String) feature.getAttribute(objIdKey);

                    if (objIdValue != null && !objIdValue.isEmpty()) {// Update the feature that was loaded from official database
                        feature.setAttribute(statusKey, ResourceHelper.getIntResource(R.integer.point_status_removed));
                        exec = AppGeoPackageService.setRemovedFeature(layer, feature);
                    } else {
                        exec = AppGeoPackageService.deleteFeature(layer, ((SFSEditableMarker) marker).getMarkerId());
                    }
                }

            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
            }

            if(!exec) {
                throw new TerraMobileException(ResourceHelper.getStringResource(R.string.feature_not_found));
            }else{
                layer.setModified(true);
                LayersService.updateModified(this.terraMobileApp, this.terraMobileApp.getTerraMobileAppController().getCurrentProject(), layer);
                this.terraMobileApp.getTerraMobileAppController().getMapFragment().updateMap();
            }
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        } catch (LowMemoryException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        } catch (SettingsException e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.fail, e.getMessage());
        }

    }

    public void moveMarker(Marker marker) throws TerraMobileException {
        GpkgLayer layer=this.terraMobileApp.getTerraMobileAppController().getTreeViewController().getSelectedEditableLayer();
        try {
            if(!AppGeoPackageService.updateFeature(layer, marker)) {
                throw new TerraMobileException(ResourceHelper.getStringResource(R.string.failure_on_save_new_location));
            }else{
                layer.setModified(true);
                LayersService.updateModified(this.terraMobileApp, this.terraMobileApp.getTerraMobileAppController().getCurrentProject(), layer);
            }
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            throw new TerraMobileException(e.getMessage());
        } catch (LowMemoryException e) {
            e.printStackTrace();
            throw new TerraMobileException(e.getMessage());
        } catch (SettingsException e) {
            e.printStackTrace();
            throw new TerraMobileException(e.getMessage());
        }
    }

    public void startActivityForm() {
        // this magic value is used to determine when opening form to new collection data.
        long codePoint=-1;
        this.startActivityForm(codePoint);
    }

    public void startActivityForm(long pointID) {

        ArrayList<GeoPoint> geoPoints=null;
        GpkgLayer editableLayer;
        String geometryType = "";

        try{
            TreeViewController tv = terraMobileApp.getTerraMobileAppController().getTreeViewController();
            editableLayer = tv.getSelectedEditableLayer();
            if(editableLayer==null) {
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.missing_editable_layer);
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.error_start_form);
            return;
        }

        SimpleFeature feature = null;
        Map<String, Object> images = null;
        Geometry geom;
        Bundle formDataValues=null;
        // if pointID >= 0 then this point exist on database
        if(pointID>=0) {
            try {
                feature = AppGeoPackageService.getFeature(editableLayer, pointID);
            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, e.getMessage());
                return;
            } catch (LowMemoryException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, e.getMessage());
                return;
            } catch (TerraMobileException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, e.getMessage());
                return;
            }catch (Exception e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.error_start_form);
                return;
            }

            try {
                images = EditableLayerService.getPhotosFromDatabase(terraMobileApp, editableLayer, pointID);
            } catch (TerraMobileException e) {
                e.printStackTrace();
                images = null;
            } catch (DAOException e) {
                e.printStackTrace();
                images = null;
            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
                images = null;
            } catch (Exception e) {
                e.printStackTrace();
                images = null;
            }

            if (feature!=null && feature.getDefaultGeometry() != null) {
                geom = (Geometry) feature.getDefaultGeometry();
                SimpleFeatureType featureType = feature.getFeatureType();
                GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
                GeometryType defaultGeometryType = geometryDescriptor.getType();
                geometryType = defaultGeometryType.getDescription().toString();
                if(geometryType.equalsIgnoreCase(FormUtilities.GEOJSON_TYPE_POINT) || geometryType.equalsIgnoreCase(FormUtilities.GEOJSON_TYPE_MULTIPOINT)) {
                    Coordinate[] coords = geom.getCoordinates();
                    int coordsLength = coords.length;
                    geoPoints=new ArrayList<GeoPoint>(coordsLength);
                    for (Coordinate coord : coords) {
                        geoPoints.add(new GeoPoint(coord.y, coord.x));
                    }
                }
                formDataValues = FeatureService.featureAttrsToBundle(feature, "");
                if(images!=null && !images.isEmpty()) {
                    Bundle b = mediaToBundle(formDataValues, images);
                    if(b!=null) formDataValues = b;
                }
            }
        }else {
            geoPoints=new ArrayList<GeoPoint>(1);
            geoPoints.add((GeoPoint) getMapView().getMapCenter());
            SimpleFeatureType featureType = editableLayer.getFeatureType();
            GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
            GeometryType defaultGeometryType = geometryDescriptor.getType();
            geometryType = defaultGeometryType.getDescription().toString();
        }

        try {
            Intent formIntent = new Intent(terraMobileApp, FragmentDetailActivity.class);
            formIntent.putExtra(LibraryConstants.SELECTED_POINT_ID, pointID);
            // The form name attribute, provided by JSON, shall be the same name of the editable layer.
            formIntent.putExtra(FormUtilities.ATTR_FORMNAME, editableLayer.getName());
            formIntent.putExtra(FormUtilities.ATTR_JSON_TAGS, editableLayer.getJSON());

            if(geoPoints!=null) {

                JSONObject geojson = new JSONObject();

                if(geometryType.equalsIgnoreCase(FormUtilities.GEOJSON_TYPE_POINT))
                    geojson.put(FormUtilities.GEOJSON_TAG_TYPE,FormUtilities.GEOJSON_TYPE_POINT);
                else if(geometryType.equalsIgnoreCase(FormUtilities.GEOJSON_TYPE_MULTIPOINT))
                    geojson.put(FormUtilities.GEOJSON_TAG_TYPE,FormUtilities.GEOJSON_TYPE_MULTIPOINT);

                JSONArray coordinates = new JSONArray();
                for (GeoPoint gp : geoPoints) {
                    JSONArray coordinate = new JSONArray();
                    coordinate.put(gp.getLongitude());
                    coordinate.put(gp.getLatitude());
                    coordinates.put(coordinate);
                }
                geojson.put(FormUtilities.GEOJSON_TAG_COORDINATES, coordinates);
                String str_geojson = geojson.toString();
                formIntent.putExtra(FormUtilities.ATTR_GEOJSON_TAGS, str_geojson);
            }
            if(formDataValues!=null) {
                formIntent.putExtra(FormUtilities.ATTR_DATA_VALUES, formDataValues);
            }
            File directory = Util.getDirectory(terraMobileApp.getResources().getString(R.string.app_workspace_temp_dir));

            formIntent.putExtra(FormUtilities.MAIN_APP_WORKING_DIRECTORY, directory.getAbsolutePath());
            terraMobileApp.startActivityForResult(formIntent, FORM_RESULT_CODE);

        } catch (Exception e) {
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.error_start_form);
        }
    }

    private Bundle mediaToBundle(Bundle bundle, Map<String, Object> images) {
        Bundle imageMapBundle = new Bundle(2);
        Bundle imageMapThumbnailBundle = new Bundle(images.size());
        Bundle imageMapDisplayBundle = new Bundle(images.size());

        Set<String> keys = images.keySet();
        Iterator<String> itKeys = keys.iterator();
        temporaryThumbnailImages = new ArrayList<File>(keys.size());
        temporaryDisplayImages = new ArrayList<File>(keys.size());
        try {
            while (itKeys.hasNext()) {
                String key = itKeys.next();
                File tmpThumbnailFile = File.createTempFile(ImageUtilities.getTempImageName(null) + "_thumbnail", key);
                File tmpDisplayFile = File.createTempFile(ImageUtilities.getTempImageName(null)+"_display", key);
                Object value = images.get(key);
                byte[][] v = (byte[][]) value;
                ImageUtilities.writeImageDataToFile(v[0], tmpThumbnailFile.getPath());
                temporaryThumbnailImages.add(tmpThumbnailFile);
                imageMapThumbnailBundle.putString(key, tmpThumbnailFile.getPath());

                ImageUtilities.writeImageDataToFile(v[1], tmpDisplayFile.getPath());
                temporaryDisplayImages.add(tmpDisplayFile);
                imageMapDisplayBundle.putString(key, tmpDisplayFile.getPath());
            }
            imageMapBundle.putBundle("thumbnail", imageMapThumbnailBundle);
            imageMapBundle.putBundle("display", imageMapDisplayBundle);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        bundle.putBundle(FormUtilities.IMAGE_MAP, imageMapBundle);

        return bundle;
    }

    public void makeSomeProcessWithResult(int requestCode, int resultCode, Intent data) {

        hideProgress();

        if (resultCode == Activity.RESULT_OK && requestCode == FORM_RESULT_CODE) {
            Bundle extras = data.getBundleExtra(LibraryConstants.PREFS_KEY_FORM);
            try {
                EditableLayerService.storeData(terraMobileApp, extras);

                TreeViewController tv = terraMobileApp.getTerraMobileAppController().getTreeViewController();
                GpkgLayer editableLayer = tv.getSelectedEditableLayer();
                editableLayer.setModified(true);
                LayersService.updateModified(this.terraMobileApp, this.terraMobileApp.getTerraMobileAppController().getCurrentProject(), editableLayer);

            }catch (TerraMobileException tme) {
                tme.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.error, R.string.missing_form_data);
            }catch (QueryException qe) {
                qe.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.error, R.string.error_while_storing_form_data);
            }catch (DAOException de) {
                de.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.error, R.string.error_while_storing_form_data);
            } catch (SettingsException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
                Message.showErrorMessage(terraMobileApp, R.string.error, e.getMessage());
            }

            this.terraMobileApp.getTerraMobileAppController().getMapFragment().updateMap();
        }
    }

    public MapView getMapView() {
        MapView mapView = (MapView) this.terraMobileApp.findViewById(R.id.mapview);
        return mapView;
    }

    public void viewFeatureData(long featureID) {
        GpkgLayer editableLayer;
        try{
            TreeViewController tv = terraMobileApp.getTerraMobileAppController().getTreeViewController();
            editableLayer = tv.getSelectedEditableLayer();
            if(editableLayer==null) {
                Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.missing_editable_layer);
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
            Message.showErrorMessage(terraMobileApp, R.string.failure_title_msg, R.string.error_start_form);
            return;
        }

        FeatureInfoPanelController controller = terraMobileApp.getTerraMobileAppController().getFeatureInfoPanelController();
        controller.startFeatureInfoPanel(editableLayer, featureID);
    }

    public void closeAllInfoWindows() {
        MapView mapView = getMapView();
        if(mapView!=null) InfoWindow.closeAllInfoWindowsOn(mapView);
    }
}
