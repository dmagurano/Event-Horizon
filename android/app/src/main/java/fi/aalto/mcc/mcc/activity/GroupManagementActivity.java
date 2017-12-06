package fi.aalto.mcc.mcc.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.model.GroupObject;
import fi.aalto.mcc.mcc.model.UserObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class GroupManagementActivity extends AppCompatActivity {

    private static boolean USER_IN_GROUP;
    private UserObject user_obj = null;
    private GroupObject group_obj = null;
    private FirebaseUser mUser;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mGroupDatabase;
    private Button mLeaveBtn;
    private Button mJoinBtn;
    private Button mCreateBtn;
    private Button mInviteBtn;
    private TextView mGroupName;
    private TextView mGroupExpiration;
    private ListView mGroupMembers;

    private OkHttpClient client;
    private String idToken;
    private JSONObject json;
    private String url;

    private ConstraintLayout no_group;
    private ConstraintLayout yes_group;

    private static String TAG = "GroupManagement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference("Users");
        mGroupDatabase = FirebaseDatabase.getInstance().getReference("Groups");
        no_group = (ConstraintLayout) findViewById(R.id.group_no);
        yes_group = (ConstraintLayout) findViewById(R.id.group_yes);

        mLeaveBtn = (Button) findViewById(R.id.deleteBtn);
        mInviteBtn = (Button) findViewById(R.id.inviteBtn);
        mCreateBtn = (Button) findViewById(R.id.createBtn);
        mJoinBtn = (Button) findViewById(R.id.joinBtn);

        mGroupName = (TextView) findViewById(R.id.groupName_text);
        mGroupExpiration = (TextView) findViewById(R.id.groupExpiration_text);
        mGroupMembers = (ListView) findViewById(R.id.membersList);

        client = new OkHttpClient();
        url = "http://10.0.2.2:8080"; //CHANGE THIS TO WHERE THE CUSTOM BACKEND IS RUNNING


        // Read from the user database
        mUserDatabase.child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_obj = dataSnapshot.getValue(UserObject.class);

                String group = user_obj.getGroup();

                USER_IN_GROUP = group != null;

                no_group.setVisibility(View.GONE);
                yes_group.setVisibility(View.GONE);

                final List<String> group_members_list = new ArrayList<String>();

                mUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            idToken = task.getResult().getToken();

                        } else {
                            // Handle error -> task.getException();
                            idToken = null;
                        }
                    }
                });

                if(USER_IN_GROUP){
                    yes_group.setVisibility(View.VISIBLE);
                    mGroupDatabase.child(user_obj.getGroup()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            group_obj = dataSnapshot.getValue(GroupObject.class);

                            mGroupName.setText(group_obj.getName());

                            Date date = new Date(group_obj.getExpirationDate());
                            Log.d(TAG, Long.toString(group_obj.getExpirationDate()));
                            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());


                            mGroupExpiration.setText(dateFormat.format(date));


                            final Map<String, Object> members = group_obj.getMembers();

                            for (Map.Entry<String, Object> entry : members.entrySet()){
                                String member = entry.getKey();
                                mUserDatabase.child(member).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        UserObject user = dataSnapshot.getValue(UserObject.class);
                                        group_members_list.add(user.getName());
                                        if(group_members_list.size() == members.size()){
                                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                                    getApplicationContext(),
                                                    android.R.layout.simple_list_item_1,
                                                    group_members_list );
                                            mGroupMembers.setAdapter(arrayAdapter);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        // Failed to read value
                                        Log.w(TAG, "Failed to read value.", error.toException());
                                    }
                                });
                            }


                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w(TAG, "Failed to read value.", error.toException());
                        }
                    });
                }else{
                    no_group.setVisibility(View.VISIBLE);
                }

                //Set button text depending on if the user is admin of the group
                if(group_obj != null && group_obj.getAdmin().equals(mUser.getUid())){
                    mLeaveBtn.setText(R.string.deleteBtn_text);
                }else{
                    mLeaveBtn.setText(R.string.leaveBtn_text);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });




        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(GroupManagementActivity.this, CreateGroupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        mJoinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //TODO: add group joining functionality here
                Intent rIntent = new Intent(GroupManagementActivity.this, ReaderActivity.class);
                startActivity(rIntent);

            }
        });

        mInviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mGroupDatabase.child(user_obj.getGroup()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        GroupObject group = dataSnapshot.getValue(GroupObject.class);
                        Log.d(TAG, group.toString());


                        json = new JSONObject();
                        try {
                            json.put("groupId", user_obj.getGroup());
                            json.put("groupToken", group.getSingleUseToken());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String input_str = json.toString();

                        Log.d(TAG, input_str);
                        Dialog qrDialog = new Dialog(GroupManagementActivity.this);
                        qrDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);


                        qrDialog.setContentView(getLayoutInflater().inflate(R.layout.qr_image_layout
                                , null));

                        ImageView iv = (ImageView) qrDialog.findViewById(R.id.qr_img);


                        QRCodeWriter qrCodeEncoder = new QRCodeWriter();

                        int width = getResources().getDimensionPixelSize(R.dimen.qr_img_width);
                        int height = getResources().getDimensionPixelSize(R.dimen.qr_img_height);

                        try {
                            BitMatrix bitMatrix = qrCodeEncoder.encode(input_str, BarcodeFormat.QR_CODE,
                                    width, height);
                            final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            for (int x = 0; x < width; x++) {
                                for (int y = 0; y < height; y++) {
                                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                                }
                            }
                            iv.setImageBitmap(bmp);
                        } catch (WriterException e) {
                            Log.d(TAG, "Something went wrong in creating BitMatrix");
                        }

                        qrDialog.show();

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Failed to read value
                        Log.w(TAG, "Failed to read value.", error.toException());
                    }
                });
            }
        });
        mLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                json = new JSONObject();
                try {
                    json.put("idToken", idToken);
                    json.put("groupId", user_obj.getGroup());

                    RequestBody body = RequestBody.create(JSON, json.toString());

                    Request request = new Request.Builder()
                            .url(url+"/leave")
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
                            GroupManagementActivity.this.runOnUiThread(new Runnable()
                            {
                                public void run()
                                {
                                    try {
                                        Toast.makeText(GroupManagementActivity.this, res.body().string(), Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
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



}
