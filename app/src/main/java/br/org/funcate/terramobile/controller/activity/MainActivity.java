/*
 * Copyright 2013, Augmented Technologies Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.org.funcate.terramobile.controller.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import br.org.funcate.jgpkg.service.GeoPackageService;
import br.org.funcate.terramobile.model.tilesource.CustomBitmapTileSourceBase;
import br.org.funcate.terramobile.model.tilesource.MapTileGeoPackageProvider;
import br.org.funcate.terramobile.test.JGPKGTestInterface;
import br.org.funcate.terramobile.R;
import com.augtech.geoapi.geopackage.GeoPackage;
//import com.augtech.geoapi.geopackage.GpkgTEST;
/** The main Activity for running test cases
 * 
 * @author Augmented Technologies Ltd.
 *
 */
public class MainActivity extends Activity implements JGPKGTestInterface {

	static final String LOG_TAG = "GeoPackage Client";
	File testDir = getDirectory("GeoPackageTest");
	TextView statusText = null;
	MainActivity thisActivity = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		thisActivity = this;
		setContentView(R.layout.activity_main);
		statusText = (TextView) findViewById(R.id.statusText);
		Button create = (Button)findViewById(R.id.btn_testCreate);
		Button read = (Button)findViewById(R.id.btn_testRead);
		Button readTiles = (Button)findViewById(R.id.btn_testReadTiles);
        Button downloadTilesFileBtn = (Button)findViewById(R.id.btn_downloadFiles);
		create.setOnClickListener(testCreateClick);
		read.setOnClickListener(testReadClick);
        readTiles.setOnClickListener(testReadTilesClick);
        downloadTilesFileBtn.setOnClickListener(downloadFiles);


        createBaseTileSource();
/*
        MapFragment mapFragment = new MapFragment();
        FragmentManager fm = this.getFragmentManager();
        fm.beginTransaction().add(R.id.mapview, (Fragment)mapFragment).commit();
*/



    }

    private void createBaseTileSource() {

    /*    MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setMaxZoomLevel(20);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        OnlineTileSourceBase mapQuestTileSource = TileSourceFactory.MAPQUESTOSM;
        String tileSourcePath = mapQuestTileSource.OSMDROID_PATH.getAbsolutePath() + "/";

        final MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
        final ITileSource tileSource = new XYTileSource("MapquestOSM", ResourceProxy.string.mapnik, 1, 18, 256, ".png", new String[] { "http://tile.openstreetmap.org/" });

        tileProvider.setTileSource(tileSource);
        final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
        tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        mapView.getOverlays().add(tilesOverlay);

        tileProvider.setTileRequestCompleteHandler(new SimpleInvalidationHandler(mapView));

        mapView.setTileSource(tileSource);
        mapView.setUseDataConnection(false); //  letting osmdroid know you would use it in offline mode, keeps the mapView from loading online tiles using network connection.*/
    }

    private void createGeoPackageTileSourceOverlay()
    {

        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setMaxZoomLevel(20);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);


        System.out.println("Overlay size:" + mapView.getOverlayManager().size());

/*        OnlineTileSourceBase mapQuestTileSource = TileSourceFactory.MAPQUESTOSM;
        String tileSourcePath = mapQuestTileSource.OSMDROID_PATH.getAbsolutePath() + "/";*/

        final MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());

        final ITileSource tileSource = new XYTileSource("Mapnik", ResourceProxy.string.mapnik, 1, 18, 256, ".png", new String[] {"http://tile.openstreetmap.org/"});

        MapTileModuleProviderBase moduleProvider = new MapTileGeoPackageProvider(tileSource);
        SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(getApplicationContext());
        MapTileProviderArray tileProviderArray = new MapTileProviderArray(tileSource, simpleReceiver, new MapTileModuleProviderBase[] { moduleProvider });

/*        tileProvider.setTileSource(tileSource);*/
        final TilesOverlay tilesOverlay = new TilesOverlay(tileProviderArray, this.getApplicationContext());
        tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        mapView.getOverlays().add(tilesOverlay);
        //mapView.getOverlayManager().overlaysReversed();
        //mapView.getTileProvider().clearTileCache();
        tileProvider.setTileRequestCompleteHandler(new SimpleInvalidationHandler(mapView));
        mapView.setTileSource(tileSource);
        mapView.setUseDataConnection(false); //  letting osmdroid know you would use it in offline mode, keeps the mapView from loading online tiles using network connection.*/
    }

	private View.OnClickListener testCreateClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			GeoPackageService.createGPKG(thisActivity,"/GeoPackageTest/test.gpkg");
			
			statusText.setText("GeoPackage file successfully created");
		}
	};
	
	private View.OnClickListener testInsertClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			try {
				//GeoPackageService.insertDataGPKG(thisActivity,"/GeoPackageTest/test.gpkg");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				statusText.setText("Error insert GML on device: " +e.getMessage());
				return;
			}
			
			
		}
	};
    private View.OnClickListener downloadFiles = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                statusText.setText("Error insert GML on device: " +e.getMessage());
                return;
            }


        }
    };
	@Override
	public void testComplete(String msg) {
		statusText.setText( msg );
	}
	
	private View.OnClickListener testReadClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			
			try {
 
				GeoPackage gpkg = GeoPackageService.readGPKG(thisActivity,"/GeoPackageTest/test.gpkg");
				
				
				List<SimpleFeature> features = GeoPackageService.getGeometries(gpkg, "municipios_2005");
				
				statusText.setText(""+features.size()+" features on the file");
			
			} catch (Exception e) {
				statusText.setText("Error reading gpkg file: " + e.getMessage());
				return;
			}
			
		}
	};

    private View.OnClickListener testReadTilesClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            try {

                String path = Environment.getExternalStorageDirectory().toString();

                GeoPackage gpkg = GeoPackageService.readGPKG(thisActivity,"/GeoPackageTest/landsat2009_tiles.gpkg");


                createGeoPackageTileSourceOverlay();

    //            List<SimpleFeature> features = GeoPackageService.getTiles(gpkg, "landsat2012_tiles");

/*
                byte[] b1 = GeoPackageService.getTile(gpkg, "landsat2012_tiles", 0,0,1 );

                byte[] b2 = GeoPackageService.getTile(gpkg, "landsat2012_tiles", 84,131,8 );

//                statusText.setText(""+features.size()+" features on the file");

                File file = new File(path+"/b1.png");
                FileOutputStream fos = new FileOutputStream(file);

                fos.write(b1);
                fos.flush();
                fos.close();

                file = new File(path+"/b2.png");
                fos = new FileOutputStream(file);

                fos.write(b2);
                fos.flush();
                fos.close();
*/



            } catch (Exception e) {
                statusText.setText("Error reading gpkg file: " + e.getMessage());
                return;
            }

        }
    };
	
	
	@Override
	public void onBackPressed() {
		System.exit(0);
	}


	/** Do we have write access to the local SD card?
	 * 
	 * @return True if we can read from storage
	 */
	public static boolean isStorageAvailable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    } else {
	    	return false;
	    }
	}
	/** Check can read/write to SD card
	 * 
	 * @return True if we can
	 */
	public static boolean isStorageWriteable() {
	    String state = Environment.getExternalStorageState();
	    return Environment.MEDIA_MOUNTED.equals(state);
	}
	/** Get a directory on extenal storage (SD card etc), ensuring it exists
	 * 
	 * @return a new File representing the chosen directory
	 */
	public static File getDirectory(String directory) {
		if (directory==null) return null;
		String path = Environment.getExternalStorageDirectory().toString();
		path += directory.startsWith("/") ? "" : "/";
		path += directory.endsWith("/") ? directory : directory + "/"; 
		File file = new File(path);
		file.mkdirs();
		return file;
	}



}