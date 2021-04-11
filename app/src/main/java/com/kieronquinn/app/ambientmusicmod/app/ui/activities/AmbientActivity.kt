package com.kieronquinn.app.ambientmusicmod.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.ambientmusicmod.R
import dev.chrisbanes.insetter.Insetter

class AmbientActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Insetter.setEdgeToEdgeSystemUiFlags(window.decorView, true)
        setContentView(R.layout.activity_main)
    }

}