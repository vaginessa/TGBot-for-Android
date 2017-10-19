package cn.drapl.tgsmsbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Created by draplater on 2017/10/15.
 */

public class Receiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        TelegramBot bot = new TelegramBot(context);
        final String action = intent.getAction();
        if(action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            onReceiveSMS(context, intent, bot);
        } else {
            bot.sendMsg(action);
        }
    }

    public void onReceiveSMS(Context context, Intent intent, TelegramBot bot) {
        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();
        try {
            if(bundle == null) return;
            final Object[] pdusObj = (Object[]) bundle.get("pdus");
            if(pdusObj == null) return;
            for (Object aPdusObj : pdusObj) {
                SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj);
                String senderNum = currentMessage.getDisplayOriginatingAddress();
                String message = currentMessage.getDisplayMessageBody();
                String mobile = senderNum.replaceAll("\\s", "");
                String body = message.replaceAll("\\s", "+");
                Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + body);
                bot.sendMsg("senderNum: " + mobile + ", message: " + message);
            } // end for loop
        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" + e);
        }
    }
}
