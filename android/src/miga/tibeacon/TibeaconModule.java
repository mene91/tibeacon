package miga.Tibeacon;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.KrollFunction;
import java.util.HashMap;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.KrollDict;
import android.content.Context;
import android.app.Activity;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import android.os.Bundle;
import android.view.ViewGroup;

import android.view.View;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.widget.BaseAdapter;

@Kroll.module(name="Tibecon", id="miga.tibeacon")
public class TibeaconModule extends KrollModule {
	
	Context context;
	Activity activity;
	private BluetoothAdapter mBluetoothAdapter;
	KrollFunction success;
	private LeDeviceListAdapter mLeDeviceListAdapter =new LeDeviceListAdapter();
	
	
       @Override
        public void onDestroy(Activity activity) {
	  Log.d("BEACON","destroy---------------------");
	  stopScanning();
	  super.onDestroy(activity);
        }
        
       @Override
        public void onResume(Activity activity) {
         super.onResume(activity);
	  Log.d("BEACON","resume---------------------");
	  startScanning();
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
	
	
	public void sendData(){
	  HashMap<String, KrollDict[]> event = new HashMap<String, KrollDict[]>();
	  KrollDict[] dList = new KrollDict[mLeDeviceListAdapter.getCount()];
	  
	  for (int i=0; i<mLeDeviceListAdapter.getCount();++i){
	    KrollDict d = new KrollDict();
	    
	    ReturnSet ret = (ReturnSet)mLeDeviceListAdapter.getItem(i);
	    BluetoothDevice device = ret.getDevice();
	    
	    d.put("name",device.getName());
	    d.put("address",device.getAddress());
	    d.put("rssi",ret.getRssi());
	    d.put("major",ret.getMajor());
	    d.put("minor",ret.getMinor());
	    d.put("power",ret.getPower());
	    d.put("accuracy",ret.getAccuracy());
	    dList[i]=d;
	  }
	  event.put("device", dList);
	 
	  // Success-Callback
	  success.call(getKrollObject(), event);
	  
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
	    @Override
	    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
	      activity.runOnUiThread(new Runnable() {
		  @Override
		  public void run() {
		      Log.d("BEACON", "found name: " + device.getName() +  " address: " + device.getAddress() + " rssi: " + rssi);
		      int major = 0;
		      int minor = 0;
		      int power = 0;
		      
		      int startByte = 2;
		      boolean patternFound = false;
		      
		      while (startByte <= 5) {
			      if (((int)scanRecord[startByte] & 0xff) == 0x4c &&
				      ((int)scanRecord[startByte+1] & 0xff) == 0x00 &&
				      ((int)scanRecord[startByte+2] & 0xff) == 0x02 &&
				      ((int)scanRecord[startByte+3] & 0xff) == 0x15) {                        
				      patternFound = true;
				      Log.d("BEACON", "found ibeacon");
				      break;
			      } else if (((int)scanRecord[startByte] & 0xff) == 0x2d &&
					      ((int)scanRecord[startByte+1] & 0xff) == 0x24 &&
					      ((int)scanRecord[startByte+2] & 0xff) == 0xbf &&
					      ((int)scanRecord[startByte+3] & 0xff) == 0x16) {        
				      Log.d("BEACON", "estimote");
			      }                                        
			      startByte++;
		      }
		      
		      if (patternFound == false) {
			Log.d("BEACON","no ibeacon found");
		      } else {
		    
			power = (int)scanRecord[startByte+24];
			major = (scanRecord[startByte+20] & 0xff) * 0x100 + (scanRecord[startByte+21] & 0xff);
			minor = (scanRecord[startByte+22] & 0xff) * 0x100 + (scanRecord[startByte+23] & 0xff);
			
		      }	      

	      
		      double ratio = rssi*1.0/power;
		      double accuracy = 0;
		      if (ratio < 1.0) {
			      accuracy = Math.pow(ratio,10);
		      }  else {
			      accuracy = (0.89976)*Math.pow(ratio,7.7095) + 0.111;
		      }
		      
		      mLeDeviceListAdapter.addDevice(device, rssi,accuracy,minor,major,power);
		      mLeDeviceListAdapter.notifyDataSetChanged();
	      
		      sendData();
	      
		      
		  }
	      });
	  }
	};
	
	
	@Kroll.method
	public void stopScanning(){
	  mBluetoothAdapter.stopLeScan(mLeScanCallback);
	  
	}
	
	@Kroll.method
	public void startScanning(){
	  mLeDeviceListAdapter.clear();
	  mBluetoothAdapter.startLeScan(mLeScanCallback);
	}
	
	@Kroll.method
	public void initBeacon(HashMap args){
	  
	  Log.d("BEACON","init");
	  KrollDict arg = new KrollDict(args);
	  success =(KrollFunction) arg.get("success");
	  
	  BluetoothManager manager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
	  mBluetoothAdapter = manager.getAdapter();
	  startScanning();
	}
	
	
	
	// Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<Integer> rssi;
        private ArrayList<Double> accuracy;
        private ArrayList<Integer> minor;
        private ArrayList<Integer> major;
        private ArrayList<Integer> power;
        
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            rssi = new ArrayList<Integer>();
            major = new ArrayList<Integer>();
            minor = new ArrayList<Integer>();
            power = new ArrayList<Integer>();
            accuracy= new ArrayList<Double>();
            
        }

        public void addDevice(BluetoothDevice device, int r, double a, int mi,int ma,int p) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                rssi.add(r);
                accuracy.add(a);
                minor.add(mi);
                major.add(ma);
                power.add(p);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            rssi.clear();
            accuracy.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

       
        
        @Override
        public Object getItem(int i) {
            return new ReturnSet(mLeDevices.get(i),rssi.get(i),accuracy.get(i),minor.get(i),major.get(i),power.get(i));
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return view;
        }
    }

	
    public class ReturnSet {
	    private BluetoothDevice device;
	    private int rssi;
	    private double accuracy;
	    private int minor;
	    private int major;
	    private int power;
	    public ReturnSet(BluetoothDevice d, int r, double a, int mi,int ma,int p) {
		device = d;
		rssi = r;
		accuracy = a;
		major = ma;
		minor = mi;
		power = p;
	    }
	    
	    public BluetoothDevice getDevice(){
	      return device;
	    }
	    
	    public int getRssi(){
	      return rssi;
	    }
	    public double getAccuracy(){
	      return accuracy;
	    }
	    
	    public int getMinor(){
	      return minor;
	    }
	    
	    public int getMajor(){
	      return major;
	    }
	    
	    public int getPower(){
	      return power;
	    }
	}
	
}


