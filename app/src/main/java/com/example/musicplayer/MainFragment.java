package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class MainFragment extends Fragment {
    private Messenger mainActivityMessenger;
    private MainActivity mainActivity;

    private CoordinatorLayout musicListRelativeLayout;
    private Toolbar toolbar;
    private TextView toolbar_title;
    private EditText searchFilter_editText;
    private TabLayout tabLayout;
    private DynamicViewPager viewPager;
    private ActionBar actionBar;
    private ImageView searchFilter_btn;
    private RippleDrawable searchFilter_btn_ripple;
    private SongListAdapter songListAdapter;
    private PlaylistAdapter playlistAdapter;
    private ImageView theme_btn;
    private RippleDrawable theme_btn_ripple;
    private boolean isThemeSelecting;
    private PagerAdapter pagerAdapter;
    private boolean isCreated = false;

    public MainFragment(Messenger mainActivityMessenger) {
        super(R.layout.fragment_main);
        this.mainActivityMessenger = mainActivityMessenger;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();

        musicListRelativeLayout = view.findViewById(R.id.layout_mainfragment);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar_title = view.findViewById(R.id.toolbar_title);
        searchFilter_editText = view.findViewById(R.id.toolbar_searchFilter);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        setHasOptionsMenu(true);
        initActionBar();

        if (isCreated){
            initViewPagerTabs();
        }
        else{
            isCreated = true;
        }
    }

    @Override
    @TargetApi(23)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflate the menu for the actionbar, if it is present.
        inflater.inflate(R.menu.toolbar_menu, menu);

        // theme button menu item
        FrameLayout menuitem_theme_layout = (FrameLayout) menu.findItem(R.id.menuitem_theme).getActionView();
        theme_btn = menuitem_theme_layout.findViewById(R.id.icon);
        theme_btn_ripple = (RippleDrawable) theme_btn.getBackground();
        theme_btn_ripple.setRadius((int) getResources().getDimension(R.dimen.theme_button_ripple));
        initThemeButton();

        // search filter button menu item and its corresponding edittext
        FrameLayout menuitem_searchfilter_layout = (FrameLayout) menu.findItem(R.id.menuitem_searchfilter).getActionView();
        searchFilter_btn = menuitem_searchfilter_layout.findViewById(R.id.icon);
        searchFilter_btn_ripple = (RippleDrawable) searchFilter_btn.getBackground();
        searchFilter_btn_ripple.setRadius((int) getResources().getDimension(R.dimen.searchfilter_button_ripple));
        super.onCreateOptionsMenu(menu,inflater);
    }

    /**
     * Sets the action bar as the toolbar, which can be overlaid by an actionmode
     */
    private void initActionBar(){
        // using toolbar as ActionBar without title
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    /**
     * should be initialized last to set the touch listener for all views
     */
    @TargetApi(23)
    @SuppressLint("ClickableViewAccessibility")
    private void initFilterSearch() {
        // set this click listener to manually call the action mode's click listener
        searchFilter_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchFilter_editText.hasFocus()){
                    // hide keyboard and clear focus and text from searchfilter
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(
                                    Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(
                            getActivity().getCurrentFocus().getWindowToken(), 0);
                    searchFilter_editText.clearFocus();
                    searchFilter_editText.getText().clear();
                    searchFilter_editText.setVisibility(View.INVISIBLE);
                }
                else {
                    // show keyboard and focus on the searchfilter
                    searchFilter_editText.setVisibility(View.VISIBLE);
                    if (searchFilter_editText.requestFocus()) {
                        InputMethodManager inputMethodManager = (InputMethodManager)
                                getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            }
        });

        // init searchfilter text usage functionality
        searchFilter_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // filter based on song name
                songListAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        // init arraylist of all views under the sliding up panel layout
        ArrayList<View> views = getAllChildren(mainActivity.getMainActivityLayout());

        // theme btn is a menu item (not a child view of this layout), so manually add to arraylist
        views.add(theme_btn);

        // set touch listener for all views to hide the keyboard when touched
        for (View innerView : views){
            // excluding the searchfilter and its button
            if (innerView != searchFilter_editText && innerView != searchFilter_btn) {
                innerView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // only attempt to hide keyboard if the list filter is in focus
                        if (searchFilter_editText.hasFocus()) {
                            InputMethodManager inputMethodManager =
                                    (InputMethodManager) getActivity().getSystemService(
                                            Activity.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(
                                    getActivity().getCurrentFocus().getWindowToken(), 0);
                        }
                        searchFilter_editText.clearFocus();
                        return false;
                    }
                });
            }
        }
    }

    public void initThemeButton() {
        isThemeSelecting = false;
        final Intent chooseThemeIntent = new Intent(getContext(), ChooseThemeActivity.class);
        chooseThemeIntent.putExtra("mainActivityMessenger", mainActivityMessenger);
        final Animation rotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_themebtn_animation);

        final MainFragment mainFragment = this;
        theme_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isThemeSelecting) {
                    isThemeSelecting = true;
                    theme_btn.startAnimation(rotate);
                    startActivity(chooseThemeIntent);
                }
            }
        });

    }

    public void initViewPagerTabs(){
        // remove any existing tabs prior to activity pause and re-add
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Playlists"));

        pagerAdapter = new PagerAdapter(getChildFragmentManager(), tabLayout.getTabCount(), songListAdapter, playlistAdapter, mainActivityMessenger, mainActivity);

        // set dynamic viewpager
        viewPager.setMaxPages(pagerAdapter.getCount());
        viewPager.setBackgroundAsset(ThemeColors.getThemeBackgroundAssetResourceId());
        viewPager.setAdapter(pagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setPageTransformer(true, new ViewPager.PageTransformer() {
            private static final float MIN_SCALE = 0.85f;
            private static final float MIN_ALPHA = 0.5f;

            @Override
            public void transformPage(@NonNull View view, float position) {
                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();

                if (position < -1) { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    view.setAlpha(0f);

                } else if (position <= 1) { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                    float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                    float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                    if (position < 0) {
                        view.setTranslationX(horzMargin - vertMargin / 2);
                    } else {
                        view.setTranslationX(-horzMargin + vertMargin / 2);
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                    // Fade the page relative to its size.
                    view.setAlpha(MIN_ALPHA +
                            (scaleFactor - MIN_SCALE) /
                                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

                } else { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    view.setAlpha(0f);
                }
            }
        });
    }

    private void updateTabLayoutColors(){
        tabLayout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        tabLayout.setTabTextColors(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.TAB_TEXT_COLOR)));
        tabLayout.setSelectedTabIndicatorColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
    }

    private void updateViewPager(){
        viewPager.setBackgroundAsset(ThemeColors.getThemeBackgroundAssetResourceId());
    }

    @TargetApi(21)
    private void updateSearchFilterColors(){
        searchFilter_editText.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        searchFilter_editText.setHintTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        searchFilter_editText.setBackgroundTintList(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));

        // update the drawable vector color
        Drawable unwrappedDrawableSearchFilter = searchFilter_btn.getDrawable();
        Drawable wrappedDrawableSearchFilter = DrawableCompat.wrap(unwrappedDrawableSearchFilter);
        DrawableCompat.setTint(wrappedDrawableSearchFilter, getResources().getColor(ThemeColors.getDrawableVectorColorId()));

        // update the ripple color
        searchFilter_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }

    @TargetApi(21)
    private void updateActionBarColors(){
        // set color of the toolbar, which is the support action bar, and its title
        toolbar.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        toolbar_title.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));

        // update ripple color of theme button
        theme_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }

    /**
     * Apply the current theme's colors to this fragment
     */
    public void updateFragmentColors() {
        theme_btn.setImageResource(ThemeColors.getThemeBtnResourceId());
        musicListRelativeLayout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        SongListTab.toggleTabColor();
        PlaylistTab.toggleTabColor();
        updateTabLayoutColors();
        updateViewPager();
        updateSearchFilterColors();
        updateActionBarColors();
    }

    /**
     * Rotate the theme button back to its original position after theme selection is done
     */
    public void rotateThemeButton(){
        // user is finished selecting a theme
        isThemeSelecting = false;

        // rotate theme btn back to original orientation
        theme_btn.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_themebtn_reverse_animation));
    }

    /**
     * Recursively find all child views (if any) from a view
     * @param v the view to find all children from
     * @return an arraylist of all child views under v
     */
    private ArrayList<View> getAllChildren(View v) {

        // base case for when the view is not a ViewGroup or is a ListView
        if (!(v instanceof ViewGroup) || (v instanceof ListView)) {
            ArrayList<View> viewArrayList = new ArrayList();
            viewArrayList.add(v);
            return viewArrayList;
        }

        // recursive case to add all child views from the ViewGroup, including the ViewGroup
        ArrayList<View> children = new ArrayList();

        ViewGroup viewGroup = (ViewGroup) v;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {

            View child = viewGroup.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            children.addAll(viewArrayList);
        }
        return children;
    }

    public void setAdapters(SongListAdapter sa, PlaylistAdapter pa){
        songListAdapter = sa;
        playlistAdapter = pa;
        initViewPagerTabs();

        // should be initialized last to set the touch listener for all views
        initFilterSearch();
    }

    public void addPlaylist(Playlist playlist){
        playlistAdapter.add(playlist);
    }

    public void updateMainFragment(Object object, final int operation) {
        switch (operation) {
            case DatabaseRepository.ASYNC_INSERT_PLAYLIST:
                // update current playlist adapter with the newly created playlist
                playlistAdapter.add((Playlist) object);
                playlistAdapter.notifyDataSetChanged();

                // move to playlist tab
                viewPager.setCurrentItem(PagerAdapter.PLAYLISTS_TAB);
                break;
            case DatabaseRepository.ASYNC_MODIFY_PLAYLIST:
                Playlist temp_playlist = (Playlist) object;
                Playlist original_playlist = (Playlist) playlistAdapter.getItem(playlistAdapter.getPosition(temp_playlist));

                // check if the playlist being modified is transient
                if (temp_playlist.getTransientId() > 0){
                    // replace old transient playlist with new
                    playlistAdapter.remove(original_playlist);
                    playlistAdapter.add(temp_playlist);
                    playlistAdapter.notifyDataSetChanged();
                }

                // check if songs were removed from existing playlist
                else if (original_playlist.getSize() > temp_playlist.getSize()) {
                    // modify the original playlist to adopt the changes
                    original_playlist.adoptSongList(temp_playlist);

                    // reconstruct viewpager adapter to reflect changes to individual playlist
                    pagerAdapter = new PagerAdapter(getChildFragmentManager(), tabLayout.getTabCount(), songListAdapter, playlistAdapter, mainActivityMessenger, mainActivity);
                    viewPager.setAdapter(pagerAdapter);

                    // adjust tab colors
                    SongListTab.toggleTabColor();
                    PlaylistTab.toggleTabColor();

                    // move to playlist tab
                    viewPager.setCurrentItem(PagerAdapter.PLAYLISTS_TAB);
                }

                // playlist was simply renamed, or extended, notify playlist adapter
                else {
                    playlistAdapter.notifyDataSetChanged();
                }
                break;
            case DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID:
                ArrayList<Playlist> playlists = (ArrayList<Playlist>) object;

                for (Playlist playlist : playlists) {
                    playlistAdapter.remove(playlist);
                }
        }
    }
}
