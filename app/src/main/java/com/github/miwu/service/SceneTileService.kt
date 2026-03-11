@file:Suppress("FunctionName")

package com.github.miwu.service

import android.content.Context
import androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BUTTON
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.github.miwu.R
import com.github.miwu.appModule
import com.github.miwu.logic.repository.AppRepository
import com.github.miwu.utils.Logger
import fr.haan.resultat.Resultat
import kndroidx.KndroidConfig
import kndroidx.KndroidX
import kndroidx.wear.tile.Clickable
import kndroidx.wear.tile.Modifier
import kndroidx.wear.tile.ShapeBackground
import kndroidx.wear.tile.alignment.HorizontalAlignment
import kndroidx.wear.tile.alignment.VerticalAlignment
import kndroidx.wear.tile.background
import kndroidx.wear.tile.clickable
import kndroidx.wear.tile.color
import kndroidx.wear.tile.dp
import kndroidx.wear.tile.fillMaxSize
import kndroidx.wear.tile.fillMaxWidth
import kndroidx.wear.tile.height
import kndroidx.wear.tile.layout
import kndroidx.wear.tile.layout.Box
import kndroidx.wear.tile.layout.Column
import kndroidx.wear.tile.padding
import kndroidx.wear.tile.service.LayoutTileService
import kndroidx.wear.tile.string
import kndroidx.wear.tile.weight
import kndroidx.wear.tile.widget.Image
import kndroidx.wear.tile.widget.Spacer
import kndroidx.wear.tile.widget.Text
import kndroidx.wear.tile.width
import kndroidx.wear.tile.wrapContentHeight
import kndroidx.wear.tile.wrapContentSize
import miwu.miot.model.miot.MiotScene
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SceneTileService : LayoutTileService() {
    private val logger = Logger()
    val appRepository: AppRepository by inject()

    override val version get() = (sceneListHash + resVersion).toString()
    private val resVersion = 1
    private var sceneListHash = 0

    init {
        onResourcesRequest()
    }

    override fun onClick(id: String) = invokeById(id)

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
        super.onTileAddEvent(requestParams)
    }

    override fun onResourcesRequest() {
        ResImage("scene_icon", R.drawable.ic_miwu_scene_tile)
    }

    override fun onLayout() = layout {
        Box(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = HorizontalAlignment.Center,
            verticalAlignment = VerticalAlignment.Center
        ) {
            val scenes = getSceneList()
            sceneListHash = scenes.hashCode()
            if (scenes.isEmpty() || appRepository.miotUser == null) {
                Text(text = "暂无场景")
            } else {
                SceneList(scenes.take(4))
            }
        }
    }

    private fun getSceneList(): List<MiotScene> {
        val scenesState = appRepository.scenes.value
        return if (scenesState is Resultat.Success) {
            scenesState.value
        } else {
            emptyList()
        }
    }

    fun Any.SceneList(list: List<MiotScene>) =
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp, vertical = 5.dp)
        ) {
            for (scene in list) {
                SceneItem(scene)
                Spacer(modifier = Modifier.height(4.dp).width(0.dp))
            }
        }

    fun Any.SceneItem(scene: MiotScene) =
        Box(
            modifier = Modifier
                .wrapContentSize()
                .fillMaxWidth()
                .background(ShapeBackground(0xFF202020.color, 12.dp))
                .clickable(
                    Clickable(
                        scene.sceneId,
                        packageName = packageName,
                        className = "com.github.miwu.ui.scene.SceneRunActivity"
                    ) {
                        string("sceneId", scene.sceneId)
                        string("sceneName", scene.name)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .weight(weight = 1f)
            ) {
                Image(
                    resId = "scene_icon",
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                )
                Spacer(modifier = Modifier.height(3.dp).width(0.dp))
                Text(
                    text = scene.name,
                    typography = TYPOGRAPHY_BUTTON
                ) {
                    setColor(0xFFFFFFFF.color)
                }
            }
        }

    companion object {
        fun refresh() {
            getUpdater(KndroidX.context).requestUpdate(SceneTileService::class.java)
        }
    }

    @Preview(device = WearDevices.LARGE_ROUND)
    fun tilePreview(context: Context) = TilePreviewData { request ->
        KndroidConfig.context = context
        startKoin {
            androidLogger()
            androidContext(context)
            modules(appModule)
        }
        onLayout()
            .let(TilePreviewHelper::singleTimelineEntryTileBuilder)
            .build()
    }
}
