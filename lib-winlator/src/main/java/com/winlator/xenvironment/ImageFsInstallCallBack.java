package com.winlator.xenvironment;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月04日 11:29
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public interface ImageFsInstallCallBack {
    void onProgress(int progress);

    void onSuccess();

    void onFailed();
}
