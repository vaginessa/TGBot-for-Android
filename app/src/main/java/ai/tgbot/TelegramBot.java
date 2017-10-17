package ai.tgbot;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by draplater on 2017/10/15.
 */

public class TelegramBot extends JsonHttpResponseHandler {
    private final SharedPreferences pref;
    private final AsyncHttpClient client;
    private final Context context;
    private String userid;
    private String UPDATES_URL = "https://api.telegram.org/bot" +
            Constant.TOKEN + "/getUpdates?limit=1&offset=%d&timeout=55";

    TelegramBot(Context context) {
        this.context = context;
        this.pref = context.getSharedPreferences("update_data", MODE_PRIVATE);
        this.userid = pref.getString("user_id", "");
        client = new AsyncHttpClient();
        client.setTimeout(60000);
        client.setConnectTimeout(60000);
        client.setResponseTimeout(60000);
    }

    void startListening() {
        int lastUpdate = pref.getInt("update_id", -2);
        client.get(String.format(UPDATES_URL, lastUpdate + 1), null, this);
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
                client.get(String.format(UPDATES_URL, lastUpdate + 1),
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
                client.get(String.format(UPDATES_URL, lastUpdate + 1), null, TelegramBot.this);
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
        client.post("https://api.telegram.org/bot" + Constant.TOKEN + "/sendMessage", params, new TextHttpResponseHandler() {
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
