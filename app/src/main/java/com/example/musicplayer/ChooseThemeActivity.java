package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageButton;

public class ChooseThemeActivity extends Activity {
    private Messenger mainActivity_messenger;
    public static final int CHOOSE_THEME_DONE = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // override the enter animation for this activity
        overridePendingTransition(R.anim.slide_in_palette_animation, R.anim.slide_out_palette_animation);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choosetheme);

        // obtain the intent that started this activity (should contain an extra with a messenger)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                if (key.equals("mainActivityMessenger")) {
                    // get the song which needs its details to be displayed
                    mainActivity_messenger = intent.getParcelableExtra("mainActivityMessenger");
                    break;
                }
            }
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        float width = getResources().getDimension(R.dimen.palette_width);
        float height = getResources().getDimension(R.dimen.palette_height);

        getWindow().setLayout((int) (width), (int) (height));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.TOP;
        params.x = (int) getResources().getDimension(R.dimen.palette_offset_x);
        params.y = (int) getResources().getDimension(R.dimen.palette_offset_y);

        getWindow().setAttributes(params);

        // set buttons
        ImageButton light = findViewById(R.id.btn_light);

        ImageButton night = findViewById(R.id.btn_night);
    }

    @Override
    public void finish() {
        // inform the main activity that this activity is finished
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", CHOOSE_THEME_DONE);
        msg.setData(bundle);
        try {
            mainActivity_messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        super.finish();

        // override the exit animation for this activity
        overridePendingTransition(0, R.anim.slide_out_palette_animation);
    }
}
