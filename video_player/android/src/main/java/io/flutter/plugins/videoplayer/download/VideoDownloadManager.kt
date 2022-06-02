package io.flutter.plugins.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSink
import com.google.android.exoplayer2.upstream.crypto.AesCipherDataSource
import com.google.android.exoplayer2.util.Util
import io.flutter.plugins.videoplayer.datasource.DefaultHttpResponseDecryptInterceptor
import io.flutter.plugins.videoplayer.datasource.NoProxyDefaultHttpDataSource
import java.io.File
import java.util.concurrent.Executors

/**
 * Created by cnting on 2019-08-05
 *
 */
class VideoDownloadManager private constructor(private val context: Context) {

    private val DOWNLOAD_CONTENT_DIRECTORY = "video_downloads"

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: VideoDownloadManager? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: VideoDownloadManager(context).also { instance = it }
        }
    }


    val downloadManager: DownloadManager by lazy {
        val downloadManager = DownloadManager(
            context,
            DefaultDownloadIndex(databaseProvider),
            DefaultDownloaderFactory(cacheDataSourceFactory, Executors.newFixedThreadPool(6))
        )
        downloadManager
    }

    val downloadTracker: VideoDownloadTracker by lazy {
        val downloadTracker = VideoDownloadTracker(downloadManager)
        downloadTracker
    }

    val downloadNotificationHelper: DownloadNotificationHelper by lazy {
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

    //必须要16位  // TODO: 需要从flutter里传  
    private val SECRET_KEY = "secretkey1234567"

    private val downloadCache: Cache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        val downloadCache =
            SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                databaseProvider,
//                Util.getUtf8Bytes(SECRET_KEY),  //对cacheIndex进行加密
//                true,
//                false
            )
        downloadCache
    }

    val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        //禁止抓包
        val factory = NoProxyDefaultHttpDataSource.Factory()
            .setUserAgent("ExoPlayer")
            .setAllowCrossProtocolRedirects(true)
            .addHttpResponseInterceptor(DefaultHttpResponseDecryptInterceptor())
        factory
    }

    val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val upstreamFactory = DefaultDataSourceFactory(context, httpDataSourceFactory)
        val factory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(getCacheReadDataSourceFactory())
            .setCacheWriteDataSinkFactory(getCacheWriteDataSinkFactory(downloadCache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        factory
    }

    /**
     * 对 metadata缓存 解密
     */
    private fun getCacheReadDataSourceFactory(): DataSource.Factory {
        return DataSource.Factory {
            AesCipherDataSource(
                Util.getUtf8Bytes(SECRET_KEY),
                FileDataSource()
            )
        }
    }

    /**
     * 对 metadata缓存 加密
     */
    private fun getCacheWriteDataSinkFactory(cache: Cache): DataSink.Factory {
        return DataSink.Factory {
            val cacheSink = CacheDataSink(
                cache,
                CacheDataSink.DEFAULT_FRAGMENT_SIZE,
                CacheDataSink.DEFAULT_BUFFER_SIZE
            )
            AesCipherDataSink(
                Util.getUtf8Bytes(SECRET_KEY), cacheSink,
                ByteArray(10 * 1024)
            )
        }
    }

}