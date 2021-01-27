package com.czt.filter.gpufilter.helper;


import com.czt.filter.gpufilter.basefilter.GPUImageFilter;
import com.czt.filter.gpufilter.filter.MagicCoolFilter;
import com.czt.filter.gpufilter.filter.MagicCrayonFilter;
import com.czt.filter.gpufilter.filter.MagicEmeraldFilter;
import com.czt.filter.gpufilter.filter.MagicEvergreenFilter;
import com.czt.filter.gpufilter.filter.MagicFairytaleFilter;
import com.czt.filter.gpufilter.filter.MagicInkwellFilter;
import com.czt.filter.gpufilter.filter.MagicKevinFilter;
import com.czt.filter.gpufilter.filter.MagicNashvilleFilter;
import com.czt.filter.gpufilter.filter.MagicPixarFilter;
import com.czt.filter.gpufilter.filter.MagicRomanceFilter;
import com.czt.filter.gpufilter.filter.MagicSketchFilter;
import com.czt.filter.gpufilter.filter.MagicTenderFilter;
import com.czt.filter.gpufilter.filter.MagicValenciaFilter;
import com.czt.filter.gpufilter.filter.MagicWarmFilter;

public class MagicFilterFactory {

    public static GPUImageFilter initFilters(String type) {
        switch (type) {
            case "默认":
                return new GPUImageFilter();
            case "童话":
                return new MagicFairytaleFilter();
            case "冰冷":
                return new MagicCoolFilter();
            case "炎热":
                return new MagicKevinFilter();
            case "浪漫":
                return new MagicRomanceFilter();
            case "温和":
                return new MagicTenderFilter();
            case "祖母绿":
                return new MagicEmeraldFilter();
            case "温暖":
                return new MagicWarmFilter();
            case "常青":
                return new MagicEvergreenFilter();
            case "日系":
                return new MagicNashvilleFilter();
            case "清凉":
                return new MagicPixarFilter();
            case "薰衣草":
                return new MagicValenciaFilter();
            case "黑白":
                return new MagicInkwellFilter();
            case "素描":
                return new MagicSketchFilter();
            case "蜡笔":
                return new MagicCrayonFilter();
            default:
                return new GPUImageFilter();
        }
    }

}
