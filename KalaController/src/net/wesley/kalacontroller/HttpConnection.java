package net.wesley.kalacontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient; 
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Asynchronous HTTP connections
 *
 * @author Greg Zavitz & Joseph Roth
 */
public class HttpConnection implements Runnable {

	public static final int DID_START = 0;
	public static final int DID_ERROR = 1;
	public static final int DID_SUCCEED = 2;
	public static final int DID_PROGRESS = 3;

	private static final int GET = 0;
	private static final int POST = 1;
	private static final int PUT = 2;
	private static final int DELETE = 3;
	private static final int BITMAP = 4;
	private static final int FILE = 5;

	private String url;
	private int method;
	private Handler handler;
	private String data;

	private DefaultHttpClient httpClient = new DefaultHttpClient();
	private String encoding="";
	private String referer="";
	private int progressMode=0;
	private boolean cancelTag=false;

	public HttpConnection(Context ctx, Handler _handler) {
		handler = _handler;
	}

	public HttpConnection(Context ctx, Handler _handler, String string) {
		handler = _handler;
		encoding=string;
	}

	public HttpConnection(Context ctx, Handler _handler, String string,
			String string2) {
		handler = _handler;
		encoding=string;
		referer=string2;
	}

	public void create(int method, String url, String data) {
		this.method = method;
		this.url = url;
		this.data = data;
		ConnectionManager.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, null);
	}

	public void post(String url, String data) {
		create(POST, url, data);
	}

	public void put(String url, String data) {
		create(PUT, url, data);
	}

	public void delete(String url) {
		create(DELETE, url, null);
	}

	public void bitmap(String url,int inSampleSize) {
		create(BITMAP, url, inSampleSize+"");
	}

	public void file(String url,String path){
		create(FILE,url,path);
	}

	public void run() {
		handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
		//httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
		//httpClient.getCookieStore().getCookies(); 
		//httpClient.addResponseInterceptor( new ResponseProcessCookies() );
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 10000);
		try {
			HttpResponse response = null;
			HttpGet get;
			switch (method) {
			case GET:
				get=new HttpGet(url);
				if (this.referer.length()>0){
					get.setHeader("Referer", referer);
				}
				response = httpClient.execute(get);
				break;
			case POST:
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(new StringEntity(data));
				httpPost.setHeader("Content-Type","application/x-www-form-urlencoded");
				response = httpClient.execute(httpPost);
				break;
			case PUT:
				HttpPut httpPut = new HttpPut(url);
				httpPut.setEntity(new StringEntity(data));
				response = httpClient.execute(httpPut);
				break;
			case DELETE:
				response = httpClient.execute(new HttpDelete(url));
				break;
			case BITMAP:
				response = httpClient.execute(new HttpGet(url));
				processBitmapEntity(response.getEntity());
				break;
			case FILE:
				response = httpClient.execute(new HttpGet(url));
				processFileEntity(response.getEntity());
				break;
			}
			if (method < BITMAP)
				processEntity(response.getEntity());
		} catch (Exception e) {
			handler.sendMessage(Message.obtain(handler,
					HttpConnection.DID_ERROR, e));
		}
		ConnectionManager.getInstance().didComplete(this);
	}

	private void processFileEntity(HttpEntity entity) throws IOException {
		FileOutputStream out = new FileOutputStream(data);
		InputStream in=entity.getContent();
		int read=0;
		byte[] bytes = new byte[1024];
		int downloadedSize = 0;
		long totalSize=entity.getContentLength();
		while((read = in.read(bytes))!= -1){
			if (cancelTag){ 
				in.close();
				out.flush();
				out.close();
				handler.sendMessage(Message.obtain(handler, DID_ERROR, null));
				new File(data).delete();
				return;
			}
			out.write(bytes, 0, read);
			downloadedSize+=read;
			if (progressMode==0){
				Integer percent=(int)(downloadedSize*100/totalSize);
				if (percent>100) percent=100;
				if (percent<0) percent=0;
				handler.sendMessage(Message.obtain(handler, HttpConnection.DID_PROGRESS, percent));
			}
			else{
				handler.sendMessage(Message.obtain(handler, HttpConnection.DID_PROGRESS, downloadedSize));
			}
		}
		in.close();
		out.flush();
		out.close();
		handler.sendMessage(Message.obtain(handler, DID_SUCCEED, data));
	}

	private void processEntity(HttpEntity entity) throws IllegalStateException,
	IOException {
		BufferedReader br;
		if (encoding.length()>0){
			br = new BufferedReader(new InputStreamReader(entity
					.getContent(),encoding));
		}
		else{
			br = new BufferedReader(new InputStreamReader(entity
					.getContent()));
		}
		String line, result = "";
		while ((line = br.readLine()) != null)
			result += line+"\n";
		Message message = Message.obtain(handler, DID_SUCCEED, result);
		Bundle mybg=new Bundle();
		List<Cookie> cookies = httpClient.getCookieStore().getCookies();
		for (int i = 0; i < cookies.size(); i++) {
			mybg.putString(cookies.get(i).getName(), cookies.get(i).getValue());
        }
		message.setData(mybg);
		handler.sendMessage(message);
	}

	private void processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize=Integer.parseInt(data);
		Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent(),null,o2);
		handler.sendMessage(Message.obtain(handler, DID_SUCCEED, bm));
	}

	public void sethandler(Handler _handler) {
		handler = _handler;
	}

	public void setProgressMode(int m) {
		progressMode=m;
	}

	public void cancelDownload() {
		cancelTag=true;
	}
}
