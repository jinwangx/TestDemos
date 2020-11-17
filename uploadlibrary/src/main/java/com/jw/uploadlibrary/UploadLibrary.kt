package com.jw.uploadlibrary

import android.content.Context
import com.facebook.stetho.Stetho
import com.jw.uploadlibrary.http.ScHttpClient
import com.jw.uploadlibrary.http.ScHttpConfig
import com.jw.uploadlibrary.upload.UploadManager

/**
 * 创建时间：2019/5/2318:07
 * 更新时间 2019/5/2318:07
 * 版本：
 * 作者：Mr.jin
 * 描述：
 */
object UploadLibrary {
    var baseUrl: String? = null
    var region: String? = null
    var appid: String? = null
    var orgId: Long = 1
    var phone: Long? = null
    var pwd: String? = null
    var type: Int = 2
    var ticket: Long = 0
    const val TYPE_UPLOAD_VIDEO = 0   //视频
    const val TYPE_UPLOAD_IMG = 1  //图片
    const val TYPE_UPLOAD_VOICE = 2   //语音
    var maxUploadThreadSize = 2
    var uploadTimeOutTime = 60
    var isEnable = true
    var isOrigin = true

    /**
     * 设置腾讯云上传配置
     * @param baseUrl String
     * @param appid String
     * @param region String
     * @param maxUploadThreadSize Int //同时上传线程数
     * @param uploadTimeOutTime Int //超时时间，单位为s
     */
    fun setCosUploadConfig(
        baseUrl: String,
        appid: String,
        region: String,
        maxUploadThreadSize: Int = 2,
        uploadTimeOutTime: Int = 60
    ) {
        this.baseUrl = baseUrl
        this.appid = appid
        this.region = region
        this.maxUploadThreadSize = maxUploadThreadSize
        this.uploadTimeOutTime = uploadTimeOutTime
    }

    /**
     * 设置用户标识配置
     * @param phone Long
     * @param pwd String
     * @param orgId Long
     * @param type Int
     * @param ticket Long
     */
    fun setUserConfig(phone: Long, pwd: String, orgId: Long, type: Int, ticket: Long) {
        this.phone = phone
        this.pwd = pwd
        this.orgId = orgId
        this.type = type
        this.ticket = ticket
    }

    fun init(context: Context) {
        //初始化http请求引擎
        ScHttpClient.init(ScHttpConfig.create().setBaseUrl(baseUrl))
        //HttpUtils.init(ScHttpClient.getOkHttpClient())
        //stetho调试集成
        Stetho.initializeWithDefaults(context)
        UploadManager.instance.init(context)
    }

    var CACHE_IMG_PATH: String? = null   //拍照缓存路径
    fun init(baseCachePath: String) {
        CACHE_IMG_PATH = baseCachePath
    }
}