package fi.aalto.mcc.mcc.activity;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

                if(USER_IN_GROUP){
                    yes_group.setVisibility(View.VISIBLE);
                    mGroupDatabase.child(user_obj.getGroup()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            group_obj = dataSnapshot.getValue(GroupObject.class);

                            mGroupName.setText(group_obj.getName());

                            Date date = new Date(group_obj.getExpirationDate() * 1000);
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

            }
        });

        mInviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //TODO: Add inviting functionality here (meaning, generate QR code and show it on the screen)

            }
        });
        mLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: IS THE LEAVING/DELETING MANAGED IN THE BACKEND OR HERE?

                if(group_obj.getAdmin().equals(mUser.getUid())){
                    //TODO: Delete the whole group from db
                }else{
                    //TODO: Leave the group
                }

            }
        });

    }



}
