package com.example.hasee.newweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by hasee on 2017/7/10.
 */

public class Basic {
	@SerializedName("city")
	public String cityName;

	@SerializedName("id")
	public String weatherId;

	public Update update;

	public class Update{
		//当地时间
		@SerializedName("loc")
		public String updateTime;

	}
}
