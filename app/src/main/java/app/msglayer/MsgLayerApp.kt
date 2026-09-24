package app.msglayer

import android.app.Application

class MsgLayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
