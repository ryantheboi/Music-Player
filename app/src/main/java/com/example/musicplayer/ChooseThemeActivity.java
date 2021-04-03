package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ChooseThemeActivity extends Activity {
    private Messenger mainActivity_messenger;
    private ImageView palette_background;
    private ImageButton light_btn;
    private ImageButton night_btn;

    public static final int THEME_SELECTED = 99;
    public static final int THEME_DONE = 98;

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

        initViews();
        initBackgroundClickListener();
        initThemeBtnClickListeners();
        setWindowLayout();
    }

    private void initViews() {
        palette_background = findViewById(R.id.palette_background);
        light_btn = findViewById(R.id.btn_light);
        night_btn = findViewById(R.id.btn_night);
    }

    private void initBackgroundClickListener(){
        // set background image to finish activity on click
        palette_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initThemeBtnClickListeners(){
        // light mode
        light_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicLight);
            }
        });

        // night mode
        night_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicNight);
            }
        });
    }

    private void setWindowLayout() {
        float width = getResources().getDimension(R.dimen.palette_width);
        float height = getResources().getDimension(R.dimen.palette_height);

        getWindow().setLayout((int) (width), (int) (height));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.TOP;
        params.x = (int) getResources().getDimension(R.dimen.palette_offset_x);
        params.y = (int) getResources().getDimension(R.dimen.palette_offset_y);

        getWindow().setAttributes(params);
    }

    /**
     * Set the theme, update ThemeColors to utilize current theme values, and inform main activity
     * @param resid the resource id of the theme that was selected by the user
     */
    private void updateTheme(int resid) {
        setTheme(resid);
        ThemeColors.generateThemeValues(this, resid);

        // inform the main activity that a theme was selected
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", THEME_SELECTED);
        msg.setData(bundle);
        try {
            mainActivity_messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        // inform main activity that the user is done choosing a theme
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", THEME_DONE);
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
