package com.jw.uploadlibrary.model

import com.jw.uploadlibrary.UploadLibrary

/**
 * 创建时间：2019/8/215:52
 * 更新时间 2019/8/215:52
 * 版本：
 * 作者：Mr.jin
 * 描述：
 */
class MediaReq {
    var mediaIds:ArrayList<Long> = ArrayList()
    var useStorage = true
    var orgId: Long = UploadLibrary.orgId
}