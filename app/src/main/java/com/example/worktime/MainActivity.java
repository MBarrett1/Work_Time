package com.example.worktime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
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

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;

    int totalMinutes;

    int hourlyIncome;

    boolean finishedCalled = false;
    boolean timeRunning = false;

    ArrayList<String> times = new ArrayList<>();
    ArrayList<String> mKeys = new ArrayList<>();

    TextView textView ;
    TextView moneyView;
    TextView totalTimeView;

    Button start, pause, reset, lap, clear, add;
    ImageButton settings;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;

    Handler handler;

    int Seconds, Minutes, MilliSeconds, Hours ;

    ListView listView ;

    ArrayAdapter<String> adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        textView = (TextView)findViewById(R.id.textView);
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

        setIncome();
        loadData();

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, times) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.WHITE);
                // Generate ListView Item using TextView
                return view;
            }
        };
        listView.setAdapter(adapter);

        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if ( !finishedCalled ) {
                    String mins = dataSnapshot.child("minutes").getValue(String.class);
                    String date = dataSnapshot.child("date").getValue(String.class);
                    String startTime = dataSnapshot.child("startTime").getValue(String.class);
                    String endTime = dataSnapshot.child("endTime").getValue(String.class);
                    times.add(String.valueOf(Integer.valueOf(mins) / 60) + "h " + String.valueOf(Integer.valueOf(mins) % 60) + "m " + "| " + startTime + " - " + endTime + " | " + date);
                    String key = dataSnapshot.getKey();
                    mKeys.add(key);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String mins = dataSnapshot.child("minutes").getValue(String.class);
                String date = dataSnapshot.child("date").getValue(String.class);
                String startTime = dataSnapshot.child("startTime").getValue(String.class);
                String endTime = dataSnapshot.child("endTime").getValue(String.class);
                if (mins != null && date != null && startTime != null && endTime != null ) {
                    times.add(String.valueOf(Integer.valueOf(mins) / 60) + "h " + String.valueOf(Integer.valueOf(mins) % 60) + "m " + "| " + startTime + " - " + endTime + " | " + date);
                    String key = dataSnapshot.getKey();
                    mKeys.add(key);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = mKeys.indexOf(key);
                times.remove( index );
                mKeys.remove( index );
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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

                textView.setText("0h 0m 00s");
                moneyView.setText("$0.00");

                timeRunning = false;

            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clearData();

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

                if( Seconds > 0 ) {

                    finishedCalled = true;
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

                    textView.setText("0h 0m 00s");
                    moneyView.setText("$0.00");

                }

            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("startTime", StartTime);
        outState.putLong("timeBuff", TimeBuff);
        outState.putBoolean("running", timeRunning);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        timeRunning = savedInstanceState.getBoolean("running");
        TimeBuff = savedInstanceState.getLong("timeBuff");

        if ( !timeRunning && TimeBuff > 0 ) {

            UpdateTime = TimeBuff;
            int secondTime = (int) (UpdateTime / 1000);
            Seconds = secondTime % 60;
            int time = (int) (UpdateTime / 60000);
            Hours = time / 60;
            Minutes = time % 60;
            MilliSeconds = (int) (UpdateTime % 1000);
            textView.setText("" + Hours + "h " + Minutes + "m "
                    + String.format("%02d", Seconds) + "s" );
            int moneyTime = (int) ( ( ( UpdateTime / 600) * hourlyIncome ) / 60 );
            int dollars = moneyTime / 100;
            int cents = moneyTime % 100;
            moneyView.setText("" + "$"+ dollars + "." + cents );
        }

        if ( timeRunning ) {

            StartTime = savedInstanceState.getLong("startTime");
            handler.postDelayed(runnable, 0);
//            Toast.makeText(getBaseContext(), "Data loaded as: " + StartTime, Toast.LENGTH_LONG).show();

        }
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        totalMinutes = sharedPreferences.getInt("minutes", 0);
//        totalTimeView.setText( "" + totalMinutes/60 + "h " + totalMinutes%60 + "m " );
    }

    private void saveData() {

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);

        SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
        String endTime = tf.format(Calendar.getInstance().getTime());

        String startTime = tf.format( new Date(System.currentTimeMillis() - 60000 * (UpdateTime/60000) ));

        DatabaseReference current_id = mDatabase.child( String.valueOf( mKeys.size() ) );
        current_id.child("minutes").setValue( String.valueOf(UpdateTime / 60000) );
        current_id.child("date").setValue( formattedDate );
        current_id.child("startTime").setValue( startTime );
        current_id.child("endTime").setValue( endTime );

        totalMinutes += UpdateTime / 60000;
        totalTimeView.setText( "" + totalMinutes/60 + "h " + totalMinutes%60 + "m " );

        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("minutes", totalMinutes);
        editor.apply();

    }

    private void clearData() {
        totalTimeView.setText( "0h 0m" );
        totalMinutes = 0;

        for (int i = 0; i < mKeys.size(); i++)
            mDatabase.child( mKeys.get( i ) ).removeValue();

        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("minutes", 0).apply();

        totalTimeView.setText( "" + 0 + "h " + 0 + "m " );

        timeRunning = false;
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            timeRunning = true;

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            int secondTime = (int) (UpdateTime / 1000);

            Seconds = secondTime % 60;

            int time = (int) (UpdateTime / 60000);

            Hours = time / 60;

            Minutes = time % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            textView.setText("" + Hours + "h " + Minutes + "m "
                    + String.format("%02d", Seconds) + "s" );

            int moneyTime = (int) ( ( ( UpdateTime / 600) * hourlyIncome ) / 60 );

            int dollars = moneyTime / 100;
            int cents = moneyTime % 100;

            moneyView.setText("" + "$"+ dollars + "." + cents );

            handler.postDelayed(this, 0);
        }

    };

    public void setIncome(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        hourlyIncome = sharedPreferences.getInt("hourly_income",0);
    }

}

