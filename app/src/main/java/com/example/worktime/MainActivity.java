package com.example.worktime;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.SystemClock;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    int totalMinutes;
    int hourlyIncome;
    int Seconds, Minutes, MilliSeconds, Hours ;
    boolean finishedCalled = false;
    boolean timeRunning = false;
    boolean dataSaved = false;
    boolean cleared = false;
    boolean prefsLoaded = false;
    String symbol = "$";
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    final Context context = this;

    ArrayList<String> times = new ArrayList<>();
    ArrayList<String> mKeys = new ArrayList<>();

    TextView timeView ;
    TextView moneyView;
    TextView totalTimeView;
    Button start, pause, reset, lap, add;
    ImageButton settings;
    ListView listView ;

    Handler handler;

    ArrayAdapter<String> adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        timeView = findViewById(R.id.timeView);
        moneyView = findViewById(R.id.moneyView);
        totalTimeView = findViewById(R.id.totalTimeView);

        start = findViewById(R.id.button);
        pause = findViewById(R.id.button2);
        reset = findViewById(R.id.button3);
        lap = findViewById(R.id.button4) ;
        add = findViewById(R.id.button6) ;
        settings = findViewById(R.id.settingsBtn);
        listView = findViewById(R.id.listview1);

        pause.setVisibility(View.INVISIBLE);

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
                setRunning( true );
                setIncome();
                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRunning( false );
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
                setMoneyView( true );

                timeRunning = false;

                setRunning( false );

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
                    setMoneyView( true );
                    setRunning( false );

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
        
        SharedPreferences prefs = getSharedPreferences("shared preferences", MODE_PRIVATE);
        cleared = prefs.getBoolean("cleared", false);

        if ( cleared ) {
            clearData();
        }

        loadData();

        setMoneyView(true);

        SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
        timeRunning = resetPrefs.getBoolean("running", false);
        dataSaved = resetPrefs.getBoolean("dataSaved", false);
        TimeBuff = resetPrefs.getLong("timeBuff", 0L);

        if (!timeRunning && TimeBuff != 0) {
            UpdateTime = TimeBuff;
            setTime();
        }

        if (timeRunning) {
            StartTime = resetPrefs.getLong("startTime", 0L);
            handler.postDelayed(runnable, 0);

            start.setVisibility(View.INVISIBLE);
            pause.setVisibility(View.VISIBLE);
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
        setTotalTimeView( false );

        if ( dataSaved && !prefsLoaded ) {

            prefsLoaded = true;

            SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
            Gson gson = new Gson();
            String json = resetPrefs.getString("mKeys", null);
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            mKeys = gson.fromJson(json, type);

            for (int i = 0; i < mKeys.size(); i++) {
                String key = "savedPrefs" + i;
                SharedPreferences savedPrefs = getSharedPreferences(key, MODE_PRIVATE);
                String mins = savedPrefs.getString("minutes", null);
                String date = savedPrefs.getString("date", null);
                String startTime = savedPrefs.getString("startTime", null);
                String endTime = savedPrefs.getString("endTime", null);
                if (mins != null && date != null && startTime != null && endTime != null) {
                    times.add(String.valueOf(Integer.valueOf(mins) / 60) + "h " + String.valueOf(Integer.valueOf(mins) % 60) + "m " + "| " + startTime + " - " + endTime + " | " + date);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void saveData() {

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault() );
        String formattedDate = df.format(c);

        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault() );
        String endTime = tf.format(Calendar.getInstance().getTime());

        String startTime = tf.format( new Date(System.currentTimeMillis() - 60000 * (UpdateTime/60000) ));

        totalMinutes += UpdateTime / 60000;
        setTotalTimeView( false );

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

            cleared = false;

            SharedPreferences resetPrefs = getSharedPreferences("resetPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor2 = resetPrefs.edit();
            editor2.clear().apply();

            times.clear();
            adapter.notifyDataSetChanged();

            setTotalTimeView( true );
            totalMinutes = 0;

            for (int i = 0; i < mKeys.size(); i++)
                mDatabase.child(mKeys.get(i)).removeValue();

            mKeys.clear();

            SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("cleared", cleared);
            editor.putInt("minutes", 0).apply();

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
        setMoneyView( false );
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

    private void setIncome() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        hourlyIncome = sharedPreferences.getInt("hourly_income",0);
    }

    private void setCurrency() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        symbol = sharedPreferences.getString("currency","$");
    }

    private void setTimeView(boolean reset) {

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

    private void setMoneyView(boolean reset) {

        int moneyTime = (int) ( ( ( UpdateTime / 600) * hourlyIncome ) / 60 );
        int dollars = moneyTime / 100;
        int cents = moneyTime % 100;
        String s;

        if ( reset ) {
            dollars = 0;
            cents = 0;
            s = getString(R.string.moneyDisplay, symbol, dollars, cents);
        } else {
            s = getString(R.string.moneyDisplay, symbol, dollars, cents);
        }
        moneyView.setText( s );
    }

    private void setTotalTimeView(boolean reset) {

        String s;
        if ( reset ) {
            s = getString(R.string.totalTimeDisplay, 0, 0);
        } else {
            s = getString(R.string.totalTimeDisplay, totalMinutes/60, totalMinutes%60 );
        }
        totalTimeView.setText( s );
    }

    private void setRunning( boolean running ) {

        if ( running ) {
            start.setVisibility(View.INVISIBLE);
            pause.setVisibility(View.VISIBLE);
        } else {
            start.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
        }

    }

}

