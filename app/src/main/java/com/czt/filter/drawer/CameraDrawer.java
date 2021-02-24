package com.czt.filter.drawer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.Toast;

import com.czt.filter.filters.AFilter;
import com.czt.filter.filters.CameraFilter;
import com.czt.filter.filters.GroupFilter;
import com.czt.filter.filters.NoFilter;
import com.czt.filter.filters.ProcessFilter;
import com.czt.filter.filters.WaterMarkFilter;
import com.czt.filter.gpufilter.basefilter.GPUImageFilter;
import com.czt.filter.gpufilter.filter.MagicBeautyFilter;
import com.czt.filter.gpufilter.filter.MagicWhitenFilter;
import com.czt.filter.gpufilter.helper.MagicFilterFactory;
import com.czt.filter.lisenter.RecordStopLisenter;
import com.czt.filter.lisenter.SavePictureTask;
import com.czt.filter.record.video.TextureMovieEncoder;
import com.czt.filter.utils.EasyGlUtils;
import com.czt.filter.utils.MatrixUtils;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cj on 2017/8/2.
 * desc 管理图像绘制的类
 * 主要用于管理各种滤镜、画面旋转、视频编码录制等
 */

public class CameraDrawer implements GLSurfaceView.Renderer {

    private float[] OM ;
    /**显示画面的filter*/
    private final AFilter showFilter;
    /**后台绘制的filter*/
    private final AFilter drawFilter;
    /**无样式filter*/
    private final GPUImageFilter mFilter;
    /**可绘制绘制多个filter的Group组(会上下翻转矩阵)*/
    private final GroupFilter mGroupFilter;
    /**不会上下翻转矩阵的filter*/
    private final AFilter mProcessFilter1;
    private final AFilter mProcessFilter2;
    private final AFilter mProcessFilter3;
    /**美颜的filter*/
    private MagicBeautyFilter mBeautyFilter;
    /**美白的filter*/
    private MagicWhitenFilter mWaldeningFilter;
    /*选择性filter*/
    private GPUImageFilter selectFilter;
    //默认的选择性filter
    private static String filterType = "默认";
    //判断可变滤镜是否已初始化完毕
    private boolean isOpen = false;


    private SurfaceTexture mSurfaceTextrue;
    /**预览数据的宽高*/
    private int mPreviewWidth=0,mPreviewHeight=0;
    /**控件的宽高*/
    private int width = 0,height = 0;

    private TextureMovieEncoder videoEncoder;
    private boolean recordingEnabled;
    private int recordingStatus;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static final int RECORDING_PAUSE=3;
    private static final int RECORDING_RESUME=4;
    private static final int RECORDING_PAUSED=5;
    private String savePath;
    private int textureID;
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];

    private float[] SM = new float[16];     //用于显示的变换矩阵

    //截图方法1
    private boolean isSaveBitmap = false;
    private SavePictureTask.BitmapListener mListener;

    //截图方法2
    /*private boolean isSaveBitmap = false;
    private GL10 mGl10;*/

    public CameraDrawer(Resources resources){
        //显示画面的filter
        showFilter = new NoFilter(resources);
        //后台绘制的filter
        drawFilter = new CameraFilter(resources);
        //无样式filter
        mFilter = new GPUImageFilter();
        //可绘制绘制多个filter的Group组(会上下翻转矩阵)
        mGroupFilter = new GroupFilter(resources);
        //不会上下翻转矩阵的filter
        mProcessFilter1 = new ProcessFilter(resources);
        mProcessFilter2 = new ProcessFilter(resources);
        mProcessFilter3 = new ProcessFilter(resources);
        //美颜filter
        mBeautyFilter = new MagicBeautyFilter();
        //美白filter
        mWaldeningFilter = new MagicWhitenFilter();
        //可选择filter
        selectFilter = MagicFilterFactory.initFilters(filterType);

        //水印filter
        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(resources);
        //waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher));
        waterMarkFilter.setWaterMark(null);
        waterMarkFilter.setPosition(30,50,0,0);
        mGroupFilter.addFilter(waterMarkFilter);

        //必须传入上下翻转的矩阵
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM,false,true);//矩阵上下翻转

        recordingEnabled = false;
   }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        textureID = createTextureID();
        mSurfaceTextrue = new SurfaceTexture(textureID);

        drawFilter.create();
        drawFilter.setTextureId(textureID);
        showFilter.create();
        mGroupFilter.create();
        mProcessFilter1.create();
        mProcessFilter2.create();
        mProcessFilter3.create();

        mFilter.init();
        mBeautyFilter.init();
        mWaldeningFilter.init();
        selectFilter.init();

        if (recordingEnabled){
            recordingStatus = RECORDING_RESUMED;
        } else{
            recordingStatus = RECORDING_OFF;
        }
    }



    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        width = i;
        height = i1;

        //清除遗留的
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
        /**创建一个帧染缓冲区对象*/
        GLES20.glGenFramebuffers(1,fFrame,0);
        /**根据纹理数量 返回的纹理索引*/
        GLES20.glGenTextures(1, fTexture, 0);
        /*GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width,
                height);*/
        /**将生产的纹理名称和对应纹理进行绑定*/
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[0]);
        /**根据指定的参数 生产一个2D的纹理 调用该函数前  必须调用glBindTexture以指定要操作的纹理*/
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mPreviewWidth, mPreviewHeight,
                0,  GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        useTexParameter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);

        drawFilter.setSize(mPreviewWidth,mPreviewHeight);
        mGroupFilter.setSize(mPreviewWidth,mPreviewHeight);
        mProcessFilter1.setSize(mPreviewWidth,mPreviewHeight);
        mProcessFilter2.setSize(mPreviewWidth,mPreviewHeight);
        mProcessFilter3.setSize(mPreviewWidth,mPreviewHeight);

        mFilter.onDisplaySizeChanged(mPreviewWidth,mPreviewHeight);
        mFilter.onInputSizeChanged(mPreviewWidth,mPreviewHeight);

        mBeautyFilter.onDisplaySizeChanged(mPreviewWidth,mPreviewHeight);
        mBeautyFilter.onInputSizeChanged(mPreviewWidth,mPreviewHeight);

        mWaldeningFilter.onDisplaySizeChanged(mPreviewWidth,mPreviewHeight);
        mWaldeningFilter.onInputSizeChanged(mPreviewWidth,mPreviewHeight);

        selectFilter.onDisplaySizeChanged(mPreviewWidth,mPreviewHeight);
        selectFilter.onInputSizeChanged(mPreviewWidth,mPreviewHeight);

        MatrixUtils.getShowMatrix(SM,mPreviewWidth, mPreviewHeight, width, height);
        showFilter.setMatrix(SM);
        //对画面进行矩阵旋转
        //MatrixUtils.flip(showFilter.getMatrix(),false,true);
    }

    /**
     * 切换摄像头的时候
     * 会出现画面颠倒的情况
     * 通过跳帧来解决
     * */
    boolean switchCamera=false;
    int skipFrame;
    public void switchCamera() {
        switchCamera = true;
    }
    @Override
    public void onDrawFrame(GL10 gl10) {
        /**更新界面中的数据*/
        mSurfaceTextrue.updateTexImage();
        if(switchCamera){
            skipFrame++;
            if(skipFrame>1){
                skipFrame=0;
                switchCamera=false;
            }
            return;
        }

        int texture = 0; //临时纹理存放

        //第一次绘制
        EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]); //渲染到fTexture[0]这个纹理上
        GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight); //起始位置坐标,预览宽高
        drawFilter.draw();
        EasyGlUtils.unBindFrameBuffer();
        texture = fTexture[0];

        //第二次绘制（关联了水印滤镜设置，矩阵翻转）
        mGroupFilter.setTextureId(texture);
        mGroupFilter.draw();
        texture = mGroupFilter.getOutputTexture();

        //第三次绘制（可变滤镜，放在第一层打底当背景，因为绘制是一层一层叠加）
        if(isOpen) {  //可变滤镜初始化完毕
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
            selectFilter.onDrawFrame(texture);
            EasyGlUtils.unBindFrameBuffer();
            texture = fTexture[0];

            //第四次绘制（防止屏幕倒转闪烁）
            mProcessFilter1.setTextureId(texture);
            mProcessFilter1.draw();
            texture = mProcessFilter1.getOutputTexture();
        }

        //第次五绘制（美颜）
        if(mBeautyFilter.getBeautyLevel() != 0) {
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
            mBeautyFilter.onDrawFrame(texture);
            EasyGlUtils.unBindFrameBuffer();
            texture = fTexture[0];
        }

        //第六次绘制（防止屏幕倒转闪烁）
        mProcessFilter2.setTextureId(texture);
        mProcessFilter2.draw();
        texture = mProcessFilter2.getOutputTexture();

        //第七次绘制（美白或不美白）
        if(mWaldeningFilter.getWaldenLevel() != 0) {
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
            mWaldeningFilter.onDrawFrame(texture);
            EasyGlUtils.unBindFrameBuffer();
            texture = fTexture[0];
        }

        //第八次绘制（防止屏幕倒转闪烁）
        mProcessFilter3.setTextureId(texture);
        mProcessFilter3.draw();
        texture = mProcessFilter3.getOutputTexture();

        GLES20.glViewport(0,0,width,height);
        showFilter.setTextureId(texture);
        showFilter.draw();

        if(isSaveBitmap) {
            Bitmap bitmap = drawPhoto();
            mListener.getBitmap(bitmap);
            isSaveBitmap = false;
        }
        //mGl10 = gl10;

        //录制相关
        if (recordingEnabled){
            /**说明是录制状态*/
            switch (recordingStatus){
                case RECORDING_OFF: //开始录制
                    videoEncoder = new TextureMovieEncoder();
                    videoEncoder.setPreviewSize(mPreviewWidth,mPreviewHeight);
                    videoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            savePath, mPreviewWidth, mPreviewHeight,
                            1500000, EGL14.eglGetCurrentContext(),
                            null));
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED: //继续录制
                    videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    videoEncoder.resumeRecording();
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                case RECORDING_PAUSED:
                    break;
                case RECORDING_PAUSE: //暂停录制
                    videoEncoder.pauseRecording();
                    recordingStatus = RECORDING_PAUSED;
                    break;
                case RECORDING_RESUME: //继续录制
                    videoEncoder.resumeRecording();
                    recordingStatus = RECORDING_ON;
                    break;
                default:
                    throw new RuntimeException("unknown recording status "+recordingStatus);
            }
        }else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                case RECORDING_PAUSE:
                case RECORDING_RESUME:
                case RECORDING_PAUSED:
                    videoEncoder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown recording status " + recordingStatus);
            }
        }

        if (videoEncoder != null && recordingEnabled && recordingStatus == RECORDING_ON){
            videoEncoder.setTextureId(texture);
            videoEncoder.frameAvailable(mSurfaceTextrue);
        }
    }

    public TextureMovieEncoder getTextureMovieEncoder() {
        return videoEncoder;
    }
    /**
     * 获取屏幕截图
     * @return
     */
    public Bitmap drawPhoto(){
        Bitmap result = null;
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(ib);
        return result;
    }

    /**
     * 录像录制完成回调
     * @param lisenter
     */
    public void setStopRecordLisenter(RecordStopLisenter lisenter) {
        if(videoEncoder != null) {
            videoEncoder.setStopRecordLisenter(lisenter);
        }else {
            Log.i("why", "坎坎坷坷扩扩扩扩扩扩扩扩扩扩扩");
        }
    }
    /*public Bitmap createBitmapFromGLSurface() {
        int bitmapBuffer[] = new int[width * height];
        int bitmapSource[] = new int[width * height];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {
            mGl10.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                    intBuffer);
            int offset1, offset2;
            for (int i = 0; i < height; i++) {
                offset1 = i * width;
                offset2 = (height - i - 1) * width;
                for (int j = 0; j < width; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.ARGB_8888);
        return bitmap;
    }*/

    public void drawPhoto(final SavePictureTask.BitmapListener listener){
        mListener = listener;
        isSaveBitmap = true;
    }

    /**设置预览效果的size*/
    public void setPreviewSize(int width,int height){
        if (mPreviewWidth != width || mPreviewHeight != height){
            mPreviewWidth = width;
            mPreviewHeight = height;
        }
    }

    /*设置滤镜*/
    public void setFilter(String type) {
        isOpen = false;
        if(selectFilter != null) {
            selectFilter.destroy();
            selectFilter = null;
        }
        filterType = type;
        selectFilter = MagicFilterFactory.initFilters(filterType);
        selectFilter.init();
        selectFilter.onDisplaySizeChanged(mPreviewWidth,mPreviewHeight);
        selectFilter.onInputSizeChanged(mPreviewWidth,mPreviewHeight);
        isOpen = true;
    }

    /**提供修改美白等级的接口*/
    public void changeBeautyLevel(int level){
        mBeautyFilter.setBeautyLevel(level);
    }

    public int getBeautyLevel(){
        return mBeautyFilter.getBeautyLevel();
    }

    public void changeWaldeningLevel(int level){
        mWaldeningFilter.setWaldenLevel(level);
    }

    public int getWaldeningLevel(){
        return mWaldeningFilter.getWaldenLevel();
    }

    /**根据摄像头设置纹理映射坐标*/
    public void setCameraId(int id) {
        drawFilter.setFlag(id);
    }

    public void startRecord() {
        recordingEnabled = true;
    }

    public void stopRecord() {
        recordingEnabled = false;
    }

    public void setSavePath(String path) {
        this.savePath=path;
    }

    public SurfaceTexture getTexture() {
        return mSurfaceTextrue;
    }

    public void onPause(boolean auto) {
        if(auto){
            videoEncoder.pauseRecording();
            if(recordingStatus==RECORDING_ON){
                recordingStatus=RECORDING_PAUSED;
            }
            return;
        }
        if(recordingStatus==RECORDING_ON){
            recordingStatus=RECORDING_PAUSE;
        }
    }

    public void onResume(boolean auto) {
        if(auto){
            if(recordingStatus==RECORDING_PAUSED){
                recordingStatus=RECORDING_RESUME;
            }
            return;
        }
        if(recordingStatus==RECORDING_PAUSED){
            recordingStatus=RECORDING_RESUME;
        }
    }

    /**创建显示的texture*/
    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public  void useTexParameter(){
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }


}
