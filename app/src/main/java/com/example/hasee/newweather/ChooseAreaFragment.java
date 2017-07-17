package com.example.hasee.newweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hasee.newweather.db.City;
import com.example.hasee.newweather.db.County;
import com.example.hasee.newweather.db.Province;
import com.example.hasee.newweather.util.HttpUtil;
import com.example.hasee.newweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hasee on 2017/7/8.
 */

public class ChooseAreaFragment extends Fragment {

	private static final int LEVEL_PROVINCE = 0;
	private static final int LEVEL_CITY = 1;
	private static final int LEVEL_COUNTY = 2;
	private TextView title_text;
	private Button back_button;
	private ListView list_view;
	private List<String> dataList = new ArrayList<>();
	private ArrayAdapter<String> adapter;

	private int currentLevel;
	private List<Province> provinceList;
	private Province selectedProvince;
	private List<City> cityList;
	private ProgressDialog progressDialog;
	private City selectedCity;
	private List<County> countyList;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.choose_area, container, false);
		title_text = (TextView) view.findViewById(R.id.title_text);
		back_button = (Button) view.findViewById(R.id.back_button);
		list_view = (ListView) view.findViewById(R.id.list_view);
		adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
		list_view.setAdapter(adapter);
		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(position);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(position);
					queryCounties();
				}else if (currentLevel==LEVEL_COUNTY){
					String weatherId = countyList.get(position).getWeatherId();
					Intent intent=new Intent(getActivity(),WeatherActivity.class);
					intent.putExtra("weather_id",weatherId);
					startActivity(intent);
					getActivity().finish();
				}

			}
		});
		back_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentLevel == LEVEL_COUNTY) {
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					queryProvinces();
				}
			}
		});
		queryProvinces();
	}

	//查询全国所有的省，优先从数据库拆线呢，如果查询到再去服务器查询
	private void queryProvinces() {
		title_text.setText("中国");
		back_button.setVisibility(View.GONE);
		provinceList = DataSupport.findAll(Province.class);
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			list_view.setSelection(0);
			currentLevel = LEVEL_PROVINCE;
		} else {
			String address = "http://guolin.tech/api/china";
			queryFromServer(address, "province");
		}
	}

	//查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
	private void queryCounties() {
		title_text.setText(selectedCity.getCityName());
		back_button.setVisibility(View.VISIBLE);
		countyList = DataSupport.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			list_view.setSelection(0);
			currentLevel = LEVEL_COUNTY;
		} else {
			int provinceCode = selectedProvince.getProvinceCode();
			int cityCode = selectedCity.getCityCode();
			String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
			queryFromServer(address, "county");
		}
	}

	//查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器查询
	private void queryCities() {
		title_text.setText(selectedProvince.getProvinceName());
		back_button.setVisibility(View.VISIBLE);
		cityList = DataSupport.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			list_view.setSelection(0);
			currentLevel = LEVEL_CITY;
		} else {
			int provinceCode = selectedProvince.getProvinceCode();
			String address = "http://guolin.tech/api/china/" + provinceCode;
			queryFromServer(address, "city");
		}
	}

	//根据传入的地址和类型从服务器查询省市县数据
	private void queryFromServer(String address, final String type) {
		showProgressDialog();
		HttpUtil.sendOkHttpRequest(address, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(getActivity(), "加载失败", Toast.LENGTH_SHORT);
					}
				});
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				String responseText = response.body().string();
				boolean result = false;
				if ("province".equals(type)) {
					result = Utility.handleProvinceResponse(responseText);
				} else if ("city".equals(type)) {
					result = Utility.handleCityResponse(responseText, selectedProvince.getId());
				} else if ("county".equals(type)) {
					result = Utility.handleCountyResponse(responseText, selectedCity.getId());
				}
				if (result) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
		});
	}

	//关闭加载的进度对话框
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(getActivity());
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
}
