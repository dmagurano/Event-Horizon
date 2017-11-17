package com.example.mccgroup04.photoshareapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDB;
    private String TAG = "Register";

    private EditText mNameInput;
    private EditText mEmailInput;
    private EditText mPasswdInput;
    private EditText mPasswdConfirmInput;
    private Button mRegisterBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mUserDB = FirebaseDatabase.getInstance().getReference().child("Users");

        mNameInput = (EditText) findViewById(R.id.nameInput);
        mRegisterBtn = (Button) findViewById(R.id.registerBtn);
        mEmailInput = (EditText) findViewById(R.id.reg_emailInput);
        mPasswdInput = (EditText) findViewById(R.id.reg_passwdInput);
        mPasswdConfirmInput = (EditText) findViewById(R.id.reg_passwdConfirmInput);

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = mNameInput.getText().toString();
                String email = mEmailInput.getText().toString().trim();
                String passwd = mPasswdInput.getText().toString().trim();
                String confirmpasswd = mPasswdConfirmInput.getText().toString().trim();

                //Check that the user has written the password correctly both times
                if(passwd.equals(confirmpasswd)){
                    createAccount(name, email, passwd);
                }else{
                    Toast.makeText(RegisterActivity.this, R.string.pw_check_failed,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void createAccount(String name, String email, String passwd){
        Log.d(TAG, "createAccount: " + email);

        final String usr_email = email;
        final String usr_name = name;

        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(passwd)) {
            mAuth.createUserWithEmailAndPassword(email, passwd)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");

                                String uid = mAuth.getCurrentUser().getUid();

                                //This is where we create the user into the realtime DB
                                DatabaseReference DBuser = mUserDB.child(uid);

                                DBuser.child("name").setValue(usr_name);
                                DBuser.child("e-mail").setValue(usr_email);

                                //ADD OTHER FIELDS HERE IF NEEDED


                                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            } else {
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(RegisterActivity.this, R.string.auth_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }else{
            Toast.makeText(RegisterActivity.this, "Please fill all the fields",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
