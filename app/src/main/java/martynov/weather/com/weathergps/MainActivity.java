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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Double lat;
    private Double lng;
    private String provider;

    private LocationManager locationManager;
    private String TAG = "WEATHER";
    private TextView tvTemp;
    private ImageView tvImage;
    private WeatherAPI.ApiInterface api;
    private Location location;

    private TextView city;
    private TextView today;
    private TextView time;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemp = findViewById(R.id.tvTemp);
        tvImage = findViewById(R.id.ivImage);
        city = findViewById(R.id.tv_cityName);
        today = findViewById(R.id.tv_todayDate);
        time = findViewById(R.id.tv_todayTime);
        loading = findViewById(R.id.pb_loading);

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

        Calendar c = Calendar.getInstance();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        String todayTime = timeFormat.format(c.getTime());
        String todayDate = dateFormat.format(c.getTime());

        today.setText(todayDate);
        time.setText(todayTime);

            Log.d(TAG, "OK");

            // get weather for today
            Call<WeatherDay> callToday = api.getToday(lat, lng, units, key);
            callToday.enqueue(new Callback<WeatherDay>() {
                @Override
                public void onResponse(Call<WeatherDay> call, Response<WeatherDay> response) {
                    Log.e(TAG, "onResponse");
                    WeatherDay data = response.body();
                    //Log.d(TAG,response.toString());
                    if (response.isSuccessful()) {

                        city.setText(cityName);
                        tvTemp.setText(data.getTempWithDegree());
                        Glide.with(MainActivity.this).load(data.getIconUrl()).into(tvImage);

                    }
                }

                @Override
                public void onFailure(Call<WeatherDay> call, Throwable t) {
                    Log.e(TAG, "onFailure");
                    Log.e(TAG, t.toString());
                }
            });

        Call<WeatherForecast> callForecast = api.getForecast(lat, lng, units, key);
        callForecast.enqueue(new Callback<WeatherForecast>() {
            @Override
            public void onResponse(Call<WeatherForecast> call, Response<WeatherForecast> response) {
                Log.e(TAG, "onResponse");
                WeatherForecast data = response.body();
                //Log.d(TAG,response.toString());

                if (response.isSuccessful()) {

                    for (WeatherDay day : data.getItems()) {
                        if (day.getDate().get(Calendar.HOUR_OF_DAY) == 15) {
                            String date = String.format("%d.%d.%d %d:%d",
                                    day.getDate().get(Calendar.DAY_OF_MONTH),
                                    day.getDate().get(Calendar.WEEK_OF_MONTH),
                                    day.getDate().get(Calendar.YEAR),
                                    day.getDate().get(Calendar.HOUR_OF_DAY),
                                    day.getDate().get(Calendar.MINUTE)
                            );
                            Log.d(TAG, date);
                            Log.d(TAG, day.getTempInteger());
                            Log.d(TAG, "---");

                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherForecast> call, Throwable t) {
                Log.e(TAG, "onFailure");
                Log.e(TAG, t.toString());
            }
        });

            loading.setVisibility(View.INVISIBLE);

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
