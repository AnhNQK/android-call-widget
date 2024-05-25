package com.stringee.widget;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by luannguyen on 7/12/2017.
 */

public class Utils {

    public static void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public static void reportMessage(Context context, int resId) {
        Toast toast = Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            TextView v = toast.getView().findViewById(android.R.id.message);
            if (v != null)
                v.setGravity(Gravity.CENTER);
        }
        toast.show();
    }

    public static void postDelayed(Runnable runnable, long delayMillis) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, delayMillis);
    }

    public static void reportMessage(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            TextView v = toast.getView().findViewById(android.R.id.message);
            if (v != null)
                v.setGravity(Gravity.CENTER);
        }
        toast.show();
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static boolean isTextEmpty(@Nullable String text) {
        if (text != null) {
            return text.trim().length() == 0;
        } else {
            return true;
        }
    }

    public static String getUserId(String connectToken) {
        if (Utils.isTextEmpty(connectToken)){
            return null;
        }
        String userId = null;
        String[] parts = connectToken.split("[.]");
        try {
            for (int i = 0; i < 2; i++) {
                String part = parts[i];
                byte[] partAsBytes = part.getBytes("UTF-8");
                String decodedPart = new String(Base64.decode(partAsBytes, Base64.DEFAULT), "UTF-8");
                JSONObject jsonObject = new JSONObject(decodedPart);
                if (jsonObject.has("userId")) {
                    userId = jsonObject.optString("userId");
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        return userId;
    }
}
