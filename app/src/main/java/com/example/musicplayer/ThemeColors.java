package com.example.musicplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.SparseIntArray;
import android.util.TypedValue;
import androidx.palette.graphics.Palette;
import java.util.List;

public class ThemeColors {
    public static final int COLOR_PRIMARY = 0;
    public static final int COLOR_SECONDARY = 1;
    public static final int COLOR_ACCENT = 2;
    public static final int ITEM_TEXT_COLOR = 3;
    public static final int TAB_TEXT_COLOR = 4;
    public static final int TITLE_TEXT_COLOR = 5;
    public static final int SUBTITLE_TEXT_COLOR = 6;

    private static SparseIntArray themeValues = new SparseIntArray();
    private static Palette.Swatch vibrantSwatch;
    private static Palette.Swatch darkVibrantSwatch;
    private static Palette.Swatch dominantSwatch;
    private static Palette.Swatch contrastSwatch;
    private static List<Palette.Swatch> swatchList;

    /**
     * Generates the theme values by resolving the theme attr values from the context's theme
     * Theme value entries are stored in a SparseIntArray where:
     * The key is the color code constant defined in this class
     * The value is the color value, or a resource id representing a stateful selector
     * @param context the calling context that sets the theme prior to generating the theme values
     */
    @TargetApi(18)
    public static void generateThemeValues(Context context){
        TypedValue colorPrimaryValue = new TypedValue();
        TypedValue colorSecondaryValue = new TypedValue();
        TypedValue colorAccentValue = new TypedValue();
        TypedValue itemTextColorValue = new TypedValue();
        TypedValue tabTextColorValue = new TypedValue();
        TypedValue titleTextColorValue = new TypedValue();
        TypedValue subtitleTextColorValue = new TypedValue();

        context.getTheme().resolveAttribute(R.attr.colorPrimary, colorPrimaryValue, true);
        context.getTheme().resolveAttribute(R.attr.colorSecondary, colorSecondaryValue, true);
        context.getTheme().resolveAttribute(R.attr.colorAccent, colorAccentValue, true);
        context.getTheme().resolveAttribute(R.attr.itemTextColor, itemTextColorValue, true);
        context.getTheme().resolveAttribute(R.attr.tabTextColor, tabTextColorValue, true);
        context.getTheme().resolveAttribute(R.attr.titleTextColor, titleTextColorValue, true);
        context.getTheme().resolveAttribute(R.attr.subtitleTextColor, subtitleTextColorValue, true);

        themeValues.append(COLOR_PRIMARY, context.getResources().getColor(colorPrimaryValue.resourceId));
        themeValues.append(COLOR_SECONDARY, context.getResources().getColor(colorSecondaryValue.resourceId));
        themeValues.append(COLOR_ACCENT, context.getResources().getColor(colorAccentValue.resourceId));
        themeValues.append(ITEM_TEXT_COLOR, itemTextColorValue.resourceId);
        themeValues.append(TAB_TEXT_COLOR, tabTextColorValue.resourceId);
        themeValues.append(TITLE_TEXT_COLOR, context.getResources().getColor(titleTextColorValue.resourceId));
        themeValues.append(SUBTITLE_TEXT_COLOR, subtitleTextColorValue.resourceId);
    }

    /**
     * Synchronously generates up to 8 palette colors from a bitmap image
     * @param image the image to extract palette swatches from
     */
    public static void generatePaletteColors(Bitmap image){
        Palette palette = Palette.from(image).maximumColorCount(8).generate();
        vibrantSwatch = palette.getVibrantSwatch();
        darkVibrantSwatch = palette.getDarkVibrantSwatch();
        dominantSwatch = palette.getDominantSwatch();
        swatchList = palette.getSwatches();
    }

    /**
     * Gets the specified theme attr color
     * @param color the color constant code defined in this class
     * @return the int color value (or selector value) associated with the constant color code
     */
    @TargetApi(18)
    public static int getColor(int color){
        return themeValues.get(color);
    }

    /**
     * Finds the color, among the list of swatches, that contrasts the most against the base
     * If there are no swatches in the list, default to gray
     * @param base the color to contrast against
     * @return a color, selected from the list of swatches available
     */
    public static int getContrastColor(int base){
        if (swatchList != null) {
            contrastSwatch = swatchList.get(0);
            int maxDiffRGB = 0;
            for (Palette.Swatch swatch : swatchList) {
                if (Math.abs(base - swatch.getRgb()) > maxDiffRGB) {
                    maxDiffRGB = Math.abs(base - swatch.getRgb());
                    contrastSwatch = swatch;
                }
            }
            return contrastSwatch.getRgb();
        }
        else {
            return Color.GRAY;
        }
    }

    /**
     * Overloaded method to get the contrast color if it already exists, otherwise returns gray
     * @return a color representing the contrast
     */
    public static int getContrastColor(){
        if (contrastSwatch != null){
            return contrastSwatch.getRgb();
        }
        else {
            return Color.GRAY;
        }
    }

    public static int getVibrantColor(){
        if (vibrantSwatch != null){
            return vibrantSwatch.getRgb();
        }
        else if (dominantSwatch != null){
            return dominantSwatch.getRgb();
        }
        return (Color.GRAY);
    }

    public static int getDarkVibrantColor(){
        if (darkVibrantSwatch != null){
            return darkVibrantSwatch.getRgb();
        }
        else if (dominantSwatch != null){
            return dominantSwatch.getRgb();
        }
        return (Color.GRAY);
    }

    public static int getDominantColor(){
        if (dominantSwatch != null){
            return dominantSwatch.getRgb();
        }
        return (Color.GRAY);
    }
}
