package com.hedvig.app.util.react

interface AsyncStorageNative {
    fun getKey(key: String): String?
    fun deleteKey(key: String)
    fun setKey(key: String, value: String)
    fun close()
}
