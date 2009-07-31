package com.joulespersecond.seattlebusbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class StopInfoActivity extends ListActivity {
	private static final String TAG = "StopInfoActivity";
	public static final String STOP_ID = ".StopId";
	
	private StopInfoListAdapter mAdapter;
	private View mListHeader;
	
	private static final int getStopDirectionText(String direction) {
		if (direction.equals("N")) {
			return R.string.direction_n;
		} else if (direction.equals("NW")) {
			return R.string.direction_nw;	    			
		} else if (direction.equals("W")) {
			return R.string.direction_w;	    			
		} else if (direction.equals("SW")) {
			return R.string.direction_sw;	
		} else if (direction.equals("S")) {
			return R.string.direction_s;	
		} else if (direction.equals("SE")) {
			return R.string.direction_se;	
		} else if (direction.equals("E")) {
			return R.string.direction_e;	
		} else if (direction.equals("NE")) {
			return R.string.direction_ne; 		    	    		
		} else {
			Log.v(TAG, "Unknown direction: " + direction);
			return R.string.direction_n;
		}	
	}
	// The results of the ArrivalInfo ObaArray aren't sorted the way we want --
	// so we'll process the data and return a list of preprocessed structures.
	//
	private class StopInfoComparator implements Comparator<StopInfo> {
		public int compare(StopInfo lhs, StopInfo rhs) {
			return (int)(lhs.eta - rhs.eta);
		}
	}
	private class StopInfo {
		private ObaArrivalInfo info;
		public long eta;
		public long displayTime;
		public String statusText;
		
		public StopInfo(ObaArrivalInfo _info, long now) {
			info = _info;
		    // TODO: Decide what to do when the departure and arrival times differ significantly.
			
			// The ETA:
			// if predictedArrivalTime != 0:
			//       ETA = predictedArrivalTime - now
			//       if predictedArrivalTime > scheduledArrivalTime:
			//			status = "delayed"
			//		 else if predictedArrivalTime < scheduledArrivalTime:
			//			status = "early"
			//		 else:
			//			status = "on time"
			// else:
			//		 ETA = scheduledArrivalTime - now
			//		 status = "scheduled arrival"
			long scheduled = info.getScheduledArrivalTime();
			long predicted = info.getPredictedArrivalTime();
			
			if (predicted != 0) {
				eta = (predicted - now)/(60*1000);
				// TODO: Put these strings in strings.xml
				if (predicted > scheduled) {
					long delay = (predicted - scheduled)/(60*1000);
					String format = getResources().getString(
							R.string.stop_info_delayed);
					statusText = String.format(format, delay);
				} else if (predicted < scheduled) {
					long delay = (scheduled - predicted)/(60*1000);
					String format = getResources().getString(
							R.string.stop_info_early);
					statusText = String.format(format, delay);					
				} else {
					statusText = getResources().getString(
							R.string.stop_info_ontime);
				}
				displayTime = predicted;
			}
			else {
				eta = (scheduled - now)/(60*1000);
				displayTime = scheduled;
				if (eta > 0) {
					statusText = getResources().getString(
							R.string.stop_info_scheduled_arrival);
				} else {
					statusText = getResources().getString(
							R.string.stop_info_scheduled_departure);					
				}
			}

		}
	}

	private final ArrayList<StopInfo>
	convertObaArrivalInfo(ObaArray arrivalInfo) {
		int len = arrivalInfo.length();
		ArrayList<StopInfo> result = new ArrayList<StopInfo>(len);
		Time nowObj = new Time();
		nowObj.setToNow();
		long ms = nowObj.toMillis(false);
		for (int i=0; i < len; ++i) {
			result.add(new StopInfo(arrivalInfo.getArrivalInfo(i), ms));
		}
		// Sort by ETA
		Collections.sort(result, new StopInfoComparator());
		return result;
	}
	
	private class StopInfoListAdapter extends BaseAdapter {
		private ArrayList<StopInfo> mInfo;
		
		public StopInfoListAdapter() {
			mInfo = new ArrayList<StopInfo>();
		}
		public int getCount() {
			return mInfo.size();
		}
		public Object getItem(int position) {
			// Replace this when we add a real stop info array
			return mInfo.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewGroup newView;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				newView = (ViewGroup)inflater.inflate(R.layout.stop_info_listitem, null);
			}
			else {
				newView = (ViewGroup)convertView;
			}
			setData(newView, position);
			return newView;
		}
		public boolean hasStableIds() {
			return false;
		}
		
		public void setData(ObaResponse response) {
			ObaData data = response.getData();
			mInfo = convertObaArrivalInfo(data.getArrivalsAndDepartures());
			notifyDataSetChanged();
		}
		private void setData(ViewGroup view, int position) {
			TextView route = (TextView)view.findViewById(R.id.route);
			TextView destination = (TextView)view.findViewById(R.id.destination);
			TextView time = (TextView)view.findViewById(R.id.time);
			TextView status = (TextView)view.findViewById(R.id.status);
			TextView etaView = (TextView)view.findViewById(R.id.eta);
			
			StopInfo info = mInfo.get(position);
			
			route.setText(info.info.getShortName());
			destination.setText(info.info.getHeadsign());
			status.setText(info.statusText);
			
			if (info.eta == 0) {
				etaView.setText(R.string.stop_info_eta_now);
			}
			else {
				etaView.setText(String.valueOf(info.eta));
			}
			
			time.setText(DateUtils.formatDateTime(StopInfoActivity.this, 
					info.displayTime, 
					DateUtils.FORMAT_SHOW_TIME|
					DateUtils.FORMAT_NO_NOON|
					DateUtils.FORMAT_NO_MIDNIGHT));			
		}

	}
	
	private class GetArrivalInfoTask extends AsyncTask<String,Void,ObaResponse> {
		@Override
		protected void onPreExecute() {
	        setProgressBarIndeterminateVisibility(true);
		}
		@Override
		protected ObaResponse doInBackground(String... params) {
			return ObaApi.getArrivalsDeparturesForStop(params[0]);
		}
		@Override
		protected void onPostExecute(ObaResponse result) {
	    	if (result.getCode() != ObaApi.OBA_OK) {
	    		Log.v(TAG, "Request failed: " + result.getText());
	    		return;
	    	}
	    	ObaStop stop = result.getData().getStop();
	    	TextView name = (TextView)mListHeader.findViewById(R.id.name);
	    	name.setText(stop.getName());
	    	TextView direction = (TextView)mListHeader.findViewById(R.id.direction);
	    	direction.setText(getStopDirectionText(stop.getDirection()));
	    	
	    	mAdapter.setData(result);
	        setProgressBarIndeterminateVisibility(false);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.stop_info);
		ListView listView = getListView();
		// Add a header view
		LayoutInflater inflater = getLayoutInflater();
		mListHeader = inflater.inflate(R.layout.stop_info_header, null);
		listView.addHeaderView(mListHeader);
		
		mAdapter = new StopInfoListAdapter();
		setListAdapter(mAdapter);
		
		Bundle bundle = getIntent().getExtras();
		String stopId = bundle.getString(STOP_ID);
		if (stopId != null) {
			new GetArrivalInfoTask().execute(stopId);		
		}
	}
}
