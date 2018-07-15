package com.example.worktime;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    int totalMinutes;
    int hourlyIncome;
    int Seconds, Minutes, MilliSeconds, Hours ;
    boolean finishedCalled = false;
    boolean timeRunning = false;
    boolean dataSaved = false;
    String symbol = "$";
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    final Context context = this;

    ArrayList<String> times = new ArrayList<>();
    ArrayList<String> mKeys = new ArrayList<>();

    TextView timeView ;
    TextView moneyView;
    TextView totalTimeView;
    Button start, pause, reset, lap, clear, add;
    ImageButton settings;
    ListView listView ;

    Handler handler;

    ArrayAdapter<String> adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        timeView = (TextView)findViewById(R.id.timeView);
        moneyView = (TextView)findViewById(R.id.moneyView);
        totalTimeView = (TextView)findViewById(R.id.totalTimeView);

        start = (Button)findViewById(R.id.button);
        pause = (Button)findViewById(R.id.button2);
        reset = (Button)findViewById(R.id.button3);
        lap = (Button)findViewById(R.id.button4) ;
        clear = (Button)findViewById(R.id.button5) ;
        add = (Button)findViewById(R.id.button6) ;
        settings = (ImageButton)findViewById(R.id.settingsBtn);
        listView = (ListView)findViewById(R.id.listview1);

        handler = new Handler() ;

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, times) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                return view;
            }
        };

        listView.setAdapter(adapter);

        setIncome();
        setCurrency();
        loadPrefs();

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setIncome();
                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;
                handler.removeCallbacks(runnable);
                reset.setEnabled(true);
                timeRunning = false;

            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;

                handler.removeCallbacks(runnable);

                reset.setEnabled(true);

                MillisecondTime = 0L;
                StartTime = 0L;
                TimeBuff = 0L;
                UpdateTime = 0L;
                Seconds = 0;
                Minutes = 0;
                MilliSeconds = 0;

                setTimeView( true );
                moneyView.setText(symbol + "0.00");

                timeRunning = false;

            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.warning_reset, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);

                alertDialogBuilder.setView(promptsView);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        clearData();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int minute = 60000;

                TimeBuff += minute * 5;

            }
        });

        lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if( Minutes > 0 || Hours > 0) {

                    timeRunning = false;
                    finishedCalled = true;
                    dataSaved = true;
                    saveData();

                    TimeBuff += MillisecondTime;

                    handler.removeCallbacks(runnable);

                    reset.setEnabled(true);

                    MillisecondTime = 0L;
                    StartTime = 0L;
                    TimeBuff = 0L;
                    UpdateTime = 0L;
                    Seconds = 0;
                    Minutes = 0;
                    MilliSeconds = 0;

                   setTimeView( true );
                    moneyView.setText(symbol + "0.00");

                } else if ( timeRunning ) {
                    Toast.makeText(getApplicationContext(),"total time must be greater than 1 minute",Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        savePrefs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrefs();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadPrefs();
    }

    private void loadPrefs() {

        loadData();

        moneyView.setText(symbol + "0.00");

        SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
        timeRunning = resetPrefs.getBoolean("running", false);
        dataSaved = resetPrefs.getBoolean("dataSaved", false);
        TimeBuff = resetPrefs.getLong("timeBuff", 0L);

        if ( !timeRunning && TimeBuff != 0 ) {
            UpdateTime = TimeBuff;
            setTime();
        }

        if ( timeRunning ) {
            StartTime = resetPrefs.getLong("startTime", 0L);
            handler.postDelayed(runnable, 0);
        }
    }

    private void savePrefs() {
        SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = resetPrefs.edit();
        editor.putLong("startTime", StartTime);
        editor.putLong("timeBuff", TimeBuff);
        editor.putBoolean("running", timeRunning);
        editor.putBoolean("dataSaved", dataSaved);

        Gson gson = new Gson();
        String json = gson.toJson( mKeys );
        editor.putString("mKeys", json);

        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("savedPrefs", MODE_PRIVATE);
        totalMinutes = sharedPreferences.getInt("totalMinutes", 0);
        totalTimeView.setText( "" + totalMinutes/60 + "h " + totalMinutes%60 + "m " );

        if ( dataSaved ) {

            SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
            Gson gson = new Gson();
            String json = resetPrefs.getString("mKeys", null);
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            mKeys = gson.fromJson(json, type);

            for (int i = 0; i < mKeys.size(); i++) {
                String key = "savedPrefs" + i;
                SharedPreferences savedPrefs = getSharedPreferences(key, MODE_PRIVATE);
                String mins = savedPrefs.getString("minutes", "");
                String date = savedPrefs.getString("date", "");
                String startTime = savedPrefs.getString("startTime", "");
                String endTime = savedPrefs.getString("endTime", "");
                if (mins != null && date != null && startTime != null && endTime != null) {
                    times.add(String.valueOf(Integer.valueOf(mins) / 60) + "h " + String.valueOf(Integer.valueOf(mins) % 60) + "m " + "| " + startTime + " - " + endTime + " | " + date);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void saveData() {

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);

        SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
        String endTime = tf.format(Calendar.getInstance().getTime());

        String startTime = tf.format( new Date(System.currentTimeMillis() - 60000 * (UpdateTime/60000) ));

        totalMinutes += UpdateTime / 60000;
        totalTimeView.setText( "" + totalMinutes/60 + "h " + totalMinutes%60 + "m " );

        String mins = String.valueOf(UpdateTime / 60000);

        DatabaseReference current_id = mDatabase.child( String.valueOf( mKeys.size() ) );                        //added to fireBase
        current_id.child("minutes").setValue( mins );
        current_id.child("date").setValue( formattedDate );
        current_id.child("startTime").setValue( startTime );
        current_id.child("endTime").setValue( endTime );

        SharedPreferences savedPrefs = getSharedPreferences("savedPrefs" + mKeys.size(), MODE_PRIVATE);   //added to SharedPreferences
        SharedPreferences.Editor editor = savedPrefs.edit();
        editor.putString("minutes", mins );
        editor.putString("date", formattedDate );
        editor.putString("startTime", startTime );
        editor.putString("endTime", endTime );
        editor.putInt("totalMinutes", totalMinutes);
        editor.apply();

        times.add(String.valueOf(Integer.valueOf(mins) / 60) + "h " + String.valueOf(Integer.valueOf(mins) % 60) + "m " + "| " + startTime + " - " + endTime + " | " + formattedDate);
        adapter.notifyDataSetChanged();

        mKeys.add( String.valueOf( mKeys.size() ) );

    }

    private void clearData() {

        if ( dataSaved ) {

            SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor2 = resetPrefs.edit();
            editor2.clear();

            times.clear();
            adapter.notifyDataSetChanged();

            totalTimeView.setText("0h 0m");
            totalMinutes = 0;

            for (int i = 0; i < mKeys.size(); i++)
                mDatabase.child(mKeys.get(i)).removeValue();

            mKeys.clear();

            SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("minutes", 0).apply();

            totalTimeView.setText("" + 0 + "h " + 0 + "m ");

            timeRunning = false;
            dataSaved = false;
        }
    }

    private void setTime() {
        int secondTime = (int) (UpdateTime / 1000);
        Seconds = secondTime % 60;
        int time = (int) (UpdateTime / 60000);
        Hours = time / 60;
        Minutes = time % 60;
        MilliSeconds = (int) (UpdateTime % 1000);
        setTimeView( false );
        int moneyTime = (int) ( ( ( UpdateTime / 600) * hourlyIncome ) / 60 );
        int dollars = moneyTime / 100;
        int cents = moneyTime % 100;
        moneyView.setText("" + symbol + dollars + "." + cents );
    }

    public Runnable runnable = new Runnable() {

        public void run() {
            timeRunning = true;
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;
            setTime();
            handler.postDelayed(this, 0);
        }

    };

    public void setIncome() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        hourlyIncome = sharedPreferences.getInt("hourly_income",0);
    }

    public void setCurrency() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        symbol = sharedPreferences.getString("currency","$");
    }

//    public void setTimeView(boolean reset) {
//
//        String s;
//        if ( reset ) {
//            s = "0h 0m 0s";
//        } else {
//            s = "" + Hours + "h " + Minutes + "m " + String.format("%02d", Seconds) + "s";
//        }
//        SpannableString ss1 = new SpannableString(s);
//        ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
//        ss1.setSpan(new RelativeSizeSpan(2f), 3, 4, 0);
//        ss1.setSpan(new RelativeSizeSpan(2f), 5, 7, 0);
//        timeView.setText( ss1 );
//    }

    public void setTimeView(boolean reset) {

        String s;
        if ( reset ) {
            s = "0h 0m 0s";
            SpannableString ss1 = new SpannableString(s);
            ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
            ss1.setSpan(new RelativeSizeSpan(2f), 3, 4, 0);
            ss1.setSpan(new RelativeSizeSpan(2f), 6, 7, 0);
            timeView.setText( ss1 );
        } else {
            s = Hours + "h " + Minutes + "m " + Seconds + "s";
            SpannableString ss1 = new SpannableString(s);
            if ( Hours < 10 && Minutes < 10 && Seconds < 10 ) { // 0h 0m 0s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 3, 4, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 6, 7, 0);}
            if ( Hours < 10 && Minutes < 10 && Seconds >= 10 ) { // 0h 0m 00s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 3, 4, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 6, 8, 0);}
            if ( Hours < 10 && Minutes >= 10 && Seconds < 10 ) { // 0h 00m 0s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 3, 5, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 7, 8, 0);}
            if ( Hours >= 10 && Minutes < 10 && Seconds < 10 ) { // 00h 0m 0s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 2, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 4, 5, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 7, 8, 0);}
            if ( Hours < 10 && Minutes >= 10 && Seconds >= 10 ) { // 0h 00m 00s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 1, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 3, 5, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 7, 9, 0);}
            if ( Hours >= 10 && Minutes >= 10 && Seconds < 10 ) { // 00h 00m 0s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 2, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 4, 6, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 8, 9, 0);}
            if ( Hours >= 10 && Minutes < 10 && Seconds >= 10 ) { // 00h 0m 00s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 2, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 4, 5, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 7, 9, 0);}
            if ( Hours >= 10 && Minutes >= 10 && Seconds >= 10 ) { // 00h 00m 00s
                ss1.setSpan(new RelativeSizeSpan(2f), 0, 2, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 4, 6, 0);
                ss1.setSpan(new RelativeSizeSpan(2f), 8, 10, 0);}
            timeView.setText( ss1 );
        }
    }

}

