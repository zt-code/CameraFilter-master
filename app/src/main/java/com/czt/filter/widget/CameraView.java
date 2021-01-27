package com.czt.filter.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.czt.filter.camera.CameraController;
import com.czt.filter.drawer.CameraDrawer;
import com.czt.filter.lisenter.RecordStopLisenter;
import com.czt.filter.lisenter.SavePictureTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cj on 2017/8/1.
 * desc
 */

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private CameraDrawer mCameraDrawer; //Camera绘制类
    private CameraController mCamera;  //照相机控制

    private int cameraId = 1; //摄像头ID
    private int dataWidth = 0, dataHeight = 0; //摄像头画面宽高
    private boolean isSetParm = false;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        /**初始化OpenGL的相关信息*/
        setEGLContextClientVersion(2);//设置版本
        setRenderer(this);//设置Renderer
        setRenderMode(RENDERMODE_WHEN_DIRTY);//主动调用渲染
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

        /**初始化Camera的绘制类*/
        mCameraDrawer = new CameraDrawer(getResources());
        /**初始化相机的管理类*/
        mCamera = new CameraController();
    }

    /**
     * 打开相机
     * @param cameraId
     */
    private void open(int cameraId) {
        mCamera.close();
        mCamera.open(cameraId);
        try {
            mCameraDrawer.setCameraId(cameraId);
            final Point previewSize = mCamera.getPreviewSize();
            dataWidth = previewSize.x;
            dataHeight = previewSize.y;
            SurfaceTexture texture = mCameraDrawer.getTexture();
            texture.setOnFrameAvailableListener(this);
            mCamera.setPreviewTexture(texture);
            mCamera.preview();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换前后置摄像头
     */
    public void switchCamera() {
        cameraId = cameraId == 0 ? 1 : 0;
        mCameraDrawer.switchCamera();
        open(cameraId);
    }

    /**
     * 照相并存储图片
     * @param listener
     */
    public void savePicture(final SavePictureTask.BitmapListener listener) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.drawPhoto(listener);
            }
        });
    }

    public void setStopRecordLisenter(final RecordStopLisenter lisenter) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.setStopRecordLisenter(lisenter);
            }
        });
    }

    /**
     * 绘制控制类
     * @return
     * @param type
     */
    public void setFilter(final String type) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.setFilter(type);
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraDrawer.onSurfaceCreated(gl, config);
        if (!isSetParm) {
            open(cameraId);
            stickerInit();
        }
        mCameraDrawer.setPreviewSize(dataWidth, dataHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraDrawer.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isSetParm) {
            mCameraDrawer.onDrawFrame(gl);
        }
    }

    /**
     * 每次Activity onResume时被调用,第一次不会打开相机
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isSetParm) {
            open(cameraId);
        }
    }

    public void onDestroy() {
        if (mCamera != null) {
            mCamera.close();
        }
    }

    /**
     * 摄像头聚焦
     */
    public void onFocus(Point point, Camera.AutoFocusCallback callback) {
        mCamera.onFocus(point, callback);
    }

    public CameraController getCamera() {
        return mCamera;
    }

    public int getCameraId() {
        return cameraId;
    }

    public int getBeautyLevel() {
        return mCameraDrawer.getBeautyLevel();
    }

    public void changeBeautyLevel(final int level) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.changeBeautyLevel(level);
            }
        });
    }

    public int getWaldenLevel() {
        return mCameraDrawer.getWaldeningLevel();
    }

    public void changeWaldenLevel(final int level) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.changeWaldeningLevel(level);
            }
        });
    }

    public void startRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.startRecord();
            }
        });
    }

    public void stopRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.stopRecord();
            }
        });
    }

    public void onTouch(final MotionEvent event) {
        queueEvent(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public void setSavePath(String path) {
        mCameraDrawer.setSavePath(path);
    }

    public void resume(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onResume(auto);
            }
        });
    }

    public void pause(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onPause(auto);
            }
        });
    }

    private void stickerInit() {
        if (!isSetParm && dataWidth > 0 && dataHeight > 0) {
            isSetParm = true;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }



}
