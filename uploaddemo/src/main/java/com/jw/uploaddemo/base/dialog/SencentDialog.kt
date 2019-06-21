package com.jw.uploaddemo.base.dialog

import android.os.Bundle
import android.support.v4.app.FragmentManager
import com.sencent.library.base.dialog.BaseDialog

/**
 * 由 Harreke 创建于 2017/10/11.
 */
abstract class SencentDialog : BaseDialog() {
    override fun show(manager: FragmentManager, tag: String) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
        }
    }

    fun show(manager: FragmentManager, tag: String, arguments: Bundle?) {
        this.arguments = arguments
        show(manager, tag)
    }
}