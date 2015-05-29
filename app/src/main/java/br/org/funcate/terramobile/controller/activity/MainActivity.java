package br.org.funcate.terramobile.controller.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import br.org.funcate.dynamicforms.FormUtilities;
import br.org.funcate.dynamicforms.FragmentDetailActivity;
import br.org.funcate.dynamicforms.TagsManager;
import br.org.funcate.dynamicforms.util.LibraryConstants;
import br.org.funcate.dynamicforms.util.PositionUtilities;
import br.org.funcate.dynamicforms.util.Utilities;
import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.configuration.ViewContextParameters;
import br.org.funcate.terramobile.controller.activity.settings.SettingsActivity;
import br.org.funcate.terramobile.model.exception.DownloadException;
import br.org.funcate.terramobile.model.exception.TerraMobileException;
import br.org.funcate.terramobile.model.gpkg.objects.GpkgLayer;
import br.org.funcate.terramobile.model.tilesource.AppGeoPackageService;
import br.org.funcate.terramobile.util.ResourceUtil;

public class MainActivity extends FragmentActivity {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private TreeView treeView;

    private ViewContextParameters parameters=new ViewContextParameters();

    // this members is used in dynamic form process.
    private static final String USE_MAPCENTER_POSITION = "USE_MAPCENTER_POSITION";
    private static final int FORM_RETURN_CODE = 669;
    private double latitude;
    private double longitude;
    private double elevation;
    private double[] gpsLocation;
    private final int RETURNCODE_DETAILACTIVITY = 665;
    // --------------------------------------------

    // Progress bar
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        treeView = new TreeView(MainActivity.this);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set a custom shadow that overlays the action_bar content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);

        int ActionBarTitleID = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView ActionBarTextView = (TextView) this.findViewById(ActionBarTitleID);
        ActionBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.title_text_size));

        getActionBar().setDisplayHomeAsUpEnabled(true);

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
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            insertMapView();
        }
    }

    @Override
    public void onBackPressed() {
        this.finish();
        System.exit(0);
        return;

    }

    public ViewContextParameters getParameters(){
        return parameters;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ExpandableListView mDrawerList=treeView.getUIComponent();
        if(mDrawerList==null) return false;
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
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {
            case R.id.download_geo_package:
                ConnectivityManager cm = (ConnectivityManager)this.getSystemService(this.CONNECTIVITY_SERVICE);

                int wifi = ConnectivityManager.TYPE_WIFI;
                int mobile = ConnectivityManager.TYPE_MOBILE;

                if (cm.getNetworkInfo(mobile).isConnected() ||
                        cm.getNetworkInfo(wifi).isConnected()) {
                    File appPath = ResourceUtil.getDirectory(getResources().getString(R.string.app_workspace_dir));
                    String tempURL = getResources().getString(R.string.gpkg_url);
                    String destinationFilePath = appPath.getPath();
                    new DownloadTask(destinationFilePath, true, this).execute(tempURL);
                }
                else{
                    Toast.makeText(this, R.string.no_connection, Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.acquire_new_point:
                FragmentManager fm = getSupportFragmentManager();
                MapFragment fragment = (MapFragment)fm.findFragmentById(R.id.content_frame);

                fragment.addBookmark();

                startForm();
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.test_raster_data:
                showTestRaster();
                break;
            case R.id.exit:
                this.finish();
                System.exit(0);
                break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showTestRaster() {

        try {
            GpkgLayer layer = treeView.getLayerByName("inpe_geoeye_2013_mosaico");
            AppGeoPackageService.createGeoPackageTileSourceOverlay(layer, MainActivity.this);

        } catch (TerraMobileException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    private void startForm() {
        // TODO: this name provided from the configuration of the "collect layer" in database.
        String sectionName = "terramobile";
        String selectedItemName = "crop features";

        checkPositionCoordinates();

        // insert note and then work on it
        try {
            JSONObject sectionObject = TagsManager.getInstance(MainActivity.this).getSectionByName(sectionName);
            String sectionObjectString = sectionObject.toString();

            long noteId = 123543;// fake number. remove this in future.

            // launch form activity
           /* Intent formIntent = new Intent(MainActivity.this, FormActivity.class);
            formIntent.putExtra(LibraryConstants.DATABASE_ID, noteId);
            formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_NAME, sectionName);
            formIntent.putExtra(LibraryConstants.LATITUDE, latitude);
            formIntent.putExtra(LibraryConstants.LONGITUDE, longitude);
            formIntent.putExtra(LibraryConstants.ELEVATION, elevation);
            startActivityForResult(formIntent, FORM_RETURN_CODE);*/

            Intent formIntent = new Intent(MainActivity.this, FragmentDetailActivity.class);
            formIntent.putExtra(LibraryConstants.DATABASE_ID, noteId);
            formIntent.putExtra(FormUtilities.ATTR_FORMNAME, selectedItemName);
            formIntent.putExtra(FormUtilities.ATTR_SECTIONOBJECTSTR, sectionObjectString);
            formIntent.putExtra(LibraryConstants.LONGITUDE, longitude);
            formIntent.putExtra(LibraryConstants.LATITUDE, latitude);
            startActivityForResult(formIntent, RETURNCODE_DETAILACTIVITY);


        } catch (Exception e) {
            Utilities.messageDialog(MainActivity.this, "falhou ao abrir formulário de coleta de dados." + e.getMessage(), null);
        }
    }

    private void checkPositionCoordinates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);
        if (useMapCenterPosition || gpsLocation == null) {
            double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
            latitude = mapCenter[1];
            longitude = mapCenter[0];
            elevation = 0.0;
        } else {
            latitude = gpsLocation[1];
            longitude = gpsLocation[0];
            elevation = gpsLocation[2];
        }
    }

    private void insertMapView() {
        // update the action_bar content by replacing fragments
        Fragment fragment = new MapFragment();
/*        Bundle args = new Bundle();
        args.putStringArrayList(MapFragment.mLayers);
        fragment.setArguments(args);*/
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
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
    protected void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.downloding));
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    public TreeView getTreeView() {
        return treeView;
    }
}