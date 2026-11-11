package com.example.data.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data object Empty : NetworkResult<Nothing>()
    data object Offline : NetworkResult<Nothing>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}
