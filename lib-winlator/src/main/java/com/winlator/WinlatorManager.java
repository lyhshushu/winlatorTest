package com.winlator;

import android.content.Context;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.Callback;
import com.winlator.core.StringUtils;
import com.winlator.core.WineRegistryEditor;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.ImageFsInstallCallBack;
import com.winlator.xenvironment.ImageFsInstallManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月04日 16:28
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class WinlatorManager {

    private static volatile WinlatorManager INSTANCE;
    private Context hostContext;

    private ContainerManager containerManager;

    private WinlatorManager(Context context) {
        this.hostContext = context.getApplicationContext(); // 避免内存泄漏
    }

    public static void init(Context context) {
        getInstance(context);
    }

    public static WinlatorManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (WinlatorManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WinlatorManager(context);
                    INSTANCE.containerManager = new ContainerManager(context);
                }
            }
        }
        return INSTANCE;
    }


    public boolean imageFsIsInstalled() {
        return ImageFs.find(hostContext).isValid();
    }

    public ArrayList<Container> getContainers() {
        return containerManager.getContainers();
    }

    public void createContainerAsync(final JSONObject data, Callback<Container> callback) {
        Callback<Container> newCallback = (Callback<Container>) container -> {
            callback.call(container);
            saveWineRegistryKeys(container);
        };
        containerManager.createContainerAsync(data, newCallback);
    }

    public Container getContainerById(int id) {
        return containerManager.getContainerById(id);
    }

    private void saveWineRegistryKeys(Container container) {
        File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "csmt", 3);

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "NVIDIA GeForce RTX 3070");
                jsonObject.put("deviceID", 9373);
                jsonObject.put("vendorID", 4318);


                JSONObject gpuName = jsonObject;
                registryEditor.setDwordValue("Software\\Wine\\Direct3D", "VideoPciDeviceID", gpuName.getInt("deviceID"));
                registryEditor.setDwordValue("Software\\Wine\\Direct3D", "VideoPciVendorID", gpuName.getInt("vendorID"));
            } catch (JSONException e) {
            }

            registryEditor.setStringValue("Software\\Wine\\Direct3D", "OffScreenRenderingMode", "FBO".toLowerCase(Locale.ENGLISH));

            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "strict_shader_math", 1);


            registryEditor.setStringValue("Software\\Wine\\Direct3D", "VideoMemorySize", StringUtils.parseNumber("2048 MB"));

            registryEditor.setStringValue("Software\\Wine\\DirectInput", "MouseWarpOverride", "Disable".toLowerCase(Locale.ENGLISH));

            registryEditor.setStringValue("Software\\Wine\\Direct3D", "shader_backend", "glsl");
            registryEditor.setStringValue("Software\\Wine\\Direct3D", "UseGLSL", "enabled");
        }
    }

    public void imageFsInstallIsNeeded(Context context, ImageFsInstallCallBack callBack){
        ImageFsInstallManager.installIfNeeded(context,callBack);
    }

    /**
     * 软连接处理，只操作上次启动container
     */
    public String getDriveCPath() {
        File rootDir = new File(hostContext.getFilesDir(), "imagefs");
        File driveCFile = new File(rootDir, ImageFs.WINEPREFIX + "/drive_c");
        return driveCFile.getAbsolutePath();
    }

    /**
     * 真实文件目录处理
     */
    public String getDriveCPathByContainerId(int containerId) {
        File rootDir = new File(hostContext.getFilesDir(), "imagefs");
        File driveCFile = new File(rootDir, "/home/" + ImageFs.USER + "-" + containerId + "/.wine" + "/drive_c");
        return driveCFile.getAbsolutePath();
    }

}
