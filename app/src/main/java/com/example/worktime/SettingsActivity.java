package com.example.worktime;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences prefs;
    TextView prompt;
    EditText userInput;

    //Hashmaps and lists
    HashMap<String, String> userValues;
    List<HashMap<String, String>> listItems;

    SimpleAdapter adapter;

    Context context;

    int hIncome;

    Iterator it;

    ImageButton home;
    ListView settingsLV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = this;

        prefs = getSharedPreferences("shared preferences", MODE_PRIVATE);


        settingsLV = (ListView) findViewById(R.id.settings_listview);
        home = (ImageButton)findViewById(R.id.homeBtn);

        hIncome = prefs.getInt("hourly_income", 0);

        userValues = new HashMap<>();
        userValues.put("Hourly Income", "$" + Integer.toString( hIncome ) + " /hr");

        listItems = new ArrayList<>();

        adapter = new SimpleAdapter(this, listItems, R.layout.custom_list_view_cell,
                new String[]{"First Line", "Second Line"},
                new int[]{R.id.text1, R.id.text2});

        //Iterate through the uservalues and set them in the adapter
        it = userValues.entrySet().iterator();
        while (it.hasNext())
        {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry)it.next();
            resultsMap.put("First Line", pair.getKey().toString());
            resultsMap.put("Second Line", pair.getValue().toString());
            listItems.add(resultsMap);
        }

        settingsLV.setAdapter(adapter);

        settingsLV.setOnItemClickListener(
                new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                        // get the view1
                        final HashMap<String, String> obj = (HashMap<String, String>) adapter.getItem(position);
                        String objStr = obj.get("First Line");

                        final LayoutInflater li = LayoutInflater.from(context);
                        View promptsView = li.inflate(R.layout.stat_change, null);
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                context);
                        final int choice;

                        switch(objStr){
                            case "Hourly Income":
                                promptsView = li.inflate(R.layout.stat_change, null);
                                prompt = (TextView) promptsView.findViewById(R.id.prompt);
                                prompt.setText(R.string.incomePrompt);
                                userInput = (EditText) promptsView
                                        .findViewById(R.id.editTextResult);
                                userInput.setInputType(InputType.TYPE_CLASS_TEXT);
                                choice = 1;
                                break;
                            default:
                                choice = 1;
                                break;

                        }

                        // set prompts.xml to alertdialog builder
                        alertDialogBuilder.setView(promptsView);

                        // set dialog message
                        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        switch(choice){
                                            case 1:
                                                String str = userInput.getText().toString();
                                                prefs.edit().putInt("hourly_income", Integer.parseInt(str) ).apply();

                                                updateAdapter();
                                                break;
                                        }

                                    }
                                })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                dialog.cancel();
                                            }
                                        });

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();

                        // show it
                        alertDialog.show();
                    }
                });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }

    private void updateAdapter(){
        userValues.put("Hourly Income", "$" + prefs.getInt("hourly_income", 0) + " /hr");

        listItems.clear();
        it = userValues.entrySet().iterator();
        while (it.hasNext())
        {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry)it.next();
            resultsMap.put("First Line", pair.getKey().toString());
            resultsMap.put("Second Line", pair.getValue().toString());
            listItems.add(resultsMap);
        }
        adapter.notifyDataSetChanged();
    }

}
