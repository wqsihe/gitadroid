package net.wesley.kalacontroller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class OrderActivity extends Activity{
	private byte[] bytes=new byte[1024];
	private DatagramPacket mPacket=new DatagramPacket(bytes, bytes.length);
	public int page=0;

	class DeleteLocalSongAsyncTask extends AsyncTask<HashMap<String,Object>,Void,String>{
		@Override
		protected String doInBackground(HashMap<String, Object>... arg0) {
			try {
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setSoTimeout(1000);
				String string="DELETE ";
				string+=String.format("%s %s %s", arg0[0].get("id").toString(),arg0[0].get("name").toString(),arg0[0].get("artist").toString());
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName(currPeer);
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, 4004); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					return cnt;
				}

			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "远端操作失败!";
		}
		@Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);
	        if (result.equals("OK")){
	        	Toast.makeText(OrderActivity.this, "搞定!", Toast.LENGTH_SHORT).show();
	        }
	        else{
	        	Toast.makeText(OrderActivity.this, result, Toast.LENGTH_SHORT).show();
	        }
	        	
		}
	}
	
	class SearchLocalLeftAsyncTask extends AsyncTask<Void,Void,String>{
		@Override
		protected String doInBackground(Void... arg0) {
			try {
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setSoTimeout(1000);
				page+=1;
				String string="SEARCHNEXT "+page;
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName(currPeer);
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, 4004); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					String[] infos = cnt.split(" ",2);
					if (Integer.parseInt(infos[0])!=page) return "";
					return infos[1];
				}

			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);
			((Button)findViewById(R.id.idlocal)).setEnabled(true);
	        if (result.length()!=0){
	        	appendLocalSongList(result);
	        }
	        else{
	        	updateSongListView();
	        }
	    }
		
	}
	
	class SearchLocalSongAsyncTask extends AsyncTask<String,Void,String>{
		@Override
		protected String doInBackground(String... arg0) {
			try {
				page=-1;
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setSoTimeout(1000);
				String string="SEARCH ";
				if (arg0[0].trim().length()==0){
					string+="EMPTY"+SystemClock.elapsedRealtime();
				}
				else{
					string+=arg0[0];
				}
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName(currPeer);
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, 4004); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					return cnt;
				}

			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		}
		
		@Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);
			((Button)findViewById(R.id.idlocal)).setEnabled(true);
	        if (result.length()==0){
	        	Toast.makeText(OrderActivity.this, "没有找到任何歌曲", Toast.LENGTH_LONG).show();
	        }
	        else{
	        	updateLocalSongList(result);
	        	SearchLocalLeftAsyncTask task=new SearchLocalLeftAsyncTask();
	        	task.execute();
	        }
	    }

	}
	
	class OrderSongAsyncTask extends AsyncTask<ArrayList<HashMap<String,Object>>,Void,String>{
		@Override
		protected String doInBackground(
				ArrayList<HashMap<String, Object>>... params) {
			try {
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setSoTimeout(1000);
				String string="ADD ";
				for (HashMap<String,Object> map:params[0]){
					string+=" "+map.get("id")+" "+(map.get("name").toString().replace(" ", "___"))+" "+(map.get("artist").toString().replace(" ", "___"))+";";
				}
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName(currPeer);
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, 4004); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					if (cnt.equals("OK")) return "";
				}

			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "连接失败!";
		}
		
		@Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);
	        if (result.length()==0) result="异步操作完成!";
	        Toast.makeText(OrderActivity.this, result, Toast.LENGTH_LONG).show();
			((Button)findViewById(R.id.idorder)).setEnabled(true);
	    }
	}
	
	ArrayList<HashMap<String,Object>> songs=new ArrayList<HashMap<String,Object>>();
	ArrayList<HashMap<String,Object>> artists=new ArrayList<HashMap<String,Object>>();
	
	private String currPeer;
	private boolean backToArtists=false;
	private boolean isLocalSong=false;
	
	static class SearchArtistSongHandler extends Handler {
	    private final WeakReference<OrderActivity> mActivity; 

	    SearchArtistSongHandler(OrderActivity service) {
	        mActivity = new WeakReference<OrderActivity>(service);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	        OrderActivity service = mActivity.get();
	        if (service != null) {
	            service.handleArtistSong(msg);
	        }
	    }
	}
	
	static class SearchSingerHandler extends Handler {
	    private final WeakReference<OrderActivity> mActivity; 

	    SearchSingerHandler(OrderActivity service) {
	    	mActivity = new WeakReference<OrderActivity>(service);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	OrderActivity service = mActivity.get();
	         if (service != null) {
	              service.handleSearchSinger(msg);
	         }
	    }
	}

	static class SearchSongHandler extends Handler {
	    private final WeakReference<OrderActivity> mActivity; 

	    SearchSongHandler(OrderActivity service) {
	    	mActivity = new WeakReference<OrderActivity>(service);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	OrderActivity service = mActivity.get();
	         if (service != null) {
	              service.handleSearchSong(msg);
	         }
	    }
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.order);

		currPeer=this.getIntent().getStringExtra("peer");
		
		ListView lv=(ListView) this.findViewById(R.id.idlistview);
		lv.setSelector(R.drawable.listrowbg);
		
		((Button)findViewById(R.id.idsong)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				hideSoftKeyboard(OrderActivity.this);
				((Button)findViewById(R.id.idsong)).setEnabled(false);
				TextView v=(TextView) findViewById(R.id.idtext);
				searchSong(v.getText().toString());
			}
		});
		
		((Button)findViewById(R.id.idsinger)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				hideSoftKeyboard(OrderActivity.this);
				((Button)findViewById(R.id.idsinger)).setEnabled(false);
				TextView v=(TextView) findViewById(R.id.idtext);
				searchSinger(v.getText().toString());
			}
		});

		((Button)findViewById(R.id.idlocal)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				hideSoftKeyboard(OrderActivity.this);
				((Button)findViewById(R.id.idlocal)).setEnabled(false);
				TextView v=(TextView) findViewById(R.id.idtext);
				searchLocal(v.getText().toString());
			}
		});

		((Button)findViewById(R.id.btnclear)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				ListView lv=(ListView) findViewById(R.id.idlistview);
				lv.clearChoices();
				((Button)findViewById(R.id.idorder)).setText("点播选中");
			}
		});
		
		((Button)findViewById(R.id.idorder)).setOnClickListener(new OnClickListener(){
			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View arg0) {
				((Button)findViewById(R.id.idorder)).setEnabled(false);
				ListView lv=(ListView) findViewById(R.id.idlistview);
				ArrayList<HashMap<String,Object>> result=new ArrayList<HashMap<String,Object>>();
				for (int i=0;i<songs.size();i++){
					if (lv.isItemChecked(i)){
						result.add(songs.get(i));
					}
				}
				OrderSongAsyncTask task=new OrderSongAsyncTask();
				task.execute(result);
				lv.clearChoices();
				((Button)findViewById(R.id.idorder)).setText("点播选中");

			}
		});
	}

	public void appendLocalSongList(String result) {
    	String[] p = result.split(";");
    	for (int i=0;i<p.length;i++){
    		String[] infos = p[i].trim().split(" ");
    		if (infos.length==3){
    			HashMap<String,Object> map=new HashMap<String,Object>();
    			map.put("id", infos[0]);
    			map.put("name", infos[1].toString().replace("___", " "));
    			map.put("artist", infos[2].toString().replace("___", " "));
    			songs.add(map);
    		}
    	}
    	SearchLocalLeftAsyncTask task=new SearchLocalLeftAsyncTask();
    	task.execute();
	}

	public void updateLocalSongList(String result) {
		backToArtists=false;
		isLocalSong=true;
    	songs.clear();
    	String[] p = result.split(";");
    	for (int i=0;i<p.length;i++){
    		String[] infos = p[i].trim().split(" ");
    		if (infos.length==3){
    			HashMap<String,Object> map=new HashMap<String,Object>();
    			map.put("id", infos[0]);
    			map.put("name", infos[1].toString().replace("___", " "));
    			map.put("artist", infos[2].toString().replace("___", " "));
    			songs.add(map);
    		}
    	}
	}

	@Override
	public void onBackPressed(){
		if (backToArtists){
			updateArtistsListView();
		}
		else{
			super.onBackPressed();
		}
	}
	
	protected void searchLocal(String string) {
		SearchLocalSongAsyncTask task=new SearchLocalSongAsyncTask();
		task.execute(string);
	}

	public void handleArtistSong(Message message) {
		switch (message.what) {
		case HttpConnection.DID_SUCCEED: {
			backToArtists=true;
			((Button)findViewById(R.id.idsong)).setEnabled(true);
			String body=(String) message.obj;
			try {  
			    JSONTokener jsonParser = new JSONTokener(body);  
			    JSONObject result = (JSONObject) jsonParser.nextValue(); 
			    if (result.getInt("result")==0){
			        Toast.makeText(this, result.getString("text"), Toast.LENGTH_LONG).show();
			    }
			    else{
			        JSONArray array = result.getJSONArray("content");
			        isLocalSong=false;
			        songs.clear();
			        for (int i=0;i<array.length();i++){
			            JSONObject song=array.getJSONObject(i);
			            HashMap<String,Object> map=new HashMap<String,Object>();
			            map.put("id", song.getInt("id"));
			            map.put("name", song.getString("name"));
			            map.put("artist", song.getString("artist"));
			            //map.put("size", song.getInt("onlineSize"));
			            songs.add(map);
			        }
			        updateSongListView();
			    }
			} catch (JSONException ex) {  
			    Log.d("", "exception", ex);
			}  
		}
		}
	}

	public void handleSearchSong(Message message) {
		switch (message.what) {
		case HttpConnection.DID_SUCCEED: {
			backToArtists=false;
			((Button)findViewById(R.id.idsong)).setEnabled(true);
			String body=(String) message.obj;
			//Log.d("myown", "response:"+body);
			try {  
			    JSONTokener jsonParser = new JSONTokener(body);  
			    JSONObject result = (JSONObject) jsonParser.nextValue(); 
			    if (result.getInt("result")==0){
			    	Toast.makeText(this, result.getString("text"), Toast.LENGTH_LONG).show();
			    }
			    else{
			    	JSONArray array = result.getJSONArray("songs");
			    	isLocalSong=false;
			    	songs.clear();
			    	for (int i=0;i<array.length();i++){
			    		JSONObject song=array.getJSONObject(i);
			    		HashMap<String,Object> map=new HashMap<String,Object>();
			    		map.put("id", song.getInt("id"));
			    		map.put("name", song.getString("name"));
			    		map.put("artist", song.getString("artist"));
			    		//map.put("size", song.getInt("onlineSize"));
			    		songs.add(map);
			    	}
			    	updateSongListView();
			    }
			} catch (JSONException ex) {  
			}  
		}
		}
	}

	public void handleSearchSinger(Message message) {
		switch (message.what) {
		case HttpConnection.DID_SUCCEED: {
			backToArtists=false;
			((Button)findViewById(R.id.idsinger)).setEnabled(true);
			String body=(String) message.obj;
			try {  
			    JSONTokener jsonParser = new JSONTokener(body);  
			    JSONObject result = (JSONObject) jsonParser.nextValue(); 
			    if (result.getInt("result")==0){
			    	Toast.makeText(this, result.getString("text"), Toast.LENGTH_LONG).show();
			    }
			    else{
			    	JSONArray array = result.getJSONArray("artists");
			    	artists.clear();
			    	for (int i=0;i<array.length();i++){
			    		JSONObject song=array.getJSONObject(i);
			    		HashMap<String,Object> map=new HashMap<String,Object>();
			    		map.put("id", song.getInt("id"));
			    		map.put("name", song.getString("name"));
			    		artists.add(map);
			    	}
			    	updateArtistsListView();
			    }
			} catch (JSONException ex) {  
			}  
		}
		}
	}
	
	private void updateArtistsListView() {
		this.backToArtists=false;
		ListView lv=(ListView) this.findViewById(R.id.idlistview);
		lv.setAdapter(new SimpleAdapter(this,
				artists, R.layout.songrow,
				new String[] { "name"}, new int[] {
						R.id.listrow_name}));
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				searchArtistSong(artists.get(arg2));
			}
		});
		((Button)findViewById(R.id.idorder)).setEnabled(false);
	}
	
	protected void searchArtistSong(HashMap<String, Object> map) {
		String request;
		try {
			Log.e("", "search artist songs:"+map.get("id"));
			request = Base64.encodeBytes(("{\"parentId\":\""+map.get("id")+"\",\"common\":{\"clientversion\":\"1.9.3\",\"model\":\"sdk\",\"imei\":\"000000000000000\",\"userid\":0,\"resolution\":\"1196X720\",\"apiversion\":\"1.9.3\",\"product\":\"KALAOK\",\"clienttype\":\"Android\",\"nettype\":\"epc.tmobile.com\",\"updatechannel\":\"37\",\"login\":0,\"language\":1,\"imsi\":\"89014103211118510720\",\"systemversion\":\"17\",\"channel\":\"YYH\"}}").getBytes("UTF-8"));
			String sign=md5(request+"1731c73ef747457e8ac6f2ddb7de9227087e337ee96b4545b71edd50ea79d367");
			String url="http://sns.audiocn.org/tlcysns/content/getCategory.action?request="+request+"&sign="+sign+"&type=52f78ffbda1e416e";
			new HttpConnection(this,new SearchArtistSongHandler(this)).post(url, "");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void updateSongListView() {
		ListView lv=(ListView) this.findViewById(R.id.idlistview);
		lv.clearChoices();
		lv.setAdapter(new SimpleAdapter(this,
				songs, R.layout.songrow,
				new String[] { "name", "artist"}, new int[] {
						R.id.listrow_name, R.id.listrow_artist}));
		lv.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				ListView lv=(ListView) findViewById(R.id.idlistview);
				((Button)findViewById(R.id.idorder)).setText(String.format("点播选中(%d)",lv.getCheckedItemCount()));
			}
			
		});
		if (isLocalSong){
			lv.setOnItemLongClickListener(new OnItemLongClickListener(){
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					final int index=arg2;
					final HashMap<String, Object>map=songs.get(index);
					new AlertDialog.Builder(OrderActivity.this).setTitle("删除本地歌曲")
						.setMessage("确定要删除本地歌曲"+map.get("name")+"?")
						.setPositiveButton("确定", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								DeleteLocalSongAsyncTask task=new DeleteLocalSongAsyncTask();
								task.execute(map);
							}
						})
						.setNegativeButton("取消",null).create().show();
					return false;
				}
			});
		}else{
			lv.setOnItemLongClickListener(null);
		}
		((Button)findViewById(R.id.idorder)).setEnabled(true);
	}

	public static String md5(String string) {
	    byte[] hash;
	    try {
	        hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("Huh, MD5 should be supported?", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new RuntimeException("Huh, UTF-8 should be supported?", e);
	    }

	    StringBuilder hex = new StringBuilder(hash.length * 2);
	    for (byte b : hash) {
	        if ((b & 0xFF) < 0x10) hex.append("0");
	        hex.append(Integer.toHexString(b & 0xFF));
	    }
	    return hex.toString();
	}
	
	public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}
	
	protected void searchSinger(String search) {
		String request;
		try {
			request = Base64.encodeBytes(("{\"firstSize\":0,\"type\":1,\"common\":{\"clientversion\":\"1.9.3\",\"model\":\"sdk\",\"imei\":\"000000000000000\",\"userid\":0,\"resolution\":\"1196X720\",\"apiversion\":\"1.9.3\",\"product\":\"KALAOK\",\"clienttype\":\"Android\",\"nettype\":\"epc.tmobile.com\",\"updatechannel\":\"37\",\"login\":0,\"language\":1,\"imsi\":\"89014103211118510720\",\"systemversion\":\"17\",\"channel\":\"YYH\"},\"maxSize\":300,\"keyWord\":\""+search+"\"}").getBytes("UTF-8"));
			String sign=md5(request+"1731c73ef747457e8ac6f2ddb7de9227087e337ee96b4545b71edd50ea79d367");
			String url="http://sns.audiocn.org/tlcysns/content/search.action?request="+request+"&sign="+sign+"&type=52f78ffbda1e416e";
			new HttpConnection(this,new SearchSingerHandler(this)).post(url, "");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	protected void searchSong(String search) {
		String request;
		try {
			request = Base64.encodeBytes(("{\"firstSize\":0,\"type\":2,\"common\":{\"clientversion\":\"1.9.3\",\"model\":\"sdk\",\"imei\":\"000000000000000\",\"userid\":0,\"resolution\":\"1196X720\",\"apiversion\":\"1.9.3\",\"product\":\"KALAOK\",\"clienttype\":\"Android\",\"nettype\":\"epc.tmobile.com\",\"updatechannel\":\"37\",\"login\":0,\"language\":1,\"imsi\":\"89014103211118510720\",\"systemversion\":\"17\",\"channel\":\"YYH\"},\"maxSize\":300,\"keyWord\":\""+search+"\"}").getBytes("UTF-8"));
			String sign=md5(request+"1731c73ef747457e8ac6f2ddb7de9227087e337ee96b4545b71edd50ea79d367");
			String url="http://sns.audiocn.org/tlcysns/content/search.action?request="+request+"&sign="+sign+"&type=52f78ffbda1e416e";
			new HttpConnection(this,new SearchSongHandler(this)).post(url, "");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
}
