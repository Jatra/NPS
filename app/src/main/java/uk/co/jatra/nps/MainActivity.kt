package uk.co.jatra.nps

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uk.co.jatra.nps.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            binding.npsSlider.value.collect {
                it?.run {

                    if (changing) Log.d("NPS","Tracking NPS to ${it.nps}")
                    else {
                        Log.d("NPS", "Selected NPS: ${it.nps}")
                        binding.nps.text = it.nps.toString()
                    }
                }
            }
        }

        binding.clearButton.setOnClickListener {
            binding.nps.text = "--"
            binding.npsSlider.clear()
        }
    }
}