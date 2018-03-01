package com.vasilkoff.easyvpnfree.activity;


import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.provider.Settings.Secure;
//import android.bluetooth.BluetoothAdapter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.view.View;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.daimajia.numberprogressbar.NumberProgressBar;

import com.vasilkoff.easyvpnfree.BuildConfig;
import com.vasilkoff.easyvpnfree.R;
import com.vasilkoff.easyvpnfree.model.Server;
import com.vasilkoff.easyvpnfree.util.PropertiesService;
import com.vasilkoff.easyvpnfree.util.Stopwatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.core.NativeUtils;
import okhttp3.OkHttpClient;
import cf.jupitervpn.mcrypt.MCrypt;


public class LoaderActivity extends BaseActivity {

    private NumberProgressBar progressBar;
    private TextView commentsText;

    private Handler updateHandler;

    private final int LOAD_ERROR = 0;
    private final int DOWNLOAD_PROGRESS = 1;
    private final int PARSE_PROGRESS = 2;
    private final int LOADING_SUCCESS = 3;
    private final int SWITCH_TO_RESULT = 4;
    private final String BASE_URL = "93bf5138228205addec318fb3f6a383f810804c0bdbbe333027a0c0f2802e11d7a34c3f7d61fefa72baf667124463b14";
    private final String BASE_FILE_NAME = ".vpngate.csv";

    //private final String android_id = Secure.getString(this.getContentResolver(),Secure.ANDROID_ID);

    private boolean premiumStage = true;

    private final String PREMIUM_URL = "93bf5138228205addec318fb3f6a383f810804c0bdbbe333027a0c0f2802e11d7a34c3f7d61fefa72baf667124463b14";
    private final String PREMIUM_FILE_NAME = ".premiumServers.csv";

    private int percentDownload = 0;
    private Stopwatch stopwatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);

        progressBar = findViewById(R.id.number_progress_bar);
        commentsText = findViewById(R.id.commentsText);

        if (getIntent().getBooleanExtra("firstPremiumLoad", false))
            findViewById(R.id.loaderPremiumText).setVisibility(View.VISIBLE);

        progressBar.setMax(100);

        updateHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.arg1) {
                    case LOAD_ERROR: {
                        commentsText.setText(msg.arg2);
                        progressBar.setProgress(100);
                    } break;
                    case DOWNLOAD_PROGRESS: {
                        commentsText.setText(R.string.downloading_csv_text);
                        progressBar.setProgress(msg.arg2);

                    } break;
                    case PARSE_PROGRESS: {
                        commentsText.setText(R.string.parsing_csv_text);
                        progressBar.setProgress(msg.arg2);
                    } break;
                    case LOADING_SUCCESS: {
                        commentsText.setText(R.string.successfully_loaded);
                        progressBar.setProgress(100);
                        Message end = new Message();
                        end.arg1 = SWITCH_TO_RESULT;
                        updateHandler.sendMessageDelayed(end,500);
                    } break;
                    case SWITCH_TO_RESULT: {
                        if (!BuildConfig.DEBUG)
                            Answers.getInstance().logCustom(new CustomEvent("Time servers loading")
                                .putCustomAttribute("Time servers loading", stopwatch.getElapsedTime()));

                        if (PropertiesService.getConnectOnStart()) {
                            Server randomServer = getRandomServer();
                            if (randomServer != null) {
                                newConnecting(randomServer, true, true);
                            } else {
                                startActivity(new Intent(LoaderActivity.this, HomeActivity.class));
                            }
                        } else {
                            startActivity(new Intent(LoaderActivity.this, HomeActivity.class));
                        }
                    }
                }
                return true;
            }
        });
        progressBar.setProgress(0);


    }

    @Override
    protected void onResume() {
        super.onResume();
        MCrypt mcrypt = new MCrypt();
        String BASE_URL_DEC = "";
        try {
            BASE_URL_DEC = new String(mcrypt.decrypt(BASE_URL));
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadCSVFile(BASE_URL_DEC, BASE_FILE_NAME);
    }

    @Override
    protected boolean useHomeButton() {
        return false;
    }

    @Override
    protected boolean useMenu() {
        return false;
    }

    private void downloadCSVFile(String url, String fileName) {
        stopwatch = new Stopwatch();

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        String deviceId = Secure.getString(this.getContentResolver(),
                Secure.ANDROID_ID);
        //BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        //String deviceName = myDevice.getName();
        String deviceName = String.format(Locale.US, "%d %s %s %s %s %s", Build.VERSION.SDK_INT, Build.VERSION.RELEASE,
                NativeUtils.getNativeAPI(), Build.BRAND, Build.BOARD, Build.MODEL);
        try {
            deviceName = URLEncoder.encode(deviceName,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        AndroidNetworking.download(url + "?devid=" + deviceId + "&devname=" + deviceName, getCacheDir().getPath(), fileName)
                //.addHeaders("ANDROID_ID", deviceId)
                .setTag("downloadCSV")
                .setPriority(Priority.MEDIUM)
                .setOkHttpClient(okHttpClient)
                .build()
                .setDownloadProgressListener(new DownloadProgressListener() {
                    @Override
                    public void onProgress(long bytesDownloaded, long totalBytes) {
                        if(totalBytes <= 0) {
                            // when we dont know the file size, assume it is 1200000 bytes :)
                            totalBytes = 1200000;
                        }

                        if (!premiumServers || !premiumStage) {
                            if (percentDownload <= 90)
                            percentDownload = percentDownload + (int)((100 * bytesDownloaded) / totalBytes);
                        } else {
                            percentDownload = (int)((100 * bytesDownloaded) / totalBytes);
                        }

                        Message msg = new Message();
                        msg.arg1 = DOWNLOAD_PROGRESS;
                        msg.arg2 = percentDownload;
                        updateHandler.sendMessage(msg);
                    }
                })
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        if (premiumServers && premiumStage) {
                            premiumStage = false;
                            downloadCSVFile(PREMIUM_URL, PREMIUM_FILE_NAME);
                        } else {
                            parseCSVFile(BASE_FILE_NAME);
                        }
                    }
                    @Override
                    public void onError(ANError error) {
                        Message msg = new Message();
                        msg.arg1 = LOAD_ERROR;
                        msg.arg2 = R.string.network_error;
                        updateHandler.sendMessage(msg);
                    }
                });
    }

    private void parseCSVFile(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getCacheDir().getPath().concat("/").concat(fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            Message msg = new Message();
            msg.arg1 = LOAD_ERROR;
            msg.arg2 = R.string.csv_file_error;
            updateHandler.sendMessage(msg);
        }
        if (reader != null) {
            try {
                int startLine = 2;
                int type = 0;

                if (premiumServers && premiumStage) {
                    startLine = 0;
                    type = 1;
                } else {
                    dbHelper.clearTable();
                }

                int counter = 0;
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (counter >= startLine) {
                        dbHelper.putLine(line, type);
                    }
                    counter++;
                    if (!premiumServers || !premiumStage) {
                        Message msg = new Message();
                        msg.arg1 = PARSE_PROGRESS;
                        msg.arg2 = counter;// we know that the server returns 100 records
                        updateHandler.sendMessage(msg);
                    }
                }

                if (premiumServers && !premiumStage) {
                    premiumStage = true;
                    parseCSVFile(PREMIUM_FILE_NAME);
                } else {
                    Message end = new Message();
                    end.arg1 = LOADING_SUCCESS;
                    updateHandler.sendMessageDelayed(end,200);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.arg1 = LOAD_ERROR;
                msg.arg2 = R.string.csv_file_error_parsing;
                updateHandler.sendMessage(msg);
            }
        }
    }

}
