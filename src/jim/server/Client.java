package jim.server;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;

import jstudio.fallDetector.DataSheet;

class Client extends Thread{
	
	static final String ADMINUUID = "839a56f1-d603-445c-8f94-dee982bb7c3d";
	final int PFALL = 42450;
	final int PDATA = 50424;
	final int REG_TIMEOUT = 10;	//10秒
	
	protected Socket socket;
	protected String ip, userName;
	protected int userID;
	boolean registered = false;
	boolean admin = false;
	
	private Hashtable<Long, SupportVectors.Data> report;
	
//===================================================================================//
//	私有成員讀取
//===================================================================================//
	Socket getSocket(){	return socket;}
	String getIp(){	return ip;}	
	int getIndex(){ return userID;}	
	int getPort(){ return socket.getPort();}
//===================================================================================//
//	使用者資料
//===================================================================================//		
	UUID getUUID(){	return ServerMain.usersList.getUUID(userID);}	
	String getUserName(){ return ServerMain.usersList.getUserName(userID);}
//===================================================================================//
//	IP轉字串
//===================================================================================//
	static String convertIP(byte[] ip){
		String s = String.valueOf(ip[0]);
		for(int i = 1; i < 4; i++){
			s += ".";
			s += String.valueOf(ip[i]);
		}
		return s;
	}	
//===================================================================================//
//	建構子及Thread實作
//===================================================================================//
	Client(Socket socket){
		this.socket = socket;
		userID = -1;
		ip = socket.getInetAddress().getHostAddress();
		report = new Hashtable();
		new Thread(new Runnable(){
			long time = System.currentTimeMillis();
			@Override
			public void run() {
				while(System.currentTimeMillis()-time < REG_TIMEOUT*1000){//等待註冊
					if(registered){
						
						return;
					}
						
				}
				dispose();//等註冊超過TIMEOUT
			}			
		}).start();
	}	

	@Override
	public void run() {
		String  usermsg;		
		try {
			BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
			while ((usermsg = inputBuffer.readLine()) != null){//監聽主要port
//				ServerMain.log("[Client" + getIndex() + " says]：" + usermsg, Color.GRAY);
				String command = usermsg.replaceFirst("[/]", "");
				if(usermsg.matches("/msg.*")){						//判斷是否為正式訊息
					ServerMain.log("[Client" + getIndex() + " says]：" + usermsg.replaceFirst("/msg[\\s]", ""));	
				}else if(usermsg.matches("/.*")){					
					interpreter(command.split("\\s"));
				}else if (admin){
					new AdminInThread().start();
					return;
				}else
					ServerMain.log("[Client" + userID + "]：" + usermsg);	//一般訊息		
			}					
			dispose();//釋放此物件	
		}catch(IOException e) {System.out.println("Client:106 " + e.toString());}	//錯誤訊息
	}	
	
//===================================================================================//
//	釋放物件
//===================================================================================//	
	protected void dispose() {
		//關閉客戶端  關閉socket unregister
		String s = (userName == null)? getIp() : userName;
		ServerMain.log(s + " 失去連線", Color.RED);
		try{
			socket.close();
		}catch(IOException i){
			ServerMain.log(userName + "關閉失敗!");
		}		
		ServerMain.unregisterClient(this);
	}

//===================================================================================//
//	要求解釋器
//===================================================================================//	
	String interpreter(String[] request){
		try{
			if(request[0].equals("register")){
			if(request.length != 3){
				ServerMain.log("命令參數錯誤");
				return "";
			}else
				rst_register(request[1], request[2]);//註冊：[UUID] [使用者名稱]					
			}else if(request[0].equals("fall")){
				if(request.length != 2){
					ServerMain.log("命令參數錯誤");
					return "";
				}else
					rst_fall(request[1]);//跌倒：[data起始時間]
			}else if(request[0].equals("data")){
				if(request.length != 3){
					ServerMain.log("命令參數錯誤");
					return "";
				}else
					rst_data(request[1], request[2]);//data：[data起始時間] [data結束時間] 
			}else if(request[0].equals("return")){
				if(request.length != 3){
					ServerMain.log("命令參數錯誤");
					return "";
				}else
					rst_return(request[1], request[2]);//回報：[跌倒時間] [是否跌倒]		
			}
		}catch(ArrayIndexOutOfBoundsException a){
			ServerMain.log("命令參數錯誤");
			return "";
		}		
		return request[0];
	}
//===================================================================================//
//	命令
//===================================================================================//	
	void sendCommand(String cmd){
		try{
			PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), false);// 指定輸出編碼為UTF-8
			outputWriter.println(cmd);
			outputWriter.flush();
			ServerMain.log("[cmd client" + getIndex() + "]：" + cmd);
		}catch(IOException f){
			ServerMain.log(f.toString());
			ServerMain.log("發送命令失敗");			
		}
	}	
//===================================================================================//
//	要求(註冊)
//===================================================================================//	
	void rst_register(String uuid, String name){
		try{
			userID = ServerMain.usersList.register(name, UUID.fromString(uuid), socket.getInetAddress().getAddress());
			if(uuid.equals(ADMINUUID)){
				if (ServerMain.registerAdmin(userID, ip, this)){
					userName = "Admin";
					registered = true;
					admin = true;
					ServerMain.log("與Admin連接成功！", Color.RED);
				}else
					dispose();				
			}else{								
				if(userID > -1){
	//				ServerMain.log("註冊成功！Client" + getIndex() + "：" + name + " " + uuid);
					userName = ServerMain.usersList.getUserName(userID);
					ServerMain.refreshClientList(this);
					registered = true;
				}else if(userID == -2){
					ServerMain.log("超過使用者上限！");
					dispose();
				}else{
					ServerMain.log("註冊失敗！Client" + getIp());
					dispose();
				}

					
									
			}
		}catch(IllegalArgumentException i){ServerMain.log("註冊失敗！UUID格式不正確");
		}
	}
//===================================================================================//
//	要求(傳送fallMsg)
//===================================================================================//	
	void rst_fall(String start){
		int port = PFALL;
		while(port < PDATA){
			try{
				ServerSocket serverSocket = new ServerSocket(port);
				if(serverSocket != null){
					new MsgSendThread("/accept " + start + " " + port).start();
					new DataAcceptThread(serverSocket);
					break;
				}
			}catch(BindException b){//傳輸中
				port++;
			}catch(IOException i){
				i.printStackTrace();
				break;
			}							
		}				
	}
//===================================================================================//
//	要求(傳送data)
//===================================================================================//	
	void rst_data(String start, String end){
		int port = PDATA;
		while(port < 65536){
			try{
				ServerSocket serverSocket = new ServerSocket(port);
				if(serverSocket != null){
					new MsgSendThread("/accept " + start + " " + port).start();
					new DataAcceptThread(serverSocket);
					break;
				}
			}catch(BindException b){//傳輸中
				port++;
			}catch(IOException i){
				i.printStackTrace();
				break;
			}							
		}		
	}
//===================================================================================//
//	要求(回報跌倒結果)
//===================================================================================//	
	void rst_return(String time, String result){
		long fallTime = Long.parseLong(time);
		SupportVectors.Data data = report.get(fallTime);
		if(data != null){
			data.result = Boolean.parseBoolean(result);
			data.saveJson();
			ServerMain.log("更新回報結果： " + data.fileName + " " + result);
			ServerMain.sqlClient.updateFall(userID, fallTime, Boolean.parseBoolean(result));
		}else
			System.out.println("data為 null(" + time + ")");
		
	}
//===================================================================================//
//	SVM分析
//===================================================================================//
	private void svm_predict(long time, double la, double oA, double max1, double min1, double eq2, double deC, long start, long end) {

	}
	
	//向量絕對值
	private double abs(float x, float y, float z){			
		double[] dv = new double[3];
		dv[0] = new Float(x).doubleValue();
		dv[1] = new Float(y).doubleValue();
		dv[2] = new Float(z).doubleValue();
		return Math.sqrt(Math.pow(dv[0], 2) + Math.pow(dv[1], 2) + Math.pow(dv[2], 2));
	}
	
	//向量投影
	private float projection(float ax, float ay, float az, float bx, float by, float bz){
		return Double.valueOf(dot(ax, ay, az, bx, by, bz)/abs(bx, by, bz)).floatValue();
	}
		
	//向量內積
	private float dot(float ax, float ay, float az, float bx, float by, float bz){
		return ax*bx + ay*by + az*bz;		
	}
	
	//向量內積
	private float dot(float[] a, float[] b){
		return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];		
	}
//===================================================================================//
//	Inner Class: Msg發送Thread
//===================================================================================//
	class MsgSendThread extends Thread{
		private String msg = null;
		
		MsgSendThread(String msg){
			this.msg = msg;
		}
		
		@Override
		public void run() {
			if(socket != null){
				try{
					PrintWriter writer = new PrintWriter(new BufferedWriter(
									new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
					 writer.println(msg);
	                 writer.flush();
	                 System.out.println("Send to " + userName + ": " + msg);
				}catch(IOException i){
					i.printStackTrace();
				}
			}
		}		
	}
//===================================================================================//
//	Inner Class: data接收Thread
//===================================================================================//
	private class DataAcceptThread extends Thread{
		
		private final int TIMEOUT = 10000;	//10秒		
		private ServerSocket dataServerSocket;
		private Socket dataSocket;
		
		private DataAcceptThread(ServerSocket serverSocket) {
			super();
			long startTime = System.currentTimeMillis();
			while(System.currentTimeMillis()-startTime < TIMEOUT){
				try{
					dataServerSocket = serverSocket;
					dataServerSocket.setSoTimeout(TIMEOUT);
					dataSocket = dataServerSocket.accept();					
					break;
				}catch(SocketTimeoutException t){
					ServerMain.log("與" + userName + "data連線逾時", Color.RED);
					return;
				}catch(IOException i){
					i.printStackTrace();;
				}
			}
			if(dataSocket.isConnected())
				start();
		}
		
		@Override
		public void run(){
			try{
				dataServerSocket.close();
				if(dataSocket != null){
					long uploadTime = System.currentTimeMillis();
					ServerMain.onDataReceive(Client.this);
					ObjectInputStream inputStream = new ObjectInputStream(dataSocket.getInputStream());
					DataSheet dataSheet = (DataSheet) inputStream.readObject();
					if(dataSheet.getFallTime() == -1){//-1表示不是跌倒，是定時上傳的資料
						store(dataSheet, uploadTime);
					}else{
						ServerMain.onDataComplete(Client.this);
						predict(dataSheet);
					}
					inputStream.close();
					dataSocket.close();					
				}else
					ServerMain.log("接收data失敗", Color.RED);
			}catch(IOException i){
				ServerMain.log(i.toString());
			}catch(ClassNotFoundException c){
				ServerMain.log(c.toString());
			}
		}
		
		private void store(final DataSheet dataSheet, final long uploadTime){
			new Thread(new Runnable(){
				@Override
				public void run() {
					for(DataSheet.Data data : dataSheet.vector())
						ServerMain.sqlClient.insert(userID, data.time, data.ac(), data.gv());
					String date = new SimpleDateFormat("MM/dd HH:mm:ss.SSS").format(dataSheet.getStart());
					String uploadCost = "共花費 " + (System.currentTimeMillis()-uploadTime)/1000 + " 秒";
					ServerMain.log("收到並儲存 "+userName+" " + dataSheet.period()/1000 + "秒的資料 (From:" + date + ") " + uploadCost);
					ServerMain.onDataComplete(Client.this);
					//儲存完才通知Client
					new MsgSendThread("/receive " + dataSheet.getStart() + " " + dataSheet.getEnd()).start();
				}				
			}).start();
		}
		
		private void predict(DataSheet dataSheet){//計算feature及呼叫SVM
			double la = Double.MIN_VALUE;
			double oA = Double.MIN_VALUE;
			double max1 = Double.MIN_VALUE;
			double min1 = Double.MAX_VALUE;
			double eq2 = 0;
			double deC = Double.MIN_VALUE;
			double[] dataSet = {};
			
			int eq1Count = 0;
			ArrayList<DataSheet.Data> buffer = new ArrayList<DataSheet.Data>();
			long fallTime = dataSheet.getFallTime(); //跌倒時間
			
			DataSheet.Data lastData = null;
			
			if(fallTime != -1){//時間有效
				System.out.println("計算data開始" + System.currentTimeMillis());
				String timeFormat = "[" + new SimpleDateFormat("HH:mm").format(new Date(fallTime))+ "] ";
				ServerMain.log(userName + " 偵測到跌倒!! at " + timeFormat, Color.RED);
				for(DataSheet.Data data : dataSheet.vector()){
					long time = data.time;
//					System.out.printf("%d, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f \n", time, data.ax, data.ay, data.az, data.gx, data.gy, data.gz);
//					System.out.printf("%d, %.4f, %.4f, %.4f, %.4f, %.4f \n", time, data.getla(), data.getoA(), data.acceleration, eq1, deC);
					if(time > (fallTime-500) && time < (fallTime+1500)){//0.5 ~ fallTime ~ 1.5 的範圍						
						//la, oA, max1, min1
						la = Math.max(la, data.getla());
						oA = Math.max(oA, data.getoA());
						max1 = Math.max(max1, data.acceleration);
						min1 = Math.min(min1, data.acceleration);
						
						//deC
						if(buffer.size() > 0 && buffer.get(0).time < (data.time - 125)){//序列不為空，且推出的data時間與目前的data差八分之一秒以上
							float[] last_gv = buffer.get(0).gv();
							deC = Math.max(Math.acos(dot(last_gv, data.gv()) / (9.81*9.81)) * (180 / Math.PI), deC);//角度
						}
						buffer.add(data);
						
						//eq2
						if(time < fallTime+1000){//0.5 ~ fallTime ~ 1.0 的範圍						
							if(lastData != null){//微分取絕對值
								eq2 += Math.abs((data.acceleration)-lastData.acceleration)/(data.time-lastData.time);
								eq1Count++;
							}
						}					
						lastData = data;
					}								
				}
				eq2 = (eq1Count == 0)? Double.NaN : eq2*1000/eq1Count;	//轉回單位成 g/s
				System.out.println("計算data結束" + System.currentTimeMillis());

				svm_predict(dataSheet.getFallTime(), la, oA, max1, min1, eq2, deC, dataSheet.getStart(), dataSheet.getEnd());
			}else
				System.out.println("時間無效");
		}		
	}
	
//===================================================================================//
//	Inner Class: ADMIN專用Thread
//===================================================================================//
	void sendLog(String log){
		new AdminOutThread(log).start();
	}
	
	private class AdminOutThread extends Thread{
		
		private String msg;
		private AdminOutThread(String msg){
			super();
			this.msg = msg;
		}
		
		@Override
		public void run(){
			try{
				PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
				writer.println("/log " + msg);
				writer.flush();
			}catch(IOException i){
				ServerMain.log(i.toString());
			}			
		}
	}
//===================================================================================//
//	Inner Class: ADMIN專用Thread
//===================================================================================//
	private class AdminInThread extends Thread{
		
		private final int SIZE = 32;//Long + Float*3 + Float*3
		private AdminInThread(){
			super();
		}
		
		@Override
		public void run(){
			try{
				int byteCount;
				byte[] dataBuffer = new byte[SIZE];
				BufferedInputStream inputBuffer = new BufferedInputStream(socket.getInputStream());
				while((byteCount = inputBuffer.read(dataBuffer, 0, SIZE)) != -1){/*讀取*/
					
				}
				dispose();//釋放此物件
			}catch(IOException i){
				ServerMain.log(i.toString());
			}			
		}
	}
}
