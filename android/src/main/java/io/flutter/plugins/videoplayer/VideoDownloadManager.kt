package io.flutter.plugins.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.util.concurrent.Executors

/**
 * Created by cnting on 2019-08-05
 *
 */
class VideoDownloadManager private constructor(private val context: Context) {

    private val DOWNLOAD_CONTENT_DIRECTORY = "video_downloads"
    private val userAgent = Util.getUserAgent(context, "ExoPlayerDemo")

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: VideoDownloadManager? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: VideoDownloadManager(context).also { instance = it }
        }
    }


    val downloadManager: DownloadManager by lazy {
//        val downloadIndex = DefaultDownloadIndex(databaseProvider)
//        val downloaderConstructorHelper = DownloaderConstructorHelper(downloadCache, buildHttpDataSourceFactory)
//        val downloadManager = DownloadManager(
//                context, downloadIndex, DefaultDownloaderFactory(downloaderConstructorHelper)
//        )
        val downloadManager = DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            buildHttpDataSourceFactory,
            Executors.newFixedThreadPool(6)
        )
        downloadManager
    }

    val downloadTracker: VideoDownloadTracker by lazy {
        val downloadTracker = VideoDownloadTracker(downloadManager)
        downloadTracker
    }

    val downloadNotificationHelper:DownloadNotificationHelper by lazy {
        val helper = DownloadNotificationHelper(context, "download_channel")
        helper
    }

    private val databaseProvider: DatabaseProvider by lazy {
        val p = ExoDatabaseProvider(context)
        p
    }

    private val downloadDirectory: File by lazy {
        var directionality = context.getExternalFilesDir(null)
        if (directionality == null) {
            directionality = context.filesDir
        }
        directionality!!
    }

    private val downloadCache: Cache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        val downloadCache = SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
        downloadCache
    }

    private val buildHttpDataSourceFactory: HttpDataSource.Factory by lazy {
        val factory = DefaultHttpDataSourceFactory(userAgent)
        factory
    }

    val localDataSourceFactory:DataSource.Factory by lazy {
        val upstreamFactory = DefaultDataSourceFactory(context, buildHttpDataSourceFactory)
        val factory = buildReadOnlyCacheDataSource(upstreamFactory, downloadCache)
        factory
    }

    private fun buildReadOnlyCacheDataSource(
            upstreamFactory: DataSource.Factory,
            cache: Cache
    ): CacheDataSourceFactory {
        return CacheDataSourceFactory(
                cache, upstreamFactory, FileDataSourceFactory(), null, CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        )
    }


}