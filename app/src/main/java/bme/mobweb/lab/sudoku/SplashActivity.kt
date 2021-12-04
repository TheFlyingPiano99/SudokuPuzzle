package bme.mobweb.lab.sudoku

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import bme.mobweb.lab.sudoku.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val animation = SpringAnimation(binding.splashSudokuLogo, DynamicAnimation.ROTATION, 0f)
        animation.setStartValue(90f)
        animation.spring.dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        animation.spring.stiffness = SpringForce.STIFFNESS_MEDIUM
        animation.addEndListener {
                animation: DynamicAnimation<*>?,
                canceled: Boolean,
                value: Float,
                velocity: Float ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
        }
        animation.start()
    }
}