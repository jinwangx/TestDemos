package com.jw.uploadlibrary.upload

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.jw.library.model.BaseItem
import com.jw.library.model.ImageItem
import com.jw.library.utils.BitmapUtil
import com.jw.library.utils.FileUtils
import com.jw.library.utils.ThemeUtils
import com.jw.uploadlibrary.UploadLibrary
import com.jw.uploadlibrary.UploadLibrary.appid
import com.jw.uploadlibrary.UploadLibrary.region
import com.jw.uploadlibrary.UploadLibrary.ticket
import com.jw.uploadlibrary.http.ScHttpClient
import com.jw.uploadlibrary.http.service.GoChatService
import com.jw.uploadlibrary.model.AuthorizationInfo
import com.jw.uploadlibrary.model.KeyReqInfo
import com.jw.uploadlibrary.model.OrgInfo
import com.jw.uploadlibrary.videoupload.TXUGCPublish
import com.jw.uploadlibrary.videoupload.TXUGCPublishTypeDef
import com.tencent.cos.xml.CosXmlService
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.cos.xml.exception.CosXmlClientException
import com.tencent.cos.xml.exception.CosXmlServiceException
import com.tencent.cos.xml.listener.CosXmlResultListener
import com.tencent.cos.xml.model.CosXmlRequest
import com.tencent.cos.xml.model.CosXmlResult
import com.tencent.cos.xml.transfer.COSXMLUploadTask
import com.tencent.cos.xml.transfer.TransferConfig
import com.tencent.cos.xml.transfer.TransferManager
import com.tencent.qcloud.core.auth.BasicLifecycleCredentialProvider
import com.tencent.qcloud.core.auth.QCloudLifecycleCredentials
import com.tencent.qcloud.core.auth.SessionQCloudCredentials
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * 创建时间：2019/5/1816:55
 * 更新时间 2019/5/1816:55
 * 版本：
 * 作者：Mr.jin
 * 描述：上传管理类
 */
class UploadManager {
    private lateinit var context: Context
    private lateinit var serviceConfig: CosXmlServiceConfig
    private var callBack: UploadProgressCallBack? = null
    private var threadPool: ExecutorService? = null
    private var imgUploadTasks: ArrayList<COSXMLUploadTask> = ArrayList()
    private var videoUploadTasks: ArrayList<TXUGCPublish> = ArrayList()

    fun init(context: Context) {
        this.context = context
        threadPool = Executors.newFixedThreadPool(UploadLibrary.maxUploadThreadSize)
        serviceConfig = CosXmlServiceConfig.Builder()
            .setAppidAndRegion(appid, region)
            .builder()
    }

    /**
     * 执行上传图片和语音
     * @param keyReqInfo KeyReqInfo
     * @param count Int
     */
    @SuppressLint("CheckResult")
    fun uploadImgOrVoice(keyReqInfo: KeyReqInfo, items: ArrayList<out BaseItem>) {
        imgUploadTasks.clear()
        //获取存储桶
        ScHttpClient.getService(GoChatService::class.java).getAuthorization(ticket, keyReqInfo)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ jsonObject ->
                if (!jsonObject.isNull("error"))
                    ThemeUtils.show(context, jsonObject.getString("message"))
                val authorizationInfo =
                    Gson().fromJson(jsonObject.toString(), AuthorizationInfo::class.java)
                for (i in 0 until items.size) {
                    //执行单个文件上传
                    excUploadImgOrVoice(i, items[i], authorizationInfo)
                }
            }, {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show();
            })
    }

    /**
     * z上传视频
     * @param orgInfo OrgInfo
     * @param count Int
     * @param videos ArrayList<VideoItem>
     */
    fun uploadVideo(orgInfo: OrgInfo, videos: ArrayList<BaseItem>) {
        videoUploadTasks.clear()
        for (video in videos) {
            val index = videos.indexOf(video)
            excUploadVideo(orgInfo, index, video)
        }
    }

    /**
     *
     * @param tmpSecretId String 开发者拥有的项目身份识别 ID，用以身份认证
     * @param tmpSecretKey String 开发者拥有的项目身份密钥
     * @param sessionToken String
     * @param bucket String COS 中用于存储数据的容器
     * @param path String
     */
    fun excUploadImgOrVoice(index: Int, item: BaseItem, authorizationInfo: AuthorizationInfo) {
        threadPool!!.submit {
            val transferConfig = TransferConfig.Builder().build()
            val credentialProvider = MyCredentialProvider(
                authorizationInfo.tmpSecretId,
                authorizationInfo.tmpSecretKey,
                authorizationInfo.sessionToken
            )
            val cosXmlService = CosXmlService(context, serviceConfig, credentialProvider)
            val transferManager = TransferManager(cosXmlService, transferConfig)
            val cosxmlUploadTask: COSXMLUploadTask
            //图片
            if (item is ImageItem) {
                Log.v("upload_orientation", item.orientation.toString())
                val originSize = FileUtils.FormetFileSize(item.size!!, 3)
                Log.v("sizeeeee", originSize.toString())
                val isNeedCompress = originSize > 1
                //原图或者小于等于1M时，不压缩
                if (UploadLibrary.isOrigin || !isNeedCompress) {
                    //有方向
                    if (item.orientation != 0) {
                        val bitmap = BitmapUtil.rotateBitmapByDegree(item.path!!, item.orientation)
                        cosxmlUploadTask = transferManager.upload(
                            authorizationInfo.bucket,
                            authorizationInfo.keys[index],
                            BitmapUtil.Bitmap2Bytes(bitmap)
                        )
                    }
                    //没方向
                    else {
                        cosxmlUploadTask = transferManager.upload(
                            authorizationInfo.bucket,
                            authorizationInfo.keys[index],
                            item.path,
                            null
                        )
                    }
                }
                //压缩
                else {
                    //有方向
                    if (item.orientation != 0) {
                        val bitmap = BitmapUtil.rotateBitmapByDegree(item.path!!, item.orientation)
                        val pictureFileName = "picture_" + System.currentTimeMillis() + ".jpg"
                        FileUtils.saveBitmap(
                            UploadLibrary.CACHE_IMG_PATH!!,
                            pictureFileName,
                            bitmap
                        )
                        val path = UploadLibrary.CACHE_IMG_PATH + File.separator + pictureFileName
                        cosxmlUploadTask = transferManager.upload(
                            authorizationInfo.bucket,
                            authorizationInfo.keys[index],
                            BitmapUtil.compressImg(path)
                        )
                    }
                    //没方向
                    else {
                        cosxmlUploadTask = transferManager.upload(
                            authorizationInfo.bucket,
                            authorizationInfo.keys[index],
                            BitmapUtil.compressImg(item.path!!)
                        )
                    }
                }
            }
            //语音
            else {
                cosxmlUploadTask =
                    transferManager.upload(
                        authorizationInfo.bucket,
                        authorizationInfo.keys[index],
                        item.path,
                        null
                    )
            }
            cosxmlUploadTask.setCosXmlResultListener(object : CosXmlResultListener {
                override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {
                    callBack!!.onSuccess(index, authorizationInfo.mediaIds, false, null)
                }

                override fun onFail(
                    request: CosXmlRequest?,
                    exception: CosXmlClientException?,
                    serviceException: CosXmlServiceException?
                ) {
                    callBack!!.onFail(
                        index,
                        item,
                        request.toString() + "--" + exception.toString() + "--" + serviceException.toString(),
                        authorizationInfo, null
                    )
                }
            })
            //设置上传进度回调
            cosxmlUploadTask.setCosXmlProgressListener { complete, target ->
                val progress = 1.0f * complete / target * 100
                callBack!!.onProgress(index, progress.toInt(), authorizationInfo)
            }
            //设置任务状态回调, 可以查看任务过程
            cosxmlUploadTask.setTransferStateListener { state ->
                Log.d(
                    "TEST22",
                    "Task state:" + state.name
                )
            }
            imgUploadTasks.add(cosxmlUploadTask)
        }
    }

    @SuppressLint("CheckResult")
    fun excUploadVideo(orgInfo: OrgInfo, index: Int, video: BaseItem) {
        threadPool!!.submit {
            ScHttpClient.getService(GoChatService::class.java).getVideoSign(ticket, orgInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { jsonObject ->
                    val sign = jsonObject.getString("sign")
                    val fileName = jsonObject.getString("fileName")
                    val mediaId = -1L
                    val mVideoPublish = TXUGCPublish(context, appid)
                    val param = TXUGCPublishTypeDef.TXPublishParam()
                    param.signature = sign
                    param.videoPath = video.path
                    mVideoPublish.setListener(object : TXUGCPublishTypeDef.ITXVideoPublishListener {
                        override fun onPublishProgress(uploadBytes: Long, totalBytes: Long) {
                            val progress = (100 * uploadBytes / totalBytes).toInt()
                            callBack!!.onProgress(index, progress, null)
                        }

                        override fun onPublishComplete(result: TXUGCPublishTypeDef.TXPublishResult) {
                            if (result.videoURL != null) {
                                val videoUrl = result.videoURL
                                val videoId = result.videoId
                                val videoJson = JSONObject()
                                val medias = JSONArray()
                                val media = JSONObject()
                                media.put("mediaType", 0)
                                media.put("videoUrl", videoUrl)
                                media.put("videoFileName", fileName)
                                media.put("mediaId", mediaId)
                                media.put("fileId", videoId)
                                medias.put(media)
                                videoJson.put("medias", medias)

                                callBack!!.onSuccess(index, arrayListOf(mediaId), true, videoJson)
                            } else {
                                callBack!!.onFail(index, video, result.descMsg, null, orgInfo)
                            }
                        }
                    })
                    mVideoPublish.publishVideo(param)
                    videoUploadTasks.add(mVideoPublish)
                }
        }
    }

    fun cancel() {
        for (task in imgUploadTasks)
            task.cancel()
        for (task in videoUploadTasks)
            task.canclePublish()
    }

    fun destroy() {
        imgUploadTasks.clear()
        videoUploadTasks.clear()
    }

    fun setUploadProgressListener(callBack: UploadProgressCallBack) {
        this.callBack = callBack
    }

    class MyCredentialProvider(
        private val id: String,
        private val key: String,
        private val token: String
    ) :
        BasicLifecycleCredentialProvider() {

        override fun fetchNewCredentials(): QCloudLifecycleCredentials {

            //解析响应，获取密钥信息

            // 使用本地永久秘钥计算得到临时秘钥
            val current = System.currentTimeMillis() / 1000
            val expired = current + 60 * 60

            // 最后返回临时密钥信息对象
            return SessionQCloudCredentials(this.id, this.key, this.token, current, expired)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        val instance = UploadManager()
    }

}