package com.jw.galarylibrary.img.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AnimationUtils
import com.jw.galarylibrary.R
import com.jw.galarylibrary.base.activity.BasePreviewActivity
import com.jw.galarylibrary.base.adapter.BasePageAdapter
import com.jw.galarylibrary.img.adapter.ImagePageAdapter
import com.jw.library.model.ImageItem
import com.jw.library.ui.BaseBindingActivity
import com.jw.library.utils.ThemeUtils
import java.util.*

/**
 * 创建时间：2021/4/309:09
 * 更新时间 2021/4/309:09
 * 版本：
 * 作者：Mr.jin
 * 描述：
 */
class ImagePreviewActivity2 : BaseBindingActivity<ActivityPreview2Binding>(),
    ViewPager.OnPageChangeListener, ImagePageAdapter.PhotoViewClickListener {

    private var mCurrentPosition = 0
    lateinit var mItems: ArrayList<ImageItem>
    lateinit var mRvAdapter: BasePageAdapter<ImageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbar(findViewById(R.id.toolbar)!!)
        //window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        //得到当前界面的装饰视图
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            //设置让应用主题内容占据状态栏和导航栏
            val option = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            decorView.systemUiVisibility = option
            //设置状态栏和导航栏颜色为透明
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        mCurrentPosition =
            intent.getIntExtra(BasePreviewActivity.EXTRA_SELECTED_ITEM_POSITION, 0)
        mItems =
            intent.getSerializableExtra(BasePreviewActivity.EXTRA_ITEMS) as ArrayList<ImageItem>
        mRvAdapter = ImagePageAdapter(this, mItems)
        (mRvAdapter as ImagePageAdapter).setPhotoViewClickListener(this)

        mBinding.apply {
            numTxt = getString(
                R.string.ip_preview_image_count,
                mCurrentPosition + 1,
                mItems.size
            )
            curItem = mItems[mCurrentPosition]
            bottomBar.apply {
                bottomBar.visibility = View.VISIBLE
            }
            clickListener = View.OnClickListener {
                when (it.id) {
                    R.id.btn_back -> finish()
                }
            }
            bottomBar.setPadding(
                0,
                0,
                0,
                ThemeUtils.getNavigationBarHeight(this@ImagePreviewActivity2) / 2
            )

            vpImg.apply {
                adapter = mRvAdapter
                setCurrentItem(mCurrentPosition, false)
                addOnPageChangeListener(this@ImagePreviewActivity2)
            }
        }
    }

    override fun getLayoutId() = R.layout.activity_preview_2

    override fun doConfig(arguments: Intent) {
    }

    override fun onPhotoTapListener(view: View, x: Float, y: Float) {
        when (mBinding.toolbar.visibility) {
            View.VISIBLE -> {
                mBinding.apply {
                    toolbar.animation =
                        AnimationUtils.loadAnimation(this@ImagePreviewActivity2, R.anim.top_out)
                    bottomBar.animation =
                        AnimationUtils.loadAnimation(this@ImagePreviewActivity2, R.anim.fade_out)
                    toolbar.visibility = View.GONE
                    bottomBar.visibility = View.GONE
                    val decorView = window.decorView
                    //设置让应用主题内容占据状态栏和导航栏
                    val option = View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    decorView.systemUiVisibility = option
                }
            }
            View.GONE -> {
                mBinding.apply {
                    toolbar.animation =
                        AnimationUtils.loadAnimation(this@ImagePreviewActivity2, R.anim.top_in)
                    bottomBar.animation =
                        AnimationUtils.loadAnimation(this@ImagePreviewActivity2, R.anim.fade_in)
                    toolbar.visibility = View.VISIBLE
                    bottomBar.visibility = View.VISIBLE
                    val decorView = window.decorView
                    //设置让应用主题内容占据状态栏和导航栏
                    val option = View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    decorView.systemUiVisibility = option
                }
            }
        }
    }

    override fun onPageScrollStateChanged(p0: Int) {
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
    }

    override fun onPageSelected(position: Int) {
        mCurrentPosition = position
        mBinding.apply {
            numTxt = getString(
                R.string.ip_preview_image_count,
                mCurrentPosition + 1,
                mItems.size
            )
            curItem = mItems[mCurrentPosition]
        }
    }

    companion object {

        fun start(
            activity: AppCompatActivity,
            position: Int,
            items: ArrayList<ImageItem>?
        ) {
            val intent = Intent(activity, ImagePreviewActivity2::class.java)
            intent.putExtra(BasePreviewActivity.EXTRA_SELECTED_ITEM_POSITION, position)
            intent.putExtra(BasePreviewActivity.EXTRA_ITEMS, items)
            ActivityCompat.startActivityForResult(
                activity, intent,
                BasePreviewActivity.REQUEST_CODE_ITEM_PREVIEW, null
            )
        }
    }
}