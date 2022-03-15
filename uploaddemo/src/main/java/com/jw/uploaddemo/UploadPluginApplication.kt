package com.jw.uploaddemo

import android.app.Application
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.jw.cameralibrary.CameraLibrary
import com.jw.croplibrary.CropLibrary
import com.jw.galarylibrary.GalaryLibrary
import com.jw.library.utils.RomUtil
import com.jw.uilibrary.base.application.BaseApplication
import com.jw.uploadlibrary.UploadLibrary
import com.jw.voicelibrary.VoiceLibrary

class UploadPluginApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        application = this
        GalaryLibrary.init()
        CameraLibrary.init(externalCacheDir.absolutePath)
        CropLibrary.init(this, externalCacheDir.absolutePath)
        VoiceLibrary.init(externalCacheDir.absolutePath)
        UploadLibrary.init(externalCacheDir.absolutePath)
        initFFmpegBinary(this)
        if (RomUtil.isEmui()) {
            // 刷新相册
            MediaScannerConnection.scanFile(
                this,
                arrayOf(Environment.getExternalStorageDirectory().toString()),
                null
            ) { path, uri -> }
        }
    }

    private fun initFFmpegBinary(context: Context) {
    }

    companion object {
        var application: Application? = null
            private set
    }
}
