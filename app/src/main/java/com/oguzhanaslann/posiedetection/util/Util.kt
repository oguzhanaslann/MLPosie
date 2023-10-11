package com.oguzhanaslann.posiedetection.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

fun Context.openActivity(openThis: Class<out AppCompatActivity>) {
    startActivity(Intent(this, openThis))
}

fun ImageView.loadImageWithMinSize(imageUrl: String) {
    val requestOptions = RequestOptions()
            .override(480, 360) // Set the minimum width and height
            .centerCrop() // Crop the image if needed to fit the dimensions

    Glide.with(this.context) // Use the context of the ImageView
            .load(imageUrl)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
            .into(this)
}

fun ImageView.loadImageWithMinSize(@DrawableRes drawableId: Int, onLoad: (Drawable) -> Unit = {}) {
    val requestOptions = RequestOptions()
            .centerCrop() // Crop the image if needed to fit the dimensions

    Glide.with(this.context) // Use the context of the ImageView
            .load(drawableId)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    onLoad(resource)
                    return false
                }
            })
            .into(this)
}