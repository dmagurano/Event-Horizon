package fi.aalto.mcc.mcc.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Handler;
import android.provider.ContactsContract;
import android.renderscript.Sampler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

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
import com.google.firebase.messaging.FirebaseMessaging;
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
import java.util.Locale;
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
import okhttp3.internal.cache.DiskLruCache;


public class GroupManagementActivity extends AppCompatActivity {

    private static boolean USER_IN_GROUP;
    private UserObject user_obj = null;
    private GroupObject group_obj = null;
    private String mGroup = null;
    private FirebaseUser mUser;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mGroupDatabase;
    private ValueEventListener mEventListener;
    private ValueEventListener mGroupEventListener;
    private Button mLeaveBtn;
    private Button mJoinBtn;
    private Button mCreateBtn;
    private Button mInviteBtn;
    private TextView mGroupName;
    private TextView mGroupExpiration;
    private ListView mGroupMembers;
    private ProgressBar mSpinner;

    private OkHttpClient client;
    private String idToken;
    private JSONObject json;
    private String url;

    private Integer dialog_msg;

    private ConstraintLayout no_group;
    private ConstraintLayout yes_group;

    private static String TAG = "GroupManagement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference("Users");
        mGroupDatabase = FirebaseDatabase.getInstance().getReference("Groups");
        no_group = (ConstraintLayout) findViewById(R.id.group_no);
        yes_group = (ConstraintLayout) findViewById(R.id.group_yes);
        idToken = null;

        mLeaveBtn = (Button) findViewById(R.id.deleteBtn);
        mInviteBtn = (Button) findViewById(R.id.inviteBtn);
        mCreateBtn = (Button) findViewById(R.id.createBtn);
        mJoinBtn = (Button) findViewById(R.id.joinBtn);

        mGroupName = (TextView) findViewById(R.id.txtGroupName);
        mGroupExpiration = (TextView) findViewById(R.id.txtGroupExpirity);
        mGroupMembers = (ListView) findViewById(R.id.membersList);


        mSpinner = (ProgressBar) findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);

        no_group.setVisibility(View.GONE);
        yes_group.setVisibility(View.GONE);

        client = new OkHttpClient();
        url = "https://mcc-fall-2017-g04.appspot.com"; //CHANGE THIS TO WHERE THE CUSTOM BACKEND IS RUNNING


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Information");
        }


        mUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "getIDTOKEN: " + task.getResult().getToken());
                    idToken = task.getResult().getToken();
                    mUserDatabase.child(mUser.getUid()).addValueEventListener(mEventListener);
                }
            }
        });


        mEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_obj = dataSnapshot.getValue(UserObject.class);

                String group = user_obj.getGroup();

                USER_IN_GROUP = group != null;

                final List<String> group_members_list = new ArrayList<String>();

                if(USER_IN_GROUP){
                    yes_group.setVisibility(View.VISIBLE);
                    mGroup = group;
                    mGroupEventListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            group_obj = dataSnapshot.getValue(GroupObject.class);

                            mGroupName.setText(group_obj.getName());

                            if(group_obj.getAdmin().equals(mUser.getUid())){
                                mLeaveBtn.setText(R.string.deleteBtn_text);
                                dialog_msg = R.string.delete_dialog;
                            }else{
                                mLeaveBtn.setText(R.string.leaveBtn_text);
                                dialog_msg = R.string.leave_dialog;
                            }
                            Date date = new Date(group_obj.getExpirationDate());
                            Log.d(TAG, Long.toString(group_obj.getExpirationDate()));
                            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);


                            mGroupExpiration.setText("Valid untill "+ dateFormat.format(date));


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
                            Integer error_code = error.getCode();

                            if(error_code.equals(DatabaseError.PERMISSION_DENIED)){
                                leaveGroup();
                            }
                        }
                    };

                    //The group event listener is added here and it's not needed to add it separately in onStart() or onResume() (this way we ensure that we have user_obj)

                    mGroupDatabase.child(user_obj.getGroup()).addValueEventListener(mGroupEventListener);
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
            public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
                Log.d(TAG, "mUser uid: " + mUser.getUid());
                Log.d(TAG, "Auth uid: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };





        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(GroupManagementActivity.this, CreateGroupActivity.class);
                i.putExtra("ID_TOKEN", idToken);
                i.putExtra("URL", url);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        mJoinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent rIntent = new Intent(GroupManagementActivity.this, ReaderActivity.class);
                rIntent.putExtra("ID_TOKEN", idToken);
                rIntent.putExtra("URL", url);
                startActivity(rIntent);

            }
        });

        mInviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user_obj == null){
                    return;
                }
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

                new AlertDialog.Builder(GroupManagementActivity.this)
                        .setMessage(dialog_msg)
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                leaveGroup();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                return;
                            }
                        })
                        .show();
            }
        });
    }

    protected void leaveGroup(){

        mSpinner.setVisibility(View.VISIBLE);


        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        json = new JSONObject();
        try {
            json.put("idToken", idToken);
            json.put("groupId", mGroup);

            Log.d(TAG, "LEAVE ID TOKEN: " + idToken);
            Log.d(TAG, "LEAVE GROUP ID: " + mGroup);

            RequestBody body = RequestBody.create(JSON, json.toString());

            Request request = new Request.Builder()
                    .url(url+"/leave")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                Handler handler = new Handler(GroupManagementActivity.this.getMainLooper());


                @Override
                public void onFailure(Call call, IOException e) {

                    call.cancel();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSpinner.setVisibility(View.GONE);
                        }
                    });

                    final Response res = response;

                    try {
                        String groupId = json.getString("groupId");
                        Log.d(TAG, "GroupId: " + groupId);
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(groupId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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



    @Override
    public void onStop() {
        mUserDatabase.child(mUser.getUid()).removeEventListener(mEventListener);
        if(mGroupEventListener != null && user_obj.getGroup() != null){
            mGroupDatabase.child(user_obj.getGroup()).removeEventListener(mGroupEventListener);
        }
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }



}
