package com.rocklinker.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("broadcastAction")
                .putExtra("actionName", intent.getAction()));
    }
}
