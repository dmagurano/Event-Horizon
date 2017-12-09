package fi.aalto.mcc.mcc.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.security.acl.Group;

import fi.aalto.mcc.mcc.R;

public class WelcomeActivity extends AppCompatActivity {

    private Button mGetStartedBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mGetStartedBtn = (Button) findViewById(R.id.get_startedBtn);

        mGetStartedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WelcomeActivity.this, GroupManagementActivity.class);
                startActivity(i);
                WelcomeActivity.this.finish();
            }
        });
    }
}
