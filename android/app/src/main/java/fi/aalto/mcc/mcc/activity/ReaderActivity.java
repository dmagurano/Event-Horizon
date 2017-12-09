package fi.aalto.mcc.mcc.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import fi.aalto.mcc.mcc.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReaderActivity extends AppCompatActivity {

    private OkHttpClient client;
    private String idToken;
    private String url;
    private static final String TAG = "ReaderActivity";
    private JSONObject json;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        final Activity activity = this;
        client = new OkHttpClient();



        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        if(b!=null)
        {
            idToken =(String) b.get("ID_TOKEN");
            url = (String) b.get("URL");
        }

        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null){
            if(result.getContents()==null){
                Toast.makeText(this, "You cancelled the scanning", Toast.LENGTH_LONG).show();
            }
            else {

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                String res = result.getContents();



                try {
                    json = new JSONObject(res);

                    json.put("idToken", idToken);
                    json.put("groupId", json.get("groupId"));
                    json.put("groupToken", json.get("groupToken"));

                    Log.d(TAG, json.toString());



                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Request request = new Request.Builder()
                            .url(url+"/join")
                            .post(body)
                            .build();



                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            call.cancel();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                            try {
                                String groupId = json.getString("groupId");
                                Log.d(TAG, "GroupId: " + groupId);
                                FirebaseMessaging.getInstance().subscribeToTopic(groupId);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "RESPONSE: " + response.toString());
                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }




            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        this.finish();
    }
}
