package com.roadstar.customerr.app.module.ui.booking_activity;
/**
 * @Developer android
 * @Company android
 **/

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.gson.Gson;
import com.roadstar.customerr.MyApplication;
import com.roadstar.customerr.R;
import com.roadstar.customerr.app.data.models.PlacePredictions;
import com.roadstar.customerr.app.data.models.RecentAddressData;
import com.roadstar.customerr.app.data.preferences.PreferenceUtils;
import com.roadstar.customerr.app.data.preferences.SharedPreferenceManager;
import com.roadstar.customerr.app.network.ApiInterface;
import com.roadstar.customerr.app.network.RetrofitClientInstance;
import com.roadstar.customerr.common.base.BaseActivity;
import com.roadstar.customerr.common.utils.AppUtils;
import com.roadstar.customerr.common.utils.SharedHelper;
import com.roadstar.customerr.common.utils.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class CustomGooglePlacesSearch extends BaseActivity  {

    private ListView mAutoCompleteList;
    private EditText txtDestination, txtaddressSource;
    private String GETPLACESHIT = "places_hit";
    private PlacePredictions predictions = new PlacePredictions();
    private AutoCompleteAdapter mAutoCompleteAdapter;
    private Location mLastLocation;
    private static final int MY_PERMISSIONS_REQUEST_LOC = 30;
    private Handler handler;
    TextView txtPickLocation;
    Utilities utils = new Utilities();
    ImageView backArrow, imgDestClose, imgSourceClose;
    LinearLayout lnrFavorite;
    Activity thisActivity;
    String strSource = "";
    private ApiInterface mApiInterface;
    String strSelected = "";
    private PlacePredictions placePredictions = new PlacePredictions();
    Bundle extras;
    TextView txtHomeLocation, txtWorkLocation;
    private int UPDATE_HOME_WORK = 1;
    LinearLayout lnrHome, lnrWork;
    ArrayList<RecentAddressData> lstRecentList = new ArrayList<RecentAddressData>();
    RelativeLayout rytAddressSource;




    RecyclerView rvRecentResults;

    String formatted_address = "";


    // The entry points to the Places API.
    //private GeoDataClient mGeoDataClient;
    //private PlaceDetectionClient mPlaceDetectionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_soruce_and_destination);
        thisActivity = this;

        // Construct a GeoDataClient.
        //mGeoDataClient =Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        //   mPlaceDetectionClient = com.google.android.libraries.places.compat.Places.getPlaceDetectionClient(this);
        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_map_api));

        // Create a new PlacesClient instance


        txtDestination = (EditText) findViewById(R.id.txtDestination);
        txtaddressSource = (EditText) findViewById(R.id.txtaddressSource);

        backArrow = (ImageView) findViewById(R.id.backArrow);
        imgDestClose = (ImageView) findViewById(R.id.imgDestClose);
        imgSourceClose = (ImageView) findViewById(R.id.imgSourceClose);

        txtPickLocation = (TextView) findViewById(R.id.txtPickLocation);
        txtWorkLocation = (TextView) findViewById(R.id.txtWorkLocation);
        txtHomeLocation = (TextView) findViewById(R.id.txtHomeLocation);

        lnrFavorite = (LinearLayout) findViewById(R.id.lnrFavorite);
        lnrHome = (LinearLayout) findViewById(R.id.lnrHome);
        lnrWork = (LinearLayout) findViewById(R.id.lnrWork);

        rytAddressSource = (RelativeLayout) findViewById(R.id.rytAddressSource);

        rvRecentResults = (RecyclerView) findViewById(R.id.rvRecentResults);
        mAutoCompleteList = (ListView) findViewById(R.id.searchResultLV);

        String cursor = getIntent().getExtras().getString("cursor");
        String s_address = getIntent().getExtras().getString("s_address");
        String d_address = getIntent().getExtras().getString("d_address");
        Log.e("CustomGoogleSearch", "onCreate: source " + s_address);
        Log.e("CustomGoogleSearch", "onCreate: destination" + d_address);
        if(!s_address.isEmpty())
            txtaddressSource.setText(s_address);

        SharedPreferenceManager.getInstance().save(PreferenceUtils.SOURCE_ADDRESS, s_address);


        if (d_address != null && !d_address.equalsIgnoreCase("")) {
            txtDestination.setText(d_address);
        }

        if (cursor.equalsIgnoreCase("source")) {
            strSelected = "source";
            txtaddressSource.requestFocus();
            imgSourceClose.setVisibility(View.VISIBLE);
            imgDestClose.setVisibility(View.GONE);
        } else {
            txtDestination.requestFocus();
            strSelected = "destination";
            imgDestClose.setVisibility(View.VISIBLE);
            imgSourceClose.setVisibility(View.GONE);
        }

        String strStatus = SharedHelper.getKey(thisActivity, "req_status");

        if (strStatus.equalsIgnoreCase("PICKEDUP")) {
            if (SharedHelper.getKey(thisActivity, "track_status").equalsIgnoreCase("YES")) {
                rytAddressSource.setVisibility(View.GONE);
            } else {
                rytAddressSource.setVisibility(View.VISIBLE);
            }
        }

        getFavoriteLocations();

        txtaddressSource.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    strSelected = "source";
                    imgSourceClose.setVisibility(View.VISIBLE);
                } else {
                    imgSourceClose.setVisibility(View.GONE);
                }
            }
        });

        txtDestination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    strSelected = "destination";
                    imgDestClose.setVisibility(View.VISIBLE);
                } else {
                    imgDestClose.setVisibility(View.GONE);
                }
            }
        });

        imgDestClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtDestination.setText("");
                mAutoCompleteList.setVisibility(View.GONE);
                imgDestClose.setVisibility(View.GONE);
                txtDestination.requestFocus();
            }
        });

        imgSourceClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtaddressSource.setText("");
                mAutoCompleteList.setVisibility(View.GONE);
                imgSourceClose.setVisibility(View.GONE);
                txtaddressSource.requestFocus();
            }
        });

        txtPickLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                utils.hideKeypad(thisActivity, thisActivity.getCurrentFocus());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.putExtra("pick_location", "yes");
                        intent.putExtra("type", strSelected);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }, 500);
            }
        });

        lnrHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SharedHelper.getKey(CustomGooglePlacesSearch.this, "home").equalsIgnoreCase("")) {
                    gotoHomeWork("home");
                } else {
                    if (strSelected.equalsIgnoreCase("destination")) {
                        placePredictions.strDestAddress = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home");
                        placePredictions.strDestLatitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home_lat");
                        placePredictions.strDestLongitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home_lng");
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strDestLatitude), Double.parseDouble(placePredictions.strDestLatitude));
                        placePredictions.strDestLatLng = "" + latlng;
                        txtDestination.setText(SharedHelper.getKey(CustomGooglePlacesSearch.this, "home"));
                        txtDestination.setSelection(0);
                    } else {
                        placePredictions.strSourceAddress = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home");
                        placePredictions.strSourceLatitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home_lat");
                        placePredictions.strSourceLongitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "home_lng");
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strSourceLatitude), Double.parseDouble(placePredictions.strSourceLongitude));
                        placePredictions.strSourceLatLng = "" + latlng;
                        txtaddressSource.setText(placePredictions.strSourceAddress);
                        txtaddressSource.setSelection(0);
                        txtDestination.requestFocus();
                        mAutoCompleteAdapter = null;
                    }
                    setAddress();
                }
            }
        });

        lnrWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SharedHelper.getKey(CustomGooglePlacesSearch.this, "work").equalsIgnoreCase("")) {
                    gotoHomeWork("work");
                } else {
                    if (strSelected.equalsIgnoreCase("destination")) {
                        placePredictions.strDestAddress = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work");
                        placePredictions.strDestLatitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work_lat");
                        placePredictions.strDestLongitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work_lng");
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strDestLatitude), Double.parseDouble(placePredictions.strDestLatitude));
                        placePredictions.strDestLatLng = "" + latlng;
                        txtDestination.setText(SharedHelper.getKey(CustomGooglePlacesSearch.this, "work"));
                        txtDestination.setSelection(0);
                    } else {
                        placePredictions.strSourceAddress = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work");
                        placePredictions.strSourceLatitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work_lat");
                        placePredictions.strSourceLongitude = SharedHelper.getKey(CustomGooglePlacesSearch.this, "work_lng");
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strSourceLatitude), Double.parseDouble(placePredictions.strSourceLongitude));
                        placePredictions.strSourceLatLng = "" + latlng;
                        txtaddressSource.setText(placePredictions.strSourceAddress);
                        txtaddressSource.setSelection(0);
                        txtDestination.requestFocus();
                        mAutoCompleteAdapter = null;
                    }
                    setAddress();
                }
            }
        });

        //get permission for Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOC);
            } else {
            }
        }

        //Add a text change listener to implement autocomplete functionality
        txtDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                imgDestClose.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // optimised way is to start searching for laction after user has typed minimum 3 chars
                imgDestClose.setVisibility(View.VISIBLE);
                strSelected = "destination";
                if (txtDestination.getText().length() > 0) {
                    lnrFavorite.setVisibility(View.GONE);
                    txtPickLocation.setVisibility(View.VISIBLE);
                    imgDestClose.setVisibility(View.VISIBLE);
                    txtPickLocation.setText("Pin location");
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            // cancel all the previous requests in the queue to optimise your network calls during autocomplete search
                            MyApplication.getInstance().cancelRequestInQueue(GETPLACESHIT);

                            JSONObject object = new JSONObject();
                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getPlaceAutoCompleteUrl(txtDestination.getText().toString()), object, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.v("PayNowRequestResponse", response.toString());

                                    Gson gson = new Gson();
                                    predictions = gson.fromJson(response.toString(), PlacePredictions.class);
                                    if (mAutoCompleteAdapter == null) {
                                        mAutoCompleteList.setVisibility(View.VISIBLE);
                                        mAutoCompleteAdapter = new AutoCompleteAdapter(CustomGooglePlacesSearch.this, predictions.getPlaces(), CustomGooglePlacesSearch.this);
                                        mAutoCompleteList.setAdapter(mAutoCompleteAdapter);
                                    } else {
                                        mAutoCompleteList.setVisibility(View.VISIBLE);
                                        mAutoCompleteAdapter.clear();
                                        mAutoCompleteAdapter.addAll(predictions.getPlaces());
                                        mAutoCompleteAdapter.notifyDataSetChanged();
                                        mAutoCompleteList.invalidate();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.v("PayNowRequestResponse", error.toString());
                                }
                            });
                            MyApplication.getInstance().addToRequestQueue(jsonObjectRequest);

                        }

                    };

                    // only canceling the network calls will not help, you need to remove all callbacks as well
                    // otherwise the pending callbacks and messages will again invoke the handler and will send the request
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    } else {
                        handler = new Handler();
                    }
                    handler.postDelayed(run, 1000);

                } else {
                    lnrFavorite.setVisibility(View.VISIBLE);
                    mAutoCompleteList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                imgDestClose.setVisibility(View.VISIBLE);
            }

        });

        //Add a text change listener to implement autocomplete functionality
        txtaddressSource.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                imgSourceClose.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // optimised way is to start searching for laction after user has typed minimum 3 chars
                strSelected = "source";
                if (txtaddressSource.getText().length() > 0) {
                    lnrFavorite.setVisibility(View.GONE);
                    txtPickLocation.setVisibility(View.VISIBLE);
                    imgSourceClose.setVisibility(View.VISIBLE);
                    txtPickLocation.setText("Pin Location");
                    if (mAutoCompleteAdapter == null)
                        mAutoCompleteList.setVisibility(View.VISIBLE);
                    Runnable run = new Runnable() {

                        @Override
                        public void run() {
                            // cancel all the previous requests in the queue to optimise your network calls during autocomplete search
                            MyApplication.getInstance().cancelRequestInQueue(GETPLACESHIT);

                            JSONObject object = new JSONObject();
                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getPlaceAutoCompleteUrl(txtaddressSource.getText().toString()),
                                    object, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.v("PayNowRequestResponse", response.toString());
                                    Log.v("PayNowRequestResponse", response.toString());
                                    Gson gson = new Gson();
                                    predictions = gson.fromJson(response.toString(), PlacePredictions.class);
                                    if (mAutoCompleteAdapter == null) {
                                        mAutoCompleteAdapter = new AutoCompleteAdapter(CustomGooglePlacesSearch.this, predictions.getPlaces(), CustomGooglePlacesSearch.this);
                                        mAutoCompleteList.setAdapter(mAutoCompleteAdapter);
                                    } else {
                                        mAutoCompleteList.setVisibility(View.VISIBLE);
                                        mAutoCompleteAdapter.clear();
                                        mAutoCompleteAdapter.addAll(predictions.getPlaces());
                                        mAutoCompleteAdapter.notifyDataSetChanged();
                                        mAutoCompleteList.invalidate();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.v("PayNowRequestResponse", error.toString());
                                }
                            });
                            MyApplication.getInstance().addToRequestQueue(jsonObjectRequest);

                        }

                    };
                    // only canceling the network calls will not help, you need to remove all callbacks as well
                    // otherwise the pending callbacks and messages will again invoke the handler and will send the request
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    } else {
                        handler = new Handler();
                    }
                    handler.postDelayed(run, 1000);

                } else {
                    lnrFavorite.setVisibility(View.VISIBLE);
                    mAutoCompleteList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                imgSourceClose.setVisibility(View.VISIBLE);
            }

        });

        //txtDestination.setText("");
        txtDestination.setSelection(txtDestination.getText().length());

        mAutoCompleteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (txtaddressSource.getText().toString().equalsIgnoreCase("")) {
                    try {
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                        LayoutInflater inflater = (LayoutInflater) thisActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        builder.setMessage("Please choose pickup location")
                                .setTitle(thisActivity.getString(R.string.app_name))
                                .setCancelable(true)
                                .setIcon(R.mipmap.ic_launcher)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        txtaddressSource.requestFocus();
                                        txtDestination.setText("");
                                        imgDestClose.setVisibility(View.GONE);
                                        mAutoCompleteList.setVisibility(View.GONE);
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    setGoogleAddress(position);
                }
            }
        });
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                setAddress();
                finish();
            }
        });

    }

    private void getFavoriteLocations() {
        mApiInterface = RetrofitClientInstance.getLiveTrackingClient().create(ApiInterface.class);

        Call<ResponseBody> call = mApiInterface.getFavoriteLocations("XMLHttpRequest",
                SharedHelper.getKey(CustomGooglePlacesSearch.this, "token_type") + " " + SharedHelper.getKey(CustomGooglePlacesSearch.this, "access_token"));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                Log.e("sUCESS", "SUCESS" + response.body());
                if (response.body() != null) {
                    try {
                        String bodyString = new String(response.body().bytes());
                        Log.e("sUCESS", "bodyString" + bodyString);
                        try {
                            JSONObject jsonObj = new JSONObject(bodyString);
                            JSONArray homeArray = jsonObj.optJSONArray("home");
                            JSONArray workArray = jsonObj.optJSONArray("work");
                            JSONArray othersArray = jsonObj.optJSONArray("others");
                            JSONArray recentArray = jsonObj.optJSONArray("recent");
                            if (homeArray != null && homeArray.length() > 0) {
                                Log.v("Home Address", "" + homeArray);
                                txtHomeLocation.setText(homeArray.optJSONObject(0).optString("address"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "home", homeArray.optJSONObject(0).optString("address"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "home_lat", homeArray.optJSONObject(0).optString("latitude"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "home_lng", homeArray.optJSONObject(0).optString("longitude"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "home_id", homeArray.optJSONObject(0).optString("id"));
                            } else {
                                txtHomeLocation.setText("Add home location");
                            }
                            if (workArray != null && workArray.length() > 0) {
                                Log.v("Work Address", "" + workArray);
                                txtWorkLocation.setText(workArray.optJSONObject(0).optString("address"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "work", workArray.optJSONObject(0).optString("address"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "work_lat", workArray.optJSONObject(0).optString("latitude"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "work_lng", workArray.optJSONObject(0).optString("longitude"));
                                SharedHelper.putKey(CustomGooglePlacesSearch.this, "work_id", workArray.optJSONObject(0).optString("id"));
                            } else {
                                txtWorkLocation.setText("Add work location");
                            }
                            if (othersArray != null && othersArray.length() > 0) {
                                Log.v("Others Address", "" + othersArray);
                            }
                            if (recentArray != null && recentArray.length() > 0) {
                                for (int i = 0; i < recentArray.length(); i++) {
                                    RecentAddressData recentAddressData = new RecentAddressData();
                                    JSONObject jsonObject = recentArray.optJSONObject(i);
                                    recentAddressData.id = jsonObject.optInt("id");
                                    recentAddressData.userId = jsonObject.optInt("user_id");
                                    recentAddressData.address = jsonObject.optString("address");
                                    recentAddressData.type = jsonObject.optString("type");
                                    recentAddressData.latitude = jsonObject.optDouble("latitude");
                                    recentAddressData.longitude = jsonObject.optDouble("longitude");
                                    if (recentAddressData.address != null && !recentAddressData.address.equalsIgnoreCase("")) {
                                        lstRecentList.add(recentAddressData);
                                    }
                                }

                                Log.v("Recent Address", "" + recentArray);
                                rvRecentResults.setVisibility(View.VISIBLE);
                                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                                rvRecentResults.setLayoutManager(mLayoutManager);
                                rvRecentResults.setItemAnimator(new DefaultItemAnimator());
                                RecentPlacesAdapter recentPlacesAdapter = new RecentPlacesAdapter(recentArray, lstRecentList);
                                rvRecentResults.setAdapter(recentPlacesAdapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("onFailure", "onFailure" + call.request().url());
            }
        });
    }

    private void setGoogleAddress(int position) {

        ////////////////////////////////////////////////////////////

//        GeoDataClient geoDataClient = Places.getGeoDataClient(CustomGooglePlacesSearch.this);
//
//        Task<PlaceBufferResponse> placeResults = geoDataClient.getPlaceById();


        // Define a Place ID.
        String placeId = predictions.getPlaces().get(position).getPlaceID();

// Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,Place.Field.ADDRESS );

// Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .build();
        PlacesClient placesClient = Places.createClient(this);


        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place myPlace = response.getPlace();


            if (myPlace.getLatLng() != null) {

                LatLng queriedLocation = myPlace.getLatLng();
                Log.v("Latitude is", "" + queriedLocation.latitude);
                Log.v("Longitude is", "" + queriedLocation.longitude);
                if (strSelected.equalsIgnoreCase("destination")) {
                    placePredictions.strDestAddress = myPlace.getAddress().toString();
                    placePredictions.strDestLatLng = myPlace.getLatLng().toString();
                    placePredictions.strDestLatitude = myPlace.getLatLng().latitude + "";
                    placePredictions.strDestLongitude = myPlace.getLatLng().longitude + "";
                    txtDestination.setText(placePredictions.strDestAddress);
                    txtDestination.setSelection(0);
                    SharedPreferenceManager.getInstance().save(PreferenceUtils.DEST_ADDRESS, placePredictions.strDestAddress);
                } else {
                    placePredictions.strSourceAddress = myPlace.getAddress().toString();
                    placePredictions.strSourceLatLng = myPlace.getLatLng().toString();
                    placePredictions.strSourceLatitude = myPlace.getLatLng().latitude + "";
                    placePredictions.strSourceLongitude = myPlace.getLatLng().longitude + "";
                    txtaddressSource.setText(placePredictions.strSourceAddress);
                    SharedPreferenceManager.getInstance().save(PreferenceUtils.SOURCE_ADDRESS, placePredictions.strSourceAddress);

                    txtaddressSource.setSelection(0);
                    txtDestination.requestFocus();
                    mAutoCompleteAdapter = null;
                }
            }
            mAutoCompleteList.setVisibility(View.GONE);

            if (txtDestination.getText().toString().length() > 0) {
                if (strSelected.equalsIgnoreCase("destination")) {
                    if (!placePredictions.strDestAddress.equalsIgnoreCase(placePredictions.strSourceAddress)) {
                        setAddress();
                    } else {
                        utils.showAlert(thisActivity, "Source and Destination address should not be same!");
                    }
                }
            } else {
                txtDestination.requestFocus();
                txtDestination.setText("");
                imgDestClose.setVisibility(View.GONE);
                mAutoCompleteList.setVisibility(View.GONE);
            }


        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.

            }
        });


    }

    public String getPlaceAutoCompleteUrl(String input) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/autocomplete/json");
        urlString.append("?input=");
        try {
            urlString.append(URLEncoder.encode(input, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&location=");
        //urlString.append(HomeFragment.latitude + "," + HomeFragment.longitude); // append lat long of current location to show nearby results.
        urlString.append(33.684422 + "," + 73.047882); // append lat long of current location to show nearby results.
        urlString.append("&radius=500&language=en");
        urlString.append("&key=" + getResources().getString(R.string.google_map_api));

        Log.d("FINAL URL:::   ", urlString.toString());
        return urlString.toString();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted!
                } else {
                    // permission denied!
                    Toast.makeText(this, "Please grant permission for using this app!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        setAddress();
        super.onBackPressed();
    }

    void setAddress() {
        AppUtils.hideSoftKeyboard(thisActivity);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                if (placePredictions != null) {
                    intent.putExtra("Location Address", placePredictions);
                    intent.putExtra("pick_location", "no");
                    setResult(RESULT_OK, intent);
                } else {
                    setResult(RESULT_CANCELED, intent);
                }
                finish();
            }
        }, 500);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {

        super.onStop();
    }

    private class RecentPlacesAdapter extends RecyclerView.Adapter<RecentPlacesAdapter.MyViewHolder> {
        JSONArray jsonArray;
        ArrayList<RecentAddressData> lstRecentList;

        public RecentPlacesAdapter(JSONArray array, ArrayList<RecentAddressData> lstRecentList) {
            this.jsonArray = array;
            this.lstRecentList = lstRecentList;
        }

        @Override
        public RecentPlacesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.autocomplete_row, parent, false);
            return new RecentPlacesAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecentPlacesAdapter.MyViewHolder holder, int position) {
            String[] name = lstRecentList.get(position).address.split(",");
            if (name.length > 0) {
                holder.name.setText(name[0]);
            } else {
                holder.name.setText(lstRecentList.get(position).address);
            }
            holder.location.setText(lstRecentList.get(position).address);

            holder.imgRecent.setImageResource(R.drawable.recent_search);

            holder.lnrLocation.setTag(position);

            holder.lnrLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = Integer.parseInt(view.getTag().toString());
                    if (strSelected.equalsIgnoreCase("destination")) {
                        placePredictions.strDestAddress = lstRecentList.get(position).address;
                        placePredictions.strDestLatitude = lstRecentList.get(position).latitude + "";
                        placePredictions.strDestLongitude = lstRecentList.get(position).longitude + "";
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strDestLatitude), Double.parseDouble(placePredictions.strDestLatitude));
                        placePredictions.strDestLatLng = "" + latlng;
                        txtDestination.setText(lstRecentList.get(position).address);
                        txtDestination.setSelection(0);
                    } else {
                        placePredictions.strSourceAddress = lstRecentList.get(position).address;
                        placePredictions.strSourceLatitude = lstRecentList.get(position).latitude + "";
                        placePredictions.strSourceLongitude = lstRecentList.get(position).longitude + "";
                        LatLng latlng = new LatLng(Double.parseDouble(placePredictions.strSourceLatitude), Double.parseDouble(placePredictions.strSourceLongitude));
                        placePredictions.strSourceLatLng = "" + latlng;
                        txtaddressSource.setText(placePredictions.strSourceAddress);
                        txtaddressSource.setSelection(0);
                        txtDestination.requestFocus();
                        mAutoCompleteAdapter = null;
                    }
                    setAddress();
                }
            });
        }

        @Override
        public int getItemCount() {
            return lstRecentList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            TextView name, location;

            LinearLayout lnrLocation;

            ImageView imgRecent;

            public MyViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.place_name);
                location = (TextView) itemView.findViewById(R.id.place_detail);
                lnrLocation = (LinearLayout) itemView.findViewById(R.id.lnrLocation);
                imgRecent = (ImageView) itemView.findViewById(R.id.imgRecent);
            }
        }
    }

    private void gotoHomeWork(String strTag) {
        Intent intentHomeWork = new Intent(CustomGooglePlacesSearch.this, AddHomeWorkActivity.class);
        intentHomeWork.putExtra("tag", strTag);
        startActivityForResult(intentHomeWork, UPDATE_HOME_WORK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_HOME_WORK) {
            if (resultCode == Activity.RESULT_OK) {
                getFavoriteLocations();
            }
        }
    }

}
