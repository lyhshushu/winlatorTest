package com.winlator.xutil;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月07日 17:49
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class ColorUtils {
    public static int setAlphaComponent(int color, int alpha) {
        // 清除原有的 alpha 分量（前 8 位），并设置新的 alpha
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
