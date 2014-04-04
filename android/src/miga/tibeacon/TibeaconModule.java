package miga.tibeacon;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.KrollFunction;
import java.util.HashMap;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.KrollDict;
import android.content.Context;
import android.app.Activity;
import java.util.Timer;
import java.util.TimerTask;


import com.easibeacon.protocol.IBeacon;
import com.easibeacon.protocol.IBeaconListener;
import com.easibeacon.protocol.IBeaconProtocol;
import com.easibeacon.protocol.Utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import java.util.ArrayList;


@Kroll.module(name="Tibeacon", id="miga.tibeacon")
public class TibeaconModule extends KrollModule implements IBeaconListener{
	
	Context context;
	Activity activity;
	private IBeaconProtocol ibp;
	int seconds=60;
	public static final int REQUEST_BLUETOOTH_ENABLE = 1;	
	KrollFunction success;
	
       @Override
        public void onDestroy(Activity activity) {
	  Log.d("BEACON","destroy---------------------");
	  super.onDestroy(activity);
        }
        
       @Override
        public void onResume(Activity activity) {
         super.onResume(activity);
	  Log.d("BEACON","resume---------------------");
        }
        
       
       @Override
        public void onStart(Activity activity) {
	  super.onStart(activity);
	  Log.d("BEACON","start---------------------");
        }
	
	public TibeaconModule () {
		super();
		TiApplication appContext = TiApplication.getInstance();
		activity = appContext.getCurrentActivity();
		context=activity.getApplicationContext();
		
	}
	
	
	@Override
	public void operationError(int status) {
		Log.i("BEACON", "Bluetooth error: " + status);	
	}
	
	@Kroll.method
	public void initBeacon(HashMap args){
	  KrollDict arg = new KrollDict(args);
	  success =(KrollFunction) arg.get("success");
	  seconds=arg.optInt("interval",60);
	  ibp = IBeaconProtocol.getInstance(activity);
	  ibp.setListener(this);
	}

	
	@Override
	public void searchState(final int state) {
		activity.runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				if(state == IBeaconProtocol.SEARCH_STARTED){
					Log.i("BEACON","started scanning");
				}else if (state == IBeaconProtocol.SEARCH_END_SUCCESS){
					sendData();
					Log.i("BEACON","scan end success");
				}else if (state == IBeaconProtocol.SEARCH_END_EMPTY){
					Log.i("BEACON","search end empty");
				}
			}
		});
	}
	
	@Override
	public void beaconFound(IBeacon ibeacon) {
		Log.i("BEACON","iBeacon found: " + ibeacon.toString());
	}
	
	@Override
	public void exitRegion(IBeacon ibeacon) {
		activity.runOnUiThread(new Runnable() {		
			@Override
			public void run() {
			}
		});
	}
	
	@Override
	public void enterRegion(IBeacon ibeacon) {
		Log.i("BEACON","Enter region: " + ibeacon.toString());
	}
	
	@Kroll.method
	public void startScanning(){
	 
	    TimerTask searchIbeaconTask = new TimerTask() {	
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable() {					
					@Override
					public void run() {
						scanBeacons();
					}
				});
			}
		};	
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(searchIbeaconTask, 1000, seconds*1000);
	}
	
	@Kroll.method
	public void stopScanning(){
	  Log.i("BEACON","stop scanning");
	  ibp = IBeaconProtocol.getInstance(activity);
	  if(ibp.isScanning()) ibp.scanIBeacons(false);
	}
	
	
	private void scanBeacons(){
		// Check Bluetooth every time
		Log.i("BEACON","Scanning...");
		ibp = IBeaconProtocol.getInstance(activity);
		
		// Filter based on default easiBeacon UUID, remove if not required
		//ibp.setScanUUID(UUID);

		if(!IBeaconProtocol.initializeBluetoothAdapter(activity)){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE );
		}else{
			ibp.setListener(this);
			if(ibp.isScanning())
				ibp.scanIBeacons(false);
			ibp.reset();
			ibp.scanIBeacons(true);		
		}		
	}
	
	public void sendData(){
	  
	  ArrayList<IBeacon> beacons = ibp.getIBeaconsByProximity();
	  HashMap<String, KrollDict[]> event = new HashMap<String, KrollDict[]>();
	  KrollDict[] dList = new KrollDict[beacons.size()]; 
	 
	  Log.i("BEACON","returning: "+beacons.size());
	  for (int i=0; i<beacons.size();++i){
	    KrollDict d = new KrollDict();
	    
	    IBeacon beacon = beacons.get(i);
	    
	    d.put("mac",beacon.getMacAddress());
	    d.put("major",beacon.getMajor());
	    d.put("minor",beacon.getMinor());
	    d.put("power",beacon.getPowerValue());
 	    d.put("proximity",beacon.getProximity());
 	    d.put("uuid",beacon.getUuidHexString());
	    dList[i]=d;
	  }
	  event.put("devices", dList);
	 
	  // Success-Callback
	  success.call(getKrollObject(), event);
	}
	
}


