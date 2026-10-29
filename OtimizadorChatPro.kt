package com.aliucord.plugins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import java.io.ByteArrayOutputStream

/**
 * Otimizador Chat Pro — base segura para Aliucord.
 *
 * Importante: esta versão ainda NÃO intercepta o envio de mensagens nem o
 * carregamento de imagens do Discord. Isso exige hooks específicos da versão
 * do Discord/Aliucord e precisa ser integrado/testado contra o template real.
 * A função de compressão abaixo só funciona quando outro código a chama; ela
 * não altera automaticamente as imagens enviadas no chat.
 */
@AliucordPlugin
class OtimizadorChatPro : Plugin() {

    override fun start(context: Context) {
        Toast.makeText(
            context,
            "Otimizador Chat Pro carregado",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun stop(context: Context) {
        // Remove quaisquer hooks que forem adicionados em futuras versões.
        patcher.unpatchAll()
    }

    /**
     * Redimensiona uma imagem para caber no limite de largura/altura e a
     * comprime em JPEG. Retorna null se os bytes não forem uma imagem válida.
     * Não é chamada automaticamente pela versão inicial do plugin.
     */
    private fun compressImage(
        input: ByteArray,
        maxDimension: Int = 1600,
        jpegQuality: Int = 85
    ): ByteArray? {
        if (input.isEmpty() || maxDimension <= 0) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxDimension * 2 ||
            bounds.outHeight / sampleSize > maxDimension * 2
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(input, 0, input.size, options) ?: return null

        val scale = minOf(
            1.0,
            maxDimension.toDouble() / decoded.width,
            maxDimension.toDouble() / decoded.height
        )
        val resized = if (scale < 1.0) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded

        return try {
            ByteArrayOutputStream().use { output ->
                resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(40, 95), output)
                output.toByteArray()
            }
        } finally {
            resized.recycle()
        }
    }
}
