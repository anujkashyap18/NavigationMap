package com.example.navigationmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

@SuppressWarnings ( { "deprecation" ,
		"MissingPermission" } )
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {
	
	private static final String ROUTE_LAYER_ID = "route-layer-id";
	private static final String ROUTE_SOURCE_ID = "route-source-id";
	private static final String ICON_SOURCE_ID = "icon-source-id";
	PlaceOptions.Builder placeOptions;
	CarmenFeature feature;
	Marker current, dest;
	LocationManager locationManager;
	Point origin, destination;
	String[] perms = { Manifest.permission.INTERNET , Manifest.permission.ACCESS_FINE_LOCATION };
	int x = 0;
	private MapView mapView;
	private MapboxMap mapboxMap;
	private PermissionsManager permissionsManager;
	private LocationComponent locationComponent;
	private DirectionsRoute currentRoute;
	private NavigationMapRoute navigationMapRoute;
	private MaterialButton button, autoBtn;
	private LinearLayoutCompat btnGrp;
	private ImageView search;
	
	@Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		Mapbox.getInstance ( this , getString ( R.string.access_token ) );
		
		setContentView ( R.layout.activity_main );
		
		mapView = findViewById ( R.id.mapView );
		button = findViewById ( R.id.startButton );
		autoBtn = findViewById ( R.id.autoBtn );
		btnGrp = findViewById ( R.id.btnGrp );
		search = findViewById ( R.id.search );
		
		button.setEnabled ( false );
		autoBtn.setEnabled ( false );
		button.animate ( ).alpha ( 0.7f );
		autoBtn.animate ( ).alpha ( 0.7f );
		
		mapView.onCreate ( savedInstanceState );
		mapView.getMapAsync ( this );
		
		placeOptions = PlaceOptions.builder ( );
		placeOptions.limit ( 10 )
				.country ( "IN" )
				.backgroundColor ( this.getResources ( ).getColor ( R.color.white ) )
				.build ( PlaceOptions.MODE_CARDS );
		
		search.setOnClickListener ( new View.OnClickListener ( ) {
			@Override
			public void onClick ( View view ) {
				Intent intent = new PlaceAutocomplete.IntentBuilder ( )
						.accessToken ( "pk.eyJ1Ijoic3RhcnRvLXRheGkiLCJhIjoiY2tlNDZ6amxjMHE2azJ0bzRvcmVhcTZkcyJ9.RmcFBGdhI8rqjl-suodU0A" )
						.placeOptions ( placeOptions.build ( ) )
						.build ( MainActivity.this );
				btnGrp.setVisibility ( View.VISIBLE );
				startActivityForResult ( intent , 11 );
			}
		} );
		
		locationManager = ( LocationManager ) getSystemService ( Context.LOCATION_SERVICE );
		
		
		button.setOnClickListener ( ( View view ) -> {
			try {
				NavigationLauncherOptions options = NavigationLauncherOptions.builder ( )
						.directionsRoute ( currentRoute )
						.shouldSimulateRoute ( false )
						.build ( );
				NavigationLauncher.startNavigation ( MainActivity.this , options );
			}
			catch ( Exception e ) {
				Timber.d ( "Exception : %s" , e.getLocalizedMessage ( ) );
			}
		} );
		autoBtn.setOnClickListener ( view -> {
			try {
				NavigationLauncherOptions options = NavigationLauncherOptions.builder ( )
						.directionsRoute ( currentRoute )
						.shouldSimulateRoute ( true )
						.build ( );
				NavigationLauncher.startNavigation ( MainActivity.this , options );
			}
			catch ( Exception e ) {
				Timber.d ( "Exception : %s" , e.getLocalizedMessage ( ) );
			}
		} );
	}
	
	@Override
	protected void onActivityResult ( int requestCode , int resultCode , @Nullable Intent data ) {
		super.onActivityResult ( requestCode , resultCode , data );
		if ( resultCode == Activity.RESULT_OK && requestCode == 11 ) {
			feature = PlaceAutocomplete.getPlace ( data );
			
			if ( mapboxMap != null ) {
				if ( ActivityCompat.checkSelfPermission ( this , Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
						&& ActivityCompat.checkSelfPermission ( this , Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
					return;
				}
				
				Location locationGPS = locationManager.getLastKnownLocation ( LocationManager.NETWORK_PROVIDER );
				double lat = locationGPS.getLatitude ( );
				double lon = locationGPS.getLongitude ( );
				
				origin = Point.fromLngLat ( lon , lat );
				destination = Point.fromLngLat ( ( ( Point ) feature.geometry ( ) ).longitude ( ) , ( ( Point ) feature.geometry ( ) ).latitude ( ) );
			}
			mapboxMap.setStyle ( Style.MAPBOX_STREETS , style -> {
				
				IconFactory iconFactory = IconFactory.getInstance ( MainActivity.this );
				Icon pickup = iconFactory.fromBitmap ( bitmapDescriptorFromVector ( MainActivity.this ) );
				Icon dropoff = iconFactory.fromBitmap ( bitmapDescriptorFromVector ( MainActivity.this ) );
				initSource ( style );
				initLayers ( style );
				getRoute ( origin , destination );
				
				mapboxMap.animateCamera ( CameraUpdateFactory.newLatLngZoom ( new LatLng ( origin.latitude ( ) , origin.longitude ( ) ) , 11 ) , 1200 );

//				current = mapboxMap.addMarker (
//						new MarkerOptions ( ).title ( "Current Location" ).position ( new LatLng ( origin.latitude ( ) , origin.longitude ( ) ) ).icon ( pickup ) );
//				current.getInfoWindow ( );
//
//				dest = new Marker ( new MarkerOptions ( ) );
//
//				dest.remove ( );
//
//				dest = mapboxMap.addMarker (
//						new MarkerOptions ( ).title ( "Destination" )
//								.position ( new LatLng ( destination.latitude ( ) , destination.longitude ( ) ) ).icon ( dropoff ) );
//				dest.getInfoWindow ( );
				current = new Marker (
						new MarkerOptions ( ).title ( "Current Location" ).position (
								new LatLng ( origin.latitude ( ) , origin.longitude ( ) ) ).icon ( pickup ) );
				
				mapboxMap.updateMarker ( current );
				
				if ( x == 0 ) {
					dest = mapboxMap.addMarker (
							new MarkerOptions ( ).title ( "Destination" )
									.position ( new LatLng ( destination.latitude ( ) , destination.longitude ( ) ) ).icon ( dropoff ) );
					x++;
				}
				else {
					mapboxMap.removeMarker ( dest );
					dest = mapboxMap.addMarker (
							new MarkerOptions ( ).title ( "Destination" )
									.position ( new LatLng ( destination.latitude ( ) , destination.longitude ( ) ) ).icon ( dropoff ) );
					mapboxMap.updateMarker ( dest );
				}
			} );
		}
	}
	
	@Override
	public void onMapReady ( @NonNull MapboxMap mapboxMap ) {
		this.mapboxMap = mapboxMap;
		
		mapboxMap.setStyle ( getString ( R.string.navigation_guidance_day ) , style -> {
			
			enableLocationComponent ( style );
			addDestinationIconSymbolLayer ( style );
			
			mapboxMap.addOnMapClickListener ( point -> {
				Point destinationPoint = Point.fromLngLat ( point.getLongitude ( ) , point.getLatitude ( ) );
				
				assert locationComponent.getLastKnownLocation ( ) != null;
				Point originPoint = Point.fromLngLat ( locationComponent.getLastKnownLocation ( ).getLongitude ( ) ,
						locationComponent.getLastKnownLocation ( ).getLatitude ( ) );
				
				GeoJsonSource source = Objects.requireNonNull ( mapboxMap.getStyle ( ) ).getSourceAs ( "destination-source-id" );
				if ( source != null ) {
					source.setGeoJson ( Feature.fromGeometry ( destinationPoint ) );
				}
				
				getRoute ( originPoint , destinationPoint );
				btnGrp.setVisibility ( View.VISIBLE );
				return true;
			} );
			
		} );
		
	}
	
	private Bitmap bitmapDescriptorFromVector ( Context mainActivity ) {
		Drawable background = ContextCompat.getDrawable ( mainActivity , R.drawable.ic_baseline_pin_drop_24 );
		background.setBounds ( 0 , 0 , background.getIntrinsicWidth ( ) , background.getIntrinsicHeight ( ) );
		Bitmap bitmap = Bitmap.createBitmap ( background.getIntrinsicWidth ( ) , background.getIntrinsicHeight ( ) , Bitmap.Config.ARGB_8888 );
		Canvas canvas = new Canvas ( bitmap );
		background.draw ( canvas );
		return bitmap;
	}
	
	
	private void addDestinationIconSymbolLayer ( @NonNull Style loadedMapStyle ) {
		
		loadedMapStyle.addImage (
				"destination-icon-id" ,
				BitmapFactory.decodeResource ( this.getResources ( ) ,
						R.drawable.mapbox_marker_icon_default )
		);
		
		GeoJsonSource geoJsonSource = new GeoJsonSource ( "destination-source-id" );
		
		loadedMapStyle.addSource ( geoJsonSource );
		SymbolLayer destinationSymbolLayer = new SymbolLayer ( "destination-symbol-layer-id" , "destination-source-id" );
		
		destinationSymbolLayer.withProperties (
				iconImage ( "destination-icon-id" ) ,
				iconAllowOverlap ( true ) ,
				iconIgnorePlacement ( true )
		);
		loadedMapStyle.addLayer ( destinationSymbolLayer );
	}
	
	private void getRoute ( Point origin , Point destination ) {
		assert Mapbox.getAccessToken ( ) != null;
		NavigationRoute.builder ( this )
				.accessToken ( Mapbox.getAccessToken ( ) )
				.origin ( origin )
				.destination ( destination )
				.enableRefresh ( true )
				.build ( )
				.getRoute ( new Callback < DirectionsResponse > ( ) {
					@Override
					public void onResponse ( @NotNull Call < DirectionsResponse > call , @NotNull Response < DirectionsResponse > response ) {
						// You can get the generic HTTP info about the response
						Timber.d ( "Response code : %s" , response.code ( ) );
						if ( response.body ( ) == null ) {
							Timber.e ( "No routes found, make sure you set the right user and access token." );
							return;
						}
						else if ( response.body ( ).routes ( ).size ( ) < 1 ) {
							Timber.e ( "No routes found" );
							return;
						}
						
						currentRoute = response.body ( ).routes ( ).get ( 0 );
						
						if ( navigationMapRoute != null ) {
							navigationMapRoute.removeRoute ( );
						}
						else {
							navigationMapRoute = new NavigationMapRoute ( null , mapView , mapboxMap , R.style.NavigationMapRoute );
						}
						navigationMapRoute.addRoute ( currentRoute );
						button.setEnabled ( true );
						autoBtn.setEnabled ( true );
						button.animate ( ).alpha ( 1f );
						autoBtn.animate ( ).alpha ( 1f );
					}
					
					@Override
					public void onFailure ( @NotNull Call < DirectionsResponse > call , @NotNull Throwable throwable ) {
						Timber.e ( "Error : %s" , throwable.getMessage ( ) );
					}
				} );
	}
	
	private void enableLocationComponent ( @NonNull Style loadedMapStyle ) {
		// Check if permissions are enabled and if not request
		if ( PermissionsManager.areLocationPermissionsGranted ( this ) ) {
			// Activate the MapboxMap LocationComponent to show user location
			// Adding in LocationComponentOptions is also an optional parameter
			locationComponent = mapboxMap.getLocationComponent ( );
			locationComponent.activateLocationComponent ( this , loadedMapStyle );
			locationComponent.setLocationComponentEnabled ( true );
			// Set the component's camera mode
			locationComponent.setCameraMode ( CameraMode.TRACKING );
		}
		else {
			permissionsManager = new PermissionsManager ( this );
			permissionsManager.requestLocationPermissions ( this );
		}
	}
	
	
	private void initSource ( @NonNull Style loadedMapStyle ) {
		loadedMapStyle.addSource ( new GeoJsonSource ( ROUTE_SOURCE_ID ) );
		GeoJsonSource iconGeoJsonSource = new GeoJsonSource ( ICON_SOURCE_ID , FeatureCollection.fromFeatures ( new Feature[] {
				Feature.fromGeometry ( Point.fromLngLat ( origin.longitude ( ) , origin.latitude ( ) ) ) ,
				Feature.fromGeometry ( Point.fromLngLat ( destination.longitude ( ) , destination.latitude ( ) ) ) } ) );
		loadedMapStyle.addSource ( iconGeoJsonSource );
	}
	
	private void initLayers ( @NonNull Style loadedMapStyle ) {
		LineLayer routeLayer = new LineLayer ( ROUTE_LAYER_ID , ROUTE_SOURCE_ID );
		
		routeLayer.setProperties (
				lineCap ( Property.LINE_CAP_ROUND ) ,
				lineJoin ( Property.LINE_JOIN_ROUND ) ,
				lineWidth ( 5f ) ,
				lineColor ( Color.parseColor ( "#ffbc01" ) )
		);
		loadedMapStyle.addLayer ( routeLayer );
		
		
	}
	
	@Override
	public void onRequestPermissionsResult ( int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults ) {
		permissionsManager.onRequestPermissionsResult ( requestCode , permissions , grantResults );
	}
	
	@Override
	public void onExplanationNeeded ( List < String > permissionsToExplain ) {
		Toast.makeText ( this , R.string.user_location_permission_explanation , Toast.LENGTH_LONG ).show ( );
		
	}
	
	@Override
	public void onPermissionResult ( boolean granted ) {
		if ( granted ) {
			enableLocationComponent ( Objects.requireNonNull ( mapboxMap.getStyle ( ) ) );
		}
		else {
			Toast.makeText ( this , R.string.user_location_permission_not_granted , Toast.LENGTH_LONG ).show ( );
			finish ( );
		}
	}
	
	@Override
	protected void onStart ( ) {
		super.onStart ( );
		mapView.onStart ( );
	}
	
	@Override
	protected void onResume ( ) {
		super.onResume ( );
		mapView.onResume ( );
	}
	
	@Override
	protected void onPause ( ) {
		super.onPause ( );
		mapView.onPause ( );
	}
	
	@Override
	protected void onStop ( ) {
		super.onStop ( );
		mapView.onStop ( );
	}
	
	@Override
	protected void onSaveInstanceState ( @NotNull Bundle outState ) {
		super.onSaveInstanceState ( outState );
		mapView.onSaveInstanceState ( outState );
	}
	
	@Override
	protected void onDestroy ( ) {
		super.onDestroy ( );
		mapView.onDestroy ( );
	}
	
	@Override
	public void onLowMemory ( ) {
		super.onLowMemory ( );
		mapView.onLowMemory ( );
	}
	
}