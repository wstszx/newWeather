package com.example.hasee.newweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hasee.newweather.gson.Forecast;
import com.example.hasee.newweather.gson.Weather;
import com.example.hasee.newweather.util.HttpUtil;
import com.example.hasee.newweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hasee on 2017/7/10.
 */

public class WeatherActivity extends AppCompatActivity {

	private ScrollView weather_layout;
	private TextView title_city;
	private TextView title_update_time;
	private TextView degree_text;
	private TextView weather_info_text;
	private LinearLayout forecast_layout;
	private TextView aqi_text;
	private TextView pm25_text;
	private TextView comfort_text;
	private TextView car_wash_text;
	private TextView sport_text;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_weather);
//		初始化各控件
		weather_layout = (ScrollView) findViewById(R.id.weather_layout);
		title_city = (TextView) findViewById(R.id.title_city);
		title_update_time = (TextView) findViewById(R.id.title_update_time);
		degree_text = (TextView) findViewById(R.id.degree_text);
		weather_info_text = (TextView) findViewById(R.id.weather_info_text);
		forecast_layout = (LinearLayout) findViewById(R.id.forecast_layout);
		aqi_text = (TextView) findViewById(R.id.aqi_text);
		pm25_text = (TextView) findViewById(R.id.pm25_text);
		comfort_text = (TextView) findViewById(R.id.comfort_text);
		car_wash_text = (TextView) findViewById(R.id.car_wash_text);
		sport_text = (TextView) findViewById(R.id.sport_text);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String weatherString = prefs.getString("weather", null);
		if (weatherString != null) {
//			有缓存时直接解析天气数据
			Weather weather = Utility.handleWeatherResponse(weatherString);
			showWeatherInfo(weather);
		} else {
//			无缓存时去服务器查询天气
			String weather_id = getIntent().getStringExtra("weather_id");
			weather_layout.setVisibility(View.INVISIBLE);
			requestWeather(weather_id);
		}
	}

	//根据天气id请求城市天气信息
	private void requestWeather(String weather_id) {
		String weatherUrl = "https://free-api.heweather.com/v5/weather?city=" + weather_id + "&key=a795978046234cc7aa101abb628f280f";
		Log.d("WeatherActivity", "weatherUrl="+weatherUrl);
		HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
					}
				});
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response!=null){
					final String responseText = response.body().string();
					final Weather weather = Utility.handleWeatherResponse(responseText);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (weather != null && "ok".equals(weather.status)) {
								SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
								editor.putString("weather", responseText);
								editor.apply();
								showWeatherInfo(weather);
							} else {
								Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
							}
						}
					});
				}
			}
		});
	}

	//处理并展示Weather实体类中的数据
	private void showWeatherInfo(Weather weather) {
		String cityName = weather.basic.cityName;
		String updateTime = weather.basic.update.updateTime.split(" ")[1];
		String degree = weather.now.temperature + "℃";
		String weatherInfo = weather.now.more.info;
		title_city.setText(cityName);
		title_update_time.setText(updateTime);
		degree_text.setText(degree);
		weather_info_text.setText(weatherInfo);
		forecast_layout.removeAllViews();
		for (Forecast forecast : weather.forecastList) {
			View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecast_layout, false);
			TextView date_text = (TextView) view.findViewById(R.id.date_text);
			TextView info_text = (TextView) view.findViewById(R.id.info_text);
			TextView max_text = (TextView) view.findViewById(R.id.max_text);
			TextView min_text = (TextView) view.findViewById(R.id.min_text);
			date_text.setText(forecast.date);
			info_text.setText(forecast.more.info);
			max_text.setText(forecast.temperature.max);
			min_text.setText(forecast.temperature.min);
			forecast_layout.addView(view);

		}
		if (weather.aqi != null) {
			aqi_text.setText(weather.aqi.city.aqi);
			pm25_text.setText(weather.aqi.city.pm25);
		}
		String comfort = "舒适度:" + weather.suggestion.comfort.info;
		String carWash = "洗车指数:" + weather.suggestion.carWash.info;
		String sport = "运动建议:" + weather.suggestion.sport.info;
		comfort_text.setText(comfort);
		car_wash_text.setText(carWash);
		sport_text.setText(sport);
		weather_layout.setVisibility(View.VISIBLE);
	}
}
