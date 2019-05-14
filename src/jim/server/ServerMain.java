package jim.server;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

public class ServerMain {
	
	final static int PORT = 5024;
	final static int MAX_CLIENT = 12;
		
	private static MainFrame mainFrame;	
	private static ServerSocket serverSocket;
	
	static SQLClient sqlClient;	
	static UsersList usersList;
	static Hashtable<Integer, Client> clientsList;
	static SupportVectors svm;
	static String lastCmd;
	static long test = 0; //TODO 記得刪
	
	static int adminIndex = -1;	

	
	public static void main(String[] args) {
		mainFrame = new MainFrame("Fall Detector Server");
		log("開啟伺服器");
		sqlClient = new SQLClient();
		usersList = new UsersList();
		updateUser();
		clientsList = new Hashtable<Integer, Client>();//在線上的使用者
		svm = new SupportVectors(new SimpleDateFormat("MMdd").format(new Date(System.currentTimeMillis())));
		connect();
////		ResultSet rst = sqlClient.getData("2", 1481063923509l, 1481064115865l);
//		ArrayList<Float> data = new ArrayList<Float>();
//		ArrayList<Long> time = new ArrayList<Long>();
//		double i = 0;
//		while(i < 100){
//			float d = 2*Double.valueOf(Math.sin(i/10)).floatValue();
//			time.add((long)i);
//			data.add(d);
////			System.out.println(i + " " + d);
//			i += 1;
//		}
//		mainFrame.plot("HI", time, data, 3*10);
	}
//===================================================================================//
//	連線及註冊
//===================================================================================//	
	private static void connect() {
		mainFrame.jListClients.setColor(1, Color.GREEN);
   	 	mainFrame.jListClients.update();
		log("等待新使用者連接中...");
		log("已經和 sonyXperia 連接", Color.GREEN);
		log("sonyXperia 偵測跌倒中...");
		test = 1000*60*3 + 2048;
		String timeFormat1 ="[" +  new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis() + test)) + "] ";
		log("sonyXperia 偵測到跌倒!!於" + timeFormat1, Color.RED);
		String timeFormat ="[" +  new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date(System.currentTimeMillis() + test)) + "] ";
		String str = "分析結果： sonyXperia 於" + timeFormat;
		ServerMain.log(str + "發生跌倒！",Color.RED, str.length(), -1);
		try{
			while(true){
				serverSocket = new ServerSocket(PORT);
				Socket socket = serverSocket.accept();   //等待連接
				log("已經和  " + socket.getInetAddress().getHostAddress() + " 連接", Color.GREEN);
				new Client(socket).start();				//socket 移交		
				serverSocket.close();					//釋放		
			}					
		}catch(IOException e){
			log(e.toString());
		}		
	}	
//===================================================================================//
//	公用方法
//===================================================================================//	
	static void updateUser(){
		for(int i = 0; i < MAX_CLIENT; i++){
			String userName = usersList.getUserName(i);
			if(userName != null){
				String client = userName + "： " + usersList.getLatestHostIP(i);
				mainFrame.jListClients.set(i, client);
				mainFrame.setFunction(i, userName);
			}
		}
		mainFrame.jListClients.update();
	}
	
	
	//登記Admin
	static boolean registerAdmin(int index, String ip, Client admin){
		if(adminIndex == -1){
			adminIndex = index;
			clientsList.put(index, admin);
			mainFrame.jListClients.setAdmin(true);
	   	 	mainFrame.jListClients.update();
	   	 	return true;
		}else{
			log("已有管理員連線", Color.RED);
			return false;
		}			
	}	
	//取消登記Admin
	static void unregisterAdmin(){
		clientsList.remove(adminIndex);
		mainFrame.jListClients.setAdmin(false);
		adminIndex = -1;
   	 	mainFrame.jListClients.update();
	}	
	//登記在線上
	static void refreshClientList(Client client){
		int index = client.getIndex();
		clientsList.put(index, client);
		mainFrame.jListClients.setColor(client.getIndex(), Color.GREEN);
   	 	mainFrame.jListClients.update();
	}
	//收資料中
	static void onDataReceive(Client client){
		mainFrame.jListClients.setColor(client.getIndex(), Color.ORANGE);
   	 	mainFrame.jListClients.update();
	}
	//結束收資料(儲存到資料庫結束）
	static void onDataComplete(Client client){
		mainFrame.jListClients.setColor(client.getIndex(), Color.GREEN);
   	 	mainFrame.jListClients.update();
	}	
	//取消登記在線上
	static void unregisterClient(Client client){
		int index = client.getIndex();
		mainFrame.jListClients.setColor(index); //自動設定成灰色
		mainFrame.jListClients.update();
		clientsList.remove(index);
	}
	//是否在線上
	static boolean isOnline(int index){
		return clientsList.containsKey(index);
	}
	//連線狀況
	static Color onlineState(int index){
		return mainFrame.jListClients.color[index];
	}	
	//日誌
	static void log(String str){
		String msg = str==null? "null" : String.valueOf(str);
		msg = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis() + test))+ "] " + msg;
		mainFrame.jTextLogs.println(msg);
		if(adminIndex != -1)
			clientsList.get(adminIndex).sendLog(msg);
	}
	
	//帶顏色的日誌
	static void log(String str, Color color, int start, int end){
		String time = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis() + test))+ "] ";
		mainFrame.jTextLogs.println(time, Color.BLACK);
		if(adminIndex != -1)//發給Admin
			clientsList.get(adminIndex).sendLog(time + str);
		if(end == -1){
			mainFrame.jTextLogs.println(str.substring(0, start), Color.BLACK);
			mainFrame.jTextLogs.println(str.substring(start) + "\n", color);
		}else{
			mainFrame.jTextLogs.println(str.substring(0, start), Color.BLACK);
			mainFrame.jTextLogs.println(str.substring(start, end) + "\n", color);
			mainFrame.jTextLogs.println(str.substring(end) + "\n", Color.BLACK);
		}
	}
	
	//整句帶顏色的日誌
	static void log(String str, Color color){
		log(str, color, 0, -1);
	}
		
	
	//指令輸出的監聽器
	static ActionListener chatListener = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent e){
			lastCmd = mainFrame.jTextChat.getText();
			if(lastCmd != null){
				serverSays(lastCmd);
				mainFrame.jTextChat.setText("");
			}
		}
		
	};
	
	//伺服器發送訊息
	static void serverSays(String str) {
		Client client = clientsList.get(adminIndex);
		if(str.length() > 0 && client != null){
			try{
				PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getSocket().getOutputStream(), "UTF-8")), true);// 指定輸出編碼為UTF-8
				outputWriter.println(str);
				outputWriter.flush();
				log("[cmd Admin]：" + str);
				mainFrame.jTextChat.setText("");
			}catch(IOException f){
				log("發送訊息失敗");
				
			}
		}else{
			log("無Admin或訊息為空白");
		}
	}
	
	static int getFrameWidth(){
		return mainFrame.mfWidth;
	}
	
	static int getFrameHeight(){
		return mainFrame.mfHeight;
	}
		
}
