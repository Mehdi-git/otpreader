package ir.batna.otpreader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Map;

import ir.batna.otpreader.utils.Constants;

/**
 * Created by Mehdi-git on October 26,2020
 * This class usage is to receive OTP message and send otp code to related app
 * <p>
 * Note: Format of OTP SMS should be like this:     <#>Your verification code is:123456.   KQYoHz5XP4y
 * <p>
 * Digit numbers between colon and dot will be otp code
 * and characters after dot will be recognized as Hash key.
 * Hash key must be 11 characters.
 */

public class SmsReceiver extends BroadcastReceiver {

    //Package name of sms retriever library
    public static final String TAG = SmsReceiver.class.getName();
    private Map<String, String> map;
    private String msgBody;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "Broadcast has received sms");

        //Using goAsync() method is for do process in background for make more time (10 seconds)
        final PendingResult result = goAsync();
        final Thread thread = new Thread() {
            public void run() {
                int i = 999;
                getSmsAndSendBroadcast(context, intent);
                result.setResultCode(i);
                result.finish();
            }
        };
        thread.start();
    }

    /**************************************
     * Entire work handles by this method *
     *************************************/
    private void getSmsAndSendBroadcast(Context context, Intent intent) {

        if (intent.getAction().equals(Constants.SMS_ACTION_NAME)) {
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
                        Log.d(TAG, "Message body:" + msgBody);
                        Log.d(TAG, "Message sender number:" + senderNumber);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }

        try {

            if (msgBody != null && msgBody.startsWith(Constants.OTP_SIGNATURE)) {

                //to extract otp code and hash code from sms body
                String otpCode = extractCodeAndHashFromSms(msgBody).getString(Constants.CODE_KEY);

                //String myHash = extractCodeAndHashFromSms(msgBody).getString("hash");
                String hashKey = extractCodeAndHashFromSms(msgBody).getString(Constants.HASH_KEY);

                //to get packageName from related hash
                String packageName = getPackageNameByHash(context, hashKey);

                if (otpCode != null & packageName != null) {

                    //for send broadcast directly without using service
                    sendBroadcast(context, otpCode, packageName);

                    //for using jobIntentService for send broadcast
                    //sendSmsToService(context, myCode, myHash);

                } else {
                    Log.d(TAG, "PackageName or OTP code NOT available ");
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
        String trimmedSms = null;
        String hash = null;
        StringBuilder code = new StringBuilder();
        Bundle bundle = new Bundle();

        if(msgBody != null) {
            //to remove all character before clone ":"
            for (int i = 0; i < msgBody.length(); i++) {
                if (msgBody.charAt(i) == ':') {
                    trimmedSms = msgBody.substring(i + 1);
                }
            }
        }

        if (trimmedSms != null) {
            //To get OTP code from message body
            for (char c : trimmedSms.toCharArray()) {
                if (c != ' ') {
                    if (c == '.') {
                        break;
                    }
                    code.append(c);
                }
            }

            //To get Hash code from message body
            for (int p = 0; p < trimmedSms.length(); p++) {
                if (trimmedSms.charAt(p) == '.') {
                    hash = trimmedSms.substring(p + 1).trim();
                }
            }
        }

        bundle.putString(Constants.CODE_KEY, code.toString());
        bundle.putString(Constants.HASH_KEY, hash);
        return bundle;
    }

    /*********************************************************
     * To get specific app's packageName by related hash key *
     ********************************************************/
    private String getPackageNameByHash(Context context, String hashKey) {
        AppSignatureHelper helper = new AppSignatureHelper(context);

        // this map file contains packageNames of all software which
        // installed on phone including related hash key
        map = helper.getAllAppSignature();
        return map.get("[" + hashKey + "]");
    }

    /*********************************************************
     * To send Broadcast including otp code to Batna library *
     ********************************************************/
    private void sendBroadcast(Context context, String code, String packageName) {

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, Constants.BATNA_LIBRARY_PACKAGE_NAME));
        intent.putExtra(Constants.CODE_KEY, code);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
        Log.d(TAG, "Broadcast sent! ");
    }

    /******************************************************************
     * To send hash and code to service and do the process in service *
     *****************************************************************/
    private void sendSmsToService(Context context, String code, String hashKey) {

        Intent i = new Intent(context, SmsService.class);
        i.putExtra(Constants.HASH_KEY, hashKey);
        i.putExtra(Constants.CODE_KEY, code);
        SmsService.enqueueWork(context, i);
        Log.d(TAG, "Code and hash sent to the JobIntentService");
    }
}
