package com.jangeby.system.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jangeby.system.MainActivity
import com.jangeby.system.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        //val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvAppDesc = findViewById<TextView>(R.id.tvAppDesc)

        // Initial state
        ivLogo.scaleX = 0.5f
        ivLogo.scaleY = 0.5f

        // Animate Logo
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Animate Text
//        tvAppName.animate()
//            .alpha(1f)
//            .translationY(-20f)
//            .setDuration(1000)
//            .setStartDelay(400)
//            .start()

        tvAppDesc.animate()
            .alpha(1f)
            .translationY(-20f)
            .setDuration(1000)
            .setStartDelay(600)
            .start()

        // Wait then move to main
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }
}
