package com.lzx.lock.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        // Called when device administration is enabled
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Called when user tries to disable device administration
        // Return a message to inform the user about the consequences
        return "Disabling device administration will remove app security features.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // Called when device administration is disabled
    }
}
