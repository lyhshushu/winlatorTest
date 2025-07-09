package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.winlator.preference.PreferenceManager;

import com.example.lib_winlator.R;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DefaultVersion;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.GPUInformation;
import com.winlator.core.KeyValueSet;
import com.winlator.core.OnExtractFileListener;
import com.winlator.core.ProcessHelper;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineInfo;
import com.winlator.core.WineRegistryEditor;
import com.winlator.core.WineStartMenuCreator;
import com.winlator.core.WineThemeManager;
import com.winlator.core.WineUtils;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.renderer.GLRenderer;
import com.winlator.shortcutview.ShortCutMgr;
import com.winlator.shortcutview.ShortcutBtn;
import com.winlator.widget.FrameRating;
import com.winlator.widget.InputControlsView;
import com.winlator.widget.TouchpadView;
import com.winlator.widget.XServerView;
import com.winlator.winhandler.IWinHandlerActivity;
import com.winlator.winhandler.WinHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.XEnvironment;
import com.winlator.xenvironment.components.ALSAServerComponent;
import com.winlator.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.xenvironment.components.NetworkInfoUpdateComponent;
import com.winlator.xenvironment.components.PulseAudioComponent;
import com.winlator.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.xenvironment.components.VirGLRendererComponent;
import com.winlator.xenvironment.components.XServerComponent;
import com.winlator.xserver.Property;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class XServiceTestActivity extends Activity implements IWinHandlerActivity {


    //虚拟机配置信息sp
    private SharedPreferences preferences;

    //虚拟系统管理
    private ImageFs imageFs;

    //虚拟机输入管理类
    private InputControlsManager inputControlsManager;

    //xServer主要类 通信虚拟机
    private XServer xServer;

    //虚拟机view
    private XServerView xServerView;

    private FrameRating frameRating;

    private int frameRatingWindowId = -1;

    //winHandler win虚拟机时间监听,生命周期管理
    private final WinHandler winHandler = new WinHandler(this);

    //光标速度
    private float globalCursorSpeed = 1.0f;

    //触摸板试图
    private TouchpadView touchpadView;

    //页面view
    private LinearLayout drawerLayout;

    //虚拟机输入控制view
    private InputControlsView inputControlsView;

    //环境参数
    private final EnvVars envVars = new EnvVars();

    //环境对象，保存启动参数等
    private XEnvironment environment;

    //音频引擎，不同类型需不同处理
    private String audioDriver = Container.DEFAULT_AUDIO_DRIVER;

    //图形驱动引擎
    private String graphicsDriver = Container.DEFAULT_GRAPHICS_DRIVER;

    //虚拟机dx类型
    private String dxwrapper = Container.DEFAULT_DXWRAPPER;

    //dx类型config
    private KeyValueSet dxwrapperConfig;

    //container管理类
    private ContainerManager containerManager;

    //虚拟机信息类
    private Container container;

    //记录cpulist方式
    private short taskAffinityMask = 0;

    //记录cpulist方式 64
    private short taskAffinityMaskWoW64 = 0;

    private boolean firstTimeBoot = false;

    //wine信息
    private WineInfo wineInfo;

    //dxwrapper 文件提取监听
    private OnExtractFileListener onExtractFileListener;

    private ShortCutMgr shortCutMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //屏幕全屏+常亮功能
        AppUtils.hideSystemUI(this);
        AppUtils.keepScreenOn(this);
        setContentView(R.layout.activity_xservice_test);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        drawerLayout = findViewById(R.id.testDrawLayout);
        drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> windowInsets.replaceSystemWindowInsets(0, 0, 0, 0));
//        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        imageFs = ImageFs.find(this);
        String screenSize = Container.DEFAULT_SCREEN_SIZE;

        //初始化 container
        //isGenerateWineprefix 是否需要修复处理 demo暂时移除盖逻辑
        containerManager = new ContainerManager(this);
        container = containerManager.getContainerById(getIntent().getIntExtra("container_id", 0));
        containerManager.activateContainer(container);

        taskAffinityMask = (short) ProcessHelper.getAffinityMask(container.getCPUList(true));
        taskAffinityMaskWoW64 = (short)ProcessHelper.getAffinityMask(container.getCPUListWoW64(true));
        firstTimeBoot = container.getExtra("appVersion").isEmpty();

        String wineVersion = container.getWineVersion();
        wineInfo = WineInfo.fromIdentifier(this, wineVersion);

        if (wineInfo != WineInfo.MAIN_WINE_VERSION) imageFs.setWinePath(wineInfo.path);

        graphicsDriver = container.getGraphicsDriver();
        audioDriver = container.getAudioDriver();
        dxwrapper = container.getDXWrapper();
        String dxwrapperConfig = container.getDXWrapperConfig();
        screenSize = container.getScreenSize();

        if (dxwrapper.equals("dxvk")) this.dxwrapperConfig = parseConfig(dxwrapperConfig);
        if (!wineInfo.isWin64()) {
            onExtractFileListener = (file, size) -> {
                String path = file.getPath();
                if (path.contains("system32/")) return null;
                return new File(path.replace("syswow64/", "system32/"));
            };
        }


        inputControlsManager = new InputControlsManager(this);
        xServer = new XServer(new ScreenInfo(screenSize));
        xServer.setWinHandler(winHandler);
        boolean[] winStarted = {false};
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onUpdateWindowContent(Window window) {
                if (!winStarted[0] && window.isApplicationWindow()) {
                    xServerView.getRenderer().setCursorVisible(true);
                    winStarted[0] = true;
                }

                if (window.id == frameRatingWindowId) frameRating.update();
            }

            @Override
            public void onModifyWindowProperty(Window window, Property property) {
                changeFrameRatingVisibility(window, property);
            }

            @Override
            public void onMapWindow(Window window) {
                assignTaskAffinity(window);
            }

            @Override
            public void onUnmapWindow(Window window) {
                changeFrameRatingVisibility(window, null);
            }
        });

        //UI设置
        setupUI();

        Executors.newSingleThreadExecutor().execute(() -> {
            setupWineSystemFiles();
            extractGraphicsDriverFiles();
            changeWineAudioDriver();

            setupXEnvironment();
        });

        shortCutMgr = new ShortCutMgr();
        shortCutMgr.addShortCut(this);

    }

    private void assignTaskAffinity(Window window) {
        if (taskAffinityMask == 0) return;
        int processId = window.getProcessId();
        String className = window.getClassName();
        int processAffinity = window.isWoW64() ? taskAffinityMaskWoW64 : taskAffinityMask;

        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity);
        }
        else if (!className.isEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity);
        }
    }

    private void setupWineSystemFiles() {
        String appVersion = String.valueOf(AppUtils.getVersionCode(this));
        String imgVersion = String.valueOf(imageFs.getVersion());
        boolean containerDataChanged = false;

        if (!container.getExtra("appVersion").equals(appVersion) || !container.getExtra("imgVersion").equals(imgVersion)) {
            applyGeneralPatches(container);
            container.putExtra("appVersion", appVersion);
            container.putExtra("imgVersion", imgVersion);
            containerDataChanged = true;
        }

        String dxwrapper = this.dxwrapper;
        if (dxwrapper.equals("dxvk")) dxwrapper = "dxvk-"+dxwrapperConfig.get("version");

        if (!dxwrapper.equals(container.getExtra("dxwrapper"))) {
            extractDXWrapperFiles(dxwrapper);
            container.putExtra("dxwrapper", dxwrapper);
            containerDataChanged = true;
        }

        if (dxwrapper.equals("cnc-ddraw")) envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini");

        String wincomponents = container.getWinComponents();
        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            extractWinComponentFiles();
            container.putExtra("wincomponents", wincomponents);
            containerDataChanged = true;
        }

        String desktopTheme = container.getDesktopTheme();
        if (!(desktopTheme+","+xServer.screenInfo).equals(container.getExtra("desktopTheme"))) {
            WineThemeManager.apply(this, new WineThemeManager.ThemeInfo(desktopTheme), xServer.screenInfo);
            container.putExtra("desktopTheme", desktopTheme+","+xServer.screenInfo);
            containerDataChanged = true;
        }

        WineStartMenuCreator.create(this, container);
        WineUtils.createDosdevicesSymlinks(container);

        String startupSelection = String.valueOf(container.getStartupSelection());
        if (!startupSelection.equals(container.getExtra("startupSelection"))) {
            WineUtils.changeServicesStatus(container, container.getStartupSelection() != Container.STARTUP_SELECTION_NORMAL);
            container.putExtra("startupSelection", startupSelection);
            containerDataChanged = true;
        }

        if (containerDataChanged) container.saveData();
    }

    private void extractGraphicsDriverFiles() {
        String cacheId = graphicsDriver;
        if (graphicsDriver.equals("turnip")) {
            cacheId += "-"+DefaultVersion.TURNIP+"-"+DefaultVersion.ZINK;
        }
        else if (graphicsDriver.equals("virgl")) {
            cacheId += "-"+DefaultVersion.VIRGL;
        }

        boolean changed = !cacheId.equals(container.getExtra("graphicsDriver"));
        File rootDir = imageFs.getRootDir();

        if (changed) {
            FileUtils.delete(new File(imageFs.getLib32Dir(), "libvulkan_freedreno.so"));
            FileUtils.delete(new File(imageFs.getLib64Dir(), "libvulkan_freedreno.so"));
            FileUtils.delete(new File(imageFs.getLib32Dir(), "libGL.so.1.7.0"));
            FileUtils.delete(new File(imageFs.getLib64Dir(), "libGL.so.1.7.0"));
            container.putExtra("graphicsDriver", cacheId);
            container.saveData();
        }

        if (graphicsDriver.equals("turnip")) {
            if (dxwrapper.equals("dxvk")) {
                setEnvVars(this, dxwrapperConfig, envVars);
            }
            else if (dxwrapper.equals("vkd3d")) envVars.put("VKD3D_FEATURE_LEVEL", "12_1");

            envVars.put("GALLIUM_DRIVER", "zink");
            envVars.put("TU_OVERRIDE_HEAP_SIZE", "4096");
            if (!envVars.has("MESA_VK_WSI_PRESENT_MODE")) envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
            envVars.put("vblank_mode", "0");

            if (!GPUInformation.isAdreno6xx(this)) {
                EnvVars userEnvVars = new EnvVars(container.getEnvVars());
                String tuDebug = userEnvVars.get("TU_DEBUG");
                if (!tuDebug.contains("sysmem")) userEnvVars.put("TU_DEBUG", (!tuDebug.isEmpty() ? tuDebug+"," : "")+"sysmem");
                container.setEnvVars(userEnvVars.toString());
            }

            boolean useDRI3 = preferences.getBoolean("use_dri3", true);
            if (!useDRI3) {
                envVars.put("MESA_VK_WSI_PRESENT_MODE", "immediate");
                envVars.put("MESA_VK_WSI_DEBUG", "sw");
            }

            if (changed) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/turnip-"+DefaultVersion.TURNIP+".tzst", rootDir);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/zink-"+DefaultVersion.ZINK+".tzst", rootDir);
            }
        }
        else if (graphicsDriver.equals("virgl")) {
            envVars.put("GALLIUM_DRIVER", "virpipe");
            envVars.put("VIRGL_NO_READBACK", "true");
            envVars.put("VIRGL_SERVER_PATH", UnixSocketConfig.VIRGL_SERVER_PATH);
            envVars.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_vertex_array_bgra");
            envVars.put("MESA_GL_VERSION_OVERRIDE", "3.1");
            envVars.put("vblank_mode", "0");
            if (changed) TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/virgl-"+DefaultVersion.VIRGL+".tzst", rootDir);
        }
    }

    private void changeWineAudioDriver() {
        if (!audioDriver.equals(container.getExtra("audioDriver"))) {
            File rootDir = imageFs.getRootDir();
            File userRegFile = new File(rootDir, ImageFs.WINEPREFIX+"/user.reg");
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                if (audioDriver.equals("alsa")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa");
                }
                else if (audioDriver.equals("pulseaudio")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse");
                }
            }
            container.putExtra("audioDriver", audioDriver);
            container.saveData();
        }
    }

    private void setupXEnvironment(){
        envVars.put("MESA_DEBUG", "silent");
        envVars.put("MESA_NO_ERROR", "1");
        envVars.put("WINEPREFIX", ImageFs.WINEPREFIX);
        //wineDebug 配置
//        boolean enableWineDebug = preferences.getBoolean("enable_wine_debug", false);
//        String wineDebugChannels = preferences.getString("wine_debug_channels", SettingsFragment.DEFAULT_WINE_DEBUG_CHANNELS);
//        envVars.put("WINEDEBUG", enableWineDebug && !wineDebugChannels.isEmpty() ? "+"+wineDebugChannels.replace(",", ",+") : "-all");

        String rootPath = imageFs.getRootDir().getPath();
        FileUtils.clear(imageFs.getTmpDir());

        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();

        if (container.getStartupSelection() == Container.STARTUP_SELECTION_AGGRESSIVE) winHandler.killProcess("services.exe");

        boolean wow64Mode = container.isWoW64Mode();
        String guestExecutable = wineInfo.getExecutable(this, wow64Mode)+" explorer /desktop=shell,"+xServer.screenInfo+" "+getWineStartCommand();
        guestProgramLauncherComponent.setWoW64Mode(wow64Mode);
        guestProgramLauncherComponent.setGuestExecutable(guestExecutable);

        envVars.putAll(container.getEnvVars());
        if (!envVars.has("WINEESYNC")) envVars.put("WINEESYNC", "1");

        ArrayList<String> bindingPaths = new ArrayList<>();
        for (String[] drive : container.drivesIterator()) bindingPaths.add(drive[1]);
        guestProgramLauncherComponent.setBindingPaths(bindingPaths.toArray(new String[0]));
        guestProgramLauncherComponent.setBox86Preset(container.getBox86Preset());
        guestProgramLauncherComponent.setBox64Preset(container.getBox64Preset());


        environment = new XEnvironment(this, imageFs);
        environment.addComponent(new SysVSharedMemoryComponent(xServer, UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH)));
        environment.addComponent(new XServerComponent(xServer, UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.XSERVER_PATH)));
        environment.addComponent(new NetworkInfoUpdateComponent());

        if (audioDriver.equals("alsa")) {
            envVars.put("ANDROID_ALSA_SERVER", UnixSocketConfig.ALSA_SERVER_PATH);
            envVars.put("ANDROID_ASERVER_USE_SHM", "true");
            environment.addComponent(new ALSAServerComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.ALSA_SERVER_PATH)));
        }
        else if (audioDriver.equals("pulseaudio")) {
            envVars.put("PULSE_SERVER", UnixSocketConfig.PULSE_SERVER_PATH);
            environment.addComponent(new PulseAudioComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.PULSE_SERVER_PATH)));
        }

        if (graphicsDriver.equals("virgl")) {
            environment.addComponent(new VirGLRendererComponent(xServer, UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH)));
        }

        guestProgramLauncherComponent.setEnvVars(envVars);
        guestProgramLauncherComponent.setTerminationCallback((status) -> exit());
        environment.addComponent(guestProgramLauncherComponent);

        environment.startEnvironmentComponents();

        winHandler.start();
        envVars.clear();
        dxwrapperConfig = null;

    }

    private void setupUI() {
        FrameLayout rootView = findViewById(R.id.fLXServerTest);
        xServerView = new XServerView(this, xServer);
        final GLRenderer renderer = xServerView.getRenderer();
        renderer.setCursorVisible(false);

        xServer.setRenderer(renderer);
        rootView.addView(xServerView);

        globalCursorSpeed = preferences.getFloat("cursor_speed", 1.0f);
        touchpadView = new TouchpadView(this, xServer);
        touchpadView.setSensitivity(globalCursorSpeed);
//        touchpadView.setFourFingersTapCallback(() -> {
//            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START);
//        });
        rootView.addView(touchpadView);

        inputControlsView = new InputControlsView(this);
        inputControlsView.setOverlayOpacity(preferences.getFloat("overlay_opacity", InputControlsView.DEFAULT_OVERLAY_OPACITY));
        inputControlsView.setTouchpadView(touchpadView);
        inputControlsView.setXServer(xServer);
        inputControlsView.setVisibility(View.GONE);
        rootView.addView(inputControlsView);
        AppUtils.observeSoftKeyboardVisibility(drawerLayout, renderer::setScreenOffsetYRelativeToCursor);
    }

    private void extractWinComponentFiles() {
        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");
        File systemRegFile = new File(rootDir, ImageFs.WINEPREFIX+"/system.reg");

        try {
            JSONObject wincomponentsJSONObject = new JSONObject(FileUtils.readString(this, "wincomponents/wincomponents.json"));
            ArrayList<String> dlls = new ArrayList<>();
            String wincomponents = container.getWinComponents();

            if (firstTimeBoot) {
                for (String[] wincomponent : new KeyValueSet(wincomponents)) {
                    JSONArray dlnames = wincomponentsJSONObject.getJSONArray(wincomponent[0]);
                    for (int i = 0; i < dlnames.length(); i++) {
                        String dlname = dlnames.getString(i);
                        dlls.add(!dlname.endsWith(".exe") ? dlname+".dll" : dlname);
                    }
                }

                cloneOriginalDllFiles(dlls.toArray(new String[0]));
                dlls.clear();
            }

            Iterator<String[]> oldWinComponentsIter = new KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator();

            for (String[] wincomponent : new KeyValueSet(wincomponents)) {
                if (wincomponent[1].equals(oldWinComponentsIter.next()[1])) continue;
                String identifier = wincomponent[0];
                boolean useNative = wincomponent[1].equals("1");

                if (useNative) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "wincomponents/"+identifier+".tzst", windowsDir, onExtractFileListener);
                }
                else {
                    JSONArray dlnames = wincomponentsJSONObject.getJSONArray(identifier);
                    for (int i = 0; i < dlnames.length(); i++) {
                        String dlname = dlnames.getString(i);
                        dlls.add(!dlname.endsWith(".exe") ? dlname+".dll" : dlname);
                    }
                }

                WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative);
            }

            if (!dlls.isEmpty()) restoreOriginalDllFiles(dlls.toArray(new String[0]));
            WineUtils.overrideWinComponentDlls(this, container, wincomponents);
        }
        catch (JSONException e) {}
    }

    private void exit() {
        winHandler.stop();
        if (environment != null) environment.stopEnvironmentComponents();
        //todo why restart
        // AppUtils.restartApplication(this);
    }

    private void applyGeneralPatches(Container container) {
        File rootDir = imageFs.getRootDir();
        FileUtils.delete(new File(rootDir, "/opt/apps"));
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "imagefs_patches.tzst", rootDir, onExtractFileListener);
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "pulseaudio.tzst", new File(getFilesDir(), "pulseaudio"));
        WineUtils.applySystemTweaks(this, wineInfo);
        container.putExtra("graphicsDriver", null);
        container.putExtra("desktopTheme", null);
//        SettingsFragment.resetBox86_64Version(this);
    }

    private void extractDXWrapperFiles(String dxwrapper) {
        final String[] dlls = {"d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll", "d3d12core.dll", "d3d8.dll", "d3d9.dll", "dxgi.dll", "ddraw.dll"};
        if (firstTimeBoot && !dxwrapper.equals("vkd3d")) cloneOriginalDllFiles(dlls);
        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");

        switch (dxwrapper) {
            case "wined3d":
                restoreOriginalDllFiles(dlls);
                break;
            case "cnc-ddraw":
                restoreOriginalDllFiles(dlls);
                final String assetDir = "dxwrapper/cnc-ddraw-"+ DefaultVersion.CNC_DDRAW;
                File configFile = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/ProgramData/cnc-ddraw/ddraw.ini");
                if (!configFile.isFile()) FileUtils.copy(this, assetDir+"/ddraw.ini", configFile);
                File shadersDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/ProgramData/cnc-ddraw/Shaders");
                FileUtils.delete(shadersDir);
                FileUtils.copy(this, assetDir+"/Shaders", shadersDir);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, assetDir+"/ddraw.tzst", windowsDir, onExtractFileListener);
                break;
            case "vkd3d":
                String[] dxvkVersions = getResources().getStringArray(R.array.dxvk_version_entries);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/dxvk-"+(dxvkVersions[dxvkVersions.length-1])+".tzst", windowsDir, onExtractFileListener);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/vkd3d-"+DefaultVersion.VKD3D+".tzst", windowsDir, onExtractFileListener);
                break;
            default:
                restoreOriginalDllFiles("d3d12.dll", "d3d12core.dll", "ddraw.dll");
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/"+dxwrapper+".tzst", windowsDir, onExtractFileListener);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/d8vk-"+DefaultVersion.D8VK+".tzst", windowsDir, onExtractFileListener);
                break;
        }
    }

    private void restoreOriginalDllFiles(final String... dlls) {
        File rootDir = imageFs.getRootDir();
        File cacheDir = new File(rootDir, ImageFs.CACHE_PATH+"/original_dlls");
        if (cacheDir.isDirectory()) {
            File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");
            String[] dirnames = cacheDir.list();
            int filesCopied = 0;

            for (String dll : dlls) {
                boolean success = false;
                for (String dirname : dirnames) {
                    File srcFile = new File(cacheDir, dirname+"/"+dll);
                    File dstFile = new File(windowsDir, dirname+"/"+dll);
                    if (FileUtils.copy(srcFile, dstFile)) success = true;
                }
                if (success) filesCopied++;
            }

            if (filesCopied == dlls.length) return;
        }

        containerManager.extractContainerPatternFile(container.getWineVersion(), container.getRootDir(), (file, size) -> {
            String path = file.getPath();
            if (path.contains("system32/") || path.contains("syswow64/")) {
                for (String dll : dlls) {
                    if (path.endsWith("system32/"+dll) || path.endsWith("syswow64/"+dll)) return file;
                }
            }
            return null;
        });

        cloneOriginalDllFiles(dlls);
    }

    private void cloneOriginalDllFiles(final String... dlls) {
        File rootDir = imageFs.getRootDir();
        File cacheDir = new File(rootDir, ImageFs.CACHE_PATH+"/original_dlls");
        if (!cacheDir.isDirectory()) cacheDir.mkdirs();
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");
        String[] dirnames = {"system32", "syswow64"};

        for (String dll : dlls) {
            for (String dirname : dirnames) {
                File dllFile = new File(windowsDir, dirname+"/"+dll);
                if (dllFile.isFile()) FileUtils.copy(dllFile, new File(cacheDir, dirname+"/"+dll));
            }
        }
    }

    private String getWineStartCommand() {
        File tempDir = new File(container.getRootDir(), ".wine/drive_c/windows/temp");
        FileUtils.clear(tempDir);

        String args = "";
        String gamePath=getIntent().getStringExtra("gameFileName");
        if (gamePath != null) {
            this.xServerView.getRenderer().setUnviewableWMClasses("explorer.exe");
            String exeDir = "C:\\Program Files (x86)\\pvzHE";
            String filename = "PlantsVsZombies.exe";
            args += "/dir " + exeDir.replace(" ", "\\ ") + " \"" + filename + "\"";
        }else {
            args += "\"wfm.exe\"";
        }
        return "winhandler.exe " + args;
    }

    //窗口属性修改
    private void changeFrameRatingVisibility(Window window, Property property) {
        if (frameRating == null) return;
        if (property != null) {
            if (frameRatingWindowId == -1 && window.attributes.isMapped() && property.nameAsString().equals("_MESA_DRV")) {
                frameRatingWindowId = window.id;
            }
        } else if (window.id == frameRatingWindowId) {
            frameRatingWindowId = -1;
            runOnUiThread(() -> frameRating.setVisibility(View.GONE));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (environment != null) {
            xServerView.onResume();
            environment.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (environment != null) {
            environment.onPause();
            xServerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        winHandler.stop();
        if (environment != null) environment.stopEnvironmentComponents();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
//        if (environment != null) {
//            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
//                drawerLayout.openDrawer(GravityCompat.START);
//            } else drawerLayout.closeDrawers();
//        }
        finish();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return !winHandler.onGenericMotionEvent(event) && !touchpadView.onExternalMouseEvent(event) && super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (!inputControlsView.onKeyEvent(event) && !winHandler.onKeyEvent(event) && xServer.keyboard.onKeyEvent(event)) ||
                (!ExternalController.isGameController(event.getDevice()) && super.dispatchKeyEvent(event));
    }

    @Override
    public InputControlsView getInputControlsView() {
        return inputControlsView;
    }

    @Override
    public XServer getXServer() {
        return xServer;
    }

    @Override
    public XServerView getXServerView() {
        return xServerView;
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        envVars.put("DXVK_STATE_CACHE_PATH", ImageFs.CACHE_PATH);
        envVars.put("DXVK_LOG_LEVEL", "none");

        File rootDir = ImageFs.find(context).getRootDir();
        File dxvkConfigFile = new File(rootDir, ImageFs.CONFIG_PATH+"/dxvk.conf");

        String content = "";
        String maxDeviceMemory = config.get("maxDeviceMemory");
        if (!maxDeviceMemory.isEmpty() && !maxDeviceMemory.equals("0")) {
            content += "dxgi.maxDeviceMemory = "+maxDeviceMemory+"\n";
            content += "dxgi.maxSharedMemory = "+maxDeviceMemory+"\n";
        }

        String framerate = config.get("framerate");
        if (!framerate.isEmpty() && !framerate.equals("0")) {
            content += "dxgi.maxFrameRate = "+framerate+"\n";
            content += "d3d9.maxFrameRate = "+framerate+"\n";
        }

        FileUtils.delete(dxvkConfigFile);
        if (!content.isEmpty() && FileUtils.writeString(dxvkConfigFile, content)) {
            envVars.put("DXVK_CONFIG_FILE", ImageFs.CONFIG_PATH+"/dxvk.conf");
        }
    }

    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() : "version="+DefaultVersion.DXVK+",framerate=0,maxDeviceMemory=0";
        return new KeyValueSet(data);
    }
}