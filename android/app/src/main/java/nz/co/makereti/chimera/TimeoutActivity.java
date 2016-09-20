package nz.co.makereti.chimera;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import nz.co.makereti.chimera.garage.GarageDoorControl;

public class TimeoutActivity extends AppCompatActivity {

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeout);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int waitTime = Integer.parseInt(sharedPreferences.getString("garageShutDoorTimeout", "60000"));
        final TextView chronometer = (TextView) findViewById(R.id.countDown);
        timer = new CountDownTimer(waitTime, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long timeUntilFinished) {
                chronometer.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(timeUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(timeUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeUntilFinished))));
            }

            @Override
            public void onFinish() {
                GarageDoorControl.closeDoor(getApplicationContext());
                finish();
            }
        };
        timer.start();
    }

    public void cancel(View view) {
        timer.cancel();
        finish();
    }
}
