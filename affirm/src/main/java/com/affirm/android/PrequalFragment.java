package com.affirm.android;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.Item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.ITEMS;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_PATH;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.REFERRING_URL;
import static com.affirm.android.AffirmTracker.TrackingEvent.PREQUAL_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingExceptionJsonObj;

public final class PrequalFragment extends AffirmFragment
        implements PrequalWebViewClient.Callbacks {

    private static final String PREQUAL = "Prequal";
    private static final String TAG = TAG_PREFIX + "." + PREQUAL;

    private Affirm.PrequalCallbacks listener;

    private String amount;
    private String promoId;
    private String pageType;
    private List<Item> items;

    private PrequalFragment() {
    }

    static PrequalFragment newInstance(@NonNull AppCompatActivity activity,
                                       @IdRes int containerViewId,
                                       @NonNull BigDecimal amount,
                                       @Nullable String promoId,
                                       @Nullable String pageType,
                                       List<Item> items) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return (PrequalFragment) fragmentManager.findFragmentByTag(TAG);
        }

        String stringAmount = String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        PrequalFragment fragment = new PrequalFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AMOUNT, stringAmount);
        bundle.putString(PROMO_ID, promoId);
        bundle.putString(PAGE_TYPE, pageType);
        if (items != null) {
            bundle.putParcelableArrayList(ITEMS, new ArrayList<>(items));
        }
        fragment.setArguments(bundle);

        addFragment(fragmentManager, containerViewId, fragment, TAG);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.requireNonNull(getArguments(), "mArguments cannot be null");

        amount = getArguments().getString(AMOUNT);
        promoId = getArguments().getString(PROMO_ID);
        pageType = getArguments().getString(PAGE_TYPE);
        items = getArguments().getParcelableArrayList(ITEMS);
    }

    @Override
    void initViews() {
        webView.setWebViewClient(new PrequalWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        StringBuilder path = new StringBuilder(String.format(PREQUAL_PATH,
                publicKey, amount, promoId, REFERRING_URL));
        if (pageType != null) {
            path.append("&page_type=").append(pageType);
        }
        if (items != null) {
            path.append("&items=").append(Uri.encode(AffirmPlugins.get().gson().toJson(items)));
        }
        webView.loadUrl(HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl() + path);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Affirm.PrequalCallbacks) {
            listener = (Affirm.PrequalCallbacks) context;
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(PREQUAL_WEBVIEW_FAIL, ERROR, createTrackingExceptionJsonObj(error));
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmPrequalError(error.toString());
        }
    }

    @Override
    public void onWebViewConfirmation() {
        removeFragment(TAG);
        if (getActivity() != null && getActivity() instanceof PrequalActivity) {
            getActivity().finish();
        }
    }
}
