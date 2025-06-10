package com.example.usbdisplay

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageView = ImageView(this)
        setContentView(imageView)
        hideSystemUI()
        Thread { receiveMJPEG("192.168.42.1", 9999) }.start()
    }

    private fun receiveMJPEG(ip: String, port: Int) {
        try {
            val socket = Socket(ip, port)
            val input = BufferedInputStream(socket.getInputStream())
            val baos = ByteArrayOutputStream()
            while (running) {
                var b: Int
                // Find SOI (0xFFD8)
                while (true) {
                    b = input.read()
                    if (b == 0xFF && input.read() == 0xD8) {
                        baos.reset()
                        baos.write(0xFF)
                        baos.write(0xD8)
                        break
                    }
                }
                // Read until EOI (0xFFD9)
                while (true) {
                    b = input.read()
                    if (b == -1) break
                    baos.write(b)
                    if (b == 0xD9 && baos.size > 100) break
                }
                val jpeg = baos.toByteArray()
                val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                runOnUiThread { imageView.setImageBitmap(bmp) }
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }
}
