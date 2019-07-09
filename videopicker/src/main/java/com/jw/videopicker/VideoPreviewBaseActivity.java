//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jw.videopicker;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.rxxb.imagepicker.R.id;
import com.rxxb.imagepicker.R.string;
import com.rxxb.imagepicker.ui.ImageBaseActivity;
import com.rxxb.imagepicker.util.SpaceItemDecoration;
import com.rxxb.imagepicker.util.Utils;
import com.rxxb.imagepicker.view.ViewPagerFixed;

import java.util.ArrayList;

import static com.jw.videopicker.VideoPicker.*;

public abstract class VideoPreviewBaseActivity extends ImageBaseActivity {
    protected VideoPicker imagePicker;
    protected ArrayList<VideoItem> mImageItems;
    protected int mCurrentPosition = 0;
    protected TextView mTitleCount;
    protected ArrayList<VideoItem> selectedImages;
    protected View content;
    protected View topBar;
    protected ViewPagerFixed mViewPager;
    protected VideoPageAdapter mAdapter;
    protected RecyclerView mRvPreview;
    protected VideoThumbPreviewAdapter thumbPreviewAdapter;
    protected boolean isFromItems = false;

    public VideoPreviewBaseActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_video_preview);
        this.mCurrentPosition = this.getIntent().getIntExtra(EXTRA_SELECTED_IMAGE_POSITION, 0);
        this.isFromItems = this.getIntent().getBooleanExtra(EXTRA_FROM_ITEMS, false);
        if (this.isFromItems) {
            this.mImageItems = (ArrayList)this.getIntent().getSerializableExtra(EXTRA_IMAGE_ITEMS);
        } else {
            this.mImageItems = (ArrayList)DataHolder2.getInstance().retrieve("dh_current_image_folder_items");
        }

        this.imagePicker = VideoPicker.getInstance();
        this.selectedImages = this.imagePicker.getSelectedVideos();
        this.content = this.findViewById(id.content);
        this.topBar = this.findViewById(id.top_bar);
        if (VERSION.SDK_INT >= 19) {
            LayoutParams params = (LayoutParams)this.topBar.getLayoutParams();
            params.topMargin = Utils.getStatusHeight(this);
            this.topBar.setLayoutParams(params);
        }

        this.topBar.findViewById(id.btn_ok).setVisibility(View.GONE);
        this.topBar.findViewById(id.btn_back).setOnClickListener(v -> VideoPreviewBaseActivity.this.finish());
        this.mTitleCount = (TextView)this.findViewById(id.tv_des);
        this.mViewPager = (ViewPagerFixed)this.findViewById(id.viewpager);
        this.mAdapter = new VideoPageAdapter(this, this.mImageItems);
        this.mAdapter.setPhotoViewClickListener(videoItem -> VideoPreviewBaseActivity.this.onImageSingleTap(videoItem));
        this.mViewPager.setAdapter(this.mAdapter);
        this.mViewPager.setCurrentItem(this.mCurrentPosition, false);
        this.mTitleCount.setText(this.getString(string.ip_preview_image_count, new Object[]{this.mCurrentPosition + 1, this.mImageItems.size()}));
        this.mRvPreview = (RecyclerView)this.findViewById(id.rv_preview);
        this.mRvPreview.setLayoutManager(new LinearLayoutManager(this.getApplicationContext(), 0, false));
        this.mRvPreview.addItemDecoration(new SpaceItemDecoration(Utils.dp2px(this, 6.0F)));
        this.thumbPreviewAdapter = new VideoThumbPreviewAdapter(this);
        this.mRvPreview.setAdapter(this.thumbPreviewAdapter);
        this.thumbPreviewAdapter.setOnThumbItemClickListener(imageItem -> {
            int position = VideoPreviewBaseActivity.this.mImageItems.indexOf(imageItem);
            if (position != -1 && VideoPreviewBaseActivity.this.mCurrentPosition != position) {
                VideoPreviewBaseActivity.this.mCurrentPosition = position;
                VideoPreviewBaseActivity.this.mViewPager.setCurrentItem(VideoPreviewBaseActivity.this.mCurrentPosition, false);
            }

        });
    }

    public abstract void onImageSingleTap(VideoItem videoItem);
}