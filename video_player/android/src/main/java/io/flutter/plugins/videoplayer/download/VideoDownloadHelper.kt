package io.flutter.plugins.videoplayer.download


import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.flutter.plugins.videoplayer.*
import java.io.IOException
import java.util.*

/**
 * Created by cnting on 2022/5/30
 *
 */
class VideoDownloadHelper(
    private val context: Context,
    private val videoDownloadManager: VideoDownloadManager,
    private val dataSourceUri: Uri,
    private val renderersFactory: RenderersFactory,
    private val dataSourceFactory: DataSource.Factory,
    private val eventSink: QueuingEventSink
) {
    private var refreshProgressTimer: Timer? = null

    private var downloadHelper: DownloadHelper? = null

    private val uiHandler = Handler(Looper.getMainLooper())

    /**
     * 获取下载状态
     */
    fun initDownloadState() {
        val download = sendDownloadState()
        if (download != null) {
            //如果在STATE_DOWNLOADING状态，直到下载完成onDownloadsChanged才会回调，所以不能用startRefreshProgressTask()方法
            startRefreshProgressTimer(null)
        }
    }

    private fun sendDownloadState(): Download? {
        val download = videoDownloadManager.downloadTracker.getDownload(dataSourceUri)
        val event: MutableMap<String, Any> = HashMap()
        event["event"] = "downloadState"
        when (download?.state ?: Download.STATE_QUEUED) {
            Download.STATE_COMPLETED -> {
                event["state"] = GpDownloadState.COMPLETED
            }
            Download.STATE_DOWNLOADING -> {
                event["state"] = GpDownloadState.DOWNLOADING
                event["progress"] = download!!.percentDownloaded
            }
            Download.STATE_FAILED -> {
                event["state"] = GpDownloadState.ERROR
            }
            else -> {
                event["state"] = GpDownloadState.UNDOWNLOAD
            }
        }
        uiHandler.post {
            eventSink.success(event)
        }
        return download
    }

    /**
     * 下载指定分辨率视频，暂时只支持hls
     */
    fun download(trackIndex: Int, downloadNotificationName: String) {
//        if (io.flutter.plugins.videoplayer.VideoPlayerPlugin.VideoPlayer.isFileOrAsset(dataSourceUri)) {
//            return
//        }
        when (Util.inferContentType(dataSourceUri)) {
            C.TYPE_HLS -> downloadHls(trackIndex, downloadNotificationName)
            C.TYPE_DASH, C.TYPE_OTHER, C.TYPE_SS -> {}
            else -> {}
        }
    }

    private fun downloadHls(trackIndex: Int, downloadNotificationName: String) {
        Log.d("===>", "正在下载 downloadHls")
        downloadHelper?.release()
        downloadHelper =
            DownloadHelper.forMediaItem(
                MediaItem.Builder().setUri(dataSourceUri).setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build(),
                DownloadHelper.getDefaultTrackSelectorParameters(context),
                renderersFactory,
                dataSourceFactory,
                null
            )
        downloadHelper?.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                for (periodIndex in 0 until helper.periodCount) {
                    helper.clearTrackSelections(periodIndex)
                    val selectionOverride = SelectionOverride(0, trackIndex)
                    val list: MutableList<SelectionOverride> = ArrayList()
                    list.add(selectionOverride)
                    helper.addTrackSelectionForSingleRenderer(
                        periodIndex,
                        0,
                        DownloadHelper.getDefaultTrackSelectorParameters(context),
                        list
                    )
                }
                val downloadRequest =
                    helper.getDownloadRequest(Util.getUtf8Bytes(downloadNotificationName))
                DownloadService.sendAddDownload(
                    context,
                    VideoDownloadService::class.java, downloadRequest, 0, false
                )
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                e.printStackTrace()
            }
        })
        startRefreshProgressTask()
    }

    private fun startRefreshProgressTask() {
        val isRunTask = booleanArrayOf(false)
        videoDownloadManager.downloadTracker.addListener(object : VideoDownloadTracker.Listener {
            override fun onDownloadsChanged() {
                if (!isRunTask[0]) {
                    startRefreshProgressTimer(this)
                    isRunTask[0] = true
                }
            }
        })
    }

    private fun startRefreshProgressTimer(listener: VideoDownloadTracker.Listener?) {
        cancelRefreshProgressTimer()
        refreshProgressTimer = Timer()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                val download: Download? =
                    videoDownloadManager.downloadTracker.getDownload(dataSourceUri)
                sendDownloadState()
                if (download != null && download.isTerminalState) {
                    cancelRefreshProgressTimer()
                    if (listener != null) {
                        videoDownloadManager.downloadTracker.removeListener(listener)
                    }
                }
            }
        }
        refreshProgressTimer?.schedule(timerTask, 1000, 1000)
    }

    private fun cancelRefreshProgressTimer() {
        refreshProgressTimer?.cancel()
        refreshProgressTimer = null
    }

    fun removeDownload() {
        val download: Download? = videoDownloadManager.downloadTracker.getDownload(dataSourceUri)
        if (download != null) {
            DownloadService.sendRemoveDownload(
                context,
                VideoDownloadService::class.java, download.request.id, false
            )
            videoDownloadManager.downloadTracker.addListener(object :
                VideoDownloadTracker.Listener {
                override fun onDownloadsChanged() {
                    if (videoDownloadManager.downloadTracker.getDownloadState(dataSourceUri) == Download.STATE_QUEUED) {
                        sendDownloadState()
                        videoDownloadManager.downloadTracker.removeListener(this)
                    }
                }
            })
        }
    }

    fun dispose() {
        downloadHelper?.release()
        cancelRefreshProgressTimer()
    }

    /**
     * 是否有已下载内容
     */
    fun getDownloadMediaSource(uri: Uri): MediaSource? {
        val download = videoDownloadManager.downloadTracker.getDownload(uri)
        if (download != null && download.state == Download.STATE_COMPLETED) {
            return DownloadHelper.createMediaSource(
                download.request,
                videoDownloadManager.localDataSourceFactory
            )
        }
        return null
    }
}