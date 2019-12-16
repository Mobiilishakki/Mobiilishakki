package fi.helsinki.mobiilishakki;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MenuActivity extends AppCompatActivity {

    // Identificator for debug messages
    private String TAG = "MenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    /**
     * Calling this function will close the application.
     *
     * @param view
     */
    public void exitApplication(View view) {
        Log.i(TAG, "Called exitApplication");
        this.finishAffinity();
    }

    /**
     * Calling this function will switch to CameraActivity.
     */
    public void startCameraActivity(View view) {
        Log.i(TAG, "Called startCameraActivity");
        Intent intent = new Intent(this, MobiiliShakki.class);
        startActivity(intent);
    }
}