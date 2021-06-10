package com.sencent.thirdpart.rxjava


//import com.sencent.library.util.cocurrent.CommonExecutors
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by wangjm on 2017/10/20.
 */

object RxjavaUtils {
    val NULL = Any()

/*    val asyncScheduers: Scheduler
        get() = Schedulers.from(CommonExecutors.THREAD_POOL_EXECUTOR)*/

    /**
     * 快速发送工作线程和UI回调
     *
     * @param workAction 工作线程
     * @param uiAction   主线程
     * @param error      错误回调
     * @return 订单
     */
    fun workWithUiThread(
        workAction: () -> Unit,
        uiAction: () -> Unit,
        errorAction: (Throwable) -> Unit
    ): Disposable {
        return Observable.fromCallable(workAction)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onTerminateDetach()
            .doOnComplete(uiAction)
            .doOnError(errorAction)
            .subscribe()
    }

    fun workWithUiThread(workAction: () -> Unit, uiAction: () -> Unit): Disposable {
        return Observable.fromCallable(workAction)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onTerminateDetach()
            .doOnComplete(uiAction)
            .doOnError { it.printStackTrace() }
            .subscribe()
    }

    /**
     * 快速发送工作线程和有返回值的UI回调
     *
     * @param workAction 工作线程
     * @param uiAction   主线程
     * @param <R>        返回值的类型
     * @return 订单
    </R> */
    fun <T> workWithUiResult(
        workAction: () -> T,
        uiAction: (T) -> Unit,
        errorAction: (Throwable) -> Unit
    ): Disposable {
        return Observable.fromCallable(workAction)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onTerminateDetach()
            .subscribe(uiAction, errorAction)
    }

    fun <T> workWithUiResult(workAction: () -> T, uiAction: (T) -> Unit): Disposable {
        return Observable.fromCallable(workAction)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onTerminateDetach()
            .subscribe(uiAction, { it.printStackTrace() })
    }

    fun <T> ioTransformer(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun <T> ioSingleTransformer(): SingleTransformer<T, T> = SingleTransformer {
        it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * @see Disposable.dispose
     */
    fun dispose(disposable: Disposable?) {
        disposable?.dispose()
    }
}