package com.p4ko.paintkeyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class ImagePickActivity:Activity() {

    private val REQUEST_PICK_MEDIA = 10001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
    startActivityForResult(intent,REQUEST_PICK_MEDIA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_MEDIA) {
            Log.d("d",data.toString())
            finish()
        }
    }
}