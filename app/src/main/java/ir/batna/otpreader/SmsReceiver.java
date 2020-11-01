package ir.batna.otpreader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Map;

/**
 * Created by Mehdi-git on October 26,2020
 * This class usage is to receive OTP message and send otp code to related app
 *
 * Note: Format of OTP SMS should be like this:     <#>Your verification code is:123456.   KQYoHz5XP4y
 *
 * digit numbers between colon and until dot will be otp code
 * and characters after dot will be recognized as Hash key.
 * Hash key must be 11 characters.
 *
 *
 */

public class SmsReceiver extends BroadcastReceiver {

    //Package name of sms retriever library
    private final String BATNA_LIBRARY_PACKAGE_NAME = "ir.batna.smsretrieverlibrary.SmsRetriever";
    public static final String TAG = AppSignatureHelper.class.getName();
    private Map<String, String> map;
    private String msgBody;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d("MBD", "broadcast received sms");

        //Using goAsync() method is for do process in background for make more time (10 seconds)
        final PendingResult result = goAsync();
        final Thread thread = new Thread() {
            public void run() {
                int i = 999;
                // Do processing
                getSmsAndSendBroadcast(context, intent);
                result.setResultCode(i);
                result.finish();
            }
        };
        thread.start();
    }

    /*************************************
     * Entire work handle by this method *
     *************************************/
    private void getSmsAndSendBroadcast(Context context, Intent intent) {

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msg;
            String senderNumber;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msg = new SmsMessage[pdus.length];
                    for (int i = 0; i < msg.length; i++) {
                        msg[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        senderNumber = msg[i].getOriginatingAddress();
                        msgBody = msg[i].getMessageBody();
                        Log.d("MBD", "message received body is: " + msgBody);
                        Log.d("MBD", "message sender number is: " + senderNumber);

                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }

        try {

            if (msgBody != null) {

                //to extract otp code and hash code from sms body
                String otpCode = extractCodeAndHashFromSms(msgBody).getString("code");

                //String myHash = extractCodeAndHashFromSms(msgBody).getString("hash");
                String hashKey = extractCodeAndHashFromSms(msgBody).getString("hash");

                //to get packageName from related hash
                String packageName = getPackageNameByHash(context, hashKey);

                if (otpCode != null & packageName != null) {
                    //for send broadcast directly without using service
                    sendBroadcast(context, otpCode, packageName);

                    //for using jobIntentService for send broadcast
                    //sendSmsToService(context, myCode, myHash);

                } else {
                    Log.d("MBD", "PackageName Or code NOT available ");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**************************************************
     * To extract OTP code and hash key from SMS body *
     **************************************************/
    private Bundle extractCodeAndHashFromSms(String msgBody) {
        String rowCode = null;
        String pureHash = null;
        StringBuilder pureCode = new StringBuilder();
        Bundle bundle = new Bundle();

        //to remove all character before clone ":"
        for (int i = 0; i <= msgBody.length() - 1; i++) {
            if (msgBody.charAt(i) == ':') {
                rowCode = msgBody.substring(i + 1);
                Log.d("MBD", "Row code is:" + rowCode);
            }
        }
        if (rowCode != null)
            for (char c : rowCode.toCharArray()) {
                if (c != ' ') {
                    if (c == '.') {
                        break;
                    }
                    pureCode.append(c);
                }
            }

        for (int p = 0; p <= rowCode.length()-1; p++) {
            if (rowCode.charAt(p) == '.') {
                pureHash = rowCode.substring(p + 1).trim();
            }
        }

        bundle.putString("code",pureCode.toString());
        bundle.putString("hash",pureHash);
        return bundle;
    }

    /*********************************************************
     * To get specific app's packageName by related hash key *
     ********************************************************/
    private String getPackageNameByHash(Context context, String hashKey) {
        AppSignatureHelper helper = new AppSignatureHelper(context);

        //this map file contains packageNames of all software which
        // installed on phone including related hash key
        map = helper.getAllAppSignature();

        return map.get("[" + hashKey + "]");
    }

    /*********************************************************
     * To send Broadcast including otp code to Batna library *
     ********************************************************/
    private void sendBroadcast(Context context, String code, String packageName) {
        Log.d("MBD", "Broadcast sent! ");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, BATNA_LIBRARY_PACKAGE_NAME));
        intent.putExtra("CODE", code);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    /******************************************************************
     * To send hash and code to service and do the process in service *
     *****************************************************************/
    private void sendSmsToService(Context context, String code, String hashKey) {
        Log.d("MBD", "Sms sent to the service");
        Intent i = new Intent(context, SmsService.class);
        i.putExtra("HASH", hashKey);
        i.putExtra("CODE", code);
        SmsService.enqueueWork(context, i);
    }
}
