package martynov.weather.com.weathergps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;


import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import martynov.weather.com.weathergps.Common.Common;
import martynov.weather.com.weathergps.Model.WeatherDay;
import martynov.weather.com.weathergps.Model.WeatherForecast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Double lat;
    private Double lng;
    private String provider;

    private LocationManager locationManager;
    private Location location;

    private static final String TAG = "WEATHER";

    private WeatherAPI.ApiInterface api;

    private RecyclerView recyclerView;

    private TextView tvTemp;
    private ImageView tvImage;
    private TextView city;
    private TextView today;
    private ProgressBar loading;
    private TextView wind;
    private TextView cloud;
    private TextView humidity;
    private TextView sunrise;
    private TextView sunset;

    private TableLayout tlTResult;
    private LinearLayout llToday;
    private LinearLayout linearLayoutForecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemp = findViewById(R.id.tvTemp);
        tvImage = findViewById(R.id.ivImage);
        city = findViewById(R.id.tv_cityName);
        today = findViewById(R.id.tv_todayDate);
        loading = findViewById(R.id.pb_loading);
        recyclerView = findViewById(R.id.rv_forecast);
        wind = findViewById(R.id.tvWindValue);
        cloud = findViewById(R.id.tvCloudValue);
        humidity = findViewById(R.id.tvHumidityValue);
        sunrise = findViewById(R.id.tvSunriseValue);
        sunset = findViewById(R.id.tvSunsetValue);
        llToday = findViewById(R.id.llToday);
        linearLayoutForecast = findViewById(R.id.llForecast);
        tlTResult = findViewById(R.id.tlWeatherResult);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        location = locationManager.getLastKnownLocation(provider);

        api = WeatherAPI.getClient().create(WeatherAPI.ApiInterface.class);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
        }
        loading.setVisibility(View.VISIBLE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 10, 10, this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        locationManager.removeUpdates(this);
    }



    public void getWeather() {

            final String cityName = getCity(lat, lng);
            String units = "metric";
            String key = WeatherAPI.KEY;

            Log.d(TAG, "OK");

            // get weather for today
            Call<WeatherDay> callToday = api.getToday(lat, lng, units, key);
            callToday.enqueue(new Callback<WeatherDay>() {
                @Override
                public void onResponse(Call<WeatherDay> call, Response<WeatherDay> response) {

                    WeatherDay data = response.body();

                    if (response.isSuccessful()) {

                        today.setText(new StringBuilder(Common.convertUnixToDate(data.getDt())));
                        city.setText(cityName);
                        humidity.setText(new StringBuilder(String.valueOf(data.getMain().getHumidity())).append("%"));
                        sunrise.setText(new StringBuilder(Common.convertUnixToHour(data.getSys().getSunrise())));
                        sunset.setText(new StringBuilder(Common.convertUnixToHour(data.getSys().getSunset())));
                        wind.setText(new StringBuilder(String.valueOf(data.getWind().getSpeed())).append(" m/s"));
                        cloud.setText(new StringBuilder(data.getClouds().getAll()));

                        tvTemp.setText(new StringBuilder(String.valueOf(data.getMain().temp())).append("°C"));

                        Picasso.with(MainActivity.this)
                                .load(new StringBuilder("http://openweathermap.org/img/w/")
                                        .append(data.getWeather().get(0).getIcon())
                                        .append(".png").toString())
                                .into(tvImage);

                    }else{
                        Log.e(TAG, "onFailure##");
                    }
                }

                @Override
                public void onFailure(Call<WeatherDay> call, Throwable t) {
                    Log.e(TAG, "onFailure1");
                    Log.e(TAG, t.toString());
                }
            });

        Call<WeatherForecast> callForecast = api.getForecast(lat, lng, units, key);
        callForecast.enqueue(new Callback<WeatherForecast>() {
            @Override
            public void onResponse(Call<WeatherForecast> call, Response<WeatherForecast> response) {
                WeatherForecast data = response.body();

                if (response.isSuccessful()) {

                    WeatherForecastAdapter adapter = new WeatherForecastAdapter(MainActivity.this, data);
                    recyclerView.setAdapter(adapter);

                }else{
                    Log.e(TAG, "onFailure2");
                }
            }
            @Override
            public void onFailure(Call<WeatherForecast> call, Throwable t) {
                Log.e(TAG, "onFailure3");
                Log.e(TAG, t.toString());
            }
        });

            loading.setVisibility(View.INVISIBLE);
            tlTResult.setVisibility(View.VISIBLE);
            llToday.setVisibility(View.VISIBLE);
            linearLayoutForecast.setVisibility(View.VISIBLE);

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();

            getWeather();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public String getCity(double lats, double lons) {

        Geocoder geocoder;
        double lat = lats;
        double lon = lons;
        geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
        } catch (IOException e) {

            e.printStackTrace();
        }

        if (addresses != null) {

            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            return city;
        } else {
            return "failed";
        }


    }
}
