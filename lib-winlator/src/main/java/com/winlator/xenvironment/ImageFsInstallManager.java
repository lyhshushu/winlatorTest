package com.winlator.xenvironment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.winlator.preference.PreferenceManager;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DefaultVersion;
import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineInfo;
import com.winlator.xenvironment.components.GuestProgramLauncherComponent;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月04日 11:18
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class ImageFsInstallManager {

    private static final byte LATEST_VERSION = 8;

    public static void installIfNeeded(Context context, ImageFsInstallCallBack callBack) {
        ImageFs imageFs = ImageFs.find(context);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) installFromAssets(context, callBack);
    }

    private static void installFromAssets(Context context, ImageFsInstallCallBack callBack) {
        ImageFs imageFs = ImageFs.find(context);
        final File rootDir = imageFs.getRootDir();
        GuestProgramLauncherComponent.resetBox86_64Version(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(rootDir);
            final byte compressionRatio = 22;
            final long contentLength = (long) (FileUtils.getSize(context, "imagefs.txz") * (100.0f / compressionRatio));
            AtomicLong totalSizeRef = new AtomicLong();

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, "imagefs.txz", rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = (int) (((float) totalSize / contentLength) * 100);
                    callBack.onProgress(progress);
                }
                return file;
            });

            if (success) {
                imageFs.createImgVersionFile(LATEST_VERSION);
                callBack.onSuccess();
                resetContainerImgVersions(context);
            } else callBack.onFailed();
        });
    }

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home") || name.equals("opt")) {
                            if (name.equals("opt")) clearOptDir(file);
                            continue;
                        }
                    }
                    FileUtils.delete(file);
                }
            }
        } else rootDir.mkdirs();
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("installed-wine")) continue;
                FileUtils.delete(file);
            }
        }
    }
}
