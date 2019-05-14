package jim.server;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

class UsersList extends Thread{
	
	private ArrayList<User> registeredUserList;

	UsersList(){
		registeredUserList = new ArrayList<User>();
		update();
	}
	//====================================================//
	//	註冊
	//====================================================//
	int register(String name, UUID id, byte[] ip){
		User user = getUser(id);
		name = (name.length()>18)? name.substring(1, 18) : name; 
		if(user == null){//新使用者，註冊
			if(getSize() >= ServerMain.MAX_CLIENT)
				return -2;//超過上限
			else{
				ServerMain.sqlClient.insert(name, id, ip);
				ServerMain.log("登記新使用者：" + name);
			}				
		}else if(!name.equals(user.userName)){//名稱不同
			ServerMain.sqlClient.updateName(user.userID, name);
			ServerMain.log("更新使用者：" + user.userName + " 為   " + name); 
		}else if(!user.compareIP(ip)){
			ServerMain.sqlClient.updateIP(user.userID, ip);
			ServerMain.log("更新使用者：" + user.userName + " IP:" + user.getIP());
		}
		
		update();
		ServerMain.updateUser();
		return getuserID(id); 
	}	
	//====================================================//
	//	更新List
	//====================================================//
	void update(){
		//TODO 檢查
		ResultSet rs = ServerMain.sqlClient.getUserList();
		registeredUserList.clear();
		try {
			while(rs.next()){					
				User user = new User(rs.getShort(1), rs.getString(2), rs.getBytes(3), rs.getBytes(4));
				registeredUserList.add(user);
//				ServerMain.log(String.valueOf(user.userID) + " "+ user.getIP());
			}
			rs.close();
		} catch (SQLException e) {
			ServerMain.log("更新UsersList失敗", Color.RED);
			ServerMain.log(e.toString());
			e.printStackTrace();
		}
//		ServerMain.log("更新UsersList成功");
	}
	
	
	//====================================================//
	//	獲得User
	//====================================================//
	User[] getAllUsers(){
		int size = registeredUserList.size()-1;
		User[] list = new User[size];
		for(int i = 0; i < size; i++){
			list[i] = registeredUserList.get(i+1);
		}
		return list;
	}
	
	User getUser(int index){
		if(index == -1)//未註冊
			return null;
		for(User user:registeredUserList){
			if(user.userID == index)
				return user;
		}
		return null;//查詢失敗
	}
	
	User getUser(UUID id){
		for(User user:registeredUserList){
			if(user.uuid.equals(id))
				return user;
		}
		return null;//查詢失敗
	}
	//====================================================//
	//	獲得userID
	//====================================================//
	int getuserID(UUID id){
		User user = getUser(id);
		return (user==null)? -1 : user.userID; 
	}
	int getAdminID(){
		User user = getUser(UUID.fromString(Client.ADMINUUID));
		return (user==null)? -1 : user.userID;
	}
	//====================================================//
	//	獲得UserName
	//====================================================//
	String getUserName(int index){
		User user = getUser(index);
		return (user==null)? null : user.userName; 
	}
	String getUserName(UUID id){
		User user = getUser(id);
		return (user==null)? null : user.userName;
	}
	//====================================================//
	//	獲得UUID
	//====================================================//
	UUID getUUID(int index){
		User user = getUser(index);
		return (user==null)? null : user.uuid;
	}
	//====================================================//
	//	獲得latest IP
	//====================================================//
	byte[] getLatestIP(int index){
		User user = getUser(index);
		return (user==null)? null : user.latestIP;
	}
	String getLatestHostIP(int index){
		User user = getUser(index);
		return (user==null)? null : user.getIP();
	}
	
	int getSize(){
		return registeredUserList.size();
	}
	
	
	
//========================================================//
//	內層類別：User
//========================================================//
	class User{
	//====================================================//
	//	資料成員
	//====================================================//
		private int userID;
		private String userName;
		private UUID uuid;
		private byte[] latestIP;
	//====================================================//
	//	建構子
	//====================================================//		
		User(int index, String name, byte[] id, byte[] ip){
			userID = index;
			userName = name;
			ByteBuffer bb = ByteBuffer.allocate(16);
			for(int i = 0; i < 16; i++)
				bb.put(id[i]);
			uuid = new UUID(bb.getLong(0), bb.getLong(8));			
			latestIP = ip;					
		}
	//====================================================//
	//	獲得ID
	//====================================================//
		int getID(){
			return userID;
		}
	//====================================================//
	//	獲得名稱
	//====================================================//
		String getName(){
			return userName;
		}
	//====================================================//
	//	IP轉十進位字串
	//====================================================//
		String getIP(){
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(0);
			String s = String.valueOf((bb.put(3, latestIP[0]).getInt(0)));
			for(int i = 1; i < 4; i++){
				s += ".";
				s += String.valueOf((bb.put(3, latestIP[i]).getInt(0)));
			}
			return s;			
		}
	//====================================================//
	//	比較IP
	//====================================================//		
		boolean compareIP(byte[] ip){
			return ByteBuffer.wrap(ip).equals(ByteBuffer.wrap(latestIP));
		}
		
		boolean compareIP(String ip){
			return ip.equals(getIP());	
		}
	}
	
	
	
}
