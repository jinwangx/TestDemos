package com.jw.library.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory.Options
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.*

/**
 * Bitmap工具类
 */
object BitmapUtil {


    /**
     * 根据路径获取图片旋转角度
     * @param path String?
     * @return Int
     */
    fun getBitmapDegree(path: String?): Int {
        var degree: Short = 0

        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (var4: IOException) {
            var4.printStackTrace()
        }

        return degree.toInt()
    }

    /**
     * 旋转图片
     * @param bitmap Bitmap?
     * @param degree Int
     * @return Bitmap
     */
    fun rotateBitmapByDegree(bitmap: Bitmap?, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val newBitmap =
            Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        return newBitmap
    }

    /**
     * 旋转图片
     * @param context Context
     * @param uri Uri
     * @param options Options
     * @return Bitmap?
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    fun rotateBitmap(context: Context, uri: Uri, options: Options): Bitmap? {
        val bis = context.contentResolver.openInputStream(uri)

        try {
            var result = BitmapFactory.decodeStream(bis, null, options)
            val degree = getBitmapDegree(uri.path)
            if (degree != 0) {
                result = rotateBitmapByDegree(result, degree)
            }
            return result
        } catch (var7: OutOfMemoryError) {
            if (options.inSampleSize >= 64) {
                return null
            }

            options.inSampleSize *= 2
            return null
        }

    }

    /**
     * 旋转图片
     * @param path String
     * @param degree Int
     * @return Bitmap
     */
    fun rotateBitmapByDegree(path: String, degree: Int): Bitmap {
        val bitmap = BitmapFactory.decodeFile(path)
        if (degree == 0)
            return bitmap
        return rotateBitmapByDegree(bitmap, degree)
    }

    /**
     * 获取正常方向的图片
     * @param activity Activity
     * @param path String
     * @return Uri
     */
    fun getRotatedUri(activity: Activity, path: String): Uri {
        val degree = getBitmapDegree(path)
        if (degree != 0) {
            val bitmap = BitmapFactory.decodeFile(path)
            val newBitmap = rotateBitmapByDegree(bitmap, degree)
            return Uri.parse(Media.insertImage(activity.contentResolver, newBitmap, null, null))
        } else {
            return Uri.fromFile(File(path))
        }
    }

    /**
     * 从文件中获取bitmap
     * @param file File?
     * @return Bitmap?
     */
    fun getBitmap(file: File?): Bitmap? {
        if (file == null) {
            return null
        } else {
            var bis: BufferedInputStream? = null

            val var3: Any?
            try {
                bis = BufferedInputStream(FileInputStream(file))
                return BitmapFactory.decodeStream(bis)
            } catch (var13: FileNotFoundException) {
                var13.printStackTrace()
                var3 = null
            } finally {
                try {
                    bis?.close()
                } catch (var12: IOException) {
                    var12.printStackTrace()
                }

            }

            return var3 as Bitmap?
        }
    }

    /**
     * 从路径中获取bitmap
     * @param filePath String
     * @return Bitmap?
     */
    fun getBitmap(filePath: String): Bitmap? {
        return if (TextUtils.isEmpty(filePath)) null else BitmapFactory.decodeFile(filePath)
    }

    /**
     * 根据宽度缩放bitmap
     * @param filePath String
     * @param width Int
     * @param height Int
     * @return Bitmap?
     */
    fun getScaledBitmap(filePath: String, width: Int, height: Int): Bitmap {
        val decodeOptions = Options()
        decodeOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, decodeOptions)
        val actualWidth = decodeOptions.outWidth
        val actualHeight = decodeOptions.outHeight
        val maxWidth: Int
        val maxHeight: Int
        if (actualHeight < actualWidth) {
            maxWidth = height
            maxHeight = width
        } else {
            maxWidth = width
            maxHeight = height
        }
        //保持比例裁剪
/*        val desiredWidth = getResizedDimension(maxWidth, maxHeight, actualWidth, actualHeight)
        val desiredHeight = getResizedDimension(maxHeight, maxWidth, actualHeight, actualWidth)*/
        //拉伸裁剪
        val desiredWidth = width
        val desiredHeight = height
        decodeOptions.inJustDecodeBounds = false
        decodeOptions.inSampleSize =
            findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight)
        val tempBitmap = BitmapFactory.decodeFile(filePath, decodeOptions)
        val bitmap: Bitmap?
        if (tempBitmap == null || tempBitmap.width <= desiredWidth && tempBitmap.height <= desiredHeight) {
            bitmap = tempBitmap
        } else {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true)
            tempBitmap.recycle()
        }

        return bitmap
    }

    /**
     * 获取指定宽高bitmap
     * @param context Context
     * @param imageResId Int
     * @param maxWidth Int
     * @param maxHeight Int
     * @return Bitmap?
     */
    fun getScaledBitmap(
        context: Context,
        imageResId: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        val decodeOptions = Options()
        decodeOptions.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, imageResId, decodeOptions)
        val actualWidth = decodeOptions.outWidth
        val actualHeight = decodeOptions.outHeight
        val desiredWidth = getResizedDimension(maxWidth, maxHeight, actualWidth, actualHeight)
        val desiredHeight = getResizedDimension(maxHeight, maxWidth, actualHeight, actualWidth)
        decodeOptions.inJustDecodeBounds = false
        decodeOptions.inSampleSize =
            findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight)
        val tempBitmap =
            BitmapFactory.decodeResource(context.resources, imageResId, decodeOptions)
        val bitmap: Bitmap?
        if (tempBitmap == null || tempBitmap.width <= desiredWidth && tempBitmap.height <= desiredHeight) {
            bitmap = tempBitmap
        } else {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true)
            tempBitmap.recycle()
        }

        return bitmap
    }

    private fun findBestSampleSize(
        actualWidth: Int,
        actualHeight: Int,
        desiredWidth: Int,
        desiredHeight: Int
    ): Int {
        val wr = actualWidth.toDouble() / desiredWidth.toDouble()
        val hr = actualHeight.toDouble() / desiredHeight.toDouble()
        val ratio = Math.min(wr, hr)

        var n: Float
        n = 1.0f
        while ((n * 2.0f).toDouble() <= ratio) {
            n *= 2.0f
        }

        return n.toInt()
    }

    private fun getResizedDimension(
        maxPrimary: Int,
        maxSecondary: Int,
        actualPrimary: Int,
        actualSecondary: Int
    ): Int {
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary
        } else {
            val ratio: Double
            if (maxPrimary == 0) {
                ratio = maxSecondary.toDouble() / actualSecondary.toDouble()
                return (actualPrimary.toDouble() * ratio).toInt()
            } else if (maxSecondary == 0) {
                return maxPrimary
            } else {
                ratio = actualSecondary.toDouble() / actualPrimary.toDouble()
                var resized = maxPrimary
                if (maxPrimary.toDouble() * ratio > maxSecondary.toDouble()) {
                    resized = (maxSecondary.toDouble() / ratio).toInt()
                }

                return resized
            }
        }
    }

    fun compress(srcImg: String): Bitmap {
        val options = Options()
        options.inSampleSize = computeSize(options.outWidth, options.outHeight)
        return BitmapFactory.decodeFile(srcImg, options)
    }

    /**
     * 将bitmap保存为文件
     * @param bm Bitmap
     * @param savePath String   保存路径
     * @return Boolean
     */
    fun saveBitmap2File(bm: Bitmap, savePath: String): Boolean {
        var saved = true

        try {
            val f = File(savePath)
            if (f.exists()) {
                f.delete()
            }

            val out = FileOutputStream(f)
            bm.compress(CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (var5: FileNotFoundException) {
            var5.printStackTrace()
            saved = false
        } catch (var6: IOException) {
            var6.printStackTrace()
            saved = false
        }

        return saved
    }

    private fun computeSize(srcWidth: Int, srcHeight: Int): Int {
        var srcWidth = srcWidth
        var srcHeight = srcHeight
        srcWidth = if (srcWidth % 2 == 1) srcWidth + 1 else srcWidth
        srcHeight = if (srcHeight % 2 == 1) srcHeight + 1 else srcHeight
        val longSide = Math.max(srcWidth, srcHeight)
        val shortSide = Math.min(srcWidth, srcHeight)
        val scale = shortSide.toFloat() / longSide.toFloat()
        return if (scale <= 1.0f && scale.toDouble() > 0.5625) {
            if (longSide < 1664) {
                1
            } else if (longSide in 1664..4989) {
                2
            } else if (longSide > 4990 && longSide < 10240) {
                4
            } else {
                if (longSide / 1280 == 0) 1 else longSide / 1280
            }
        } else if (scale.toDouble() <= 0.5625 && scale.toDouble() > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            Math.ceil(longSide.toDouble() / (1280.0 / scale.toDouble())).toInt()
        }
    }

    /**
     * 将bitmap存入相册
     * @param bm Bitmap
     * @param activity Activity
     * @return Uri
     */
    fun saveMedia2Galary(context: Context, path: String): Uri {
        val uri: Uri?
        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //如果是7.0android系统
            val contentValues = ContentValues(1)
            contentValues.put(Media.DATA, path)
            context.contentResolver
                .insert(Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            Uri.fromFile(File(path))
        }
        return uri
    }


    fun compressImg(filePath: String): ByteArray {
        val needSize = 1
        val originSize = FileUtils.getFileOrFilesSize(filePath, 3)
        Log.v("upload_originSize", originSize.toString())
        val scale = originSize / needSize
        var quality = (100 / scale).toInt()
        if (quality > 100)
            quality = 100
        return Bitmap2Bytes(filePath, quality)
    }

    /**
     * 将bitmap转为字节
     * @param bm Bitmap
     * @return ByteArray
     */
    fun Bitmap2Bytes(filePath: String, quality: Int = 100): ByteArray {
        val byteArray = qualityCompress(filePath, quality, null)
        return byteArray
    }

    fun Bitmap2Bytes(bitmap: Bitmap, quality: Int = 100): ByteArray {
        val byteArray = qualityCompress(bitmap, quality, null)
        return byteArray
    }

    fun base64Image(filePath: String): String {
        var imgBase64 = ""

        try {
            val file = File(filePath)
            val content = ByteArray(file.length().toInt())
            val finputstream = FileInputStream(file)
            finputstream.read(content)
            finputstream.close()
            imgBase64 = Base64.encodeToString(content, 0)
        } catch (var5: IOException) {
            var5.printStackTrace()
        }

        return imgBase64
    }


    /**
     * 质量压缩
     * 设置bitmap options属性，降低图片的质量，像素不会减少
     * 第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
     * 设置options 属性0-100，来实现压缩（因为png是无损压缩，所以该属性对png是无效的）
     *
     * @param bmp
     * @param quality   0-100 100为不压缩
     * @param file
     */
    fun qualityCompress(bmp: Bitmap, quality: Int = 100, file: File?): ByteArray {
        val baos = ByteArrayOutputStream()
        // 把压缩后的数据存放到baos中
        bmp.compress(CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }

    fun qualityCompress(filePath: String, quality: Int = 100, file: File?): ByteArray {
        val bitmap = BitmapFactory.decodeFile(filePath)
        val baos = ByteArrayOutputStream()
        // 把压缩后的数据存放到baos中
        bitmap.compress(CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }

    /**
     * 尺寸压缩（通过缩放图片像素来减少图片占用内存大小）
     *
     * @param bmp
     * @param file
     */
    fun sizeCompress(bmp: Bitmap, file: File?) {
        // 尺寸压缩倍数,值越大，图片尺寸越小
        val ratio = 8
        // 压缩Bitmap到对应尺寸
        val result =
            Bitmap.createBitmap(bmp.width / ratio, bmp.height / ratio, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val rect = Rect(0, 0, bmp.width / ratio, bmp.height / ratio)
        canvas.drawBitmap(bmp, null, rect, null)
        val baos = ByteArrayOutputStream()
        // 把压缩后的数据存放到baos中
        result.compress(CompressFormat.JPEG, 100, baos)
        try {
            val fos = FileOutputStream(file)
            fos.write(baos.toByteArray())
            fos.flush()
            fos.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 采样率压缩（设置图片的采样率，降低图片像素）
     * @param filePath
     * @param inSampleSize 数值越高，图片像素越低
     */
    fun samplingRateCompress(filePath: String, inSampleSize: Int = 1): ByteArray {
        val options = Options()
        options.inJustDecodeBounds = false
        //          options.inJustDecodeBounds = true;//为true的时候不会真正加载图片，而是得到图片的宽高信息。
        //采样率
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(filePath, options)
        val baos = ByteArrayOutputStream()
        // 把压缩后的数据存放到baos中
        bitmap.compress(CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }
}
