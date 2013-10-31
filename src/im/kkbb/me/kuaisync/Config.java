package im.kkbb.me.kuaisync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import com.alibaba.fastjson.JSONObject;
import com.kuaipan.client.KuaipanAPI;
import com.kuaipan.client.exception.KuaipanAuthExpiredException;
import com.kuaipan.client.exception.KuaipanIOException;
import com.kuaipan.client.exception.KuaipanServerException;
import com.kuaipan.client.hook.SleepyProgressListener;

public class Config {
	public static void init() throws Exception{
		java.io.File cfile=new java.io.File(CONFIG_FILE_PATH);
		if(!cfile.exists()){
			System.err.println("Config file not exist");
			throw new Exception();
		}
		 FileInputStream fis = new FileInputStream(cfile);
	    byte[] data = new byte[(int)cfile.length()];
	    fis.read(data);
	    fis.close();
	    //
	    String s = new String(data, "UTF-8");
		JSONObject o=com.alibaba.fastjson.JSON.parseObject(s);
		CONSUMER_KEY=o.getString("CONSUMER_KEY");
		CONSUMER_SECRET=o.getString("CONSUMER_SECRET");
	}
	private Config() {}
	
	public static String CONSUMER_KEY = "";
	public static String CONSUMER_SECRET = "";
	
	public final static String USERNAME = "";
	public final static String PASSWROD = "";
	
	public final static String AUTH_FILE_PATH = "myauth.json";
	public final static String CONFIG_FILE_PATH = "config.json";
	
	private static final String RANDOM_SAMPLE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	/**
	 * generate some random bytes stream.
	 * @param size 
	 * 			do not be too large or it may eat up all your memory.
	 * @return
	 */
	public static String generateByteString(int size) {
		StringBuffer buf = new StringBuffer();
		Random random = new Random();
		
		int MAX_LEN = RANDOM_SAMPLE.length();
		for (int i = 0; i < size; i++) {
			buf.append(RANDOM_SAMPLE.charAt(random.nextInt(MAX_LEN)));
		}
		return buf.toString();
	}
	
	public static InputStream generateByteStream(int size) {
		InputStream is = new ByteArrayInputStream(generateByteString(size).getBytes());
		return is;
	}
	
	public static long randomSize() {
		Random random = new Random();
		return random.nextInt(10000) + 200L;
	}
	
	public static String outputStream2String(ByteArrayOutputStream os) {
			String result = null;
			try {
				result = new String(os.toByteArray(), "UTF-8");	
			} catch (UnsupportedEncodingException e) {
				// bug??
				result = new String(os.toByteArray());
			} 
			return result;
	}
	
	public static String upload(KuaipanAPI api, String path) 
			throws KuaipanIOException, KuaipanServerException, KuaipanAuthExpiredException {
		return upload(api, path, randomSize());
	}
	
	public static String upload(KuaipanAPI api, String path, long size) 
			throws KuaipanIOException, KuaipanServerException, KuaipanAuthExpiredException {
		long size_before = Config.randomSize();
		String upload_content = Config.generateByteString((int)size);
		InputStream is = new ByteArrayInputStream(upload_content.getBytes());
		api.uploadFile(path, is, size_before, true, new SleepyProgressListener());
		try {
			is.close();
		} catch (IOException e) {}
		return upload_content;
	}
	
	public static boolean isExisted(String path, KuaipanAPI api) 
			throws KuaipanIOException, KuaipanAuthExpiredException {
		try {
			api.metadata(path, null);
		} catch (KuaipanServerException e) {
			if (e.code == 404 && e.raw.contains("file not exist"))
				return false;
		}
		return true;
	}
	
	public static void openBrowser(String url) {
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
		}
		try {
			java.awt.Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
		}
	}
}
