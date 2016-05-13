package com.example.wbf486.armdisarmweapon;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ErrorDialogFragment;

import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.PutDataMapRequest;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements
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
		private byte mArmDisarmByteArray[] = new byte[] {0};

    private TextView mTextView;

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        
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
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Now you can use the Data Layer API
    }
    
    @Override
    public void onConnectionSuspended(int cause) {
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

    public void onMessageButtonClicked(View target) {
        if (mGoogleApiClient == null)
            return;

				//Toggle the arm/disarm status
    		mArmDisarmStatus ^= 1;
				mArmDisarmByteArray[0] = (byte)mArmDisarmStatus;

        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                if (nodes != null) {
                    for (int i=0; i<nodes.size(); i++) {
                        final Node node = nodes.get(i);

                        // You can just send a message
                        PendingResult<MessageApi.SendMessageResult> pendingSendMessageResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), ARMDISARM_MESSAGE_PATH, mArmDisarmByteArray);

                        // or you may want to also check check for a result:
                        // final PendingResult<SendMessageResult> pendingSendMessageResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/MESSAGE", null);
                        // pendingSendMessageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        //      public void onResult(SendMessageResult sendMessageResult) {
                        //          if (sendMessageResult.getStatus().getStatusCode()==WearableStatusCodes.SUCCESS) {
                        //              // do something is successed
                        //          }
                        //      }
                        // });
                    }
                }
            }
        });
    }
    
    public void onDataItemButtonClicked(View target) {
    		if (mGoogleApiClient == null)
            return;
            
    		//Toggle the arm/disarm status
    		mArmDisarmStatus ^= 1;
        
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(ARMDISARM_DATAITEM_PATH);
        putDataMapReq.getDataMap().putInt(ARMDISARM_KEY, mArmDisarmStatus);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);    	
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
