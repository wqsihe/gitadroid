package net.wesley.gitadroia;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class LocallistActivity extends Activity {
	
	private ArrayList<HashMap<String,String>> locallist=new ArrayList<HashMap<String,String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.locallist);
		((Button)this.findViewById(R.id.btnsearch)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				EditText searchText=(EditText) findViewById(R.id.textinlocallist);
				searchText.setFocusable(true);
				searchText.setFocusableInTouchMode(true);
				

				Intent i=new Intent(LocallistActivity.this,SearchActivity.class);
				i.putExtra("search", searchText.getText().toString());
				startActivity(i);
			}
		});
		
		ListView lv=(ListView) findViewById(R.id.listinlocallist);
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Intent i=new Intent(LocallistActivity.this,ShowActivity.class);
				Log.e("", "arg2:"+arg2);
				i.putExtra("path", locallist.get(arg2).get("path"));
				startActivity(i);
			}
		});
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				final int pos=arg2;
				new AlertDialog.Builder(LocallistActivity.this).setTitle("提示")
					.setMessage("您确定要删除本地曲谱 "+locallist.get(arg2).get("song")+" 吗？")
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new File(locallist.get(pos).get("path")).delete();
							locallist.remove(pos);
							ListView lv=(ListView) findViewById(R.id.listinlocallist);
							lv.setAdapter(new SimpleAdapter(LocallistActivity.this,
									locallist, R.layout.songrow,
									new String[] { "song", "singer"}, new int[] {
											R.id.listrow_name, R.id.listrow_artist}));
						}
					})
					.setNegativeButton("取消", null).create().show();
				return false;
			}
		});
		
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		refreshLocalList();
	}

	private void refreshLocalList() {
		locallist.clear();
		File f=getApplicationContext().getFilesDir();
		File[] files = f.listFiles();
		for (File fi:files){
			if (fi.getAbsolutePath().endsWith(".gita")){
				DataInputStream in;
				try {
					in = new DataInputStream(new FileInputStream(fi));
					String song=in.readUTF();
					String singer=in.readUTF();
					HashMap<String,String>map=new HashMap<String,String>();
					map.put("song", song);
					map.put("path", fi.getAbsolutePath());
					map.put("singer", singer);
					locallist.add(map);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		ListView lv=(ListView) findViewById(R.id.listinlocallist);
		lv.setAdapter(new SimpleAdapter(this,
				locallist, R.layout.songrow,
				new String[] { "song", "singer"}, new int[] {
						R.id.listrow_name, R.id.listrow_artist}));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.locallist, menu);
		return true;
	}

}
