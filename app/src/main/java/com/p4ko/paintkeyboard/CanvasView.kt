package com.p4ko.paintkeyboard

import android.app.ActionBar
import android.content.ContentProvider
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.FileProvider
import androidx.core.graphics.get
import animatedgifencoder.AnimatedGIFEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class CanvasView:View{
    private val drawWidths = listOf<Float>(10.0f,30.0f,60.0f)
    private val b_button_ids = listOf<Int>(R.drawable.ic_b_light,R.drawable.ic_b_normal,R.drawable.ic_b_bold)

    private var paint = Paint()
    private var drawingPath = Path()
    private var drawWidthIndex = 2
    private var bitmapArray = ArrayList<Bitmap>()

    private lateinit var bitmap:Bitmap
    private lateinit var bufCanvas:Canvas

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context,attrs:AttributeSet):super(context,attrs){
        init()
    }

    private fun init(){
        paint.color = getColor(context,R.color.p_wet_asphalt)
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = drawWidths[drawWidthIndex]
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bitmap,0f,0f,null)
        canvas.drawPath(drawingPath,paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        bufCanvas = Canvas(bitmap)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        val x = event.x
        val y = event.y

        when (event.action){
            MotionEvent.ACTION_DOWN -> {
                drawingPath.reset()
                drawingPath.moveTo(x,y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                drawingPath.lineTo(x,y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                bitmapArray.add(bitmap.copy(Bitmap.Config.ARGB_8888,true))

                drawingPath.lineTo(x,y)
                bufCanvas.drawPath(drawingPath,paint)
                drawingPath.reset()
                invalidate()
            }
        }
        return true
    }

    fun ResetCanvas(){
        bitmap = Bitmap.createBitmap(this.width,this.height,Bitmap.Config.ARGB_8888)
        bufCanvas = Canvas(bitmap)
        invalidate()
        bitmapArray.clear()
    }

    fun RewindCanvas(){
        if(bitmapArray.size > 0){
            bitmap = bitmapArray.last()
            bufCanvas = Canvas(bitmap)
            invalidate()
            bitmapArray.removeAt(bitmapArray.size-1)
        }
    }

    fun ChangeWidth():Drawable?{
        drawWidthIndex = (drawWidthIndex+1)%drawWidths.size
        paint.strokeWidth = drawWidths[drawWidthIndex]
        return getDrawable(context,b_button_ids[drawWidthIndex])
    }

    fun ChangeColor(color:Int){
        paint.color = color
        return
    }

    fun GetImageUri():Uri{
        val fileName = "paint%d.png".format(System.currentTimeMillis())
        try {
            val fileOutputStream:FileOutputStream = context.openFileOutput(fileName,Context.MODE_PRIVATE)

            //saveGif(fileOutputStream)
            savePng(fileOutputStream)

            val file = context.getFileStreamPath(fileName)
            return FileProvider.getUriForFile(context,"com.p4ko.paintkeyboard.content",file)
        }
        catch(e:Exception){
            e.printStackTrace()
        }

        return Uri.parse("")
    }

    private fun saveGif(fileOutputStream: FileOutputStream):Boolean{
        val gifEncoder = AnimatedGIFEncoder()
        gifEncoder.start(fileOutputStream)
        var pixelArray = IntArray(bitmap.width*bitmap.height){0}
        bitmap.getPixels(pixelArray,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        gifEncoder.addFrame(pixelArray,bitmap.width,bitmap.height)
        gifEncoder.finish()
        return true
    }

    private fun savePng(fileOutputStream: FileOutputStream):Boolean{
        val byteArrOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrOutputStream)
        fileOutputStream.write(byteArrOutputStream.toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()
        return true
    }
}