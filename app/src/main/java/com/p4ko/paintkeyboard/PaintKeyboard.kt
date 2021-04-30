package com.p4ko.paintkeyboard

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.jar.Manifest

const val MSG_ACTIVITY_TO_SERVICE = 1
const val IMAGE_FORMAT = "image/png"

class PaintKeyboard : InputMethodService(),View.OnClickListener,View.OnTouchListener {

    private lateinit var canvasView:CanvasView
    private lateinit var palletMenu:ConstraintLayout
    private lateinit var topOverlay:ConstraintLayout
    private var isPNGSupported = false
    private val buttonColorMap:Map<Int, Int> = mapOf(
            R.id.pallet_button1 to R.color.p_turquoise,
            R.id.pallet_button2 to R.color.p_emerald,
            R.id.pallet_button3 to R.color.p_peter_river,
            R.id.pallet_button4 to R.color.p_amethyst,
            R.id.pallet_button5 to R.color.p_wet_asphalt,
            R.id.pallet_button6 to R.color.p_sun_flower,
            R.id.pallet_button7 to R.color.p_carrot,
            R.id.pallet_button8 to R.color.p_alizarin,
            R.id.pallet_button9 to R.color.p_clouds
    )
    private var inputPackageName = ""

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard, null).also { view ->
            view.findViewById<ImageButton>(R.id.key_back).also { it.setOnClickListener(this) }
            view.findViewById<ImageButton>(R.id.key_delete).also { it.setOnClickListener(this) }
            view.findViewById<ImageButton>(R.id.key_bold).also { it.setOnClickListener(this) }
            view.findViewById<ImageButton>(R.id.key_enter).also { it.setOnClickListener(this) }
            view.findViewById<ImageButton>(R.id.key_color).also { it.setOnClickListener(this) }
            view.findViewById<ImageButton>(R.id.key_image).also { it.setOnClickListener(this) }

            palletMenu = view.findViewById<ConstraintLayout>(R.id.pallet_menu).also{it.setOnTouchListener(this)}
            topOverlay = view.findViewById<ConstraintLayout>(R.id.top_overlay).also{it.setOnTouchListener(this)}
            for((button, color) in buttonColorMap){
                view.findViewById<ImageButton>(button).also {
                    it.background.mutate().setTint(getColor(color))
                    it.setOnClickListener {
                        canvasView.ChangeColor(getColor(color))
                        palletMenu.isVisible = false
                    }
                }
            }
            canvasView = view.findViewById<CanvasView>(R.id.canvas_view)
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isPNGSupported = EditorInfoCompat.getContentMimeTypes(info).any{
            ClipDescription.compareMimeTypes(it, IMAGE_FORMAT) || ClipDescription.compareMimeTypes(it, "image/*")
        }
        inputPackageName = info.packageName
        topOverlay.isVisible = !isPostable()
    }

     private fun sendImage(uri: Uri):Boolean{
         Log.d("d",uri.toString())
         //IMEのAPIで画像を挿入
        if(isPNGSupported){
            val inputContentInfo = InputContentInfoCompat(
                    uri,
                    ClipDescription("desc", arrayOf(IMAGE_FORMAT)),
                    null
            )
            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1){
                flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            }
            InputConnectionCompat.commitContent(
                    currentInputConnection,
                    currentInputEditorInfo,
                    inputContentInfo,
                    flags,
                    null
            )
            return true
        //ShareAPIで画像を挿入
        }else{
            val imageIntent = Intent(Intent.ACTION_SEND).apply{
                setDataAndType(uri,IMAGE_FORMAT)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM,uri)
            }
            val shareIntent = Intent.createChooser(
                    imageIntent,
                    getString(R.string.select_share)
            )
            val resolvedActivityList = packageManager.queryIntentActivities(imageIntent, PackageManager.MATCH_ALL)
            val ngComponentList = resolvedActivityList.filter {
                it.activityInfo.packageName != inputPackageName
            }.map {
                ComponentName(
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
            }.toTypedArray()

            if(resolvedActivityList.size != ngComponentList.size){
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                shareIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, ngComponentList)
                startActivity(shareIntent)
                return true
            }else{
                Toast.makeText(this, R.string.cant_send, Toast.LENGTH_SHORT).show()
            }

        }
        return false
    }

    override fun onClick(v: View?) {
        if(v!=null){
            when(v.id){
                R.id.key_back -> {
                    canvasView.RewindCanvas()
                }
                R.id.key_delete -> {
                    canvasView.ResetCanvas()
                }
                R.id.key_bold -> {
                    v.findViewById<ImageButton>(R.id.key_bold).setImageDrawable(canvasView.ChangeWidth())
                }
                R.id.key_enter -> {
                    if (sendImage(canvasView.GetImageUri())) {
                        canvasView.ResetCanvas()
                    } else {

                    }
                }
                R.id.key_color -> {
                    palletMenu.isVisible = !palletMenu.isVisible
                }
                R.id.key_image -> {
                    val intent = Intent(this, ImagePickActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val a = startActivity(intent)
                }
            }
        }
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return true
    }

    private fun isPostable():Boolean{
        if(isPNGSupported){
            return true
        }
        val resolvedActivityList = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_SEND).apply {
                    setType(IMAGE_FORMAT)
                }
                ,PackageManager.MATCH_ALL).filter {
            it.activityInfo.packageName == inputPackageName
        }
        return resolvedActivityList.size > 0
    }

}