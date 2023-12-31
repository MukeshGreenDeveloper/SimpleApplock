package com.lzx.lock.activities.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lzx.lock.R;
import com.lzx.lock.base.AppConstants;
import com.lzx.lock.base.BaseActivity;
import com.lzx.lock.activities.lock.GestureSelfUnlockActivity;
import com.lzx.lock.activities.pwd.CreatePwdActivity;
import com.lzx.lock.receiver.MyDeviceAdminReceiver;
import com.lzx.lock.services.BackgroundManager;
import com.lzx.lock.services.LoadAppListService;
import com.lzx.lock.services.LockService;
import com.lzx.lock.utils.AppUtils;
import com.lzx.lock.utils.LockUtil;
import com.lzx.lock.utils.SpUtil;
import com.lzx.lock.utils.ToastUtil;
import com.lzx.lock.widget.DialogPermission;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by xian on 2017/2/17.
 */

public class SplashActivity extends BaseActivity {
    private static final int RESULT_ACTION_USAGE_ACCESS_SETTINGS = 1;
    private static final int RESULT_ACTION_ACCESSIBILITY_SETTINGS = 3;
    private static final int RESULT_ACTION_APPEAR_ON_TOP_SETTINGS = 1234;
    private static final int REQUEST_CODE_DEVICE_ADMIN = 100;
    private static final String TAG = "SplashActivity";

    private ImageView mImgSplash;
    @Nullable
    private ObjectAnimator animator;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        AppUtils.hideStatusBar(getWindow(), true);
        mImgSplash = findViewById(R.id.img_splash);
//        requestDeviceAdminPermission();
        initiateFirebase();
    }

    private void initiateFirebase() {
        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.setValue("Hello, World!");
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    @Override
    protected void initData() {
        //startService(new Intent(this, LoadAppListService.class));
        BackgroundManager.getInstance().init(this).startService(LoadAppListService.class);

        //start lock services if  everything is already  setup
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            BackgroundManager.getInstance().init(this).startService(LockService.class);
        }

        animator = ObjectAnimator.ofFloat(mImgSplash, "alpha", 0.5f, 1);
        animator.setDuration(1500);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boolean isFirstLock = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_FIRST_LOCK, true);
                if (isFirstLock) {
                    showDialog();
                } else {
                    Intent intent = new Intent(SplashActivity.this, GestureSelfUnlockActivity.class);
                    intent.putExtra(AppConstants.LOCK_PACKAGE_NAME, AppConstants.APP_PACKAGE_NAME);
                    intent.putExtra(AppConstants.LOCK_FROM, AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        });
    }

    private void showDialog() {
        // If you do not have access to view usage rights and the phone exists to view usage this interface
        if (!LockUtil.isStatAccessPermissionSet(SplashActivity.this) && LockUtil.isNoOption(SplashActivity.this)) {
            DialogPermission dialog = new DialogPermission(SplashActivity.this);
            dialog.show();
            dialog.setOnClickListener(new DialogPermission.onClickListener() {
                @Override
                public void onClick() {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Intent usageAccessIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivityForResult(usageAccessIntent, RESULT_ACTION_USAGE_ACCESS_SETTINGS);
                    }
                }
            });
        } else if (!hasAppearOnTopPermission()) {
            requestAppearOnTopPermission();
        } else if (!isDeviceAdminEnabled()) {
            requestDeviceAdminPermission();
        }else {
            gotoCreatePwdActivity();
        }
    }

    private boolean isDeviceAdminEnabled() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName componentName = new ComponentName(this, MyDeviceAdminReceiver.class);
        return devicePolicyManager.isAdminActive(componentName);
    }
    private void requestDeviceAdminPermission() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName componentName = new ComponentName(this, MyDeviceAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Your explanation message");
        startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_ACTION_USAGE_ACCESS_SETTINGS) {
            if (LockUtil.isStatAccessPermissionSet(SplashActivity.this)) {
                if (!hasAppearOnTopPermission()) {
                    requestAppearOnTopPermission();
                } else {
                    gotoCreatePwdActivity();
                }
            } else {
                ToastUtil.showToast("Permission denied");
                finish();
            }
        } else if (requestCode == RESULT_ACTION_APPEAR_ON_TOP_SETTINGS) {
            if (hasAppearOnTopPermission()) {
                gotoCreatePwdActivity();
            } else {
                ToastUtil.showToast("Permission denied");
                finish();
            }
        } else if (requestCode == REQUEST_CODE_DEVICE_ADMIN){

        }
    }

    private void requestAppearOnTopPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasAppearOnTopPermission()) {
                Intent appearOnTopIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                appearOnTopIntent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(appearOnTopIntent, RESULT_ACTION_APPEAR_ON_TOP_SETTINGS);
            }
        }
    }
    private boolean hasAppearOnTopPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    public boolean isAccessibilityEnabled() {
        int accessibilityEnabled = 0;
        final String ACCESSIBILITY_SERVICE = "io.github.subhamtyagi.privacyapplock/com.lzx.lock.service.LockAccessibilityService";
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            //setting not found so your phone is not supported
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessabilityService = mStringColonSplitter.next();
                    if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void gotoCreatePwdActivity() {
        Intent intent2 = new Intent(SplashActivity.this, CreatePwdActivity.class);
        startActivity(intent2);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }



    @Override
    protected void initAction() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animator = null;
    }
}
