package com.andronncollmider.argus

import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ObjectWebsocket(viewModel: MainViewModel) {
    private val wsListener = WebsocketListener(viewModel)
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    fun connect(url: String) {
        socket?.close(1000, "")
        socket = client.newWebSocket(Request.Builder().url(url).build(), wsListener)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
open class WebsocketListener(
    private val viewModel: MainViewModel
) : WebSocketListener() {
    private val TAG = "ObjectSocket"

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        viewModel.updateObjects(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d(TAG, "onOpen:")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Log.d(TAG, "onClosing: $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d(TAG, "onClosed: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure: ${t.message} $response")
        super.onFailure(webSocket, t, response)
    }
}
