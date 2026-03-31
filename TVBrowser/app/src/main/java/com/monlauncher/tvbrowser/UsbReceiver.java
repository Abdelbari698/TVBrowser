package com.monlauncher.tvbrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UsbReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MEDIA_MOUNTED.equals(action) ||
            "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action) ||
            "android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
            // Clé USB branchée — lance MainActivity
            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launch.putExtra("from_usb", true);
            context.startActivity(launch);
        }
    }
}
