package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.Button;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

public class AlarmActivity extends AppCompatActivity {

    private TextView alarmText;
    private Vibrator vibrator;
    private static Ringtone ringtone;
    private static boolean isAlarmTriggered = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DeviceService.ACTION_REALTIME_SAMPLES.equals(intent.getAction())) {
                Log.d("AlarmActivity", "Received heart rate data update");
                ActivitySample sample = (ActivitySample) intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE);
                if (sample != null) {
                    int heartRate = sample.getHeartRate();
                    Log.d("HeartRateData", "Received heart rate data: " + heartRate);
                    if (heartRate < 60 || heartRate > 80) {
                        // Trigger the alarm
                        triggerAlarm();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // Make the activity show over the lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        alarmText = findViewById(R.id.alarm_text);

        // Get an instance of the Vibrator service
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        Button stopAlarmButton = findViewById(R.id.stop_vibration_button);
        stopAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarm(v);
            }
        });
    }

    private void triggerAlarm() {
        // Check if the alarm is already triggered
        if (isAlarmTriggered) {
            return;
        }
        isAlarmTriggered = true;

        // Start the vibration
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        // Play the ringtone
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop(); // Stop the current ringtone if it's playing
            }
            ringtone = RingtoneManager.getRingtone(this, ringtoneUri); // Store the ringtone instance
            if (ringtone != null) {
                ringtone.play();
            }
        }

        // Start the AlarmActivity if not already running
        if (!isFinishing() && !isDestroyed()) {
            Intent intent = new Intent(this, AlarmActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    public void stopAlarm(View view) {
        Log.d("AlarmActivity", "Stop Alarm button clicked");
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        isAlarmTriggered = false; // Reset the flag when the alarm is stopped
    }

    public void goBack(View view) {
        Intent intent = new Intent(this, ControlCenterv2.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }
}
