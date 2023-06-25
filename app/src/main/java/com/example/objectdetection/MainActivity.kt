package com.example.objectdetection


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.navigation.fragment.NavHostFragment
import com.example.objectdetection.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private lateinit var navHostFragment: NavHostFragment
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_nav) as NavHostFragment

    }
}