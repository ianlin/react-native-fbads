/**
 * NativeAdManager.java
 * io.callstack.react.fbads;
 *
 * Created by Mike Grabowski on 25/09/16.
 * Copyright © 2016 Callstack.io. All rights reserved.
 */
package io.callstack.react.fbads;

import android.util.Log;

import com.facebook.ads.AdError;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdsManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.util.HashMap;
import java.util.Map;

public class NativeAdManager extends ReactContextBaseJavaModule implements NativeAdsManager.Listener {
    /** @{Map} with all registered fb ads managers **/
    private Map<String, NativeAdsManager> mAdsManagers = new HashMap<>();

    /** @{Map} with latest NativeAd from all managers **/
    private Map<String, NativeAd> mLatestAds = new HashMap<>();

    public NativeAdManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "CTKNativeAdManager";
    }

    /**
     * Initialises native ad manager for a given placement id and ads to request.
     * This method is run on the UI thread
     *
     * @param placementId
     * @param adsToRequest
     */
    @ReactMethod
    public void init(final String placementId, final int adsToRequest) {
        final ReactApplicationContext reactContext = this.getReactApplicationContext();

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final NativeAdsManager adsManager = new NativeAdsManager(reactContext, placementId, adsToRequest);

                adsManager.setListener(NativeAdManager.this);

                mAdsManagers.put(placementId, adsManager);

                adsManager.loadAds();
            }
        });
    }

    /**
     * Reload ads
     */
    @ReactMethod
    public void reloadAds(String placementId) {
        NativeAdsManager adsManager = mAdsManagers.get(placementId);
        adsManager.loadAds();
    }

    /**
     * Disables auto refresh
     *
     * @param placementId
     */
    @ReactMethod
    public void disableAutoRefresh(String placementId) {
        mAdsManagers.get(placementId).disableAutoRefresh();
    }

    /**
     * Sets media cache policy
     *
     * @param placementId
     * @param cachePolicy
     */
    @ReactMethod
    public void setMediaCachePolicy(String placementId, String cachePolicy) {
        Log.w("NativeAdManager", "This method is not supported on Android");
    }

    /**
     * Called when one of the registered ads managers loads ads. Sends state of all
     * managers back to JS
     */
    @Override
    public void onAdsLoaded() {
        WritableMap adsManagersState = Arguments.createMap();

        for (String key : mAdsManagers.keySet()) {
            NativeAdsManager adsManager = mAdsManagers.get(key);
            adsManagersState.putBoolean(key, adsManager.isLoaded());
        }

        sendAppEvent("CTKNativeAdsManagersChanged", adsManagersState);
    }

    @Override
    public void onAdError(AdError adError) {
        // @todo handle errors here
        WritableMap error = Arguments.createMap();
        error.putInt("code", adError.getErrorCode());
        error.putString("message", adError.getErrorMessage());
        sendAppEvent("CTKNativeAdsManagersError", error);
    }

    /**
     * Returns FBAdsManager for a given placement id
     *
     * @param placementId
     * @return
     */
    public NativeAdsManager getFBAdsManager(String placementId) {
        return mAdsManagers.get(placementId);
    }

    /**
     * Returns the next native ad for the given placement
     *
     * @param placementId
     * @return
     */
    public NativeAd getNextNativeAd(String placementId) {
        NativeAdsManager adsManager = this.getFBAdsManager(placementId);
        NativeAd ad = adsManager.nextNativeAd();
        mLatestAds.put(placementId, ad);
        return ad;
    }

    /**
     * Returns the latest native ad for the given placement
     *
     * @param placementId
     * @return NativeAd
     */
    public NativeAd getLastNativeAd(String placementId) {
        NativeAd ad;
        if (mLatestAds.containsKey(placementId)) {
            ad = mLatestAds.get(placementId);
        } else {
            ad = this.getNextNativeAd(placementId);
        }
        return ad;
    }

    /**
     * Helper for sending events back to Javascript.
     *
     * @param eventName
     * @param params
     */
    private void sendAppEvent(String eventName, Object params) {
        ReactApplicationContext context = this.getReactApplicationContext();

        if (context == null || !context.hasActiveCatalystInstance()) {
            return;
        }

        context
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit(eventName, params);
    }
}