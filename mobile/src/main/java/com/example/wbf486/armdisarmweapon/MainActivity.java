package com.example.wbf486.armdisarmweapon;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ErrorDialogFragment;

import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import java.lang.Byte;

public class MainActivity extends AppCompatActivity implements
		DataApi.DataListener,
		MessageApi.MessageListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

		// Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

		public static final String ARMDISARM_MESSAGE_PATH = "/armdisarm_message";
		public static final String ARMDISARM_DATAITEM_PATH = "/armdisarm_dataitem";
		private static final String ARMDISARM_KEY = "com.example.key.armdisarm";

    /*1 indicates ARM, 0 indicates DISARM*/
    private int mArmDisarmStatus = 0;
    
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    
    private GoogleApiClient mGoogleApiClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mResolvingError = (savedInstanceState != null) && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        // Request access only to the Wearable API
        .addApi(Wearable.API)
        .build();        
    }

		@Override
    public void onStart() {
    		super.onStart();
    		mGoogleApiClient.connect();           
    }
    
		@Override
    public void onStop() {
    		mGoogleApiClient.disconnect();
    		super.onStop();
    }    

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    		TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }
    
    @Override
    public void onConnectionSuspended(int cause) {
        TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("onConnectionSuspended: " + cause);
    }

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    if (requestCode == REQUEST_RESOLVE_ERROR) {
		        mResolvingError = false;
		        if (resultCode == RESULT_OK) {
		            // Make sure the app is not already connected or attempting to connect
		            if (!mGoogleApiClient.isConnecting() &&
		                    !mGoogleApiClient.isConnected()) {
		                mGoogleApiClient.connect();
		            }
		        }
		    }
		}

		@Override
		protected void onSaveInstanceState(Bundle outState) {
		    super.onSaveInstanceState(outState);
		    outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
		}
		
    @Override
    public void onConnectionFailed(ConnectionResult result) {
				if (mResolvingError) {
    				// Already attempting to resolve an error.
    				return;
    		} else if (result.hasResolution()) {
		        try {
		            mResolvingError = true;
		            result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
		        } catch (SendIntentException e) {
		            // There was an error with the resolution intent. Try again.
		            mGoogleApiClient.connect();
		        }
		    } else {
		        // Show dialog using GoogleApiAvailability.getErrorDialog()
		        showErrorDialog(result.getErrorCode());
		        mResolvingError = true;
		    }
    }		

    public void ButtonArmPress(View view)
    {
        mArmDisarmStatus = 1;

        TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("ARM");
    }

    public void ButtonDisarmPress(View view)
    {
        mArmDisarmStatus = 0;

        TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("DISARM");
    }

		@Override
		public void onMessageReceived(MessageEvent messageEvent) {
		    if (messageEvent.getPath().equals(ARMDISARM_MESSAGE_PATH)) {
						DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
						if(dataMap.getInt(ARMDISARM_KEY) == 0) {
            		ButtonDisarmPress(this.getCurrentFocus());
            }
            else if(dataMap.getInt(ARMDISARM_KEY) == 1) {
            		ButtonArmPress(this.getCurrentFocus());
            }
		    }
		}

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(ARMDISARM_DATAITEM_PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if(dataMap.getInt(ARMDISARM_KEY) == 0) {
                    		ButtonDisarmPress(this.getCurrentFocus());
                    }
                    else if(dataMap.getInt(ARMDISARM_KEY) == 1) {
                    		ButtonArmPress(this.getCurrentFocus());
                    }
                }				                
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

/*----- The rest of this code is all about building the error dialog -----*/

    /* 1. Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        //dialogFragment.show(getSupportFragmentManager(), "errordialog");
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* 2. Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }
    
    /* 3. A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }
}
