package com.czt.filter.gpufilter.filter;

import android.opengl.GLES20;

import com.czt.filter.R;
import com.czt.filter.gpufilter.basefilter.GPUImageFilter;
import com.czt.filter.gpufilter.utils.OpenGlUtils;

/**
 * 蜡笔
 */
public class MagicCrayonFilter extends GPUImageFilter {
	
	private int mSingleStepOffsetLocation;
	//1.0 - 5.0
	private int mStrengthLocation;
	
	public MagicCrayonFilter(){
		super(NO_FILTER_VERTEX_SHADER, OpenGlUtils.readShaderFromRawResource(R.raw.filter_crayon));
	}
	
	protected void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mStrengthLocation = GLES20.glGetUniformLocation(getProgram(), "strength");
        setFloat(mStrengthLocation, 2.0f);
    }
    
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onInitialized(){
        super.onInitialized();
        setFloat(mStrengthLocation, 0.5f);
    }

    private void setTexelSize(final float w, final float h) {
		setFloatVec2(mSingleStepOffsetLocation, new float[] {1.0f / w, 1.0f / h});
	}
	
	@Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }
}
