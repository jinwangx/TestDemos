package com.jw.galarylibrary.img

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore.Images.Media
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.util.Log
import com.jw.galarylibrary.R
import com.jw.galarylibrary.base.adapter.GridAdapter
import com.jw.galarylibrary.base.bean.Folder
import com.jw.library.model.ImageItem
import com.sencent.thirdpart.rxjava.RxjavaUtils
import java.io.File
import java.util.*

class ImageDataSource(
    private val activity: FragmentActivity,
    path: String?,
    private val loadedListener: GridAdapter.OnItemsLoadedListener<ImageItem>
) : LoaderCallbacks<Cursor> {

    private val IMAGE_PROJECTION = arrayOf(
        Media.DISPLAY_NAME,
        Media.DATA,
        Media.SIZE,
        Media.WIDTH,
        Media.HEIGHT,
        Media.MIME_TYPE,
        Media.DATE_ADDED,
        Media.ORIENTATION
    )
    private val imageFolders = ArrayList<Folder<ImageItem>>()
    private var cursorLoader: CursorLoader? = null

    init {
        if (path == null) {
            LoaderManager.getInstance(activity).initLoader(0, null, this)
        } else {
            val bundle = Bundle()
            bundle.putString("path", path)
            LoaderManager.getInstance(activity).initLoader(1, bundle, this)
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id == LOADER_ALL) {
            cursorLoader = CursorLoader(
                this.activity,
                Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                this.IMAGE_PROJECTION[6] + " DESC"
            )
        }

        if (id == LOADER_CATEGORY) {
            cursorLoader = CursorLoader(
                this.activity,
                Media.EXTERNAL_CONTENT_URI,
                this.IMAGE_PROJECTION,
                this.IMAGE_PROJECTION[1] + " like '%" + args!!.getString("path") + "%'",
                null,
                this.IMAGE_PROJECTION[6] + " DESC"
            )
        }

        return cursorLoader!!
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (this.imageFolders.size != 0)
            return

        RxjavaUtils.workWithUiThread({
            this.imageFolders.clear()
            if (data != null) {
                val allImages = ArrayList<ImageItem>()

                while (data.moveToNext()) {
                    val imageName =
                        data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[0]))
                    val imagePath =
                        data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[1]))
                    val imageSize =
                        data.getLong(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[2]))
                    val imageWidth =
                        data.getInt(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[3]))
                    val imageHeight =
                        data.getInt(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[4]))
                    val imageMimeType =
                        data.getString(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[5]))
                    val imageAddTime =
                        data.getLong(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[6]))
                    val orientation =
                        data.getInt(data.getColumnIndexOrThrow(this.IMAGE_PROJECTION[7]))
                    Log.v(
                        "imageInfo",
                        "size：$imageSize  width：$imageWidth   height：$imageHeight   imageAddTime：$imageAddTime  orientation：$orientation"
                    )

                    val imageItem = ImageItem()
                    imageItem.name = imageName
                    imageItem.path = imagePath
                    imageItem.size = imageSize
                    imageItem.width = imageWidth
                    imageItem.height = imageHeight
                    imageItem.mimeType = imageMimeType
                    imageItem.addTime = imageAddTime
                    imageItem.orientation = orientation
                    allImages.add(imageItem)
                    val imageFile = File(imagePath)
                    val imageParentFile = imageFile.parentFile
                    val imageFolder = Folder<ImageItem>()
                    imageFolder.name = imageParentFile.name
                    imageFolder.path = imageParentFile.absolutePath
                    if (!this.imageFolders.contains(imageFolder)) {
                        var images = ArrayList<ImageItem>()
                        images.add(imageItem)
                        imageFolder.cover = imageItem
                        imageFolder.items = images
                        this.imageFolders.add(imageFolder)
                    } else {
                        this.imageFolders[this.imageFolders.indexOf(imageFolder)].items!!.add(
                            imageItem
                        )
                    }
                }

                if (data.count > 0 && allImages.size > 0) {
                    val allImagesFolder = Folder<ImageItem>()
                    allImagesFolder.name = this.activity.resources.getString(R.string.ip_all_images)
                    allImagesFolder.path = "/"
                    allImagesFolder.cover = allImages[0]
                    allImagesFolder.items = allImages
                    this.imageFolders.add(0, allImagesFolder)
                }
            }

            ImagePicker.itemFolders = this.imageFolders
        }, {
            this.loadedListener.onItemsLoaded(this.imageFolders)
        })
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        println("--------")
    }

    companion object {
        private const val LOADER_ALL = 0
        private const val LOADER_CATEGORY = 1
    }
}
