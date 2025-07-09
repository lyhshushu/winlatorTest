package com.example.myapplication.zip;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月08日 17:37
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public interface UnzipProgressListener {
    void onStart(int totalFiles);

    void onProgress(int currentIndex, String fileName);

    void onFinish();

    void onError(Exception e);
}
