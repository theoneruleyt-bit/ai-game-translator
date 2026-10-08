package com.optima.gametranslator

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SpeechSettings(context: Context) {
    data class Credentials(val key: String, val region: String)
    private val prefs=context.getSharedPreferences("cloud_speech",Context.MODE_PRIVATE)
    private val alias="translator_cloud_speech_v1"
    private fun secret(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load(): Credentials? = try {
        val encrypted=prefs.getString("cipher",null)
        val iv=prefs.getString("iv",null)
        if(encrypted==null||iv==null) null else {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,secret(),GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)))
            Credentials(String(cipher.doFinal(Base64.decode(encrypted,Base64.NO_WRAP)),Charsets.UTF_8),prefs.getString("region","").orEmpty())
        }
    } catch (_: Exception) { null }
    fun save(key: String,region: String) {
        require(region.matches(Regex("[a-z][a-z0-9]{1,30}")))
        val value=key.trim().ifBlank { load()?.key.orEmpty() }
        require(value.isNotBlank())
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,secret()) }
        check(prefs.edit().putString("region",region)
            .putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
            .putString("cipher",Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)).commit())
    }
    fun clear() { prefs.edit().clear().apply() }
}
