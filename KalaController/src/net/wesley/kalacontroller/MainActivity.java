package net.wesley.kalacontroller;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final Integer DEFAULT_SERVER_PORT = 4004;
	public DatagramSocket mSocket;
	private byte[] bytes=new byte[1024];
	private DatagramPacket mPacket=new DatagramPacket(bytes, bytes.length);
	private String currpeer="";
	ArrayList<HashMap<String,String>> remotePlayList=new ArrayList<HashMap<String,String>>();
	
	class SendUdpCmdTask extends AsyncTask<String,Void,String>{
		private String cmd;

		@Override
		protected String doInBackground(String... arg0) {
			this.cmd=arg0[0];
			try {
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setSoTimeout(1000);
				String string=arg0[0]+" ";
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName(currpeer);
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, DEFAULT_SERVER_PORT); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String curPeer = mPacket.getAddress().toString().substring(1);
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					return curPeer+" "+cnt;
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
	        String[] params = result.split(" ",2);
	        if (params.length==2){
	        	if (this.cmd.trim().split(" ")[0].equalsIgnoreCase("list")){
					processList(params[1]);
	        	}
	        }
	    }

	}
	
	class LookForPeerTask extends AsyncTask<Integer,Void,String>{
		@Override
		protected String doInBackground(Integer... params) {
			try {
				DatagramSocket mSocket = new DatagramSocket();
				mSocket.setBroadcast(true);
				mSocket.setSoTimeout(1000);
				String string="LIST "+SystemClock.elapsedRealtime();
				byte[] bytes=string.getBytes();
				for (int i=0;i<3;i++){
					InetAddress addr = InetAddress.getByName("255.255.255.255");
					DatagramPacket dPacket = new DatagramPacket(bytes, 
							bytes.length,addr, params[0]); 
					mSocket.send(dPacket);
					try{
						mSocket.receive(mPacket);
					}
					catch (SocketTimeoutException e){
						continue;
					}
					String curPeer = mPacket.getAddress().toString().substring(1);
					String cnt=new String(mPacket.getData(),0, mPacket.getLength(),"UTF-8");
					return curPeer+" "+cnt;
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
	        String[] params = result.split(" ",2);
	        if (params.length==2){
	        	currpeer=params[0];
				((Button)findViewById(R.id.idorder)).setEnabled(true);
				Toast.makeText(MainActivity.this, "在地址 "+currpeer+ " 找到播放器!", Toast.LENGTH_SHORT).show();
				processList(params[1]);
	        }
	        else{
	        	Toast.makeText(MainActivity.this, "找不到播放器!", Toast.LENGTH_LONG).show();
	        }
	    }

	}
	
	public void processList(String list) {
		remotePlayList.clear();
		String[] songs = list.trim().split(";");
		for (String song:songs){
			String[] infos = song.trim().split(" ");
			if (infos.length==3){
				HashMap<String,String> map=new HashMap<String,String>();
				map.put("id", infos[0]);
				map.put("name", infos[1].toString().replace("___", " "));
				map.put("artist", infos[2].toString().replace("___", " "));
				remotePlayList.add(map);
			}
		}
		
		ListView lv=(ListView) this.findViewById(R.id.idremotelist);
		lv.setAdapter(new SimpleAdapter(this,
				remotePlayList, R.layout.songrow,
				new String[] { "name", "artist"}, new int[] {
						R.id.listrow_name, R.id.listrow_artist}));

		
	}

	@Override
	protected void onResume(){
		super.onResume();
		if (currpeer.length()>0){
			SendUdpCmdTask task=new SendUdpCmdTask();
			task.execute("LIST "+SystemClock.elapsedRealtime());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if(null != savedInstanceState){
			currpeer=savedInstanceState.getString("peer");
		}
		
		enableSocketOnMainUI();
		
		((Button)this.findViewById(R.id.idorder)).setEnabled(false);
		((Button)this.findViewById(R.id.idorder)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent i=new Intent(MainActivity.this,OrderActivity.class);
				i.putExtra("peer", currpeer);
				startActivity(i);
			}
		});
		
		((Button)this.findViewById(R.id.idplaypause)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendUdpCmdTask task=new SendUdpCmdTask();
				task.execute("PAUSE"+" "+SystemClock.elapsedRealtime());
			}
		});
		
		((Button)this.findViewById(R.id.idkalaok)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendUdpCmdTask task=new SendUdpCmdTask();
				task.execute("KALAOK"+" "+SystemClock.elapsedRealtime());
			}
		});

		((Button)this.findViewById(R.id.idremotequit)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendUdpCmdTask task=new SendUdpCmdTask();
				task.execute("QUIT");
			}
		});

		((Button)this.findViewById(R.id.idnext)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendUdpCmdTask task=new SendUdpCmdTask();
				task.execute("NEXT");
			}
		});
		
		((Button)this.findViewById(R.id.idremoteupdate)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendUdpCmdTask task=new SendUdpCmdTask();
				task.execute("LIST "+SystemClock.elapsedRealtime());
			}
		});

		if (currpeer.length()<=0){
			LookForPeerTask task=new LookForPeerTask();
			task.execute(DEFAULT_SERVER_PORT);
		}
		
	}
	
	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("peer", currpeer);
	}
	
	protected void broadcastString(String string) {
		byte[] bytes=string.getBytes();
		try {
			InetAddress addr = InetAddress.getByName("255.255.255.255");
			DatagramPacket dPacket = new DatagramPacket(bytes, 
					bytes.length,addr, 4004); 
			mSocket.send(dPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void enableSocketOnMainUI() {
		if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 9) {
		    try {
		        // StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
		           Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, Thread.currentThread()
		                        .getContextClassLoader());
		           Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, Thread.currentThread()
		                        .getContextClassLoader());
		           Field laxField = threadPolicyClass.getField("LAX");
		           Method setThreadPolicyMethod = strictModeClass.getMethod("setThreadPolicy", threadPolicyClass);
		                setThreadPolicyMethod.invoke(strictModeClass, laxField.get(null));
		    } 
		    catch (Exception e) { }
		}
	}


	@Override 
	public void onBackPressed(){
		try{
			mSocket.close();
		}
		catch (Exception e){
			
		}
		super.onBackPressed();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
