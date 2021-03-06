package com.example.ibeacondemo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.widget.TextView;

import com.example.ibeacondemo.aop.Log;
import com.example.ibeacondemo.manager.ActivityManager;
import com.example.ibeacondemo.model.RequestHandler;
import com.example.ibeacondemo.other.AppConfig;
import com.example.ibeacondemo.other.DebugLoggerTree;
import com.example.ibeacondemo.server.RequestServer;
import com.hjq.bar.TitleBar;
import com.hjq.bar.initializer.LightBarInitializer;
import com.hjq.gson.factory.GsonFactory;
import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestInterceptor;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;
import com.hjq.toast.ToastInterceptor;
import com.hjq.toast.ToastUtils;
import com.hjq.toast.style.ToastBlackStyle;
import com.tamsiree.rxkit.RxTool;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class ApplicationApp extends Application {
    @Log("启动耗时")
    @Override
    public void onCreate() {
        super.onCreate();
        RxTool.init(this);
        initSdk(this);




    }


    public static void initSdk(Application application) {

        //设置启动延时为1s（默认延时3s），APP启动1s后初始化SDK，避免影响APP启动速度;
        Beta.initDelay = 1 * 1000;

            // 初始化吐司
        ToastUtils.init(application, new ToastBlackStyle(application) {

            @Override
            public int getCornerRadius() {
                return (int) application.getResources().getDimension(R.dimen.button_round_size);
            }
        });

        // 设置 Toast 拦截器
        ToastUtils.setToastInterceptor(new ToastInterceptor());

        // 设置标题栏初始化器
        TitleBar.setDefaultInitializer(new LightBarInitializer() {

            @Override
            public Drawable getBackgroundDrawable(Context context) {
                return new ColorDrawable(ContextCompat.getColor(application, R.color.common_primary_color));
            }

            @Override
            public Drawable getBackIcon(Context context) {
                return ContextCompat.getDrawable(context, R.drawable.arrows_left_ic);
            }

            @Override
            protected TextView createTextView(Context context) {
                return new AppCompatTextView(context);
            }
        });

        //***** 统一初始化Bugly产品，包含Beta *****/
        ActivityManager.getInstance().init(application);

        // 初始化日志打印
        if (AppConfig.isLogEnable()) {
            Timber.plant(new DebugLoggerTree());
        }

        // Bugly 异常捕捉
//        CrashReport.initCrashReport(application, AppConfig.getBuglyId(), AppConfig.isBuglyEnable());
        Bugly.init(application, AppConfig.getBuglyId(), AppConfig.isBuglyEnable());

        // 网络请求框架初始化
        IRequestServer server = new RequestServer();
        EasyConfig.with(new OkHttpClient())
                // 是否打印日志
                .setLogEnabled(AppConfig.isLogEnable())
                // 设置服务器配置
                .setServer(server)
                // 设置请求处理策略
                .setHandler(new RequestHandler(application))
                // 设置请求参数拦截器
                .setInterceptor(new IRequestInterceptor() {
                    @Override
                    public void interceptArguments(IRequestApi api, HttpParams params, HttpHeaders headers) {
                        headers.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    }
                })
                // 设置请求重试次数
                .setRetryCount(1)
                // 设置请求重试时间
                .setRetryTime(2000)
                // 添加全局请求参数
//                .addParam("token", "6666666")
                // 添加全局请求头
                //.addHeader("time", "20191030")
                .into();


        // 设置 Json 解析容错监听
        GsonFactory.setJsonCallback((typeToken, fieldName, jsonToken) -> {
            // 上报到 Bugly 错误列表
            CrashReport.postCatchedException(new IllegalArgumentException(
                    "类型解析异常：" + typeToken + "#" + fieldName + "，后台返回的类型为：" + jsonToken));
        });


        // 注册网络状态变化监听
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(application, ConnectivityManager.class);
        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    Activity topActivity = ActivityManager.getInstance().getTopActivity();
                    if (!(topActivity instanceof LifecycleOwner)) {
                        return;
                    }

                    LifecycleOwner lifecycleOwner = ((LifecycleOwner) topActivity);
                    if (lifecycleOwner.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
                        return;
                    }

                    ToastUtils.show(R.string.common_network_error);
                }
            });
        }


    }
}
