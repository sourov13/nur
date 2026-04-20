package com.nur.app.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nur.app.R

fun View.show()      { visibility = View.VISIBLE }
fun View.hide()      { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun ImageView.loadUrl(url: String?, placeholder: Int = R.drawable.ic_placeholder) {
    Glide.with(this).load(url)
        .placeholder(placeholder).error(placeholder)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}

fun ImageView.loadCircle(url: String?) {
    Glide.with(this).load(url)
        .placeholder(R.drawable.ic_avatar_placeholder)
        .circleCrop()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}
