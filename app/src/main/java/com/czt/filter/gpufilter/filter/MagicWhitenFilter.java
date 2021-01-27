package com.czt.filter.gpufilter.filter;

import android.opengl.GLES20;

import com.czt.filter.OpenGL;
import com.czt.filter.R;
import com.czt.filter.gpufilter.basefilter.GPUImageFilter;
import com.czt.filter.gpufilter.utils.OpenGlUtils;

public class MagicWhitenFilter extends GPUImageFilter {

    private int[] inputTextureHandles = {-1,-1};
    private int[] inputTextureUniformLocations = {-1,-1};
    private int mGLStrengthLocation;
    private int mParamsLocation;
    private int mLevel;

    public MagicWhitenFilter(){
        super(NO_FILTER_VERTEX_SHADER, OpenGlUtils.readShaderFromRawResource(R.raw.filter_walden));
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(inputTextureHandles.length, inputTextureHandles, 0);
        for(int i = 0; i < inputTextureHandles.length; i++)
            inputTextureHandles[i] = -1;
    }

    protected void onDrawArraysAfter(){
        for(int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }

    protected void onDrawArraysPre(){
        for(int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3) );
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
            GLES20.glUniform1i(inputTextureUniformLocations[i], (i+3));
        }
    }

    public void onInit(){
        super.onInit();
        for(int i = 0; i < inputTextureUniformLocations.length; i++)
            inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture"+(2+i));
        mGLStrengthLocation = GLES20.glGetUniformLocation(mGLProgId, "strength");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
    }

    public void onInitialized(){
        super.onInitialized();
        setFloat(mGLStrengthLocation, 1.0f);
        runOnDraw(new Runnable(){
            public void run(){
                inputTextureHandles[0] = OpenGlUtils.loadTexture(OpenGL.context, "filter/filter_walden_map.png");
                inputTextureHandles[1] = OpenGlUtils.loadTexture(OpenGL.context, "filter/filter_vignette_map.png");
            }
        });
    }

    public void setWaldenLevel(int level){
        mLevel = level;
        switch (level) {
            case 1:
                setFloatVec4(mParamsLocation, new float[] {1.0f, 1.0f, 0.15f, 0.15f});
                break;
            case 2:
                setFloatVec4(mParamsLocation, new float[] {0.8f, 0.9f, 0.2f, 0.2f});
                break;
            case 3:
                setFloatVec4(mParamsLocation, new float[] {0.6f, 0.8f, 0.25f, 0.25f});
                break;
            case 4:
                setFloatVec4(mParamsLocation, new float[] {0.4f, 0.7f, 0.38f, 0.3f});
                break;
            case 5:
                setFloatVec4(mParamsLocation, new float[] {0.33f, 0.63f, 0.4f, 0.35f});
                break;
            default:
                break;
        }
    }

    public int getWaldenLevel(){
        return mLevel;
    }

}
