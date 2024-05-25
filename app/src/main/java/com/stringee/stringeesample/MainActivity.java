package com.stringee.stringeesample;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.widget.CallConfig;
import com.stringee.widget.StringeeListener;
import com.stringee.widget.StringeeWidget;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //put your token here
//    public static String token = "eyJjdHkiOiJzdHJpbmdlZS1hcGk7dj0xIiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJTS0NsejhzQ2tKeDNzdU13SmdCdDJ6bUc2T01JbVRYb2Y1LTE2ODAwOTAyNTkiLCJpc3MiOiJTS0NsejhzQ2tKeDNzdU13SmdCdDJ6bUc2T01JbVRYb2Y1IiwiZXhwIjoxNjgyNjgyMjU5LCJ1c2VySWQiOiJsdWFuIn0.j2tlboAf12Wx6v0Nn2qhBgZRRTRYZ-grInoid9NydYk";
        private static String token = "eyJjdHkiOiJzdHJpbmdlZS1hcGk7dj0xIiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJTS0NsejhzQ2tKeDNzdU13SmdCdDJ6bUc2T01JbVRYb2Y1LTE2ODAwOTA0MzMiLCJpc3MiOiJTS0NsejhzQ2tKeDNzdU13SmdCdDJ6bUc2T01JbVRYb2Y1IiwiZXhwIjoxNjgyNjgyNDMzLCJ1c2VySWQiOiJsdWFubmIifQ.3eAVx1m7yETWn6Q8jcTu82SIxfWmSeAlrCLas6xqLAU";
    private StringeeWidget stringeeWidget;
    private String to;

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = findViewById(R.id.tv_userid);

        Button btnVoiceCall = findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(this);
        Button btnVideoCall = findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(this);
        etTo = findViewById(R.id.et_to);

        initAndConnectStringee(token);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice_call:
                to = etTo.getText().toString().trim();
                if (to.length() > 0) {
                    CallConfig config = new CallConfig(stringeeWidget.getClient().getUserId(), to);
                    stringeeWidget.makeCall(config, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
                break;
            case R.id.btn_video_call:
                to = etTo.getText().toString().trim();
                if (to.length() > 0) {
                    CallConfig config = new CallConfig(stringeeWidget.getClient().getUserId(), to);
                    config.setVideoCall(true);
                    config.setCameraFacing(CallConfig.CameraFacing.FRONT);
                    stringeeWidget.makeCall(config, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
                break;
        }
    }

    private void initAndConnectStringee(String token) {
        stringeeWidget = StringeeWidget.getInstance(this);
        stringeeWidget.setListener(new StringeeListener() {
            @Override
            public void onConnectionConnected() {
                Log.d("Stringee", "onConnectionConnected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        tvUserId.setText("Connected as: " + stringeeWidget.getClient().getUserId());
                    }
                });
            }

            @Override
            public void onConnectionDisconnected() {
                Log.d("Stringee", "onConnectionDisconnected");
            }

            @Override
            public void onConnectionError(StringeeError error) {
                Log.d("Stringee", "onConnectionError: " + error.getMessage());
            }

            @Override
            public void onRequestNewToken() {
                Log.d("Stringee", "onRequestNewToken");
            }

            @Override
            public void onCallStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState) {

            }

            @Override
            public void onCallStateChange2(StringeeCall2 stringeeCall, StringeeCall2.SignalingState signalingState) {

            }
        });
        stringeeWidget.connect(token);
    }
}
