package im.kkbb.me.kuaisync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.kuaipan.client.KuaipanAPI;
import com.kuaipan.client.ProgressListener;
import com.kuaipan.client.exception.KuaipanAuthExpiredException;
import com.kuaipan.client.exception.KuaipanException;
import com.kuaipan.client.exception.KuaipanIOException;
import com.kuaipan.client.exception.KuaipanServerException;
import com.kuaipan.client.model.AccessToken;
import com.kuaipan.client.model.TokenPair;
import com.kuaipan.client.session.OauthSession;

public class Main {
	private Main() {}
	private static KuaipanAPI api = null;
	private static boolean initing=false;
	public static synchronized KuaipanAPI getInstance() throws Exception {
		if (api == null) {
			Config.init();
			if (Config.CONSUMER_KEY.isEmpty() || Config.CONSUMER_SECRET.isEmpty()) {
				System.err.println("请先在 com.kuaipan.test.KPTestUtility 中设置你的consumer_key 和 consumer_secret。");
				System.exit(1);
			}
			
			OauthSession session = new OauthSession(Config.CONSUMER_KEY, 
					Config.CONSUMER_SECRET, OauthSession.Root.APP_FOLDER);
			api = new KuaipanAPI(session);
			
			AccessToken t = loadAuthFile();
			if (t != null) {
				api.getSession().setAuthToken(t.key, t.secret);

				try {
					api.accountInfo();
					return api;
				} catch (KuaipanException e) {
					api.getSession().unAuth();
				} 
			}
			if(!initing){
				System.out.println("api授权过期，请初始化！");					
				return null;
			}
			
			try {
				String url = api.requestToken();
				System.out.println("到以下网址中使用你的快盘帐号授权，完成后按下ENTER键：\n" + url);					
				
				try {
					System.in.read();
				} catch (IOException e) {}
				
				api.accessToken();
				
				if (api.getSession().isAuth())
					saveAuthFile(api.getSession().token);
				
				System.out.println("成功，有效期1年" );	
			} catch (KuaipanException e) {
				e.printStackTrace();
				return null;
			}

		}		
		return api;
	}
	
	
	private static AccessToken loadAuthFile() {
        File file=new File(Config.AUTH_FILE_PATH);
        if(!file.exists())
            return null;
        
        BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			return null;
		}
		
		String key = null;
		String secret = null;
		try {
			key = br.readLine();
			secret = br.readLine();
		} catch (IOException e) {
			return null;
		}
		
		if (key == null || secret == null)
			return null;
		
		return new AccessToken(key, secret);
	}
	
	
	private static boolean saveAuthFile(TokenPair token) {
		FileWriter f = null;		
		try {
			f = new FileWriter(Config.AUTH_FILE_PATH, false);
			f.write(token.key);
			f.write('\n');
			f.write(token.secret);		
			f.flush();
		} catch (IOException e) {
			return false;
		} finally {
			if (f != null)
				try {
					f.close();
				} catch (IOException e) {}
		}
		return true;
	}
	
	
	public static void main(String[] args) 
			throws KuaipanIOException, KuaipanServerException, KuaipanAuthExpiredException, FileNotFoundException {		
		if(args.length<1){
			initing=true;
		}
		try {
			api = getInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		System.out.println(api.accountInfo());
		
		if(args.length<=0){
			return;
		}
		
		String uploadfile=null;
		if("upload".equalsIgnoreCase(args[0])){
			File f=new File(args[1]);
			if(!f.exists()){
				System.out.println("源文件不存在");
				return;
			}
			ProgressListener lr=null;//new MyProgressListener();
			long size=f.length();
			InputStream is=new FileInputStream(f);
			File file = new File(args[2]);			
			String parentDirName = file.getParent(); 
			if(parentDirName!=null){
				parentDirName=parentDirName.replace('\\', '/');
				if(!Config.isExisted(parentDirName, api)){
					System.out.println("Create "+parentDirName);				
					api.createFolder(parentDirName);
				}
			}
			System.err.println("开始上传\n");	
			api.uploadFile(args[2], is, size, true, lr);
			System.err.println("上传完成\n");	
		}
	}
	static class MyProgressListener implements ProgressListener {
		@Override
		public void completed() {
			System.err.println("\n完成100%");					
		}
		@Override
		public int getUpdateInterval() {
			return 0;
		}
		@Override
		public void processing(long arg0, long arg1) {
			float rate=(float)arg0/(float)arg1;
			int count=(int)(50*rate);
			StringBuffer sb=new StringBuffer();
			sb.append("[");
			for(int i=0;i<count;++i){
				sb.append("=");
			}
			sb.append('>');
			while(sb.length()<52)sb.append(' ');
			sb.append("]");			
			sb.append(""+arg0+"/"+arg1);
			sb.append("="+(int)(rate*100)+"%");
			System.err.print("\r"+sb);;					
		}
		@Override
		public void started() {
			System.err.println("开始\n");	
		}};
}
