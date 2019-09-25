// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;
import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS;

public class VideoPlayerPlugin implements MethodCallHandler {

    public static void registerWith(Registrar registrar) {
        final VideoPlayerPlugin plugin = new VideoPlayerPlugin(registrar);
        final MethodChannel channel =
                new MethodChannel(registrar.messenger(), "flutter.io/videoPlayer");
        channel.setMethodCallHandler(plugin);
        registrar.addViewDestroyListener(
                view -> {
                    plugin.onDestroy();
                    return false; // We are not interested in assuming ownership of the NativeView.
                });
    }

    private VideoPlayerPlugin(Registrar registrar) {
        this.registrar = registrar;
        this.videoPlayers = new LongSparseArray<>();
        this.videoDownloadManager = VideoDownloadManager.Companion.getInstance(registrar.activeContext().getApplicationContext());
    }

    private final LongSparseArray<VideoPlayer> videoPlayers;
    private final Registrar registrar;
    private final VideoDownloadManager videoDownloadManager;

    private void disposeAllPlayers() {
        for (int i = 0; i < videoPlayers.size(); i++) {
            videoPlayers.valueAt(i).dispose();
        }
        videoPlayers.clear();
    }

    private void onDestroy() {
        // The whole FlutterView is being destroyed. Here we release resources acquired for all instances
        // of VideoPlayer. Once https://github.com/flutter/flutter/issues/19358 is resolved this may
        // be replaced with just asserting that videoPlayers.isEmpty().
        // https://github.com/flutter/flutter/issues/20989 tracks this.
        disposeAllPlayers();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        TextureRegistry textures = registrar.textures();
        if (textures == null) {
            result.error("no_activity", "video_player plugin requires a foreground activity", null);
            return;
        }
        switch (call.method) {
            case "init":
                disposeAllPlayers();
                break;
            case "create": {
                TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                EventChannel eventChannel =
                        new EventChannel(
                                registrar.messenger(), "flutter.io/videoPlayer/videoEvents" + handle.id());

                VideoPlayer player;
                if (call.argument("asset") != null) {
                    String assetLookupKey;
                    if (call.argument("package") != null) {
                        assetLookupKey =
                                registrar.lookupKeyForAsset(call.argument("asset"), call.argument("package"));
                    } else {
                        assetLookupKey = registrar.lookupKeyForAsset(call.argument("asset"));
                    }
                    player =
                            new VideoPlayer(
                                    registrar.context(),
                                    eventChannel,
                                    handle,
                                    "asset:///" + assetLookupKey,
                                    result);
                    videoPlayers.put(handle.id(), player);
                } else {
                    player =
                            new VideoPlayer(
                                    registrar.context(), eventChannel, handle, call.argument("uri"), result);
                    videoPlayers.put(handle.id(), player);
                }
                break;
            }
            default: {
                long textureId = ((Number) call.argument("textureId")).longValue();
                VideoPlayer player = videoPlayers.get(textureId);
                if (player == null) {
                    result.error(
                            "Unknown textureId",
                            "No video player associated with texture id " + textureId,
                            null);
                    return;
                }
                onMethodCall(call, result, textureId, player);
                break;
            }
        }
    }

    private void onMethodCall(MethodCall call, Result result, long textureId, VideoPlayer player) {
        switch (call.method) {
            case "setLooping":
                player.setLooping(call.argument("looping"));
                result.success(null);
                break;
            case "setVolume":
                player.setVolume(call.argument("volume"));
                result.success(null);
                break;
            case "play":
                player.play();
                result.success(null);
                break;
            case "pause":
                player.pause();
                result.success(null);
                break;
            case "seekTo":
                int location = ((Number) call.argument("location")).intValue();
                player.seekTo(location);
                result.success(null);
                break;
            case "position":
                result.success(player.getPosition());
                player.sendBufferingUpdate();
                break;
            case "dispose":
                player.dispose();
                videoPlayers.remove(textureId);
                result.success(null);
                break;
            case "getResolutions":   //获取分辨率
                result.success(player.getResolutions());
                break;
            case "switchResolutions":  //切换分辨率
                player.switchResolution(((Number) call.argument("trackIndex")).intValue());
                result.success(null);
                break;
            case "download": //缓存视频
                int trackIndex = ((Number) call.argument("trackIndex")).intValue();
                String name = call.argument("name");
                player.download(videoDownloadManager, trackIndex, name);
                result.success(null);
                break;
            case "setSpeed":
                double speed = ((Number) call.argument("speed")).doubleValue();
                player.setSpeed(speed);
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private static class VideoPlayer {

        private SimpleExoPlayer exoPlayer;
        private DefaultTrackSelector trackSelector;
        private DataSource.Factory dataSourceFactory;
        private RenderersFactory renderersFactory;
        private Surface surface;
        private final TextureRegistry.SurfaceTextureEntry textureEntry;
        private QueuingEventSink eventSink = new QueuingEventSink();
        private final EventChannel eventChannel;
        private boolean isInitialized = false;
        private Uri dataSourceUri;
        private DownloadHelper downloadHelper;
        private Context context;

        VideoPlayer(
                Context context,
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                String dataSource,
                Result result) {
            this.eventChannel = eventChannel;
            this.textureEntry = textureEntry;
            this.dataSourceUri = Uri.parse(dataSource);
            this.context = context.getApplicationContext();

            renderersFactory = new DefaultRenderersFactory(context);
            trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);

            if (isFileOrAsset(dataSourceUri)) {
                dataSourceFactory = new DefaultDataSourceFactory(context, "ExoPlayer");
            } else {
                dataSourceFactory =
                        new DefaultHttpDataSourceFactory(
                                "ExoPlayer",
                                null,
                                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                                true);
            }

            MediaSource mediaSource = buildMediaSource(dataSourceUri, dataSourceFactory, context);
            exoPlayer.prepare(mediaSource);

            setupVideoPlayer(eventChannel, textureEntry, result);
        }

        private static boolean isFileOrAsset(Uri uri) {
            if (uri == null || uri.getScheme() == null) {
                return false;
            }
            String scheme = uri.getScheme();
            return scheme.equals("file") || scheme.equals("asset");
        }

        private MediaSource buildMediaSource(
                Uri uri, DataSource.Factory mediaDataSourceFactory, Context context) {
            int type = Util.inferContentType(uri.getLastPathSegment());
            switch (type) {
                case C.TYPE_SS:
                    return new SsMediaSource.Factory(
                            new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                            .createMediaSource(uri);
                case C.TYPE_DASH:
                    return new DashMediaSource.Factory(
                            new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                            .createMediaSource(uri);
                case C.TYPE_HLS:
                    return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
                case C.TYPE_OTHER:
                    return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                            .setExtractorsFactory(new DefaultExtractorsFactory())
                            .createMediaSource(uri);
                default: {
                    throw new IllegalStateException("Unsupported type: " + type);
                }
            }
        }

        private void setupVideoPlayer(
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                Result result) {

            eventChannel.setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object o, EventChannel.EventSink sink) {
                            eventSink.setDelegate(sink);
                        }

                        @Override
                        public void onCancel(Object o) {
                            eventSink.setDelegate(null);
                        }
                    });

            surface = new Surface(textureEntry.surfaceTexture());
            exoPlayer.setVideoSurface(surface);
            setAudioAttributes(exoPlayer);

            exoPlayer.addListener(
                    new EventListener() {
                        @Override
                        public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
                            if (playbackState == Player.STATE_BUFFERING) {
                                sendBufferingStart();
                                sendBufferingUpdate();
                            } else if (playbackState == Player.STATE_READY) {
                                sendBufferingEnd();
                                sendPlayStateChange(playWhenReady);
                                if (!isInitialized) {
                                    isInitialized = true;
                                    sendInitialized();
                                }
                            } else if (playbackState == Player.STATE_ENDED) {
                                Map<String, Object> event = new HashMap<>();
                                event.put("event", "completed");
                                eventSink.success(event);
                            }
                        }

                        @Override
                        public void onPlayerError(final ExoPlaybackException error) {
                            if (eventSink != null) {
                                eventSink.error("VideoError", "Video player had error " + error, null);
                            }
                        }

                        @Override
                        public void onLoadingChanged(boolean isLoading) {
                            if(isLoading){
                                sendBufferingStart();
                            }else{
                                sendBufferingEnd();
                            }
                        }
                    });

            Map<String, Object> reply = new HashMap<>();
            reply.put("textureId", textureEntry.id());
            result.success(reply);
        }

        private void sendBufferingStart() {
            Map<String, Object> event = new HashMap<>();
            event.put("event", "bufferingStart");
            eventSink.success(event);
        }

        private void sendBufferingEnd() {
            Map<String, Object> event = new HashMap<>();
            event.put("event", "bufferingEnd");
            eventSink.success(event);
        }

        private void sendBufferingUpdate() {
            Map<String, Object> event = new HashMap<>();
            event.put("event", "bufferingUpdate");
            List<? extends Number> range = Arrays.asList(0, exoPlayer.getBufferedPosition());
            // iOS supports a list of buffered ranges, so here is a list with a single range.
            event.put("values", Collections.singletonList(range));
            eventSink.success(event);
        }

        private void sendPlayStateChange(boolean playWhenReady) {
            Map<String, Object> event = new HashMap<>();
            event.put("event", "playStateChanged");
            event.put("isPlaying", playWhenReady);
            eventSink.success(event);
        }

        @SuppressWarnings("deprecation")
        private static void setAudioAttributes(SimpleExoPlayer exoPlayer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                exoPlayer.setAudioAttributes(
                        new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).build());
            } else {
                exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);
            }
        }

        void play() {
            if (exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                exoPlayer.retry();
            } else if (exoPlayer.getPlaybackState() == Player.STATE_ENDED) {
                seekTo(0);
            }
            exoPlayer.setPlayWhenReady(true);
        }

        void pause() {
            exoPlayer.setPlayWhenReady(false);
        }

        void setLooping(boolean value) {
            exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
        }

        void setVolume(double value) {
            float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
            exoPlayer.setVolume(bracketedValue);
        }

        void seekTo(int location) {
            exoPlayer.seekTo(location);
        }

        long getPosition() {
            return exoPlayer.getCurrentPosition();
        }

        @SuppressWarnings("SuspiciousNameCombination")
        private void sendInitialized() {
            if (isInitialized) {
                Map<String, Object> event = new HashMap<>();
                event.put("event", "initialized");
                event.put("duration", exoPlayer.getDuration());

                if (exoPlayer.getVideoFormat() != null) {
                    Format videoFormat = exoPlayer.getVideoFormat();
                    int width = videoFormat.width;
                    int height = videoFormat.height;
                    int rotationDegrees = videoFormat.rotationDegrees;
                    // Switch the width/height if video was taken in portrait mode
                    if (rotationDegrees == 90 || rotationDegrees == 270) {
                        width = exoPlayer.getVideoFormat().height;
                        height = exoPlayer.getVideoFormat().width;
                    }
                    event.put("width", width);
                    event.put("height", height);
                }
                eventSink.success(event);
            }
        }

        void dispose() {
            if (isInitialized) {
                exoPlayer.stop();
            }
            textureEntry.release();
            eventChannel.setStreamHandler(null);
            if (surface != null) {
                surface.release();
            }
            if (exoPlayer != null) {
                exoPlayer.release();
            }
            if (downloadHelper != null) {
                downloadHelper.release();
            }
        }

        /**
         * 获取分辨率
         */
        Map<String, Object> getResolutions() {
            if (!isInitialized) {
                return null;
            }
            MappingTrackSelector.MappedTrackInfo currentMappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (currentMappedTrackInfo == null) {
                return null;
            }
            Map<Integer, String> map = new HashMap<>();
            TrackGroupArray trackGroups = currentMappedTrackInfo.getTrackGroups(RENDERER_SUPPORT_NO_TRACKS);
            for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                TrackGroup trackGroup = trackGroups.get(groupIndex);
                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    Format format = trackGroup.getFormat(trackIndex);
                    map.put(trackIndex, format.width + ":" + format.height);
                }
            }
            Map<String, Object> event = new HashMap<>();
            event.put("resolutions", map);
            return event;
        }

        void switchResolution(int trackIndex) {
            if (!isInitialized) {
                return;
            }
            DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
            MappingTrackSelector.MappedTrackInfo currentMappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (currentMappedTrackInfo == null || parameters == null) {
                return;
            }
            TrackGroupArray trackGroups = currentMappedTrackInfo.getTrackGroups(RENDERER_SUPPORT_NO_TRACKS);
            DefaultTrackSelector.ParametersBuilder parametersBuilder = parameters.buildUpon();
            parametersBuilder.clearSelectionOverrides();
            DefaultTrackSelector.SelectionOverride selectionOverride = new DefaultTrackSelector.SelectionOverride(0, trackIndex);
            parametersBuilder.setSelectionOverride(RENDERER_SUPPORT_NO_TRACKS, trackGroups, selectionOverride);
            trackSelector.setParameters(parametersBuilder);
        }


        /**
         * 下载指定分辨率视频，暂时只支持hls
         *
         * @param trackIndex
         */
        void download(VideoDownloadManager videoDownloadManager, int trackIndex, String downloadNotificationName) {
            if (isFileOrAsset(dataSourceUri)) {
                return;
            }
            int type = Util.inferContentType(dataSourceUri.getLastPathSegment());
            switch (type) {
                case C.TYPE_HLS:
                    downloadHls(videoDownloadManager, trackIndex, downloadNotificationName);
                    break;
                case C.TYPE_DASH:
                    break;
                case C.TYPE_OTHER:
                    break;
                case C.TYPE_SS:
                    break;
                default: {
                }
            }
        }

        private void downloadHls(VideoDownloadManager videoDownloadManager, int trackIndex, String downloadNotificationName) {
            if (downloadHelper != null) {
                downloadHelper.release();
            }
            downloadHelper = DownloadHelper.forHls(dataSourceUri, dataSourceFactory, renderersFactory);
            downloadHelper.prepare(new DownloadHelper.Callback() {
                @Override
                public void onPrepared(DownloadHelper helper) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(0);
                    for (int periodIndex = 0; periodIndex < helper.getPeriodCount(); periodIndex++) {
                        helper.clearTrackSelections(periodIndex);
                        if (mappedTrackInfo != null) {
                            DefaultTrackSelector.SelectionOverride selectionOverride = new DefaultTrackSelector.SelectionOverride(0, trackIndex);
                            List<DefaultTrackSelector.SelectionOverride> list = new ArrayList<>();
                            list.add(selectionOverride);
                            helper.addTrackSelectionForSingleRenderer(periodIndex, 0, DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS, list);
                        }
                    }
                    DownloadRequest downloadRequest = helper.getDownloadRequest(Util.getUtf8Bytes(downloadNotificationName));
                    DownloadService.sendAddDownload(context, VideoDownloadService.class, downloadRequest, false);
                }

                @Override
                public void onPrepareError(DownloadHelper helper, IOException e) {
                    e.printStackTrace();
                }
            });

            videoDownloadManager.getDownloadTracker().addListener(new VideoDownloadTracker.Listener() {
                @Override
                public void onDownloadsChanged() {
                    Download download = videoDownloadManager.getDownloadTracker().getDownload(dataSourceUri);
                    Log.d("下载", "===>" + download == null ? "null" : download.getPercentDownloaded() + "");
                }
            });
        }

        void setSpeed(double speed) {
            if (!isInitialized) {
                return;
            }
            PlaybackParameters playbackParameters = new PlaybackParameters((float) speed);
            exoPlayer.setPlaybackParameters(playbackParameters);
        }
    }
}
