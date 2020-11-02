package ir.batna.otpreader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import java.util.Map;

import ir.batna.otpreader.utils.Constants;

public class SmsService extends JobIntentService {

    public static final String TAG = AppSignatureHelper.class.getSimpleName();
    private static final int JOB_ID = 55;
    Map<String, String> map;

    public static void enqueueWork(Context context, Intent intent) {
        Log.d("MBD", "job  Received! ");
        enqueueWork(context, SmsService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MBD", "job Service created! ");

    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        Log.d("MBD", "job Started! ");

        AppSignatureHelper helper = new AppSignatureHelper(this);
        map = helper.getAllAppSignature();

        String otpCode = intent.getStringExtra(Constants.CODE_KEY);
        String hashKey = intent.getStringExtra(Constants.HASH_KEY);

        Log.d("MBD", "hash is =" + hashKey);

        String packageName = getPackageNameByHashKey(hashKey);

        Log.d("MBD", "PackageName  =" + packageName + "  OTP =" + otpCode + "  hash =" + hashKey);

        if (otpCode != null && packageName != null) {
            sendBroadcast(this, otpCode, packageName, Constants.BATNA_LIBRARY_PACKAGE_NAME);
        } else {
            Log.d("MBD", "Package name or code not provided");
        }

    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MBD", "Service has destroyed");
    }

    private String getPackageNameByHashKey(String hashKey) {
        return map.get("[" + hashKey + "]");
    }

    private void sendBroadcast(Context context, String code, String packageName, String packageAndClass) {
        Log.d("MBD", "Broadcast send! ");

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, packageAndClass));
        intent.putExtra(Constants.CODE_KEY, code);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);

    }


}
