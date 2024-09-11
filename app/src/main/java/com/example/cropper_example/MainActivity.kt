package com.example.cropper_example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import com.github.arielmazor.Cropper
import com.github.arielmazor.rememberCropperState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            val state =
                rememberCropperState(image = ImageBitmap.imageResource(id = R.drawable.fish))

            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.75f)
                ) {
                    Cropper(state)
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    Button(onClick = {
                        state.crop()
                        Toast.makeText(ctx, "cropped!", Toast.LENGTH_SHORT).show()
                    }
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}