package com.jw.galarylibrary.video

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import com.jw.galarylibrary.R
import com.jw.galarylibrary.base.adapter.GridAdapter
import com.jw.galarylibrary.base.bean.Folder
import com.jw.library.model.VideoItem
import java.io.File
import java.util.*


class VideoDataSource internal constructor(
    private val activity: FragmentActivity,
    path: String?,
    private val loadedListener: GridAdapter.OnItemsLoadedListener<VideoItem>
) : LoaderCallbacks<Cursor> {

    private val IMAGE_PROJECTION = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Thumbnails.DATA,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.MIME_TYPE
    )
    private val videoFolders = ArrayList<Folder<VideoItem>>()

    init {
        val loaderManager = LoaderManager.getInstance(activity)
        if (path == null) {
            loaderManager.initLoader(0, null, this)
        } else {
            val bundle = Bundle()
            bundle.putString("path", path)
            loaderManager.initLoader(1, bundle, this)
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        var cursorLoader: CursorLoader? = null
        if (id == LOADER_ALL) {
            cursorLoader = CursorLoader(
                this.activity,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                this.IMAGE_PROJECTION,
                MediaStore.Video.Media.MIME_TYPE + "=?",
                arrayOf("video/mp4"),
                MediaStore.Video.Media.DATE_MODIFIED + " desc"
            )
        }

        if (id == LOADER_CATEGORY) {
            cursorLoader = CursorLoader(
                this.activity,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                this.IMAGE_PROJECTION,
                this.IMAGE_PROJECTION[1] + " like '%" + args!!.getString("path") + "%'",
                null,
                this.IMAGE_PROJECTION[6] + " DESC"
            )
        }

        return cursorLoader!!
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (this.videoFolders.size != 0)
            return
        this.videoFolders.clear()
        if (data != null) {
            val allVideos = ArrayList<VideoItem>()

            while (data.moveToNext()) {
                val videoName = data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[0]))
                val videoPath = data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[1]))
                val videoSize = data.getLong(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[2]))
                val videoId = data.getLong(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[3]))
                val duration = data.getLong(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[4]))
                val mineType = data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[5]))
                /*                if(duration>MAX_LENGTH)
                    continue;*/
                var uri: Uri? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val baseUri = Uri.parse("content://media/external/video/media")
                    uri = Uri.withAppendedPath(baseUri, "" + videoId)
                }
                val file = File(videoPath)
                if (file.exists() && file.length() > 0L) {

                    val videoItem = VideoItem()
                    videoItem.name = videoName
                    videoItem.path = videoPath
                    videoItem.size = videoSize
                    videoItem.duration = duration
                    videoItem.uri = uri
                    videoItem.mimeType = mineType
                    allVideos.add(videoItem)
                    val videoFile = File(videoPath)
                    val videoParentFile = videoFile.parentFile
                    val videoFolder = Folder<VideoItem>()
                    videoFolder.name = videoParentFile.name
                    videoFolder.path = videoParentFile.absolutePath
                    if (!this.videoFolders.contains(videoFolder)) {
                        val videos = ArrayList<VideoItem>()
                        videos.add(videoItem)
                        videoFolder.cover = videoItem
                        videoFolder.items = videos
                        this.videoFolders.add(videoFolder)
                    } else {
                        this.videoFolders.get(this.videoFolders.indexOf(videoFolder)).items!!.add(
                            videoItem
                        )
                    }
                }
            }

            if (data.count > 0 && allVideos.size > 0) {
                val allVideosFolder = Folder<VideoItem>()
                allVideosFolder.name = this.activity.resources.getString(R.string.ip_all_videos)
                allVideosFolder.path = "/"
                allVideosFolder.cover = allVideos[0]
                allVideosFolder.items = allVideos
                this.videoFolders.add(0, allVideosFolder)
            }
        }

        VideoPicker.itemFolders = this.videoFolders
        this.loadedListener.onItemsLoaded(this.videoFolders)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        println("--------")
    }

    companion object {
        private val LOADER_ALL = 0
        private val LOADER_CATEGORY = 1
        var MAX_LENGTH = VideoPicker.VIDEO_RECORD_LENGTH
    }
}
