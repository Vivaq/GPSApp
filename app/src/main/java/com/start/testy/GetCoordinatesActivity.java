package com.start.testy;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.MalformedInputException;
import java.security.Security;
import java.util.Scanner;

public class GetCoordinatesActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "com.start.testy.MESSAGE";
    LocationManager lm;
    Criteria cr;
    Location l;
    static URL url;
    boolean flag = false;
    private String serverResp = "";
    private boolean serverCompleted = false;
    String coordinates = "Can't get localization";
    String bestProvider;
    private final static String[] LOCATION_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            if(url == null)
                url = new URL("http://192.168.1.29:11111/enter/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bestProvider != null)
                    Snackbar.make(view, "You are using: " + bestProvider, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    protected void onStart()  {
        super.onStart();
        ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.change_ip) {
            Intent intent = new Intent();
            intent.setClass(this, ChangeIpActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_settings) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendMessage(View view) {
        if (flag) {
            int i = 0;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String toSend = "param=" + URLEncoder.encode(coordinates);

                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setDoOutput(true);
                        urlConnection.setDoInput(true);

                        PrintWriter sender = new PrintWriter(urlConnection.getOutputStream());
                        sender.write(toSend);
                        sender.close();

                        Scanner reader = new Scanner(urlConnection.getInputStream());
                        while (reader.hasNextLine()) {
                            serverResp += reader.nextLine();
                        }
                        serverCompleted = true;
                        urlConnection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            while (!serverCompleted && i < 100) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
            if (!serverCompleted) serverResp = "Server Error";

            setContentView(R.layout.final_menu);
            TextView text = (TextView) findViewById(R.id.response);
            text.setTextSize(40);
            text.setText(serverResp);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    throws SecurityException{
        if (requestCode == 1) {
                flag = true;
                cr = new Criteria();
                cr.setAccuracy(Criteria.ACCURACY_COARSE);
                lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                bestProvider = lm.getBestProvider(cr, true);
                lm.requestLocationUpdates(bestProvider, 1000, 10, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) throws SecurityException {
                        bestProvider = lm.getBestProvider(cr, true);
                        l = lm.getLastKnownLocation(bestProvider);
                        printCrd();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });
            {
                //l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                printCrd();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void printCrd(){
        TextView text = (TextView) findViewById(R.id.edit_message);
        if(l !=  null){
            coordinates = String.valueOf(l.getLatitude()) + "\n" + String.valueOf(l.getLongitude());
        }
        text.setText(String.valueOf(coordinates));
    }
}
