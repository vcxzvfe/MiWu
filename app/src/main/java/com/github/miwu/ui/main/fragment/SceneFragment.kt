package com.github.miwu.ui.main.fragment

import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.github.miwu.R
import com.github.miwu.databinding.FragmentMainSceneBinding as Binding
import com.github.miwu.logic.setting.AppSetting
import com.github.miwu.ui.main.MainViewModel
import kndroidx.extension.toast
import kndroidx.fragment.ViewFragmentX
import kotlinx.coroutines.launch
import miwu.miot.model.miot.MiotScenes
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class SceneFragment : ViewFragmentX<Binding>(Binding::inflate) {
    override val viewModel: MainViewModel by viewModel()
    val homeId get() = AppSetting.homeId.value
    val ownerUid get() = AppSetting.ownerId.value

    fun onItemClick(item: Any?) {
        item as MiotScenes.Result.Scene
        lifecycleScope.launch {
            viewModel.appRepository.runScene(homeId, ownerUid, item)
        }
    }

    fun onItemLongClick(item: Any?): Boolean {
        item as MiotScenes.Result.Scene
        val context = context ?: return true
        val actionCount = item.sceneAction.actions.size
        val message = buildString {
            append(item.name)
            append("\n")
            append(getString(R.string.scene_detail_actions, actionCount))
        }
        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(R.string.scene_detail_run) { _, _ ->
                lifecycleScope.launch {
                    viewModel.appRepository.runScene(homeId, ownerUid, item)
                    R.string.scene_detail_running.toast()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        return true
    }
}
