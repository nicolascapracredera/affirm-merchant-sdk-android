package com.affirm.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK;
import static com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER;
import static com.affirm.android.AffirmConstants.PLACEHOLDER_END;
import static com.affirm.android.AffirmConstants.PLACEHOLDER_START;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT;

public final class AffirmUtils {

    private AffirmUtils() {
    }

    public static int decimalDollarsToIntegerCents(BigDecimal amount) {
        return amount.movePointRight(2).intValue();
    }

    static String readInputStream(@NonNull InputStream inputStream) throws IOException {
        final BufferedReader r =
                new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        final StringBuilder total = new StringBuilder();
        String line;

        while ((line = r.readLine()) != null) {
            total.append(line).append('\n');
        }

        inputStream.close();
        r.close();
        return total.toString();
    }

    static String replacePlaceholders(@NonNull String text, @NonNull Map<String, String> map) {
        for (Object o : map.entrySet()) {
            Map.Entry pair = (Map.Entry) o;

            final String placeholder = PLACEHOLDER_START + pair.getKey() + PLACEHOLDER_END;
            text = text.replace(placeholder, (String) pair.getValue());
        }

        return text;
    }

    static void debuggableWebView(@NonNull Context context) {
        if (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    static void hideActionBar(@NonNull AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (activity.getActionBar() != null) {
            activity.getActionBar().hide();
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }

    static void showCloseActionBar(@NonNull AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        Drawable drawable =
                ContextCompat.getDrawable(activity, R.drawable.affirm_ic_baseline_close);
        if (drawable != null) {
            drawable.setColorFilter(
                    ContextCompat.getColor(activity, R.color.affirm_ic_close_color),
                    PorterDuff.Mode.SRC_ATOP);
        }
        if (activity.getActionBar() != null) {
            activity.getActionBar().show();
            activity.getActionBar().setDisplayShowTitleEnabled(false);
            activity.getActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getActionBar().setHomeAsUpIndicator(drawable);
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setHomeAsUpIndicator(drawable);
        }
    }

    static Activity getActivityFromView(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static <T> void requireNonNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
    }

    public static <T> void requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }

    static SpannableString createSpannableForText(
            @NonNull String template,
            float textSize,
            @NonNull AffirmLogoType affirmLogoType,
            @NonNull AffirmColor affirmColor,
            @NonNull Context context
    ) {
        Resources resources = context.getResources();

        Drawable logoDrawable = null;
        if (affirmLogoType != AFFIRM_DISPLAY_TYPE_TEXT) {
            logoDrawable = resources.getDrawable(
                    affirmLogoType.getDrawableRes(affirmColor)).mutate();
        }

        return getSpannable(template, textSize, logoDrawable, affirmColor, resources);
    }

    private static SpannableString getSpannable(
            @NonNull String template,
            float textSize,
            @Nullable Drawable logoDrawable,
            @NonNull AffirmColor affirmColor,
            @NonNull Resources resources
    ) {
        SpannableString spannableString;

        int index = template.indexOf(LOGO_PLACEHOLDER);
        if (logoDrawable != null && index != -1) {
            spannableString = new SpannableString(template);
            ImageSpan imageSpan = getLogoSpan(textSize, logoDrawable, affirmColor, resources);
            spannableString.setSpan(
                    imageSpan,
                    index,
                    index + LOGO_PLACEHOLDER.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            );
        } else {
            String onlyText = template.replace(LOGO_PLACEHOLDER, "");
            spannableString = new SpannableString(onlyText);
        }

        return spannableString;
    }

    private static ImageSpan getLogoSpan(
            float textSize,
            @NonNull Drawable logoDrawable,
            @NonNull AffirmColor affirmColor,
            @NonNull Resources resources
    ) {

        float logoHeight = textSize * 1.f;
        float ratio = (float) logoDrawable.getIntrinsicWidth() / logoDrawable.getIntrinsicHeight();

        // Should not setColorFilter for blue_black & blue logo
        if (affirmColor != AFFIRM_COLOR_TYPE_BLUE_BLACK && affirmColor != AFFIRM_COLOR_TYPE_BLUE) {
            logoDrawable.setColorFilter(
                    resources.getColor(affirmColor.getColorRes()), PorterDuff.Mode.SRC_ATOP);
        }

        logoDrawable.setBounds(0, 0,
                Math.round(logoHeight * ratio), Math.round(logoHeight));
        return new ImageSpan(logoDrawable, ImageSpan.ALIGN_BASELINE);
    }
}
