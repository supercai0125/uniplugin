package io.dcloud.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import io.dcloud.auth.account.AccountHelper;
import io.dcloud.auth.activity1.KeepManager;
import io.dcloud.auth.guide.FloatWindowGuide;
import io.dcloud.auth.guide.IntentWrapper;
import io.dcloud.auth.jobscheduler.AliveJobService;
import io.dcloud.auth.service.ForegroundService;
import io.dcloud.auth.service.StickyService;
import io.dcloud.auth.whitelist.FcfrtAppBhUtils;
import io.dcloud.auth.workmanager.KeepLiveWork;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

/**
 * @author ercai caishiqi0125@163.com
 * @date 2020/9/15 - 14:24
 */
public class StUpgradeUtils extends WXModule {

    /**
     * 开启悬浮窗
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @JSMethod(uiThread = false)
    public void openFloatWindow(JSONObject options, JSCallback callback) throws ParseException {

        if (mWXSDKInstance.getContext() instanceof Activity) {
            // 检测是否授权
            if (!FloatWindowGuide.canDrawOverlays(mWXSDKInstance.getContext())) {
                // 跳转到悬浮窗权限设置页
                FloatWindowGuide.requestSettingCanDrawOverlays(mWXSDKInstance.getContext());
            }
        }
    }

    /**
     * 忽略电池优化
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @JSMethod(uiThread = false)
    public void ignoreBatteryOptimizations (JSONObject options, JSCallback callback) throws ParseException {

        JSONObject result = new JSONObject();
        if (mWXSDKInstance.getContext() instanceof Activity) {
            // 检测是否授权
            if (!FcfrtAppBhUtils.isIgnoringBatteryOptimizations(mWXSDKInstance.getContext())) {
                // 跳转到悬浮窗权限设置页
                FcfrtAppBhUtils.requestIgnoreBatteryOptimizations(mWXSDKInstance.getContext());
            } else {
                result.put("code", 200);
                result.put("msg", "请勿重复授权");
            }
            result.put("code", 200);
            callback.invoke(result);
        }
    }

    /**
     * 开启自启动
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @JSMethod(uiThread = false)
    public void openSlefRestart(JSONObject options, JSCallback callback) throws ParseException {

        JSONObject result = new JSONObject();
        String appName = options.getString("appName");
        if (appName == null) {
            appName = "应用";
        }
        if (mWXSDKInstance.getContext() instanceof Activity) {
            IntentWrapper.whiteListMatters(appName, mWXSDKInstance.getContext());
            result.put("code", 200);
            callback.invoke(result);
        }
    }

    @JSMethod(uiThread = true)
    public void openProtection(JSONObject options, JSCallback callback) {

        JSONObject result = new JSONObject();
        if (mWXSDKInstance != null && mWXSDKInstance.getContext() instanceof Activity) {
            //申请电池白名单
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!FcfrtAppBhUtils.isIgnoringBatteryOptimizations(mWXSDKInstance.getContext())) {
                    FcfrtAppBhUtils.requestIgnoreBatteryOptimizations(mWXSDKInstance.getContext());
                }
            }

            //1像素广播注册
            KeepManager.getInstance().registerKeep(mWXSDKInstance.getContext());

            //前台服务保活
            Intent foregroundServiceIntent = new Intent(mWXSDKInstance.getContext(), ForegroundService.class);
            mWXSDKInstance.getContext().startService(foregroundServiceIntent);

            //开启粘性服务进行保活
            Intent stickyServiceIntent = new Intent(mWXSDKInstance.getContext(), StickyService.class);
            mWXSDKInstance.getContext().startService(stickyServiceIntent);

            //使用JobScheduler进行保活
            AliveJobService.startJob(mWXSDKInstance.getContext());

            //利用WorkManager后台启动JobScheduler进行保活
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest
                    .Builder(KeepLiveWork.class)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();

            WorkManager.getInstance(mWXSDKInstance.getContext()).enqueue(oneTimeWorkRequest);

            // 账户拉活
            AccountHelper.addAccount(mWXSDKInstance.getContext());
            AccountHelper.autoSync();

            result.put("code", 200);
            callback.invoke(result);
        }
    }
}
