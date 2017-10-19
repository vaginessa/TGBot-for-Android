package cn.drapl.tgsmsbot;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import cn.drapl.tgsmsbot.ui.MainActivity;
import cz.msebera.android.httpclient.Header;

/**
 * Created by draplater on 2017/10/15.
 */

public class TelegramBot extends JsonHttpResponseHandler {
    private final SharedPreferences pref;
    private final AsyncHttpClient client;
    private final Context context;
    private final String api_key;
    private String userid;
    private String UPDATES_URL =
            "https://api.telegram.org/bot%s/getUpdates?limit=1&offset=%d&timeout=55";

    TelegramBot(Context context) {
        this.context = context;
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
        this.userid = pref.getString("user_id", "");
        this.api_key = pref.getString("api_key", "");
        if(this.api_key.length() == 0) {
            NotificationManagerCompat notiMan = NotificationManagerCompat.from(context);
            Intent notificationIntent = new Intent(context, MainActivity.class);
            Notification noti = new NotificationCompat.Builder(context)
                    .setContentText("No API Key!")
                    .setContentIntent(PendingIntent.getActivity(
                            context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                    ))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .build();
            notiMan.notify(2, noti);
        }
        client = new AsyncHttpClient();
        client.setTimeout(60000);
        client.setConnectTimeout(60000);
        client.setResponseTimeout(60000);
    }

    void startListening() {
        int lastUpdate = pref.getInt("update_id", -2);
        client.get(String.format(UPDATES_URL, api_key, lastUpdate + 1), null, this);
    }

    private void handleMsg(String msg) throws JSONException {
        switch (msg) {
            case "/system_info":
                sendMsg(handleSystemInfo());
                break;
            case "/battery":
                sendMsg(handleBattery());
                break;
            default:
                sendMsg("Unknown Operation.");
        }
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
        int lastUpdate = pref.getInt("update_id", -2);

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastUpdate = pref.getInt("update_id", -2);
                client.get(String.format(UPDATES_URL, api_key, lastUpdate + 1),
                        null, TelegramBot.this);
                android.util.Log.d(Constant.TAG, "Check for Update " + System.currentTimeMillis());
            }
        }, Constant.POLLING_INTERVAL);

        try {
            JSONArray reslst = res.getJSONArray("result");
            if (reslst.length() <= 0) {
                return;
            }
            int upid = reslst.getJSONObject(0).getInt("update_id");
            if (upid <= lastUpdate) {
                android.util.Log.d(Constant.TAG, "No Update " + System.currentTimeMillis());
            } else {
                android.util.Log.d(Constant.TAG, "Found Update!! " + System.currentTimeMillis());
                SharedPreferences.Editor ed = pref.edit();
                ed.putInt("update_id", upid);
                ed.commit();
                JSONObject msgfrom = reslst.getJSONObject(0)
                        .getJSONObject("message").getJSONObject("from");
                String from_username = msgfrom.getString("username");
                String from_id = msgfrom.getString("id");
                android.util.Log.d(Constant.TAG,
                        String.format("Username: %s, ID: %s", from_username, from_id));
                if (!from_username.equals(Constant.USERNAME)) {
                    android.util.Log.d(Constant.TAG, "Invalid Username " + from_username);
                } else if (!userid.equals(from_id)) {
                    userid = from_id;
                    ed = pref.edit();
                    ed.putString("user_id", userid);
                    ed.commit();
                }
                String msg = reslst.getJSONObject(0).getJSONObject("message").getString("text");
                handleMsg(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
        android.util.Log.d(Constant.TAG, "Check Error: " + t.getMessage());
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastUpdate = pref.getInt("update_id", -2);
                client.get(String.format(UPDATES_URL, api_key, lastUpdate + 1), null, TelegramBot.this);
                android.util.Log.d(Constant.TAG, "Check for Update " + System.currentTimeMillis());
            }
        }, Constant.POLLING_INTERVAL);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {
        android.util.Log.d(Constant.TAG, "Check Error: " + t.getMessage());
        if(res != null) {
            android.util.Log.d(Constant.TAG, res.toString());
        } else {
            return;
        }
        if(res.optInt("error_code", -1) == 404) {
            SharedPreferences.Editor ed = pref.edit();
            ed.putString("api_key", "");
            ed.commit();
            return;
        }

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastUpdate = pref.getInt("update_id", -2);
                client.get(String.format(UPDATES_URL, api_key, lastUpdate + 1), null, TelegramBot.this);
                android.util.Log.d(Constant.TAG, "Check for Update " + System.currentTimeMillis());
            }
        }, Constant.POLLING_INTERVAL);
    }

    public void sendMsg(String msg) {
        RequestParams params = new RequestParams();
        if (userid.length() == 0) {
            android.util.Log.d(Constant.TAG, "Chat not exist, don't send it.");
            return;
        }

        params.put("chat_id", userid);
        params.put("text", msg);
        client.post("https://api.telegram.org/bot" + api_key + "/sendMessage", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int p1, Header[] p2, String p3, Throwable p4) {
                android.util.Log.d(Constant.TAG, "send msg FAIL, " + p4.getMessage());
            }

            @Override
            public void onSuccess(int p1, Header[] p2, String p3) {
                android.util.Log.d(Constant.TAG, "send msg OK " + p3);
            }
        });
    }

    public static String handleSystemInfo() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        return (manufacturer + " (" + model + ")").toUpperCase();
    }

    public String handleBattery() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        if (batteryStatus == null) {
            return "Cannot get battery status.";
        }
        return getBatteryStatusFromIntent(batteryStatus);
    }

    public String getBatteryStatusFromIntent(Intent batteryStatus) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        if (level == -1 || scale == -1 || plugged == -1) {
            return "Invalid battery status.";
        }

        float batteryPct = level / (float) scale;
        return String.format(Locale.getDefault(),
                "Percentage: %f, Status: %s", batteryPct,
                plugged == 0 ? "Disharging" : "Charging");
    }
}
