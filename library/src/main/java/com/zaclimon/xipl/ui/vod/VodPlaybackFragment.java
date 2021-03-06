/*
 * Copyright 2017 Isaac Pateau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaclimon.xipl.ui.vod;

import android.app.FragmentTransaction;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v17.leanback.app.ErrorFragment;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.util.DisplayMetrics;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.zaclimon.xipl.R;
import com.zaclimon.xipl.player.ExoPlayerAdapter;
import com.zaclimon.xipl.player.SeekableTsExoPlayerAdapter;
import com.zaclimon.xipl.properties.VodProperties;

/**
 * Fragment responsible for a provider's VOD content playback
 *
 * @author zaclimon
 * Creation date: 11/08/17
 */

public abstract class VodPlaybackFragment extends VideoFragment {

    private PlaybackTransportControlGlue<ExoPlayerAdapter> mPlayerGlue;

    /**
     * Retrieves the properties for a given VOD content
     *
     * @return the properties for a given content
     */
    protected abstract VodProperties getVodProperties();

    /**
     * Retrieves the provider name for contact info if an error happens
     *
     * @return the provider name for this media content
     */
    protected abstract String getProviderName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExoPlayerAdapter exoPlayerAdapter;
        Bundle arguments = getArguments();
        String url = arguments.getString(VodTvSectionFragment.AV_CONTENT_LINK_BUNDLE);

        if (url != null && url.contains("duration=")) {
            String durationString = url.substring(url.lastIndexOf("=")).replace("=", "");
            exoPlayerAdapter = new SeekableTsExoPlayerAdapter(getActivity(), Integer.parseInt(durationString));
        } else {
            exoPlayerAdapter = new SeekableTsExoPlayerAdapter(getActivity());
        }


        exoPlayerAdapter.setAudioStreamType(AudioManager.USE_DEFAULT_STREAM_TYPE);
        mPlayerGlue = new ProviderVideoMediaPlayerGlue<>(getActivity(), exoPlayerAdapter);
        mPlayerGlue.setHost(new VideoFragmentGlueHost(this));
        mPlayerGlue.setTitle(arguments.getString(VodTvSectionFragment.AV_CONTENT_TITLE_BUNDLE));
        mPlayerGlue.setSubtitle(arguments.getString(VodTvSectionFragment.AV_CONTENT_GROUP_BUNDLE));
        mPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(url));

        if (mPlayerGlue.isPrepared()) {
            mPlayerGlue.play();
        } else {
            mPlayerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    super.onPreparedStateChanged(glue);
                    if (glue.isPrepared()) {

                        // Only add seek for capable videos...
                        if (mPlayerGlue.getDuration() > 0) {
                            mPlayerGlue.setSeekProvider(new ProviderPlaybackSeekDataProvider(mPlayerGlue.getDuration()));
                        }

                        // Force content to fit to screen if wanted.
                        if (getVodProperties().isVideoFitToScreen()) {
                            DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
                            mPlayerGlue.getPlayerAdapter().getCallback().onVideoSizeChanged(mPlayerGlue.getPlayerAdapter(), displayMetrics.widthPixels, displayMetrics.heightPixels);
                        }

                        glue.removePlayerCallback(this);
                        glue.play();
                    }
                }
            });
        }
        setBackgroundType(BG_LIGHT);
    }

    @Override
    public void onPause() {
        if (mPlayerGlue != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !getActivity().isInPictureInPictureMode()) {
                mPlayerGlue.pause();
            }
        }
        super.onPause();

    }

    @Override
    public void onError(int errorCode, CharSequence errorMessage) {

        // Notify the user if a video can't be played.
        if (errorCode == ExoPlaybackException.TYPE_SOURCE) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            ErrorFragment errorFragment = new ErrorFragment();
            errorFragment.setDefaultBackground(true);
            errorFragment.setMessage(getString(R.string.video_not_playable, getProviderName()));
            errorFragment.setImageDrawable(getActivity().getDrawable(R.drawable.lb_ic_sad_cloud));
            fragmentTransaction.replace(R.id.activity_vod_playback_fragment, errorFragment);
            fragmentTransaction.commit();
        }
    }

}
