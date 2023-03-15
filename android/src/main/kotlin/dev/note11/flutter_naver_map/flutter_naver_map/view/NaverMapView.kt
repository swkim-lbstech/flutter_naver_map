package dev.note11.flutter_naver_map.flutter_naver_map.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import dev.note11.flutter_naver_map.flutter_naver_map.R
import dev.note11.flutter_naver_map.flutter_naver_map.controller.NaverMapControlSender
import dev.note11.flutter_naver_map.flutter_naver_map.controller.NaverMapController
import dev.note11.flutter_naver_map.flutter_naver_map.controller.overlay.OverlayHandler
import dev.note11.flutter_naver_map.flutter_naver_map.converter.DefaultTypeConverter.asMap
import dev.note11.flutter_naver_map.flutter_naver_map.model.flutter_default_custom.NPoint
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.NaverMapViewOptions
import dev.note11.flutter_naver_map.flutter_naver_map.util.NLocationSource
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


internal class NaverMapView(
    context: Context,
    private val activity: Activity,
    private val naverMapViewOptions: NaverMapViewOptions,
    private val channel: MethodChannel,
    private val overlayController: OverlayHandler,
) : PlatformView, Application.ActivityLifecycleCallbacks {

    private lateinit var naverMap: NaverMap
    private lateinit var naverMapControlSender: NaverMapControlSender
    private val mapView = MapView(context, naverMapViewOptions.naverMapOptions).apply {
        setTempMethodCallHandler()
        getMapAsync { naverMap ->
            this@NaverMapView.naverMap = naverMap
            onMapReady()
        }
    }
    private var rawNaverMapOptionTempCache: Any? = null

    init {
        setActivityThemeAppCompat()
        registerLifecycleCallback()
    }

    private fun setTempMethodCallHandler() {
        channel.setMethodCallHandler { call, _ ->
            if (call.method == "updateOptions") { // todo : test
                rawNaverMapOptionTempCache = call.arguments
            }
        }
    }

    private fun onMapReady() {
        initializeMapController()
        setLocationSource()
        setMapTapListener()

        mapView.onCreate(null)
        naverMapControlSender.onMapReady()
    }

    private fun initializeMapController() {
        naverMapControlSender = NaverMapController(
            naverMap, channel, activity.applicationContext, overlayController
        ).apply {
            rawNaverMapOptionTempCache?.let { updateOptions(it.asMap()) {} }
        }
    }

    private fun setLocationSource() {
        naverMap.locationSource = NLocationSource(activity)
    }

    private fun setMapTapListener() {
        naverMap.run {
            setOnMapClickListener { pointFPx, latLng ->
                naverMapControlSender.onMapTapped(NPoint.fromPointFWithPx(pointFPx), latLng)
            }
            setOnSymbolClickListener {
                naverMapControlSender.onSymbolTapped(it)
                    ?: naverMapViewOptions.consumeSymbolTapEvents
            }
            addOnCameraChangeListener(naverMapControlSender::onCameraChange)
            addOnCameraIdleListener(naverMapControlSender::onCameraIdle)
            addOnIndoorSelectionChangeListener(naverMapControlSender::onSelectedIndoorChanged)
        }
    }

    override fun getView(): View = mapView

    override fun dispose() {
        unRegisterLifecycleCallback()

        mapView.run {
            onPause()
            onStop()
            onDestroy()
        }

        (naverMapControlSender as NaverMapController).remove()
    }

    // Using AppCompat Theme.
    // default flutter android theme not support naverMap's AppCompatDialog.
    private fun setActivityThemeAppCompat() {
        activity.setTheme(R.style.Theme_AppCompat_Light)
    }

    private fun registerLifecycleCallback() {
        activity.application.registerActivityLifecycleCallbacks(this)
    }

    private fun unRegisterLifecycleCallback() {
        activity.application.unregisterActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity != this.activity) return

        mapView.onCreate(savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity != this.activity) return

        mapView.onStart()
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity != this.activity) return

        reloadMap()
        mapView.onResume()
    }

    private fun reloadMap() {
        if (this::naverMap.isInitialized) {
            val nowMapType = naverMap.mapType
            naverMap.mapType = NaverMap.MapType.None
            naverMap.mapType = nowMapType
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity != this.activity) return

        mapView.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity != this.activity) return

        mapView.onStop()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (activity != this.activity) return

        mapView.onSaveInstanceState(outState)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity != this.activity) return

        mapView.onDestroy()
        unRegisterLifecycleCallback()
    }
}