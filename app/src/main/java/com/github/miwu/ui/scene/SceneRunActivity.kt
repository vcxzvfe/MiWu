package com.github.miwu.ui.scene

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.github.miwu.logic.repository.AppRepository
import com.github.miwu.logic.setting.AppSetting
import com.github.miwu.utils.Logger
import fr.haan.resultat.Resultat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SceneRunActivity : Activity() {
    private val logger = Logger()
    private val appRepository: AppRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sceneId = intent.getStringExtra("sceneId")
        val sceneName = intent.getStringExtra("sceneName") ?: "场景"
        if (sceneId == null) {
            finish()
            return
        }
        val homeId = AppSetting.homeId.value
        val ownerUid = AppSetting.ownerId.value
        if (homeId == 0L || ownerUid == 0L) {
            Toast.makeText(this, "请先登录并选择家庭", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val scenesState = appRepository.scenes.value
        val scenes = if (scenesState is Resultat.Success) scenesState.value else emptyList()
        val scene = scenes.find { it.sceneId == sceneId }
        if (scene == null) {
            Toast.makeText(this, "场景未找到", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(this, "执行: $sceneName", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                appRepository.runScene(homeId, ownerUid, scene)
            }.onSuccess {
                logger.info("Scene {} executed successfully", sceneName)
            }.onFailure {
                logger.error("Scene {} execution failed: {}", sceneName, it.message)
            }
            runOnUiThread { finish() }
        }
    }
}
