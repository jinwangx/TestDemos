package com.jw.galarylibrary.img.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.jw.galarylibrary.base.adapter.BasePageAdapter
import com.jw.library.loader.GlideImageLoader
import com.jw.library.model.ImageItem
import uk.co.senab.photoview.PhotoView
import java.util.*

class ImagePageAdapter(activity: Activity, images: ArrayList<ImageItem>) :
    BasePageAdapter<ImageItem>(activity, images) {
    private var mListener: PhotoViewClickListener? = null

    fun setPhotoViewClickListener(listener: PhotoViewClickListener) {
        this.mListener = listener
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = PhotoView(mActivity)
        val imageItem = mItems[position]
        GlideImageLoader.displayImage(
            mActivity,
            imageItem.path!!,
            photoView
        )
        photoView.setOnPhotoTapListener { view, x, y ->
            if (mListener != null) {
                mListener!!.onPhotoTapListener(view, x, y)
            }
        }
        container.addView(photoView)
        return photoView
    }

    interface PhotoViewClickListener {
        fun onPhotoTapListener(view: View, x: Float, y: Float)
    }
}
