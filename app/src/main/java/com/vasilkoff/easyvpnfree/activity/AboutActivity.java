package com.vasilkoff.easyvpnfree.activity;


//import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import com.vasilkoff.easyvpnfree.R;

import java.util.Locale;

import de.blinkt.openvpn.core.NativeUtils;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionNumber = pinfo.versionCode;
            String versionName = pinfo.versionName;
            String appName =  AboutActivity.this.getString(R.string.app_name);

            String deviceId = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            //BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
            //String deviceName = myDevice.getName();
            String deviceName = String.format(Locale.US, "%d %s %s %s %s %s", Build.VERSION.SDK_INT, Build.VERSION.RELEASE,
                    NativeUtils.getNativeAPI(), Build.BRAND, Build.BOARD, Build.MODEL);
            TextView versionText = (TextView)findViewById(R.id.appVersion);
            versionText.setText(
                    String.format("%s version %s build %d \nBuild Info: " + deviceName + "\nDevice ID: " + deviceId,
                            appName,
                            versionName,
                            versionNumber
                            )
            );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
