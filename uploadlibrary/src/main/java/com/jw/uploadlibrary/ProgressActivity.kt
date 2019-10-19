package com.jw.uploadlibrary

import android.content.Intent
import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.jw.library.ColorCofig
import com.jw.library.model.BaseItem
import com.jw.library.model.ImageItem
import com.jw.library.model.VideoItem
import com.jw.library.model.VoiceItem
import com.jw.library.ui.BaseBindingActivity
import com.jw.uploadlibrary.adapter.ProgressAdapter
import com.jw.uploadlibrary.databinding.ActivityProgressBinding
import com.jw.uploadlibrary.http.ScHttpClient
import com.jw.uploadlibrary.http.service.GoChatService
import com.jw.uploadlibrary.model.AuthorizationInfo
import com.jw.uploadlibrary.model.KeyReqInfo
import com.jw.uploadlibrary.model.MediaReq
import com.jw.uploadlibrary.model.OrgInfo
import com.jw.uploadlibrary.upload.UploadManager
import com.jw.uploadlibrary.upload.UploadProgressCallBack
import kotlinx.android.synthetic.main.activity_progress.*
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


/**
 * 创建时间：2019/6/1417:35
 * 更新时间 2019/6/1417:35
 * 版本：
 * 作者：Mr.jin
 * 描述：
 */
open class ProgressActivity : BaseBindingActivity<ActivityProgressBinding>(),
    UploadProgressCallBack {
    var results: ArrayList<Boolean> = ArrayList()
    var result: JSONObject? = null
    val mediaReq = MediaReq()
    var isExcuteUpload = false
    private var mRecyclerAdapter: ProgressAdapter? = null
    val STATE_START = 0
    val STATE_PROGRESS = 1
    val STATE_END = 2
    val STATE_ERROR = 3
    val STATE_COMPRESSING = 4

    override fun getLayoutId() = R.layout.activity_progress

    override fun doConfig(arguments: Intent) {
        setConfirmButtonBg(mBinding.topBar.btnOk)
        mBinding.apply {
            topBar.tvDes.text = "上传进度"
            topBar.btnOk.text = "确定"
            topBar.btnOk.isEnabled = false
            topBar.btnOk.setTextColor(Color.parseColor(ColorCofig.toolbarTitleColorDisabled))
            topBar.btnBack.isEnabled = false
            clickListener = View.OnClickListener {
                when (it.id) {
                    R.id.btn_ok -> {
                        backProgress()
                    }
                    R.id.btn_back -> {
                        finish()
                    }
                }
            }
        }
        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mRecyclerAdapter =
            ProgressAdapter(this, ArrayList())
        mBinding.recycler.adapter = mRecyclerAdapter
        val type = arguments.getIntExtra("type", UploadLibrary.TYPE_UPLOAD_IMG)
        when (type) {
            UploadLibrary.TYPE_UPLOAD_VIDEO -> {
                val videos =
                    arguments.getSerializableExtra("items") as ArrayList<VideoItem>
                uploadVideo(videos)
            }
            UploadLibrary.TYPE_UPLOAD_IMG -> {
                val images =
                    arguments.getSerializableExtra("items") as ArrayList<ImageItem>
                uploadImgOrVoice(images, true)
            }
            UploadLibrary.TYPE_UPLOAD_VOICE -> {
                val voices =
                    arguments.getSerializableExtra("items") as ArrayList<VoiceItem>
                uploadImgOrVoice(voices, false)
            }
        }
    }


    /**
     * 上传图片或语音
     * @param items ArrayList<out BaseItem>
     * @param isImage Boolean
     */
    private fun uploadImgOrVoice(items: ArrayList<out BaseItem>, isImage: Boolean) {
        addProgressView(items)
        val keyReqInfo = KeyReqInfo()
        keyReqInfo.orgId = UploadLibrary.orgId
        for (image in items) {
            val fileInfo = KeyReqInfo.FileInfo()
            fileInfo.name = image.name
            if (isImage)
                fileInfo.type = UploadLibrary.TYPE_UPLOAD_IMG
            else
                fileInfo.type = UploadLibrary.TYPE_UPLOAD_VOICE
            keyReqInfo.files.add(fileInfo)
            results.add(false)
        }
        UploadManager.instance.uploadImgOrVoice(keyReqInfo, items)
        UploadManager.instance.setUploadProgressListener(this)
    }

    /**
     * 上传视频
     * @param videoItems ArrayList<VideoItem>
     */
    private fun uploadVideo(videoItems: ArrayList<VideoItem>) {
        val orgInfo = OrgInfo()
        orgInfo.orgId = UploadLibrary.orgId
        addProgressView(videoItems)
        UploadManager.instance.setVideoCompressListener(object :
            nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler {
            override fun onFinish() {
                Log.v("compress:onFinish", "finish")
            }

            override fun onSuccess(message: String?) {
                Log.v("compress:onSuccess", message)
                mRecyclerAdapter!!.refresh(0, STATE_START, 0, null)
            }

            override fun onFailure(message: String?) {
                Log.v("compress:onFailure", message)
                mRecyclerAdapter!!.refresh(0, STATE_COMPRESSING, 0, "文件压缩失败,请重新上传！")
                setConfirmEnable(true)
            }

            override fun onProgress(message: String?) {
                Log.v("compress:onProgress", message)
                val timePattern = Pattern.compile("(?<=time=)[\\d:.]*")
                val sc = Scanner(message)
                val match = sc.findWithinHorizon(timePattern, 0)
                if (match != null) {
                    val matchSplit =
                        match!!.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (android.R.attr.duration != 0) {
                        val progress = ((Integer.parseInt(matchSplit[0]) * 3600).toFloat() +
                                (Integer.parseInt(matchSplit[1]) * 60).toFloat() +
                                java.lang.Float.parseFloat(matchSplit[2])) / android.R.attr.duration
                        var showProgress = (progress * 100).toInt()
                        if (showProgress > 100) {
                            showProgress = 100
                        }
                        mRecyclerAdapter!!
                            .refresh(0, STATE_COMPRESSING, 0, "文件压缩中$showProgress%")
                    }
                }
            }

            override fun onStart() {
                mRecyclerAdapter!!.refresh(0, STATE_COMPRESSING, 0, "文件压缩中...")
            }

        })
        UploadManager.instance.setUploadProgressListener(this)
        UploadManager.instance.uploadVideo(orgInfo, videoItems)
        for (image in videoItems)
            results.add(false)
    }

    /**
     * 上传成功回调
     * @param index Int
     * @param mediaId Long
     * @param isVideo Boolean
     * @param videoJson JSONObject?
     */
    override fun onSuccess(
        index: Int,
        mediaIds: ArrayList<Long>,
        isVideo: Boolean,
        videoJson: JSONObject?
    ) {
        //按上传成功顺序
        //mediaReq.mediaIds.saveToGalary(mediaId)
        //按选择顺序
        mediaReq.mediaIds = mediaIds
        runOnUiThread {
            results[index] = true
            setConfirmEnable(true)
            for (result in results) {
                if (!result)
                    setConfirmEnable(false)
            }
            if (mBinding.topBar.btnOk.isEnabled && !isExcuteUpload) {
                //按选择顺序
                //mediaReq.mediaIds.saveToGalary(mediaId)
                isExcuteUpload = true
                setConfirmEnable(false)
                if (isVideo) {
                    result = videoJson
                    Log.v("upload_success", result.toString())
                    setConfirmEnable(true)
                    backProgress()
                } else {
                    ScHttpClient.getService(GoChatService::class.java)
                        .getMedias(UploadLibrary.ticket, mediaReq)
                        .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe({ jsonObject ->
                            result = jsonObject
                            Log.v("upload_success", result.toString())
                            setConfirmEnable(true)
                            backProgress()
                        }, { })
                }
            }
        }
    }

    /**
     * 上传失败回调
     * @param index Int
     * @param error String
     */
    override fun onFail(
        index: Int,
        item: BaseItem,
        error: String,
        authorizationInfo: AuthorizationInfo?,
        orgInfo: OrgInfo?
    ) {
        runOnUiThread {
            setConfirmEnable(true)
            mRecyclerAdapter!!.refresh(index, STATE_ERROR, 0, null)
            mRecyclerAdapter!!.getHolder(index).setUploadItemListener(object :
                ProgressAdapter.UploadItemListener {
                override fun error() {
                    Toast.makeText(this@ProgressActivity, error, Toast.LENGTH_SHORT).show()
                    Log.v("upload_error", error)
                    //重新上传
                    if (item is VideoItem) {
                        if (UploadLibrary.isCompress)
                            UploadManager.instance.compress(orgInfo!!, index, item)
                        else
                            UploadManager.instance.excUploadVideo(orgInfo!!, index, item)
                    } else {
                        UploadManager.instance.excUploadImgOrVoice(
                            index,
                            item,
                            authorizationInfo!!

                        )
                    }
                }

                override fun success() {
                }

            })
        }
    }

    private fun setConfirmEnable(isEnable: Boolean) {
        if (isEnable) {
            mBinding.apply {
                topBar.btnOk.isEnabled = true
                topBar.btnOk.setTextColor(Color.parseColor(ColorCofig.toolbarTitleColorNormal))
                topBar.btnBack.isEnabled = true
            }
        } else {
            mBinding.apply {
                topBar.btnOk.isEnabled = false
                topBar.btnBack.isEnabled = false
                topBar.btnOk.setTextColor(Color.parseColor(ColorCofig.toolbarTitleColorDisabled))
            }
        }
    }

    private fun backProgress() {
        val intent = Intent()
        intent.putExtra("medias", result.toString())
        setResult(UploadLibrary.RESULT_UPLOAD_SUCCESS, intent)
        finish()
    }

    /**
     * 上传进度回调
     * @param index Int
     * @param progress Int
     * @param authorizationInfo AuthorizationInfo?
     */
    override fun onProgress(index: Int, progress: Int, authorizationInfo: AuthorizationInfo?) {
        runOnUiThread {
            mRecyclerAdapter!!.refresh(index, STATE_PROGRESS, progress, null)
        }
    }

    /**
     * 新增上传卡片
     * @param list ArrayList<*>
     * @param type Int
     */
    private fun addProgressView(list: ArrayList<*>) {
        mRecyclerAdapter?.lists = list
        mRecyclerAdapter?.notifyDataSetChanged()
        recycler.smoothScrollToPosition(list.size - 1)
        recycler.smoothScrollToPosition(0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBinding.topBar.btnOk.isEnabled) {
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}