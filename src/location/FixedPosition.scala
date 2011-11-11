package org.aprsdroid.app

import _root_.android.app.AlarmManager
import _root_.android.app.PendingIntent
import _root_.android.content.BroadcastReceiver
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.content.IntentFilter
import _root_.android.location.Location
import _root_.android.os.{Bundle, Handler}
import _root_.android.util.Log

class FixedPosition(service : AprsService, prefs : PrefsWrapper) extends LocationSource {
	val TAG = "APRSdroid.FixedPosition"
	val ALARM_ACTION = "org.aprsdroid.app.FixedPosition.ALARM"

	val intent = new Intent(ALARM_ACTION)
	val pendingIntent = PendingIntent.getBroadcast(service, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT)

	// get called on alarm
	val receiver = new BroadcastReceiver() {
		override def onReceive(ctx : Context, i : Intent) {
			Log.d(TAG, "onReceive: " + i)
			postPosition()
			postRefresh()
		}
	}

	val manager = service.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]

	var alreadyRunning = false

	override def start(singleShot : Boolean) = {
		if (alreadyRunning)
			stop()
		alreadyRunning = true

		service.registerReceiver(receiver, new IntentFilter(ALARM_ACTION))
		postPosition()
		if (!singleShot)
			postRefresh()


		service.getString(R.string.p_source_manual)
	}

	override def stop() {
		manager.cancel(pendingIntent)
		service.unregisterReceiver(receiver)
	}

	def postRefresh() {
		// get update interval
		val upd_int = prefs.getStringInt("interval", 10)
		Log.d(TAG, "postRefresh(): " + upd_int + " min")
		manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + upd_int*60*1000,
			pendingIntent)
	}

	def postPosition() {
		val location = new Location("manual")
		location.setLatitude(prefs.getStringFloat("manual_lat", 0))
		location.setLongitude(prefs.getStringFloat("manual_lon", 0))
		service.postLocation(location)
	}

}
