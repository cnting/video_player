package io.flutter.plugins.videoplayer.datasource

import android.util.Log
import java.io.InputStream

/**
 * Created by cnting on 2022/6/2
 *
 */
interface HttpResponseInterceptor {
    fun intercept(url: String, inputStream: InputStream): InputStream
}

/**
 * 返回的内容可能需要解密
 */
class DefaultHttpResponseDecryptInterceptor : HttpResponseInterceptor {
    override fun intercept(url: String, inputStream: InputStream): InputStream {
        // TODO: 判断是否拦截  
        return inputStream
    }

}

