package br.org.funcate.terramobile.controller.activity;

import android.content.Context;
import android.util.AttributeSet;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.views.MapView;

import br.org.funcate.terramobile.model.tilesource.TerraMobileInvalidationHandler;

/**
 * Created by bogo on 05/11/15.
 */
public class TerraMobileMapView extends MapView {

    private TerraMobileAppController terraMobileAppController;
    private boolean initialized=false;
    private MapEventsOverlay mapEventsOverlay;
    public TerraMobileMapView(Context context, AttributeSet attrs) {
        super(context, 256, new DefaultResourceProxyImpl(context), null, new TerraMobileInvalidationHandler(null), attrs);
        //super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if(terraMobileAppController !=null
                &&!initialized)
        {
            terraMobileAppController.onMapViewInitialized();
            initialized = true;
        }
        ((TerraMobileInvalidationHandler)getTileRequestCompleteHandler()).setMapView(this);
       /* try {
            configureMapView(this);
        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
        }*/
        this.getOverlays().add(0, mapEventsOverlay);
    }

    public void setMapEventsOverlay() {
        mapEventsOverlay = new MapEventsOverlay(terraMobileAppController.getMapFragment().getActivity(), (TerraMobileApp) terraMobileAppController.getMapFragment().getActivity());
    }

    public void setTerraMobileAppController(TerraMobileAppController terraMobileAppController) {
        this.terraMobileAppController = terraMobileAppController;
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
    }
}
