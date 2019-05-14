package jim.server;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

import jstudio.fallDetector.DataSheet;

class SQLClient{	
//===================================================================================//
//	成員及建構子
//===================================================================================//
	private final String TABLE_USER = "dbo.FD_User ";
	private final String TABLE_DATA = "dbo.FD_Data ";
	private final String TABLE_FALL = "dbo.FD_Fall";
	private final String Connection = "jdbc:sqlserver://localhost:1433;" + 
									"databaseName=Fall Detector;" +
									"user=sa;" + 
									"password=SQLs4202;";	
	Connection con = null;
	
	SQLClient(){//註冊
		try{
			ServerMain.log("SQL Server連接中.........");
			con = DriverManager.getConnection(Connection);
			if(con != null)
				ServerMain.log("SQL連接成功");
		}catch(SQLException s){
			ServerMain.log(s.toString());
		}		
	}
//===================================================================================//
//	Transact-SQL
//===================================================================================//		
	private ResultSet query(String sql){
		ResultSet rs = null;
		try{
			Statement smt = con.createStatement();
			rs = smt.executeQuery(sql);			
			smt.closeOnCompletion();				//等待ResultSet關閉時一起關閉
		}catch(SQLException e) {
			ServerMain.log("SQL Query Error, SQL command = " + sql);
		}
		return rs;		
	}
	
	private int update(String sql){
		int result = 0;
		try{
			Statement smt = con.createStatement();
			result = smt.executeUpdate(sql);	
			smt.close();
		}catch(SQLException e) {
			ServerMain.log("SQL Update Error, SQL command = " + sql);
		}
		return result;	
	}
//===================================================================================//
//	Transact-SQL(Prepared)
//===================================================================================//
	//寫入data用
	private int update(String sql, short userID, long dateTime, float[] la, float[] g){//linear acceleration & gravity
		int result = 0;
		try{
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setShort(1, userID);
			ps.setLong(2, dateTime);
			ps.setFloat(3, la[0]);
			ps.setFloat(4, la[1]);	
			ps.setFloat(5, la[2]);
			ps.setFloat(6, g[0]);
			ps.setFloat(7, g[1]);	
			ps.setFloat(8, g[2]);
			ps.setLong(9, dateTime + userID*100000000000L);	//唯一識別欄位
			result = ps.executeUpdate();			
			ps.close();
		}catch(SQLException e) {				
			String log = sql.replaceFirst("\\?", String.valueOf(userID));
			String data = " "+ dateTime + " " + la[0] + " "  + la[1] + " " + la[2] + " " + g[0] + " " + g[1] + " " + g[2];
			if(e.getErrorCode() == 2601)//索引重複
				System.out.println(e.toString());
			else
				ServerMain.log("SQL Update Error, " + e.toString() + " SQL command = " + log + data);
		}
		return result;	
	}	
	//新增使用者資料用
	private int update(String sql, String userName, byte[] uuid, byte[] latestIP){
		int result = 0;
		try{
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userName);
			ps.setBytes(2, uuid);
			ps.setBytes(3, latestIP);			
			result = ps.executeUpdate();	
			ps.close();
		}catch(SQLException e) {
			String log = sql.replaceFirst("\\?", String.valueOf(userName));
			ServerMain.log("SQL Update Error, SQL command = " + log);
		}
		return result;	
	}	
	//更新LatestIP用
	private int update(String sql, short userIndex, byte[] latestIP){
		int result = 0;
		try{
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setBytes(1, latestIP);
			ps.setShort(2, userIndex);			
			result = ps.executeUpdate();	
			ps.close();
		}catch(SQLException e) {
			String log = sql.replaceFirst("\\?", String.valueOf(userIndex));
			ServerMain.log("SQL Update Error, SQL command = " + log);
		}
		return result;	
	}
	//更新UserName用
	private int update(String sql, short userIndex, String userName){
		int result = 0;
		try{
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userName);
			ps.setShort(2, userIndex);			
			result = ps.executeUpdate();	
			ps.close();
		}catch(SQLException e) {
			String log = sql.replaceFirst("\\?", String.valueOf(userIndex));
			ServerMain.log("SQL Update Error, SQL command = " + log);
		}
		return result;	
	}
	//加入跌倒事件用
	private int update(String sql, short userID, long fallTime, long start, long end, Boolean report){
		int result = 0;
		try{
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setShort(1, userID);
			ps.setLong(2, fallTime);
			ps.setLong(3, start);
			ps.setLong(4, end);
			if(report == null)
				ps.setNull(5, Types.BIT);
			else
				ps.setBoolean(5, report);			
			result = ps.executeUpdate();	
			ps.close();
		}catch(SQLException e) {
			String log = sql.replaceFirst("\\?", String.valueOf(userID));
			ServerMain.log("SQL Update Error, SQL command = " + log);
		}
		return result;	
	}
	//更新跌倒事件用
		private int update(String sql, short userID, long fallTime, boolean report){
			int result = 0;
			try{
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setBoolean(1, report);
				ps.setShort(2, userID);
				ps.setLong(3, fallTime);		
				result = ps.executeUpdate();	
				ps.close();
			}catch(SQLException e) {
				String log = sql.replaceFirst("\\?", String.valueOf(report));
				log = log.replaceFirst("\\?", String.valueOf(userID));
				log = log.replaceFirst("\\?", String.valueOf(fallTime));
				ServerMain.log("SQL Update Error, SQL command = " + log);
			}
			return result;	
		}
	
//===================================================================================//
//	SQL String Command
//===================================================================================//
	int insert(int userID, long time, float[] ac, float[] gv){//FD_Data
		//建立SQL字串
		String sql = "INSERT " +  TABLE_DATA + " (UserID, DateTime, Ax, Ay, Az, Gx, Gy, Gz, DateHash) " +
					    		"VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		return update(sql, (short)userID, time, ac, gv);
	}
		
	int insert(String userName, UUID id, byte[] latestIP){//FD_User
		//建立UUID陣列
		ByteBuffer uuidBuffer = ByteBuffer.allocate(16);
		uuidBuffer.putLong(id.getMostSignificantBits());
		uuidBuffer.putLong(id.getLeastSignificantBits());
		
		//建立SQL字串
		String sql = "INSERT " + TABLE_USER + " (UserName, UUID, LatestIP) " +
			    			  "VALUES ( ?, ?, ? )";		
		//執行UPDATE及回傳
		return update(sql, userName, uuidBuffer.array(), latestIP);		
	}
	
	int insert(int userID, long fallTime, long start, long end, Boolean report){//FD_Fall
		//建立SQL字串
//		String column = (report==null)? "" : ", Report";
//		String param = (report==null)? "" : ", ?";
		String sql = "INSERT " + TABLE_FALL + " (UserID, FallTime, StartTime, EndTime, Report)" +
					    		" VALUES ( ?, ?, ?, ?, ? )";//執行UPDATE及回傳
		return update(sql, (short)userID, fallTime, start, end, report);		
	}
	
	int updateIP(int index, byte[] latestIP){
		short userIndex = (short) index;
		String sql = "UPDATE " +  TABLE_USER + 
					 "SET LatestIP = ? " + 
					 "WHERE UserID = ? ";		
		return update(sql, userIndex, latestIP);		
	}
	
	int updateName(int index, String name){
		short userIndex = (short) index;
		String sql = "UPDATE " +  TABLE_USER +  
					 "SET UserName = ? " + 
					 "WHERE UserID = ? ";		
		return update(sql, userIndex, name);		
	}
	
	int updateFall(int userID, long fallTime, boolean report){
		String sql = "UPDATE " + TABLE_FALL + " " + 
						"SET Report = ? " + 
						"WHERE UserID = ? AND FallTime = ?";//執行UPDATE及回傳
		return update(sql, (short)userID, fallTime, report);	
	}
//===================================================================================//
//	SQL String Command Query
//===================================================================================//
	ResultSet getUserList(){
		String sql = "SELECT * FROM " + TABLE_USER;		
		return query(sql);		
	}
	
	ResultSet getData(int userID, long startTime, long endTime){
		String sT = String.valueOf(startTime);
		String eT = String.valueOf(endTime);
		String sql = "SELECT DateTime, Ax, Ay, Az, Gx, Gy, Gz FROM " + TABLE_DATA + 
					" WHERE (DateTime BETWEEN " + sT + " AND " + eT + ")" + 
					" AND UserID = " + userID + 
					" ORDER BY DateTime ASC";
		return query(sql);		
	}
	
	ResultSet getFall(int userID){
		String sql = "SELECT UserID, FallTime, StartTime, EndTime, Report" +
					 " FROM " + TABLE_FALL + " WHERE userID = " + userID;		
		return query(sql);		
	}

//===================================================================================//
//	DataSheet
//===================================================================================//
	DataSheet getData(int userID, long startTime, long endTime, long fallTime){
		String sT = String.valueOf(startTime);
		String eT = String.valueOf(endTime);
		String sql = "SELECT DateTime, Ax, Ay, Az, Gx, Gy, Gz FROM " + TABLE_DATA + 
					" WHERE (DateTime BETWEEN " + sT + " AND " + eT + ")" + 
					" AND UserID = " + userID + 
					" ORDER BY DateTime ASC";
		//自動生成DataSheet
		DataSheet result = new DataSheet(fallTime);
		ResultSet qr = query(sql);
		try{
			while(qr.next())
				result.add(qr.getLong(1), qr.getFloat(2), qr.getFloat(3), qr.getFloat(4), qr.getFloat(5), qr.getFloat(6), qr.getFloat(7));
			qr.close();
		}catch(SQLException s){
			s.printStackTrace();
			result = null;
		}
		
		return result;
	}
	
	DataSheet getAllData(int userID){
		String sql = "SELECT TOP (60000) DateTime, Ax, Ay, Az, Gx, Gy, Gz FROM " + TABLE_DATA + 
				" WHERE UserID = " + userID + 
				" ORDER BY DateTime ASC";
		DataSheet result = new DataSheet(-1);
		ResultSet qr = query(sql);
		try{
			while(qr.next())
				result.add(qr.getLong(1), qr.getFloat(2), qr.getFloat(3), qr.getFloat(4), qr.getFloat(5), qr.getFloat(6), qr.getFloat(7));
			qr.close();
		}catch(SQLException s){
			s.printStackTrace();
			result = null;
		}
		return result;
	}
	
	
	
	
	
	
	
	
}

