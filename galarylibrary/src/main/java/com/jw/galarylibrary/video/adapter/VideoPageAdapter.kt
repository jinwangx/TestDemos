package com.jw.galarylibrary.video.adapter

import android.app.Activity
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.jw.galarylibrary.R
import com.jw.galarylibrary.base.adapter.BasePageAdapter
import com.jw.library.loader.GlideImageLoader
import com.jw.library.model.VideoItem
import com.jw.library.utils.ThemeUtils
import java.util.*

class VideoPageAdapter(activity: Activity, videos: ArrayList<VideoItem>) :
    BasePageAdapter<VideoItem>(activity, videos) {
    private var mListener: PhotoViewClickListener? = null

    fun setPhotoViewClickListener(listener: PhotoViewClickListener) {
        mListener = listener
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val videoItem = mItems[position]
        val view = View.inflate(mActivity, R.layout.pager_preview, null)
        val iv = view.findViewById<ImageView>(R.id.iv1)
        val ivStart = view.findViewById<ImageView>(R.id.iv_start)

        GlideImageLoader.displayVideoThumbnailImage(
            mActivity,
            iv,
            videoItem,
            Size(ThemeUtils.getWindowWidth(mActivity), ThemeUtils.getWindowHeight(mActivity))
        )
        iv.setOnClickListener { v ->
            run {
                if (mListener != null)
                    mListener?.onImageClickListener(videoItem)
            }
        }
        ivStart.setOnClickListener { v ->
            run {
                if (mListener != null)
                    mListener?.onStartClickListener(videoItem)
            }
        }
        container.addView(view)
        return view
    }

    override fun getItemPosition(`object`: Any): Int {
        return -2
    }

    interface PhotoViewClickListener {
        fun onStartClickListener(videoItem: VideoItem)

        fun onImageClickListener(videoItem: VideoItem)
    }
}
