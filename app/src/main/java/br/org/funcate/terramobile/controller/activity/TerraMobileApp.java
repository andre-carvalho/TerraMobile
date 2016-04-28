package br.org.funcate.terramobile.controller.activity;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.settings.SettingsActivity;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.util.GlobalParameters;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.util.ResourceHelper;

public class TerraMobileApp extends FragmentActivity implements MapEventsReceiver,Marker.OnMarkerClickListener {
    private ActionBarDrawerToggle mDrawerToggle;

    private ActionBar actionBar;

    private CharSequence mTitle;
    // Progress bar
    private ProgressDialog progressDialog;

    private ProjectListFragment projectListFragment;

    private TerraMobileAppController terraMobileAppController;

    private BroadcastReceiver mMainActivityReceiver;

    /**
     * Temporary variable to test GPKG
     */
    public boolean useNewOverlaySFS = false;


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getTerraMobileAppController().getMarkerInfoWindowController().makeSomeProcessWithResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getActionBar();

        mMainActivityReceiver = new MainActivityReceiver();

        IntentFilter filter = new IntentFilter(GlobalParameters.ACTION_BROADCAST_MAIN_ACTIVITY);
        this.registerReceiver(mMainActivityReceiver, filter);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        ResourceHelper.setResources(getResources());

        try
        {
            terraMobileAppController = new TerraMobileAppController(this);

        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(this, R.string.error, e.getMessage());
        }


        mTitle = getTitle();
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // set a custom shadow that overlays the action_bar content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        int ActionBarTitleID = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView ActionBarTextView = (TextView) this.findViewById(ActionBarTitleID);
        ActionBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.title_text_size));

        actionBar.setDisplayHomeAsUpEnabled(true);
        // ActionBarDrawerToggle ties together the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  //host Activity
                mDrawerLayout,         //DrawerLayout object
                R.drawable.ic_drawer,  //nav drawer image to replace 'Up' caret
                R.string.drawer_open,  //"open drawer" description for accessibility
                R.string.drawer_close  //"close drawer" description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                actionBar.setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            insertMapView();
        }

        terraMobileAppController.initMain();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.mMainActivityReceiver);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        System.exit(0);
        return;
    }

    @Override
    public void onPause() {
        System.out.println("TerraMobileApp - onPause");
        super.onPause();
        // disableGPSTrackerLayer unregister location events listener too.
        if(getTerraMobileAppController().getGpsOverlayController().isOverlayAdded()) {
            getTerraMobileAppController().getGpsOverlayController().disableGPSTrackerLayer();
        }
    }

    @Override
    public void onResume() {
        System.out.println("TerraMobileApp - onResume");
        super.onResume();
        // enableGPSTrackerLayer register location events listener too.
        if(getTerraMobileAppController().getGpsOverlayController().isOverlayAdded()) {
            getTerraMobileAppController().getGpsOverlayController().enableGPSTrackerLayer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);

        MenuItem menuItem = menu.findItem(R.id.project);
        menuItem.setTitle(terraMobileAppController.getCurrentProject() != null ? terraMobileAppController.getCurrentProject().toString() : "Project");

        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ExpandableListView mDrawerList= terraMobileAppController.getTreeViewController().getUIComponent();
        if(mDrawerList==null) return false;

        MenuItem menuItem = menu.findItem(R.id.project);
        menuItem.setTitle(terraMobileAppController.getCurrentProject() != null ? terraMobileAppController.getCurrentProject().toString() : "Project");

        // If the nav drawer is open, hide action items related to the content view
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * This method is called when item from action bar is selected.
     * @param item, the menu item component
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        // Handle action buttons
        switch(item.getItemId()) {
            case R.id.project:
                projectListFragment = new ProjectListFragment();
                projectListFragment.show(getFragmentManager(), "packageList");
                return true;
                                                                                                                                                                                                                                                                                                                                                                            case R.id.acquire_new_point:
                getTerraMobileAppController().getMarkerInfoWindowController().startActivityForm();
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
/*            case R.id.tooglesfsbboxquery:
                this.useNewOverlaySFS=!this.useNewOverlaySFS;
                break;*/
            case R.id.exit:
                this.finish();
                System.exit(0);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void insertMapView() {

        // update the action_bar content by replacing fragments
        MapFragment fragment = new MapFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        fragment.setMenuMapController(terraMobileAppController.getMenuMapController());
        terraMobileAppController.getMenuMapController().setMapFragment(fragment);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        actionBar.setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Shows a progress bar with the download progress
     */
    public void showDownloadProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, TerraMobileApp.this.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TerraMobileApp.this.getProjectListFragment().getDownloadTask().cancel(true);
            }
        });
        progressDialog.show();
    }

    public void showUploadProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
    }

    public void showDefaultLoadingDialog(String message)
    {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    public ProjectListFragment getProjectListFragment() {
        return projectListFragment;
    }

    public TerraMobileAppController getTerraMobileAppController() {
        return terraMobileAppController;
    }

    public void setTerraMobileAppController(TerraMobileAppController terraMobileAppController) {
        this.terraMobileAppController = terraMobileAppController;
    }

    @Override
    public boolean onMarkerClick(Marker marker, MapView mapView) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        marker.showInfoWindow();
        return true;
    }

    private class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.hasExtra(GlobalParameters.STATE_GPS_LOCATION)) {
                Boolean showGPSLocation = intent.getBooleanExtra(GlobalParameters.STATE_GPS_LOCATION, false);
                if (showGPSLocation) {
                    getTerraMobileAppController().getGpsOverlayController().addGPSTrackerLayer();
                } else {
                    getTerraMobileAppController().getGpsOverlayController().removeGPSTrackerLayer();
                }
            }
            if(intent.hasExtra(GlobalParameters.STATE_GPS_CENTER)) {
                Boolean showGPSLocationOnCenter = intent.getBooleanExtra(GlobalParameters.STATE_GPS_CENTER, false);
                getTerraMobileAppController().getGpsOverlayController().setKeepOnCenter(showGPSLocationOnCenter);
            }
        }
    }
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        MapView mapView = getTerraMobileAppController().getMapFragment().getMapView();
        if(mapView != null){
//            terraMobileAppController.getFeatureInfoPanelController().startFeatureInfoPanel();

        }
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint geoPoint) {
        return false;
    }
}