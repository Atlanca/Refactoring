package android.app.printerapp.settings;

import android.app.Fragment;
import android.app.printerapp.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Created by sara on 5/02/15.
 */
public class SettingsDevicesFragment  extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retain instance to keep the Fragment from destroying itself
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Reference to View
        View rootView = null;

        //If is not new
        if (savedInstanceState==null){

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.settings_devices_fragment, container, false);

            /*********************************************************/

        }
        return rootView;
    }

}
