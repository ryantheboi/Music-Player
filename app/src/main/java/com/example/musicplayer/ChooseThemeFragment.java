package com.example.musicplayer;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ChooseThemeFragment extends Fragment {

    private static final String MESSENGER_TAG = "Messenger";

    private Messenger mainActivity_messenger;
    private ImageView palette_background;
    private ImageView exit_background;
    private ImageButton light_btn;
    private ImageButton night_btn;
    private ImageButton ram_btn;
    private ImageButton rem_btn;
    private ImageButton subaru_btn;
    private ImageButton puck_btn;
    private ImageButton sucrose_btn;
    private ImageButton bronya_btn;
    private ImageButton noelle_btn;
    private Animation slide_in_palette_anim;
    private MainActivity mainActivity;

    public static final int THEME_SELECTED = 99;
    public static final int THEME_DONE = 98;

    public ChooseThemeFragment() {
        super(R.layout.activity_choosetheme);
    }

    public static ChooseThemeFragment getInstance(Messenger messenger) {
        ChooseThemeFragment fragment = new ChooseThemeFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MESSENGER_TAG, messenger);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.mainActivity_messenger = getArguments().getParcelable(MESSENGER_TAG);
        }
        this.mainActivity = (MainActivity) getActivity();
        initAnimations();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.startAnimation(slide_in_palette_anim);
        initViews(view);
        initBackgroundClickListeners();
        initThemeBtnClickListeners();
    }

    @Override
    public void onDetach() {
        finish();
        super.onDetach();
    }

    private void initViews(View view) {
        palette_background = view.findViewById(R.id.palette_background);
        exit_background = view.findViewById(R.id.palette_exit_background);
        light_btn = view.findViewById(R.id.btn_light);
        night_btn = view.findViewById(R.id.btn_night);
        ram_btn = view.findViewById(R.id.btn_ram);
        rem_btn = view.findViewById(R.id.btn_rem);
        subaru_btn = view.findViewById(R.id.btn_subaru);
        puck_btn = view.findViewById(R.id.btn_puck);
        sucrose_btn = view.findViewById(R.id.btn_sucrose);
        bronya_btn = view.findViewById(R.id.btn_bronya);
        noelle_btn = view.findViewById(R.id.btn_noelle);
    }

    private void initAnimations(){
        slide_in_palette_anim = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_palette_animation);
    }

    private void initBackgroundClickListeners(){
        // set background images to finish activity on click
        palette_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.getSupportFragmentManager().popBackStackImmediate();
                onDetach();
            }
        });

        exit_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.getSupportFragmentManager().popBackStackImmediate();
                onDetach();
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

        // ram mode
        ram_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicRam);
            }
        });

        rem_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicRem);
            }
        });

        subaru_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicSubaru);
            }
        });

        puck_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicPuck);
            }
        });

        sucrose_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicSucrose);
            }
        });

        bronya_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicBronya);
            }
        });
        noelle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(R.style.ThemeOverlay_AppCompat_MusicNoelle);
            }
        });
    }

    /**
     * Set the theme, update ThemeColors to utilize current theme values, and inform main activity
     * @param resid the resource id of the theme that was selected by the user
     */
    private void updateTheme(int resid) {
        getActivity().setTheme(resid);
        ThemeColors.generateThemeValues(getContext(), resid);

        // inform the main activity that a theme was selected
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", THEME_SELECTED);
        msg.setData(bundle);
        try {
            mainActivity_messenger.send(msg);
        } catch (RemoteException e) {
            Logger.logException(e, "ChooseThemeActivity");
        }
    }

    public void finish() {
        // inform main activity that the user is done choosing a theme
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", THEME_DONE);
        msg.setData(bundle);
        try {
            mainActivity_messenger.send(msg);
        } catch (RemoteException e) {
            Logger.logException(e, "ChooseThemeActivity");
        }
    }
}
