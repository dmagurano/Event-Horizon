package fi.aalto.mcc.mcc.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseUser;

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
    private TextView mGroupName;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private Button mCreateBtn;
    private String idToken;
    private String url;
    private Long millis;

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        mDisplayDate = (TextView) findViewById(R.id.expireDate);
        mGroupName = (EditText) findViewById(R.id.CreateGroupName);
        mDisplayDate.setInputType(InputType.TYPE_NULL);
        mCreateBtn = (Button) findViewById(R.id.createGroupBtn);

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
            getSupportActionBar().setTitle("Create New Group");
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

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                Log.d(TAG, "onDateSet: mm/dd/yyy: " + month + "/" + day + "/" + year);

                String date_str = month + "/" + day + "/" + year;

                mDisplayDate.setText(date_str);
            }
        };

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = mGroupName.getText().toString();
                String date_str = mDisplayDate.getText().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

                Date date = null;
                try {
                    date = sdf.parse(date_str);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                millis = date.getTime();
                Log.d(TAG, millis.toString());
                Long expiration = millis;

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                JSONObject json = new JSONObject();
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
                        @Override
                        public void onFailure(Call call, IOException e) {
                            call.cancel();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final Response res = response;
                            CreateGroupActivity.this.finish();
                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

    }


}
