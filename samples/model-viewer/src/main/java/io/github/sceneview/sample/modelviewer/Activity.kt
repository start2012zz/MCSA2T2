package io.github.sceneview.sample.modelviewer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import io.github.sceneview.utils.setFullScreen

class Activity : AppCompatActivity(R.layout.viewactivity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

//        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar)?.apply {
//            doOnApplyWindowInsets { systemBarsInsets ->
//                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
//            }
//            title = ""
//        })
        Log.e("Activity", "onCreate")
        supportFragmentManager.commit {
            add(R.id.containerFragment, ViewFragment::class.java, Bundle())
        }
    }
}