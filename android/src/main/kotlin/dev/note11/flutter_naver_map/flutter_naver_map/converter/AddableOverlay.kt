package dev.note11.flutter_naver_map.flutter_naver_map.converter

import android.content.Context
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.Overlay
import dev.note11.flutter_naver_map.flutter_naver_map.controller.NaverMapControlHandler
import dev.note11.flutter_naver_map.flutter_naver_map.model.enum.NOverlayType
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.NOverlayInfo
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.*
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.NArrowHeadPathOverlay
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.NCircleOverlay
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.NGroundOverlay
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.NInfoWindow
import dev.note11.flutter_naver_map.flutter_naver_map.model.map.overlay.overlay.NMarker
import dev.note11.flutter_naver_map.flutter_naver_map.model.overlay.overlay.*

internal interface AddableOverlay<T : Overlay> {
    val info: NOverlayInfo
    fun createMapOverlay(): T

    fun toMap(): Map<String, Any?>

    companion object {
        /** Used on @see [NaverMapControlHandler.addOverlayAll] */
        fun fromJson(
            info: NOverlayInfo,
            args: Map<String, Any>,
            context: Context,
        ): AddableOverlay<out Overlay> {
            val creator = when (info.type) {
                NOverlayType.MARKER -> NMarker::fromMap
                NOverlayType.INFO_WINDOW -> { rawMap ->
                    NInfoWindow.fromMap(rawMap, context = context)
                }
                NOverlayType.CIRCLE_OVERLAY -> NCircleOverlay::fromMap
                NOverlayType.GROUND_OVERLAY -> NGroundOverlay::fromMap
                NOverlayType.POLYGON_OVERLAY -> NPolygonOverlay::fromMap
                NOverlayType.POLYLINE_OVERLAY -> NPolylineOverlay::fromMap
                NOverlayType.PATH_OVERLAY -> NPathOverlay::fromMap
                NOverlayType.MULTI_PART_PATH_OVERLAY -> NMultipartPathOverlay::fromMap
                NOverlayType.ARROW_HEAD_PATH_OVERLAY -> NArrowHeadPathOverlay::fromMap
                NOverlayType.LOCATION_OVERLAY -> throw IllegalArgumentException("LocationOverlay can not be created from json")
            }
            return creator.invoke(args)
        }


        /** Used on @see [NaverMapControlHandler.pickAll] */
        fun fromOverlay(
            overlay: Overlay,
            info: NOverlayInfo,
        ): AddableOverlay<out Overlay> {
            val creator = when (info.type) {
                NOverlayType.MARKER -> NMarker::fromMarker
                NOverlayType.INFO_WINDOW -> NInfoWindow::fromInfoWindow
                NOverlayType.CIRCLE_OVERLAY -> NCircleOverlay::fromCircleOverlay
                NOverlayType.GROUND_OVERLAY -> NGroundOverlay::fromGroundOverlay
                NOverlayType.POLYGON_OVERLAY -> NPolygonOverlay::fromPolygonOverlay
                NOverlayType.POLYLINE_OVERLAY -> NPolylineOverlay::fromPolylineOverlay
                NOverlayType.PATH_OVERLAY -> NPathOverlay::fromPathOverlay
                NOverlayType.MULTI_PART_PATH_OVERLAY -> NMultipartPathOverlay::fromMultipartPathOverlay
                NOverlayType.ARROW_HEAD_PATH_OVERLAY -> NArrowHeadPathOverlay::fromArrowheadPathOverlay
                NOverlayType.LOCATION_OVERLAY -> ::makeLocationOverlayCreator
            }
            return creator.invoke(overlay, info.id)
        }

        private fun makeLocationOverlayCreator(
            overlay: Overlay,
            id: String,
        ): AddableOverlay<out Overlay> = object : AddableOverlay<LocationOverlay> {
            override val info: NOverlayInfo = NOverlayInfo(NOverlayType.LOCATION_OVERLAY, id)
            override fun toMap(): Map<String, Any?> = mapOf("info" to info.toMap())
            override fun createMapOverlay(): LocationOverlay = overlay as LocationOverlay
        }
    }
}