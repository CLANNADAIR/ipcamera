package com.codecc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.sipservice.BroadcastEventReceiver;
import net.gotev.sipservice.SipAccountData;
import net.gotev.sipservice.SipCall;
import net.gotev.sipservice.SipServiceCommand;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.Arrays;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity  {

    @BindView(R.id.status)
    TextView statusText;



    @BindView(R.id.account)
    EditText account;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.toAccount)
    EditText toAccount;

    @BindView(R.id.local)
    SurfaceView localVideo;

    @BindView(R.id.remote)
    SurfaceView remoteVideo;

    @BindString(R.string.hello)
    String hello;


    protected PermissionChecker permissionChecker = new PermissionChecker();
    private static final String[] RequiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.INTERNET,Manifest.permission.USE_SIP};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //SipServiceCommand.removeAccount(this,"sip:wangcker@sip.1.codecc.cn");
        checkPermissions();

        init();
    }


    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, RequiredPermissions, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(MainActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void init(){

        event = new BroadcastEventReceiver(){
            @Override
            public void onStackStatus(boolean started) {
                if( ! started) {
                    // something went wrong
                }
            }

            @Override
            public void onRegistration(String accountID, pjsip_status_code registrationStateCode) {
                Log.d("xx",registrationStateCode.toString());
                if (registrationStateCode == pjsip_status_code.PJSIP_SC_OK) {
                    statusText.setText("注册成功");
                    //Toast.makeText(MainActivity.this,"注册成功",Toast.LENGTH_SHORT);
                    //Uri sipUri = Uri.parse(String.format("sip:%s@%s:%d", toAccount.getText().toString(), sipAccountData.getHost(), sipAccountData.getPort()));
                    //SipServiceCommand.makeCall(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), sipUri.toString(),true,false);
                }else if (registrationStateCode == pjsip_status_code.PJSIP_SC_REQUEST_TIMEOUT) {
                    Log.d("xx","timeout");
                    statusText.setText("请求超时");
                }else if (registrationStateCode == pjsip_status_code.PJSIP_SC_FORBIDDEN) {
                    Log.d("xx","forbidden");
                    statusText.setText("禁止注册");
                    //Uri sipUri = Uri.parse(String.format("sip:%d@%s:%d", callId, sipAccountData.getHost(), sipAccountData.getPort()));
                    //SipServiceCommand.makeCall(MainActivity.this, accountId, sipUri.toString(),true,false);
                }
            }

            @Override
            public void onCallState(String accountID, int callID, pjsip_inv_state callStateCode, pjsip_status_code callStatusCode,
                                    long connectTimestamp, boolean isLocalHold, boolean isLocalMute, boolean isLocalVideoMute) {
                Log.d("callstate:",callStateCode.toString());
                if(callStateCode == pjsip_inv_state.PJSIP_INV_STATE_CALLING){

                    //statusText.setText("接受到来自"+accountID+":"+callID+"的呼叫……");
                    statusText.setText("正在呼叫");
                    cId=callID;
                    //SipServiceCommand.acceptIncomingCall(MainActivity.this,accountID,callID);


                }
                if(callStateCode == pjsip_inv_state.PJSIP_INV_STATE_INCOMING){

                }

                if(callStateCode == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED){
                    // end of call
                    statusText.setText("连接断开");
                }

                if(callStateCode ==pjsip_inv_state.PJSIP_INV_STATE_EARLY ){
                    //statusText.setText("协商成功，准备通信");
                    cId=callID;
                    accId=accountID;

                    //SipServiceCommand.setupIncomingVideoFeed(MainActivity.this, accountID, cId, remoteVideo.getHolder().getSurface());
                }

                if(callStateCode ==pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED ){
                    statusText.setText("已接通");
                    if(true) {
                        //SipServiceCommand.startVideoPreview(MainActivity.this, accountID, cId, localVideo.getHolder().getSurface());
                        //SipServiceCommand.setupIncomingVideoFeed(MainActivity.this, accountID, cId, remoteVideo.getHolder().getSurface());
                        //SipServiceCommand.setVideoMute(MainActivity.this, accountID, cId, true);
                    }
                }

                if(callStateCode ==pjsip_inv_state.PJSIP_INV_STATE_CONNECTING){
                    statusText.setText("正在接通");

                }


            }

            @Override
            public void onOutgoingCall(String accountID, int callID, String number, boolean isVideo, boolean isVideoConference) {
                super.onOutgoingCall(accountID, callID, number, isVideo, isVideoConference);
                statusText.setText("正在呼叫"+number);
            }

            @Override
            public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
                //super.onIncomingCall(accountID, callID, displayName, remoteUri, isVideo);

                statusText.setText("接受到来自"+displayName+":"+callID+"的呼叫……");
                cId=callID;
                accId=accountID;
                /*if(isVideo) {

                    //SipServiceCommand.setVideoMute(MainActivity.this, accountID, cId, true);
                }*/
            }
        };


        event.register(MainActivity.this);

    }

    int cId=0 ;

    String accId;
    BroadcastEventReceiver event;
    SipAccountData sipAccountData = new SipAccountData();
    @OnClick(R.id.register)
    public void register(){




        /*IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("net.gotev.OUTGOING_CALL");
        registerReceiver(event,intentFilter);*/

        sipAccountData.setUsername(account.getText().toString());
        sipAccountData.setPassword(password.getText().toString());
        sipAccountData.setHost("sip.1.codecc.cn");
        sipAccountData.setRealm("sip.1.codecc.cn");

        sipAccountData.setPort(5262);
        sipAccountData.setTcpTransport(false);


        accId = SipServiceCommand.setAccount(MainActivity.this, sipAccountData);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }



    @OnClick(R.id.call)
    public void call(){
        Uri sipUri = Uri.parse(String.format("sip:%s@%s:%d", toAccount.getText().toString(), "sip.1.codecc.cn", sipAccountData.getPort()));
        //SipServiceCommand.switchVideoCaptureDevice(this,accId,cId);
        cId++;
        //SipServiceCommand.
        SipServiceCommand.makeCall(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), sipUri.toString(),true,false);
        /*SipServiceCommand.startVideoPreview(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), cId, localVideo.getHolder().getSurface());
        SipServiceCommand.setupIncomingVideoFeed(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), cId, remoteVideo.getHolder().getSurface());*/
        statusText.setText("正在呼叫……"+toAccount.getText().toString());
    }

    @OnClick(R.id.accept)
    public void accept(){
        //statusText.setText("正在");
        SipServiceCommand.acceptIncomingCall(MainActivity.this,accId,cId,true);


    }

    @OnClick(R.id.break_call)
    public void breakCall(){
        SipServiceCommand.hangUpCall(MainActivity.this,accId,cId);
    }
    private CameraDevice mCameraDevice;
    @OnClick(R.id.open_video)
    public void openVideo(){

SipServiceCommand.startVideoPreview(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), cId, localVideo.getHolder().getSurface());
        SipServiceCommand.setupIncomingVideoFeed(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), cId, remoteVideo.getHolder().getSurface());
        SipServiceCommand.setVideoMute(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(),cId,false);
        //SipServiceCommand.startVideoPreview(MainActivity.this, "sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(), cId, localVideo.getHolder().getSurface());
/*        SipServiceCommand.startVideoPreview(MainActivity.this, accId, cId, localVideo.getHolder().getSurface());
        SipServiceCommand.setupIncomingVideoFeed(MainActivity.this, accId, cId, remoteVideo.getHolder().getSurface());*/

/*        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA},100);
            }else {
                //打开摄像头
                CameraManager cameraManager  = (CameraManager)this.getSystemService(Context.CAMERA_SERVICE);
                cameraManager.openCamera("" + CameraCharacteristics.LENS_FACING_FRONT, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice cameraDevice) {//打卡摄像头
                        mCameraDevice = cameraDevice;
                        //开启预览
                        takePreview();
                    }

                    @Override
                    public void onDisconnected(CameraDevice cameraDevice) {//关闭摄像头
                        if (null != mCameraDevice){
                            mCameraDevice.close();
                            MainActivity.this.mCameraDevice = null;
                        }
                    }

                    @Override
                    public void onError(CameraDevice cameraDevice, int i) {
                        Toast.makeText(MainActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
                    }
                }, new Handler(Looper.getMainLooper()));
                SipServiceCommand.startVideoPreview(this,"sip:"+sipAccountData.getUsername()+"@"+sipAccountData.getRealm(),cId,localVideo.getHolder().getSurface());
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }*/

    }



    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        event.unregister(MainActivity.this);
    }

/*    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private void takePreview() {
        try{
            HandlerThread handlerThread = new HandlerThread("Camera2");
            handlerThread.start();
            Handler childHandler = new Handler(handlerThread.getLooper());
            mImageReader = ImageReader.newInstance(1080,1920, ImageFormat.JPEG,1);
            //创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将SurfceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(localVideo.getHolder().getSurface());
            //创建CameraCaptureSession,该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(localVideo.getHolder().getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice)return;
                    //摄像头已经准备好后显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        //自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //打卡闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //显示预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest,null,childHandler);// 进行预览
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            },childHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }*/



}

