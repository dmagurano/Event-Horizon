package fi.aalto.mcc.mcc.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import fi.aalto.mcc.mcc.R;


public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String TAG = "Login";

    SharedPreferences prefs = null;

    private Button mLoginBtn;
    private Button mSignupBtn;
    private EditText mEmailInput;
    private EditText mPasswdInput;
    private ProgressBar mSpinner;
    private DatabaseReference mUserDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupUI(findViewById(R.id.loginPage));

        mAuth = FirebaseAuth.getInstance();

        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mSignupBtn = (Button) findViewById(R.id.signupBtn);
        mEmailInput = (EditText) findViewById(R.id.emailInput);
        mPasswdInput = (EditText) findViewById(R.id.passwdInput);

        mUserDatabase = FirebaseDatabase.getInstance().getReference("Users");

        mSpinner = (ProgressBar) findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);

        prefs = getSharedPreferences("fi.aalto.mcc.mcc", MODE_PRIVATE);


        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logIn();
            }
        });

        mSignupBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

    }

    private void logIn(){
        String email = mEmailInput.getText().toString().trim();
        String passwd = mPasswdInput.getText().toString().trim();

        if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(passwd)){

            mSpinner.setVisibility(View.VISIBLE);


            mAuth.signInWithEmailAndPassword(email, passwd)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            Handler handler = new Handler(LoginActivity.this.getMainLooper());

                            Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Log.d(TAG, user.getUid());

                                Log.d(TAG, user.getUid() + " " + Boolean.toString(prefs.getBoolean("firstTimeUser_"+user.getUid(),false)));

                                if(prefs.getBoolean("firstTimeUser_"+user.getUid(), false)){

                                    Intent i = new Intent(LoginActivity.this, WelcomeActivity.class);
                                    startActivity(i);

                                    prefs.edit().putBoolean("firstTimeUser_"+user.getUid(), false).commit();
                                }else {
                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(i);
                                }
                            }else{
                                Log.w(TAG, "signInWithEmail:failed", task.getException());
                                Toast.makeText(LoginActivity.this, R.string.auth_failed,
                                        Toast.LENGTH_SHORT).show();
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mSpinner.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
        }
    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    View view = LoginActivity.this.getCurrentFocus();
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
