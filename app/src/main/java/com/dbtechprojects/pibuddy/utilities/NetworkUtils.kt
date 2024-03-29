package com.dbtechprojects.pibuddy.utilities

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.net.*
import java.util.*
import kotlin.concurrent.schedule

/*
Networking Utility functions, validating IP Address, testing ports, and executing remote commands over ssh
 */

object NetworkUtils {

    private const val TAG = "NetworkUtils"

    private var session: Session? = null
    

    fun validate(ip: String): Boolean {
        val PATTERN =
            "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN.toRegex());
    }

    fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {

// validate if IP is properly formated

        val validationResult = validate(ip)

        if (!validationResult) {
            Log.d(TAG, "isPortOpen: validation result : $validationResult")
            return false
        } else {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeout)
                socket.close()
                //Log.d(TAG, "isPortOpen: socketattempt")
                return true

            } catch (ce: ConnectException) {
                ce.printStackTrace()
                Log.d(TAG, "Connect Exception:${ce.message}")
                return false
            } catch (ce: SocketTimeoutException) {
                Log.d(TAG, "Timeout Exception:${ce.message}")
                return false
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.d(TAG, " Exception:${ex.message}")
                return false
            }
        }
    }

    suspend fun runCommandInSession(command: String): Resource<String> {

        if (session?.isConnected == false) try {
            session?.connect()
        } catch (ex: java.lang.Exception) {
            Log.d("exc", "exc: $ex")
            return Resource.Error(Constants.CONNECTION_ERROR)
        }
        var channel = session?.openChannel("exec") as ChannelExec?
        channel?.setCommand(command)

        val responseStream = ByteArrayOutputStream()

        channel?.outputStream = responseStream
        try {
            channel?.connect(7500) //set session timeout
        }catch (ex: java.lang.Exception){
            return Resource.Error(Constants.CONNECTION_ERROR)
        }


        while (channel?.isConnected == true) {
            Thread.sleep(100)
            Timer().schedule(7500) {
                channel!!.disconnect() //disconnect channel if command output lasts longer than 15secs
            }
        }


        val responseString = String(responseStream.toByteArray())
        return Resource.Success(responseString)
    }

    suspend fun executeRemoteCommand(
        username: String,
        password: String,
        hostname: String,
        command: String,
        port: Int
    ): String {
        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            session = JSch().getSession(username, hostname, port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.timeout = 15000
            session.connect()

            channel = session.openChannel("exec") as ChannelExec?
            channel!!.setCommand(command)

            val responseStream = ByteArrayOutputStream()

            channel.outputStream = responseStream
            channel.connect(15000) //set session timeout


            while (channel.isConnected) {
                Thread.sleep(100)
                Timer().schedule(15000) {
                    channel.disconnect() //disconnect channel if command output lasts longer than 15secs
                }
            }


            val responseString = String(responseStream.toByteArray())
            return (responseString)
        } catch (ce: JSchException) {

            Log.d("exception", "ex: $ce")

            return "error - Please check Username/Password"

        } finally {
            if (session != null) {
                session.disconnect()
            }
            channel?.disconnect()
        }
    }

    suspend fun createSSHSession(
        username: String,
        password: String,
        hostname: String,
        port: Int
    ) {

        try {
            session = JSch().getSession(username, hostname, port)
            session?.setPassword(password)
            session?.setConfig("StrictHostKeyChecking", "no")
            session?.timeout = 600000

        } catch (ce: JSchException) {

            Log.d("exception", "ex: $ce")

        }
    }

    fun disconnectSession() {
        session?.disconnect()
        session = null
    }
}





