package com.android.launcherkiosk.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object AppIconLoader {
    fun loadBoundedIcon(context: Context, packageName: String, sizeDp: Int): Drawable {
        val targetPx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val packageManager = context.packageManager
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val iconRes = appInfo.icon
            if (iconRes != 0) {
                val remoteResources = packageManager.getResourcesForApplication(appInfo)
                decodeSampledBitmapDrawable(context.resources, remoteResources, iconRes, targetPx)
                    ?: loadDrawable(remoteResources, iconRes)?.toBoundedDrawable(context.resources, targetPx)
                    ?: packageManager.defaultActivityIcon.toBoundedDrawable(context.resources, targetPx)
            } else {
                packageManager.defaultActivityIcon.toBoundedDrawable(context.resources, targetPx)
            }
        }.getOrElse {
            packageManager.defaultActivityIcon.toBoundedDrawable(context.resources, targetPx)
        }
    }

    private fun decodeSampledBitmapDrawable(
        appResources: Resources,
        remoteResources: Resources,
        iconRes: Int,
        targetPx: Int
    ): Drawable? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(remoteResources, iconRes, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeResource(remoteResources, iconRes, options) ?: return null
        val scaled = if (decoded.width == targetPx && decoded.height == targetPx) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, targetPx, targetPx, true).also {
                if (decoded != it) decoded.recycle()
            }
        }
        return BitmapDrawable(appResources, scaled)
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetPx: Int): Int {
        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / inSampleSize >= targetPx && halfHeight / inSampleSize >= targetPx) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun loadDrawable(resources: Resources, iconRes: Int): Drawable? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources.getDrawable(iconRes, null)
            } else {
                @Suppress("DEPRECATION")
                resources.getDrawable(iconRes)
            }
        }.getOrNull()
    }

    private fun Drawable.toBoundedDrawable(resources: Resources, targetPx: Int): Drawable {
        val bitmap = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val width = intrinsicWidth.takeIf { it > 0 } ?: targetPx
        val height = intrinsicHeight.takeIf { it > 0 } ?: targetPx
        val scale = minOf(targetPx.toFloat() / width, targetPx.toFloat() / height)
        val drawWidth = (width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (height * scale).toInt().coerceAtLeast(1)
        val left = (targetPx - drawWidth) / 2
        val top = (targetPx - drawHeight) / 2
        setBounds(left, top, left + drawWidth, top + drawHeight)
        draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }
}
