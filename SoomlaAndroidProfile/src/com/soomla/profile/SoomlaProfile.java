/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.soomla.profile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.soomla.BusProvider;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaMarketUtils;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.events.UserRatingEvent;
import com.soomla.profile.events.ProfileInitializedEvent;
import com.soomla.profile.exceptions.ProviderNotFoundException;
import com.soomla.profile.exceptions.UserProfileNotFoundException;
import com.soomla.rewards.Reward;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the main class for the SOOMLA User Profile module.  This class
 * should be initialized once, after <code>Soomla.initialize()</code> is invoked.
 * Use this class to perform authentication and social actions on behalf of
 * the user that will grant him \ her rewards in your game.
 */
public class SoomlaProfile {

    /**
     * {@link #initialize(boolean, java.util.Map<com.soomla.profile.domain.IProvider.Provider, Map<String, String>>)}
     */
    public void initialize() {
        initialize(null);
    }

    /**
     * {@link #initialize(boolean, java.util.Map<com.soomla.profile.domain.IProvider.Provider, Map<String, String>>)}
     */
    public void initialize(Map<IProvider.Provider, HashMap<String, String>> customParams) {
        initialize(false, customParams);
    }

    /**
     * Initializes the Profile module.  Call this method after <code>Soomla.initialize()</code>
     *
     * @param usingExternalProvider If using an external SDK (like Unity FB SDK) pass true
     *                              here so we know not to complain about native providers
     *                              not found
     * @param customParams provides custom values for specific social providers
     */
    public void initialize(boolean usingExternalProvider, Map<IProvider.Provider, ? extends Map<String, String>> customParams) {

        mAuthController = new AuthController(usingExternalProvider, customParams);
        mSocialController = new SocialController(usingExternalProvider, customParams);

        BusProvider.getInstance().post(new ProfileInitializedEvent());
    }

    /**
     * Login to the given provider
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider) throws ProviderNotFoundException {
        login(activity, provider, "", null);
    }

    /**
     * Login to the given provider and grant the user a reward.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param reward   The reward to give the user for logging in.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        login(activity, provider, "", reward);
    }

    /**
     * Login to the given provider and grant the user a reward.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user for logging in.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider,
                      String payload, final Reward reward) throws ProviderNotFoundException {
        try {
            mAuthController.login(activity, provider, payload, reward);
        } catch (ProviderNotFoundException e) {
            mSocialController.login(activity, provider,  payload, reward);
        }
    }

    /**
     * Checks if the user is logged-in to the given provider
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @return true if the user is logged-in with the authentication provider,
     * false otherwise
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public boolean isLoggedIn(Activity activity, final IProvider.Provider provider) throws ProviderNotFoundException {
        try {
            return mAuthController.isLoggedIn(activity, provider);
        } catch (ProviderNotFoundException e) {
            return mSocialController.isLoggedIn(activity, provider);
        }
    }

    /**
     * Get the active access token from the given provider
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @return a String containing the access token or null if unavailable.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public String getAccessToken(Activity activity, final IProvider.Provider provider) throws ProviderNotFoundException {
        try {
            return mAuthController.getAccessToken(activity, provider);
        } catch (ProviderNotFoundException e) {
            return mSocialController.getAccessToken(activity, provider);
        }
    }

    /**
     * Logout of the given provider
     *
     * @param provider The provider to use
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void logout(final IProvider.Provider provider) throws ProviderNotFoundException {
        try {
            mAuthController.logout(provider);
        } catch (ProviderNotFoundException e) {
            mSocialController.logout(provider);
        }
    }

    /**
     * Fetches the user's profile for the given provider from the local device storage
     *
     * @param provider The provider to use
     * @return The user profile
     * @throws UserProfileNotFoundException if the supplied provider is not
     *                                      supported by the framework
     */
    public UserProfile getStoredUserProfile(IProvider.Provider provider) {
        UserProfile userProfile = mAuthController.getStoredUserProfile(provider);
        if (userProfile != null)
            return userProfile;

        return mSocialController.getStoredUserProfile(provider);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param status   The text to share
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatus(IProvider.Provider provider, String status, final Reward reward) throws ProviderNotFoundException {
        updateStatus(provider, status, "", reward);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param status   The text to share
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatus(IProvider.Provider provider, String status, String payload, final Reward reward) throws ProviderNotFoundException {
        mSocialController.updateStatus(provider, status, payload, reward);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider The provider to us
     * @param link     The link to share (could be null)
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatusDialog(IProvider.Provider provider, String link, final Reward reward) throws ProviderNotFoundException {
        updateStatusDialog(provider, link, "", reward);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider The provider to us
     * @param link     The link to share (could be null)
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatusDialog(IProvider.Provider provider, String link, String payload, final Reward reward) throws ProviderNotFoundException {
        mSocialController.updateStatusDialog(provider, link, payload, reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     The main text which will appear in the story
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStory(IProvider.Provider provider, String message, String name, String caption,
                            String description, String link, String picture,
                            final Reward reward) throws ProviderNotFoundException {
        updateStory(provider, message, name, caption, description, link, picture, "", reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     The main text which will appear in the story
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStory(IProvider.Provider provider, String message, String name, String caption,
                            String description, String link, String picture, String payload,
                            final Reward reward) throws ProviderNotFoundException {
        mSocialController.updateStory(provider, message, name, caption, description, link, picture, payload, reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider    The provider to use
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStoryDialog(IProvider.Provider provider, String name, String caption,
                                  String description, String link, String picture,
                                  final Reward reward) throws ProviderNotFoundException {
        updateStoryDialog(provider, name, caption, description, link, picture, "", reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider    The provider to use
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStoryDialog(IProvider.Provider provider, String name, String caption,
                                  String description, String link, String picture, String payload,
                                  final Reward reward) throws ProviderNotFoundException {
        mSocialController.updateStoryDialog(provider, name, caption, description, link, picture, payload, reward);
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     A text that will accompany the image
     * @param fileName    The desired image's file name
     * @param bitmap      The image to share
     * @param jpegQuality The image's numeric quality
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward to grant for sharing the photo
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String fileName, Bitmap bitmap, int jpegQuality,
                            String payload, final Reward reward) throws ProviderNotFoundException {
        mSocialController.uploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward);
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     A text that will accompany the image
     * @param file        An image file handler
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward to grant for sharing the photo
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider, String message, File file,  String payload, final Reward reward) throws ProviderNotFoundException{
        mSocialController.uploadImage(provider, message, file, payload, reward);
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param filePath The desired image's location on the device
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String filePath,
                            final Reward reward) throws ProviderNotFoundException {
        uploadImage(provider, message, filePath, "", reward);
    }


    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param filePath The desired image's location on the device
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String filePath, String payload,
                            final Reward reward) throws ProviderNotFoundException {
        mSocialController.uploadImage(provider, message, filePath, payload, reward);
    }

    /**
     * Fetches the user's contact list and grants the user a reward.
     *
     * @param provider The provider to use
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getContacts(IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        getContacts(provider, "", reward);
    }

    /**
     * Fetches the user's contact list and grants the user a reward.
     *
     * @param provider The provider to use
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getContacts(IProvider.Provider provider, String payload, final Reward reward) throws ProviderNotFoundException {
        mSocialController.getContacts(provider, payload, reward);
    }

    /**
     * Fetches the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getFeed(IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        getFeed(provider, "", reward);
    }

    /**
     * Fetches the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getFeed(IProvider.Provider provider, String payload, final Reward reward) throws ProviderNotFoundException {
        mSocialController.getFeed(provider, payload, reward);
    }

    /**
     * Opens up a provider page to "like" (external), and grants the user the supplied reward
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param pageId The page to open up
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void like(final Activity activity, final IProvider.Provider provider,
                     String pageId,
                     final Reward reward) throws ProviderNotFoundException {
        mSocialController.like(activity, provider, pageId, reward);
    }

    /**
     * Utility method to open up the market application rating page
     *
     * @param context The main context of the app
     */
    public void openAppRatingPage(Context context) {
        SoomlaMarketUtils.openMarketAppPage(context);

        BusProvider.getInstance().post(new UserRatingEvent());
    }

    /**
     * Private Members *
     */

    private AuthController mAuthController;
    private SocialController mSocialController;


    /**
     * Singleton *
     */

    private SoomlaProfile() {
    }

    private static final SoomlaProfile mInstance = new SoomlaProfile();

    public static SoomlaProfile getInstance() {
        return mInstance;
    }

    private static final String TAG = "SOOMLA SoomlaProfile";
}
