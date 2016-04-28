package br.org.funcate.terramobile.controller.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;

import br.org.funcate.jgpkg.exception.QueryException;
import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.tasks.BuildUploadGPKGTask;
import br.org.funcate.terramobile.controller.activity.tasks.UploadTask;
import br.org.funcate.terramobile.model.domain.Project;
import br.org.funcate.terramobile.model.exception.InvalidAppConfigException;
import br.org.funcate.terramobile.model.exception.InvalidGeopackageException;
import br.org.funcate.terramobile.model.exception.ProjectException;
import br.org.funcate.terramobile.model.exception.SettingsException;
import br.org.funcate.terramobile.model.gpkg.objects.GpkgLayer;
import br.org.funcate.terramobile.model.service.AppGeoPackageService;
import br.org.funcate.terramobile.model.service.LayersService;
import br.org.funcate.terramobile.model.service.ProjectsService;
import br.org.funcate.terramobile.util.CallbackConfirmMessage;
import br.org.funcate.terramobile.util.Message;
import br.org.funcate.terramobile.view.UploadLayerListAdapter;

/**
 * DialogFragment to show the user's credentials form on the settings menu
 *
 * Created by marcelo on 5/25/15.
 */
public class UploadProjectFragment extends DialogFragment{
    Project project;
//    private EditText eTServerURL;
    private UploadProjectController controller;

    private UploadLayerListAdapter uploadListAdapter;

    View view=null;
    public static UploadProjectFragment newInstance() {
        UploadProjectFragment fragment = new UploadProjectFragment();

        return fragment;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        view = inflater.inflate(R.layout.fragment_upload_project, null);
        controller=new UploadProjectController();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        builder.setView(view);
        builder.setTitle(R.string.project_upload);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.upload, null);
        final AlertDialog dialog = builder.create();


        dialog.show();

        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uploadProject()) {
                    dismiss();
                }
            }
        });

        return dialog;
    }

    public void setProject(Project project)
    {
        this.project=project;
    }

    private void buildLayersList()
    {
        if(this.project!=null)
        {
            try {

                ArrayList<GpkgLayer> layers= LayersService.getLayers((Context)getActivity());

                ArrayList<GpkgLayer> editableLayers = LayersService.getEditableLayers(layers);

                ArrayList<GpkgLayer> modifiedEditableLayers = LayersService.getModifiedLayers(editableLayers);

                ListView layersList = (ListView)view.findViewById(R.id.layersListView);

                uploadListAdapter = new UploadLayerListAdapter(getActivity(), R.id.layersListView, modifiedEditableLayers);

                layersList.setAdapter(uploadListAdapter);

            } catch (InvalidGeopackageException e) {
                e.printStackTrace();
                Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
            } catch (QueryException e) {
                e.printStackTrace();
                Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
            } catch (SettingsException e) {
                e.printStackTrace();
                Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
            } catch (InvalidAppConfigException e) {
                e.printStackTrace();
                Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        if (getDialog() == null) {  // Returns mDialog
            // Tells DialogFragment to not use the fragment as a dialog, and so won't try to use mDialog
            setShowsDialog(false);
        }
        buildLayersList();
        super.onActivityCreated(arg0);  // Will now complete and not crash
    }

    private boolean uploadProject()
    {
        ListView listView = (ListView)view.findViewById(R.id.layersListView);
        ArrayList<GpkgLayer> layers = new ArrayList<GpkgLayer>();
        for (int i = 0; i < listView.getCount(); i++) {
            GpkgLayer layer = (GpkgLayer) listView.getChildAt(i).getTag();
            CheckBox cb = (CheckBox) listView.getChildAt(i).findViewById(R.id.cbUploadLayer);
            if(cb.isChecked())
            {
                layers.add(layer);
            }
        }

        if(layers.size()==0)
        {
            Message.showErrorMessage(getActivity(), R.string.fail, R.string.error_uploding_missing_layers);
            return false;
        }

        try {
            if(AppGeoPackageService.uploadPackageExists(getActivity(), this.project)) {

                Message.showConfirmMessage(getActivity(), R.string.upload, R.string.upload_package_exists, new CallbackUploadMessage(this.project));

            }else {
                if(this.project!=null && layers!=null && !layers.isEmpty()) {

                    BuildUploadGPKGTask uploadGPKGTask = new BuildUploadGPKGTask((TerraMobileApp) getActivity(), this.project, layers);
                    uploadGPKGTask.execute();
                    return true;
                }
            }

        } catch (InvalidAppConfigException e) {
            e.printStackTrace();
            Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
        } catch (ProjectException e) {
            e.printStackTrace();
            Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
        }


        return false;

    }

    class CallbackUploadMessage implements CallbackConfirmMessage {

        private Project project;

        public CallbackUploadMessage(Project project) {
            this.project=project;
        }

        @Override
        public void confirmResponse(boolean response) {
            if(response) {
                final String serverURL = ((TerraMobileApp) getActivity()).getTerraMobileAppController().getServerURL();
                try {
                    String filePath = ProjectsService.getUploadFilePath(getActivity(), project);
                    UploadTask uploadTask = (UploadTask) new UploadTask(filePath, (TerraMobileApp)getActivity()).execute(serverURL + "uploadproject/");

                } catch (InvalidAppConfigException e) {
                    e.printStackTrace();
                    Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
                } catch (ProjectException e) {
                    e.printStackTrace();
                    Message.showErrorMessage(getActivity(), R.string.fail, e.getMessage());
                }
            }
        }

        @Override
        public void setWhoCall(int whoCall) {

        }
    }
}