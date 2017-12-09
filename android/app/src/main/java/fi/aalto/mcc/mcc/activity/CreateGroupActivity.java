package fi.aalto.mcc.mcc.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import fi.aalto.mcc.mcc.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateGroupActivity extends AppCompatActivity {


    private static final String TAG = "CreateGroup";
    public static TextView SelectedDateView;

    private TextView mDisplayDate;
    private TextView mDisplayTime;
    private TextView mGroupName;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private TimePickerDialog.OnTimeSetListener mTimeSetListener;
    private Button mCreateBtn;
    private String idToken;
    private String url;
    private Long millis;

    private JSONObject json;

    private ProgressBar mSpinner;

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        setupUI(findViewById(R.id.createGroupPage));

        mDisplayDate = (EditText) findViewById(R.id.expireDate);
        mGroupName = (EditText) findViewById(R.id.CreateGroupName);
        mDisplayDate.setInputType(InputType.TYPE_NULL);
        mDisplayDate.setFocusable(false);

        mDisplayTime = (EditText) findViewById(R.id.expireTime);
        mDisplayTime.setInputType(InputType.TYPE_NULL);
        mDisplayTime.setFocusable(false);
        mCreateBtn = (Button) findViewById(R.id.createGroupBtn);
        mSpinner = (ProgressBar) findViewById(R.id.progressBar1);

        mSpinner.setVisibility(View.GONE);

        client = new OkHttpClient();

        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        if(b!=null)
        {
            idToken =(String) b.get("ID_TOKEN");
            url = (String) b.get("URL");
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create New Event");
        }

        mDisplayDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);


                DatePickerDialog dialog = new DatePickerDialog(
                        CreateGroupActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 5000);
                dialog.show();
            }
        });

        mDisplayTime.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Calendar cal = Calendar.getInstance();
                int hours = cal.get(Calendar.HOUR_OF_DAY);
                int minutes = cal.get(Calendar.MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(
                        CreateGroupActivity.this,
                        mTimeSetListener,
                        hours,
                        minutes, true);

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }

        });

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                Log.d(TAG, "onDateSet: dd/mm/yyy: " + day + "/" + month + "/" + year);

                String date_str = day + "/" + month + "/" + year;

                mDisplayDate.setText(date_str);
            }
        };

        mTimeSetListener = new TimePickerDialog.OnTimeSetListener(){
            @Override
            public void onTimeSet(TimePicker timePicker, int hours, int minutes){
                String mins = Integer.toString(minutes);
                if(minutes < 10){
                    mins = "0" + minutes;
                }
                String time_str = hours+":"+mins;

                mDisplayTime.setText(time_str);
            }

        };

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String groupName = mGroupName.getText().toString();
                String date_str = mDisplayDate.getText().toString();
                String time_str = mDisplayTime.getText().toString();

                if(groupName.length() == 0){
                    Toast.makeText(CreateGroupActivity.this, R.string.ng_groupName_missing,
                            Toast.LENGTH_SHORT).show();
                    return;
                }else if(date_str.length() == 0){
                    Toast.makeText(CreateGroupActivity.this, R.string.ng_expireDate_missing,
                            Toast.LENGTH_SHORT).show();
                    return;
                }else if(date_str.length() == 0){
                    Toast.makeText(CreateGroupActivity.this, R.string.ng_expireTime_missing,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                mSpinner.setVisibility(View.VISIBLE);



                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                Date date = null;
                try {
                    date = sdf.parse(date_str+" "+time_str);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                millis = date.getTime();
                Log.d(TAG, millis.toString());
                Long expiration = millis;

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                json = new JSONObject();
                try {
                    json.put("idToken", idToken);
                    json.put("groupName", groupName);
                    json.put("expiration", expiration);

                    Log.d(TAG, json.toString());



                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Request request = new Request.Builder()
                            .url(url+"/create")
                            .post(body)
                            .build();

                    client.newCall(request).enqueue(new Callback() {

                        Handler handler = new Handler(CreateGroupActivity.this.getMainLooper());

                        @Override
                        public void onFailure(Call call, IOException e) {
                            call.cancel();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                            String responseData = response.body().string();

                            try {
                                JSONObject j = new JSONObject(responseData);
                                String groupId = j.getString("groupId");
                                FirebaseMessaging.getInstance().subscribeToTopic(groupId);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mSpinner.setVisibility(View.GONE);
                                    CreateGroupActivity.this.finish();
                                }
                            });

                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    View view = CreateGroupActivity.this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }




}
