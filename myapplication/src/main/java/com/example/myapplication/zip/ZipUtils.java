package com.example.myapplication.zip;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月08日 17:18
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class ZipUtils {
    public static void unzipFromAssets(Context context, String assetZipName, File outputDir, UnzipProgressListener listener) {
        try (InputStream assetInput = context.getAssets().open(assetZipName);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(assetInput))) {

            // 预读取所有 entry 来统计文件总数
            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream copyBuffer = new ByteArrayOutputStream();
            int totalCount = 0;

            ZipEntry tmpEntry;
            ZipInputStream countStream = new ZipInputStream(context.getAssets().open(assetZipName));
            while ((tmpEntry = countStream.getNextEntry()) != null) {
                if (!tmpEntry.isDirectory()) totalCount++;
                countStream.closeEntry();
            }
            countStream.close();

            if (listener != null) listener.onStart(totalCount);

            ZipEntry entry;
            int currentIndex = 0;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(outputDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }

                    currentIndex++;
                    if (listener != null) {
                        listener.onProgress(currentIndex, entry.getName());
                    }
                }

                zis.closeEntry();
            }

            if (listener != null) listener.onFinish();

        } catch (IOException e) {
            if (listener != null) listener.onError(e);
        }
    }

}
