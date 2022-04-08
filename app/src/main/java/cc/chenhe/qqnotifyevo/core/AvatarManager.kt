package cc.chenhe.qqnotifyevo.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

/**
 * 管理会话头像磁盘+内存二级缓存。
 *
 * 在某些情况，例如群聊消息或旧版 QQ 有多个联系人发来消息时，通知不会显示联系人头像。故缓存以作备用。
 *
 * 过期后依然可以成功读取缓存，下次请求保存头像时会覆盖。
 *
 * @param cacheDir 缓存文件夹。
 * @param period 缓存有效期（毫秒）。
 */
class AvatarManager private constructor(
    private val cacheDir: File,
    var period: Long
) {

    companion object {
        private const val TAG = "AvatarManager"
        private const val MAX_MEMORY_CACHE_SIZE = 5 * 1024 * 1024L

        private var instance: AvatarManager? = null

        fun get(cacheDir: File, period: Long): AvatarManager {
            if (instance == null) {
                synchronized(AvatarManager::class) {
                    if (instance == null)
                        instance = AvatarManager(cacheDir, period)
                }
            }
            return requireNotNull(instance)
        }
    }

    private class AvatarLruCache : LruCache<Int, Bitmap>(
        min((Runtime.getRuntime().freeMemory() / 4), MAX_MEMORY_CACHE_SIZE).toInt()
    ) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }

    private val lru: AvatarLruCache = AvatarLruCache()

    init {
        if (!cacheDir.isDirectory) {
            cacheDir.mkdirs()
        }
    }

    fun saveAvatar(conversionId: Int, bmp: Bitmap): File {
        val file = File(cacheDir, conversionId.toString())
        if (!file.isFile || System.currentTimeMillis() - file.lastModified() > period) {
            lru.remove(conversionId)
            try {
                val outStream = FileOutputStream(file)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return file
    }

    fun getAvatar(conversionId: Int): Bitmap? {
        lru[conversionId].also { if (it != null) return it }
        val file = File(cacheDir, conversionId.toString())
        if (file.isFile) {
            return try {
                BitmapFactory.decodeFile(file.absolutePath).also {
                    lru.put(conversionId, it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Decode avatar file error, delete the cache. conversionId=$conversionId")
                e.printStackTrace()
                file.delete()
                lru.remove(conversionId)
                null
            }
        }
        return null
    }

    /**
     * 清空磁盘与内存缓存。
     */
    fun clearCache() {
        Log.d(TAG, "Clear avatar cache in disk and memory.")
        cacheDir.listFiles()?.forEach { f ->
            f?.deleteRecursively()
        }
        lru.evictAll()
    }
}
