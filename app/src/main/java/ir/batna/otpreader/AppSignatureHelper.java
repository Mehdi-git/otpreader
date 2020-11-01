package ir.batna.otpreader;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mehdi-git on October 26,2020
 * This class usage is to get list of apps installed on phone package name and
 * store each package name with related hash key in packageNameAndHashKey file
 *
 */


public class AppSignatureHelper extends ContextWrapper {

    public static final String TAG = AppSignatureHelper.class.getName();
    private static final String HASH_TYPE = "SHA-256";
    public static final int NUM_HASHED_BYTES = 9;
    public static final int NUM_BASE64_CHAR = 11;
    private List<String> appList ;
    private Map<String,String> packageNameAndHashKey;


    public AppSignatureHelper(Context context) {
        super(context);
        getListOfInstalledApp();
    }

    /** To get all apps packageName ********************************************************/
    private void getListOfInstalledApp(){
        final PackageManager pm = getPackageManager();
        appList = new ArrayList<>();

        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            appList.add(packageInfo.packageName);
        }
    }

    /** To get all apps signature and store in map file with related packageName ***********/
    public Map<String,String> getAllAppSignature(){
        AppSignatureHelper appSignature = new AppSignatureHelper(this);
        packageNameAndHashKey = new HashMap<>();

        for(String packageName : appList){
            packageNameAndHashKey.put(appSignature.getAppSignatures(packageName).toString(),packageName);
        }
        return packageNameAndHashKey;
    }


    /**
     * Obtain the app signatures for the specific packageName
     * @return Hash key of specific app
     *
     */
    public ArrayList<String> getAppSignatures(String packageName) {
        ArrayList<String> appCodes = new ArrayList<>();

        try {
           // String packageName = getPackageName();
            PackageManager packageManager = getPackageManager();
            Signature[] signatures = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;

            // For each signature create a compatible hash
            for (Signature signature : signatures) {
                String hash = hash(packageName, signature.toCharsString());
                if (hash != null) {
                    appCodes.add(String.format("%s", hash));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.v(TAG, "Unable to find package to obtain hash.", e);
        }
        return appCodes;
    }

    private static String hash(String packageName, String signature) {
        String appInfo = packageName + " " + signature;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_TYPE);
            messageDigest.update(appInfo.getBytes(StandardCharsets.UTF_8));
            byte[] hashSignature = messageDigest.digest();

            // truncated into NUM_HASHED_BYTES
            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES);
            // encode into Base64
            String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);

            Log.v(TAG + "sms_sample_test", String.format("pkg: %s -- hash: %s", packageName, base64Hash));
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            Log.v(TAG+ "sms_sample_test", "hash:NoSuchAlgorithm", e);
        }
        return null;
    }
}