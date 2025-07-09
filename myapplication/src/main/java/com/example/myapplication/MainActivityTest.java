package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.zip.UnzipProgressListener;
import com.example.myapplication.zip.ZipUtils;
import com.winlator.WinlatorManager;
import com.winlator.XServiceTestActivity;
import com.winlator.xenvironment.ImageFsInstallCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivityTest extends Activity {

    private WinlatorManager winlatorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        Button installImgFs = (Button) findViewById(R.id.installImgFs);
        Button createContainer = (Button) findViewById(R.id.createContainer);
        Button defaultStart = (Button) findViewById(R.id.defaultStart);
        TextView imgsInfos = (TextView) findViewById(R.id.imgfsInfo);
        Button extraGame = (Button) findViewById(R.id.extraGame);
        Button startPVZ = (Button) findViewById(R.id.startPVZ);
        TextView gameInfo = (TextView) findViewById(R.id.gameInfo);


        StringBuilder builder = new StringBuilder();

        winlatorManager = WinlatorManager.getInstance(MainActivityTest.this);

        String driveDPath = winlatorManager.getDriveCPathByContainerId(1);
        File gameFile = new File(driveDPath, "Program Files (x86)/pvzHE/PlantsVsZombies.exe");

        if (winlatorManager.imageFsIsInstalled()) {
            builder.append("IMageFs已安装\n");
            if (!winlatorManager.getContainers().isEmpty()) {
                builder.append("container已完成创建\n");
            }
            imgsInfos.setText(builder.toString());
        }

        if (gameFile.exists()) {
            gameInfo.setText("Pvz游戏已安装");
        }

        installImgFs.setOnClickListener(v -> {
            if (winlatorManager.imageFsIsInstalled()) {
                Toast.makeText(MainActivityTest.this, "IMageFs已安装", Toast.LENGTH_LONG).show();
                return;
            }

            winlatorManager.imageFsInstallIsNeeded(MainActivityTest.this, new ImageFsInstallCallBack() {
                boolean imageFsIsInstalled = false;

                @Override
                public void onProgress(int progress) {
                    (new Handler(Looper.getMainLooper())).post(new Runnable() {
                        @Override
                        public void run() {
                            if (imageFsIsInstalled) {
                                return;
                            }
                            imgsInfos.setText("IMageFs安装中:" + progress + "%");
                        }
                    });
                }

                @Override
                public void onSuccess() {
                    imageFsIsInstalled = true;
                    imgsInfos.setText("IMageFs已安装\n");
                }

                @Override
                public void onFailed() {
                    imgsInfos.setText("IMageFs安装失败\n");
                }
            });
        });

        createContainer.setOnClickListener(v -> {
            if (!winlatorManager.imageFsIsInstalled()) {
                Toast.makeText(MainActivityTest.this, "请先安装ImageFS", Toast.LENGTH_LONG).show();
                return;
            }
            if (!winlatorManager.getContainers().isEmpty()) {
                Toast.makeText(MainActivityTest.this, "虚拟机container已创建", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                JSONObject data = new JSONObject();
                data.put("name", "Container-1");
                data.put("screenSize", "1280x720");
                data.put("envVars", "\"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 MESA_VK_WSI_PRESENT_MODE=mailbox TU_DEBUG=noconform,sysmem\"");
                data.put("cpuList", "0,1,2,3,4,5,6,7");
                data.put("cpuListWoW64", "4,5,6,7");
                data.put("graphicsDriver", "turnip");
                data.put("dxwrapper", "dxvk");
                data.put("dxwrapperConfig", "");
                data.put("audioDriver", "alsa");
                data.put("wincomponents", "direct3d=1,directsound=1,directmusic=0,directshow=0,directplay=0,vcrun2010=1,wmdecoder=1");
                data.put("drives", "D:/storage/emulated/0/DownloadE:/data/data/com.winlator/storage");
                data.put("showFPS", false);
                data.put("wow64Mode", true);
                data.put("startupSelection", 1);
                data.put("box86Preset", "COMPATIBILITY");
                data.put("box64Preset", "COMPATIBILITY");
                data.put("desktopTheme", "LIGHT,IMAGE,#0277BD,0");

//                if (wineInfos.size() > 1) {
//                    data.put("wineVersion", wineInfos.get(sWineVersion.getSelectedItemPosition()).identifier());
//                }


                winlatorManager.createContainerAsync(data, container -> {
                    if (container != null) {
                        Log.d("lyh_test", "container Id " + container.id);
                        imgsInfos.append("container已完成创建\n");
                    }
                });
            } catch (JSONException e) {
                Toast.makeText(MainActivityTest.this, "container create Error", Toast.LENGTH_LONG).show();
                Log.e("container", "create Error" + e);
            }
        });

        defaultStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityTest.this, XServiceTestActivity.class);
            if (winlatorManager.getContainerById(1) == null) {
                Toast.makeText(MainActivityTest.this, "默认container不存在", Toast.LENGTH_LONG).show();
                return;
            }

            intent.putExtra("container_id", 1);
            MainActivityTest.this.startActivity(intent);
        });

        extraGame.setOnClickListener(v -> {
            if (winlatorManager.getContainers().isEmpty()) {
                Toast.makeText(MainActivityTest.this, "请先创建container", Toast.LENGTH_LONG).show();
                return;
            }
            if (gameFile.exists()) {
                Toast.makeText(MainActivityTest.this, "游戏文件已存在", Toast.LENGTH_LONG).show();
                return;
            }


            new Thread(() -> {
                ZipUtils.unzipFromAssets(MainActivityTest.this, "game.zip", new File(driveDPath), new UnzipProgressListener() {
                    StringBuilder info = new StringBuilder();
                    int totalSize = 0;

                    @Override
                    public void onStart(int totalFiles) {
                        Log.d("Unzip", "开始解压，总文件数: " + totalFiles);
                        totalSize = totalFiles;
                        (new Handler(Looper.getMainLooper())).post(() -> {
                            info.append("开始解压，总文件数: " + totalFiles);
                            gameInfo.setText(info.toString());
                        });
                    }

                    @Override
                    public void onProgress(int currentIndex, String fileName) {
                        Log.d("Unzip", "正在解压第 " + currentIndex + " 个文件: " + fileName);
                        (new Handler(Looper.getMainLooper())).post(() -> {
                            gameInfo.setText(info.toString() + "\n正在解压第 " + currentIndex + " 个文件: " + fileName + currentIndex / totalSize + "%");
                        });
                    }

                    @Override
                    public void onFinish() {
                        Log.d("Unzip", "解压完成！");
                        (new Handler(Looper.getMainLooper())).post(() -> {
                            gameInfo.setText("解压完成！");
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        Log.e("Unzip", "解压出错: " + e.getMessage());
                        (new Handler(Looper.getMainLooper())).post(() -> {
                            gameInfo.setText("解压出错: " + e.getMessage());
                        });
                    }
                });
            }).start();
        });

        startPVZ.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityTest.this, XServiceTestActivity.class);
            if (winlatorManager.getContainerById(1) == null) {
                Toast.makeText(MainActivityTest.this, "默认container不存在", Toast.LENGTH_LONG).show();
                return;
            }

            intent.putExtra("container_id", 1);
            intent.putExtra("gameFileName", "C:\\Program Files (x86)\\pvzHE" + " \"" + "PlantsVsZombies.exe" + "\"");
            MainActivityTest.this.startActivity(intent);
        });

    }
}