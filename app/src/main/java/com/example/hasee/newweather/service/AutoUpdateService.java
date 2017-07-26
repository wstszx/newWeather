package com.example.hasee.newweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.example.hasee.newweather.gson.Weather;
import com.example.hasee.newweather.util.HttpUtil;
import com.example.hasee.newweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
	public AutoUpdateService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
//		throw new UnsupportedOperationException("Not yet implemented");
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		updateWeather();
		updateBingPic();
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//		8小时的毫秒数
		int anHour=8*60*60*1000;
		long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
		Intent i = new Intent(this, AutoUpdateService.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		alarmManager.cancel(pi);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
		return super.onStartCommand(intent, flags, startId);
	}
//更新必应每日一图
	private void updateBingPic() {
		String requestBingPic = "http://guolin.tech/api/bing_pic";
		HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				String bingPic = response.body().string();
				SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
				edit.putString("bing_pic", bingPic);
				edit.apply();
			}
		});
	}

	/*更新天气信息*/
	private void updateWeather() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String weatherString = preferences.getString("weather", null);
		if (weatherString != null) {
//			有缓存时解析直接解析天气数据
			Weather weather = Utility.handleWeatherResponse(weatherString);
			String weatherId = weather.basic.weatherId;
			String weatherUrl = "https://free-api.heweather.com/v5/weather?city=" + weatherId + "&key=a795978046234cc7aa101abb628f280f";
			HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {

				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					String responText = response.body().string();
					Weather weather = Utility.handleWeatherResponse(responText);
					if (weather != null && "ok".equals(weather.status)) {
						SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
						edit.putString("weather", responText);
						edit.apply();
					}
				}
			});
		}
	}
}
