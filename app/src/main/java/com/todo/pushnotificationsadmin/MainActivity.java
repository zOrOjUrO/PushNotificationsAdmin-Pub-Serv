package com.todo.pushnotificationsadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;



public class MainActivity extends AppCompatActivity {

    private @SuppressLint("SimpleDateFormat")
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private String getDate(){
        MaterialDatePicker<Long> dpd = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick Date")
                 .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        dpd.show(getSupportFragmentManager(), "Date Picker Dialog");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String[] date = {dateFormat.format(System.currentTimeMillis())};
        dpd.addOnPositiveButtonClickListener(selection -> {
            date[0] =  dateFormat.format(selection);
            //Log.i("Date Picked: ", date[0]);
        });
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 30000);
        return date[0];
    }

    private String getTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        MaterialTimePicker mtp = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Pick the Schedule Time")
                .build();
        mtp.show(getSupportFragmentManager(), "Time Picker Dialog");
        final String[] time = {hour + ":" + minute + ":" + second};

        mtp.addOnPositiveButtonClickListener(view -> {
            time[0] = mtp.getHour()+":"+mtp.getMinute()+":00";
            //Log.i("Time Picked: ", time[0]);
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 30000);
        return time[0];
    }

    private String checkTopic(){
        EditText topic = ((EditText)findViewById(R.id.broadcastTopic));
        if( TextUtils.isEmpty(topic.getText())){
            Toast.makeText(this.getApplicationContext(), "Enter The Topic to Publish", Toast.LENGTH_SHORT).show();
            topic.setError( "Topic is required!" );
        }
        return String.valueOf(topic.getText());
    }

    public void schedNotif(View view) {
        // The topic name can be optionally prefixed with "/topics/".
        String topic = checkTopic();
        String message = String.valueOf(((EditText) findViewById(R.id.notifBody)).getText());
        String title = String.valueOf(((EditText) findViewById(R.id.notifTitle)).getText());

        String scheduledTime = dateFormat.format(new Date(System.currentTimeMillis() + 20 * 1000));
        Log.d("scheduledTime: ", scheduledTime);

        if (!topic.isEmpty()) {
            Snackbar.make(view.getRootView(), "Sending Notification...", BaseTransientBottomBar.LENGTH_SHORT).show();

            /*
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = DatePickerDialog.newInstance(
                    MainActivity.this,
                    now.get(Calendar.YEAR), // Initial year selection
                    now.get(Calendar.MONTH), // Initial month selection
                    now.get(Calendar.DAY_OF_MONTH) // Inital day selection
            );
            // If you're calling this from a support Fragment
            //dpd.show(getFragmentManager(), "Datepickerdialog");
            // If you're calling this from an AppCompatActivity
            dpd.setVersion(DatePickerDialog.Version.VERSION_2);
            dpd.setThemeDark(true);
            dpd.show(getSupportFragmentManager(), "Datepickerdialog");

            TimePickerDialog tpd = TimePickerDialog.newInstance(MainActivity.this,
                    true);
            tpd.setVersion(TimePickerDialog.Version.VERSION_2);
            tpd.show(getSupportFragmentManager(), "Timeepickerdialog");
             */

            String time = getTime();
            String date = getDate();
            Log.d("Selected Schedule Time: ", date+" "+time);
            //send message
            String endpoint = "https://fcm.googleapis.com/fcm/send";

            new Thread(() -> {
                try {
                    URL url = new URL(endpoint);

                    HttpsURLConnection httpsURLConnection = ((HttpsURLConnection) url.openConnection());
                    httpsURLConnection.setReadTimeout(10000);
                    httpsURLConnection.setConnectTimeout(15000);
                    httpsURLConnection.setRequestMethod("POST");
                    httpsURLConnection.setDoInput(true);
                    httpsURLConnection.setDoOutput(true);
                    httpsURLConnection.setRequestProperty("Authorization", "key="+getString(R.string.api_key));
                    httpsURLConnection.setRequestProperty("Content-Type", "application/json");

                    JSONObject body = new JSONObject();
                    JSONObject data = (new JSONObject())
                            .put("title", title)
                            .put("message", message)
                            .put("isScheduled", "true")
                            .put("scheduledTime",scheduledTime);

                    body
                            .put("data", data)
                            .put("to", "/topics/"+topic);

                    OutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                    bufferedWriter.write(body.toString());
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    outputStream.close();

                    Log.d("HTTPS Response: ", httpsURLConnection.getResponseCode() + "\t" + httpsURLConnection.getResponseMessage());

                    InputStream inputStream = (400 <= httpsURLConnection.getResponseCode() && httpsURLConnection.getResponseCode() <= 499)?
                            httpsURLConnection.getErrorStream():
                            httpsURLConnection.getInputStream();

                    if(httpsURLConnection.getResponseCode() == 200)
                        Log.e("HTTPS REQ Success: ", "Notification Scheduled Successfully");
                    else
                        Log.e("HTTPS REQ Error", "Error Response received");

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }).start();


        }

    }

    public void sendNotif(View view) {
        // The topic name can be optionally prefixed with "/topics/".
        String topic = checkTopic();
        String message = String.valueOf(((EditText) findViewById(R.id.notifBody)).getText());
        String title = String.valueOf(((EditText) findViewById(R.id.notifTitle)).getText());

        if (!topic.isEmpty()) {
            Snackbar.make(view.getRootView(), "Sending Notification...", BaseTransientBottomBar.LENGTH_SHORT).show();
            //send message
            String endpoint = "https://fcm.googleapis.com/fcm/send";
            new Thread(() -> {
                try {
                    URL url = new URL(endpoint);

                    HttpsURLConnection httpsURLConnection = ((HttpsURLConnection) url.openConnection());
                    httpsURLConnection.setReadTimeout(10000);
                    httpsURLConnection.setConnectTimeout(15000);
                    httpsURLConnection.setRequestMethod("POST");
                    httpsURLConnection.setDoInput(true);
                    httpsURLConnection.setDoOutput(true);
                    httpsURLConnection.setRequestProperty("Authorization", "key="+getString(R.string.api_key));
                    httpsURLConnection.setRequestProperty("Content-Type", "application/json");

                    JSONObject body = new JSONObject();
                    JSONObject data = (new JSONObject())
                            .put("title", title)
                            .put("message", message)
                            .put("isScheduled", "false")
                            .put("scheduledTime", dateFormat.format(new Date()));

                    body
                            .put("data", data)
                            .put("to", "/topics/"+topic);

                    OutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                    bufferedWriter.write(body.toString());
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    outputStream.close();

                    Log.d("HTTPS Response: ", httpsURLConnection.getResponseCode() + "\t" + httpsURLConnection.getResponseMessage());

                    InputStream inputStream = (400 <= httpsURLConnection.getResponseCode() && httpsURLConnection.getResponseCode() <= 499)?
                            httpsURLConnection.getErrorStream():
                            httpsURLConnection.getInputStream();

                    if(httpsURLConnection.getResponseCode() == 200)
                        Log.e("HTTPS REQ Success: ", "Notification sent");
                    else
                        Log.e("HTTPS REQ Error", "Error Response received");

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /*
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = "You picked the following date: "+dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        Log.d("Time Picked: ", date);
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        String time = "You picked the following time: "+hourOfDay+"h"+minute+"m"+second;
        Log.d("Time Picked: ", time);
    }
     */
}