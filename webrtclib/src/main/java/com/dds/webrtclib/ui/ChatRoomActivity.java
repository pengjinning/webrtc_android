package com.dds.webrtclib.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.dds.webrtclib.R;
import com.dds.webrtclib.WebRTCHelper;
import com.dds.webrtclib.WebRTCManager;
import com.dds.webrtclib.callback.IViewCallback;
import com.dds.webrtclib.utils.PermissionUtil;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;

/**
 * 群聊界面
 * 1. 一对一视频通话
 * 2. 一对一语音通话
 */


public class ChatRoomActivity extends AppCompatActivity implements IViewCallback {

    private WebRTCManager helper;
    private Map<String, VideoTrack> _remoteVideoTracks = new HashMap();
    private Map<String, VideoRenderer.Callbacks> _remoteVideoView = new HashMap();
    private static int x;
    private static int y;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private double width = 480;
    private double height = 640;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    private ChatRoomFragment chatRoomFragment;

    public static void openActivity(Context activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        if (activity instanceof Activity) {
            activity.startActivity(intent);
            ((Activity) activity).overridePendingTransition(0, 0);
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wr_activity_chat_room);
        initView();
        initVar();
        chatRoomFragment = new ChatRoomFragment();
        replaceFragment(chatRoomFragment);
        startCall();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void initView() {
        vsv = findViewById(R.id.wr_glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
    }

    private void initVar() {
        // 设置宽高比例
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (manager != null) {
            width = manager.getDefaultDisplay().getWidth() / 3.0;
        }
        height = width * 32.0 / 24.0;
        x = 32;
        y = 0;


    }

    private void startCall() {
        helper = WebRTCManager.getInstance();
        helper.setCallback(this);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                Log.i("dds_webrtc", "surfaceView准备完毕");
                if (!PermissionUtil.isNeedRequestPermission(ChatRoomActivity.this)) {
                    helper.joinRoom();
                }

            }
        });


        localRender = VideoRendererGui.create(0, 0,
                30, 30, scalingType, true);


    }

    @Override
    public void onSetLocalStream(MediaStream stream, String socketId) {
        Log.i("dds_webrtc", "在本地添加视频");
        stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                0, 0,
                30, 30,
                scalingType, true);
    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String socketId) {
        Log.i("dds_webrtc", "接受到远端视频流     " + socketId);
        _remoteVideoTracks.put(socketId, stream.videoTracks.get(0));
        VideoRenderer.Callbacks vr = VideoRendererGui.create(
                0, 0,
                0, 0, scalingType, false);
        _remoteVideoView.put(socketId, vr);

        stream.videoTracks.get(0).addRenderer(new VideoRenderer(vr));

        VideoRendererGui.update(vr,
                x, y,
                30, 30,
                scalingType, false);

        x = x + 32;
        if (x > 90) {
            y += 32;
            x = 0;
        }

    }

    @Override
    public void onReceiveAck(String userId) {
        // 收到对方的回执，显示此人的头像

    }


    @Override
    public void onCloseWithId(String socketId) {
        Log.i("dds_webrtc", "有用户离开    " + socketId);
        VideoRenderer.Callbacks callbacks = _remoteVideoView.get(socketId);
        VideoRendererGui.remove(callbacks);
        _remoteVideoTracks.remove(socketId);
        _remoteVideoView.remove(socketId);
        if (_remoteVideoTracks.size() == 0) {
            x = 0;
        }

        if (_remoteVideoTracks.size() == 0) {
            //
            exit();
            this.finish();


        }


    }

    @Override
    public void onDecline() {
    }

    @Override
    public void onError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatRoomActivity.this.finish();
            }
        });
    }

    @Override  // 屏蔽返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        exit();
        super.onDestroy();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();

    }

    // 切换摄像头
    public void switchCamera() {
        helper.switchCamera();
    }

    // 挂断
    public void hangUp() {
        exit();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        helper.toggleMute(enable);
    }

    private void exit() {
        helper.exitRoom();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(WebRTCHelper.TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }

        helper.joinRoom();

    }
}
