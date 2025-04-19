package com.imtiaz.agora

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.imtiaz.agora.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private  lateinit var  binding: ActivityMainBinding
    private val PERMISSION_ID = 12;
    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
    )
    private  val appId = "0b162ce6f1f748f18984f6fdbd8fc8f5"
    private  val channelName = "imtiaz"
    private  val tokenName = "007eJxTYLh01fWX7DOT9VMPzysWSkwx5ow+Ozc5SVpacMPb3g63N28VGAySDM2MklPN0gzTzE0s0gwtLC1M0szSUpJSLNKSLdJMH91nzmgIZGRwT/3PxMgAgSA+G0NmbklmYhUDAwDKviF9"
    private  val uId = 0
    private var isJoined  = false

    private var agoraEngine : RtcEngine? = null
    private var localSurfaceView : SurfaceView? = null
    private var remoteSurfaceView : SurfaceView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(!checkSelfPermission()){
            ActivityCompat.requestPermissions(this,REQUESTED_PERMISSION,PERMISSION_ID)
        }

        setUpVideoSdkEngine()

        binding.joinBtn.setOnClickListener{
            joinCall()
        }

        binding.leaveBtn.setOnClickListener{
            leaveCall()
        }
    }

    private fun leaveCall() {
        if(!isJoined){
            showMessage("Join a channel first")
        }
        else {
            agoraEngine!!.leaveChannel()
            showMessage("you left the channel")
            if(remoteSurfaceView != null) remoteSurfaceView!!.visibility = GONE
            if(localSurfaceView != null) localSurfaceView!!.visibility = GONE
            isJoined = false
        }

    }

    private fun joinCall() {
            if(checkSelfPermission()){
                val option = ChannelMediaOptions()
                option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                setUpLocalVideo()
                localSurfaceView!!.visibility = VISIBLE
                agoraEngine!!.startPreview()
                agoraEngine!!.joinChannel(tokenName,channelName,uId,option)
            }else {
                showMessage("permission not granted")
            }
    }

    private  fun  checkSelfPermission() : Boolean{

        return  !(ContextCompat.checkSelfPermission(
            this,REQUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,REQUESTED_PERMISSION[1]
        ) != PackageManager.PERMISSION_GRANTED)
    }

    private  fun showMessage(message: String){
        runOnUiThread{
            Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpVideoSdkEngine(){
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        }catch (e:Exception){
            showMessage(e.message.toString())
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread{
            RtcEngine.destroy()
            agoraEngine = null

        }.start()
    }
    
    private val mRtcEventHandler : IRtcEngineEventHandler = object : IRtcEngineEventHandler()  {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote User Joined $uid")
            runOnUiThread {
                setUpRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("User Offline $uid")
            runOnUiThread {
                remoteSurfaceView!!.visibility = GONE
            }
        }

    }

    private fun  setUpRemoteVideo(uid: Int){
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,uId
            )
        )
    }

    private fun  setUpLocalVideo(){
        localSurfaceView = SurfaceView(baseContext)
        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,0
            )
        )
    }
}