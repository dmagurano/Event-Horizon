package fi.aalto.mcc.mcc.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

import fi.aalto.mcc.mcc.R;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDB;
    private String TAG = "Register";

    private EditText mNameInput;
    private EditText mEmailInput;
    private EditText mPasswdInput;
    private EditText mPasswdConfirmInput;
    private Button mRegisterBtn;

    private SharedPreferences prefs;

    private ProgressBar mSpinner;

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

        mSpinner = (ProgressBar) findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);

        prefs = getSharedPreferences("fi.aalto.mcc.mcc", MODE_PRIVATE);


        setupUI(findViewById(R.id.registerPage));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create account");
        }



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
        final String usr_password = passwd;



        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(passwd)) {
            mSpinner.setVisibility(View.VISIBLE);



            mAuth.createUserWithEmailAndPassword(email, passwd)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            Handler handler = new Handler(RegisterActivity.this.getMainLooper());

                            Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");

                                String uid = mAuth.getCurrentUser().getUid();

                                //This is where we create the user into the realtime DB
                                DatabaseReference DBuser = mUserDB.child(uid);

                                DBuser.child("name").setValue(usr_name);
                                DBuser.child("e-mail").setValue(usr_email);

                                //ADD OTHER FIELDS HERE IF NEEDED

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSpinner.setVisibility(View.GONE);
                                    }
                                });

                                prefs.edit().putBoolean("firstTimeUser_"+uid, true).commit();
                                Log.d(TAG, uid + " " + Boolean.toString(prefs.getBoolean("firstTimeUser_"+uid,false)));

                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("email", usr_email);
                                returnIntent.putExtra("password", usr_password);
                                setResult(Activity.RESULT_OK, returnIntent);
                                finish();


                            } else {
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Intent returnIntent = new Intent();
                                setResult(Activity.RESULT_CANCELED, returnIntent);
                                finish();
                            }
                        }
                    });
        }else{
            Toast.makeText(RegisterActivity.this, "Please fill all the fields",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    View view = RegisterActivity.this.getCurrentFocus();
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
