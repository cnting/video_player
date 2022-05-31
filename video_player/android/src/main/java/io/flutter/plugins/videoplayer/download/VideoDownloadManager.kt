package io.flutter.plugins.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
//        val downloadManager = DownloadManager(
//            context,
//            databaseProvider,
//            downloadCache,
//            dataSourceFactory,
//            Executors.newFixedThreadPool(6)
//        )
        val downloadManager = DownloadManager(
            context,
            DefaultDownloadIndex(databaseProvider),
            DefaultDownloaderFactory(localDataSourceFactory, Executors.newFixedThreadPool(6))
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

    //必须要16位
    private val SECRET_KEY = "secretkey1234567"

    private val downloadCache: Cache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        val downloadCache =
            SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                databaseProvider,
                Util.getUtf8Bytes(SECRET_KEY),  //对cacheIndex进行加密
                true,
                false
            )
        downloadCache
    }

    private val dataSourceFactory: HttpDataSource.Factory by lazy {
        val factory = NoProxyDefaultHttpDataSource.Factory()
        factory
    }

    val localDataSourceFactory: CacheDataSource.Factory by lazy {
        val upstreamFactory = DefaultDataSourceFactory(context, dataSourceFactory)
        val factory = buildCacheDataSourceFactory(upstreamFactory, downloadCache)
        factory
    }

    private fun buildCacheDataSourceFactory(
        upstreamFactory: DataSource.Factory,
        cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(getCacheReadDataSourceFactory(cache))
            .setCacheWriteDataSinkFactory(getCacheWriteDataSinkFactory(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * 对 metadata缓存 解密
     */
    private fun getCacheReadDataSourceFactory(cache: Cache): DataSource.Factory {
        val m3u8Key = cache.keys.filter { it.endsWith(".m3u8") }
        Log.d("===>","m3u8Key:$m3u8Key")
        if(m3u8Key.isNotEmpty()){
            val contentMetadata: ContentMetadata = cache.getContentMetadata(m3u8Key.first())
            Log.d("===>","metadata:$contentMetadata")
        }

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