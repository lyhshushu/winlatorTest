# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.winlator.WinlatorManager{*;}
-keep interface com.winlator.xenvironment.ImageFsInstallCallBack{*;}
-keep class com.winlator.container.Container{*;}
-keep interface com.winlator.core.Callback{*;}

-keep class com.github.luben.zstd.ZstdInputStreamNoFinalizer{*;}

-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.brotli.dec.BrotliInputStream

-keepclassmembers class com.winlator.xconnector.ClientSocket {
    public void addAncillaryFd(int);
}
-keepclassmembers class com.winlator.xconnector.XConnectorEpoll {
    private void handleNewConnection(int);
    private void handleExistingConnection(int);
}

-keepclassmembers class com.winlator.renderer.GPUImage {
    private void setStride(short);
}

-keepclassmembers class com.winlator.xenvironment.components.VirGLRendererComponent {
    private void killConnection(int);
    private long getSharedEGLContext();
    private void flushFrontbuffer(int,int);
}
