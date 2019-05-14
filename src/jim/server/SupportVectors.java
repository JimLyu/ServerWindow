package jim.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import libsvm.*;

class SupportVectors {	
	
	private final int[] USED_VECTOR = {1,2,3,4,5,11};
	
	private final String DIR = "./data/SVM/";
	
	private String dataName;
	private Data[] data;
	
	public SupportVectors(String name){		
		try{
			dataName = name + "/";
			JsonReader reader = Json.createReader(new FileReader(DIR + "support_vector.json"));
			JSONArray root = new JSONArray(reader.readArray().toString());
			reader.close();
			
			/*加入user.json*/ //TODO 視情況
//			reader = Json.createReader(new FileReader(DIR + "user.json"));
//			JSONArray user = new JSONArray(reader.readArray().toString());
//			reader.close();
//			for(int j = 0; j < user.length(); j++){
//				root.put(user.getJSONObject(j));
//			}
			
			
			data = new Data[root.length()];
			for(int i = 0; i < root.length(); i++){
				JSONObject json = root.getJSONObject(i);
				double[] vector = new double[14];
				vector[0] = json.getJSONObject("vector").getDouble("dif");
				vector[1] = json.getJSONObject("vector").getDouble("max1");
				vector[2] = json.getJSONObject("vector").getDouble("min1");
				vector[3] = json.getJSONObject("vector").getDouble("deC");
				vector[4] = json.getJSONObject("vector").getDouble("la");
				vector[5] = json.getJSONObject("vector").getDouble("oA");
				vector[6] = json.getJSONObject("vector").getDouble("sl1mx");
				vector[7] = json.getJSONObject("vector").getDouble("sl1mi");
				vector[8] = json.getJSONObject("vector").getDouble("sl2mx");
				vector[9] = json.getJSONObject("vector").getDouble("sl2mi");
				vector[10] = json.getJSONObject("vector").getDouble("eq1");
				vector[11] = json.getJSONObject("vector").getDouble("eq2");
				data[i] = new Data(json.getString("fileName"), json.getBoolean("result"), vector);
			}
			/*生成.scale檔*/
			File file = new File("./data/" + dataName + ".scale");
			if(!file.exists()){
				file.getParentFile().mkdirs();
				file.createNewFile();
			}				
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(int i = 0; i < data.length; i++){
				writer.write(data[i].getScaleString(USED_VECTOR, false));
			}
			writer.close();
			
			train();
//			double[] v = {7.378462, 4.296277, -0.643961, 0,0,0,0,0,0,0,0,0};
//			predict(new Data("test", true, v));
		}catch(IOException | JSONException e){
			e.printStackTrace();
		}
	}
	
	void train() throws IOException{	
		String[] arg = {"./data/" + dataName + ".scale", "./data/" + dataName + ".model"};
		svm_train.main(arg);	
	}
	
	Data predict(int userID, long time, double la, double oA, double max1, double min1, double eq2, double deC) throws IOException{
		//從Android獲得Feature並產生Data物件
		String userName = ServerMain.usersList.getUserName(userID);
		String timeFormat = "[" + new SimpleDateFormat("MMdd HH_mm_ss").format(new Date(time)) + "]";
		double nan = 0;
		double[] vector = {nan, max1, min1, deC, la, oA, nan, nan, nan, nan, nan, eq2};
		Data test = new Data(userName + timeFormat, true, vector);
		
		//產生.test檔案
		BufferedWriter writer = new BufferedWriter(new FileWriter("./data/" + dataName + ".test", false));
		writer.write(test.getScaleString(USED_VECTOR, true));
		writer.close();
		
		//svm predict
		String[] arg = {"./data/" + dataName + ".test", "./data/" + dataName + ".model", "./data/" + dataName + ".result"};
		svm_predict.main(arg);
		
		//回傳結果
		BufferedReader reader = new BufferedReader(new FileReader("./data/" + dataName + ".result"));
		String result = reader.readLine();
		reader.close();	
		
		System.out.println("========================================================");
		
		test.result = ("1.0".equals(result))? true : false;
		
		return test;
	}
	
	class Data{
		private final String[] VECTOR = {"dif", "max1", "min1", "deC", "la", "oA", "sl1mx", "sl1mi", "sl2mx", "sl2mi", "eq1", "eq2"}; 
		
		String fileName;
		double[] vector;
		double dif, max1, min1, deC, la, oA, sl1mx, sl1mi, sl2mx, sl2mi, eq1, eq2;
		boolean result;
		
		ArrayList<ByteBuffer> data = null; //test專用
		
		private Data(String fileName, boolean result, double[] vector){
			this.fileName = fileName;
			this.result = result;
			this.vector = new double[vector.length];
			for(int i = 0; i < vector.length; i++)//取小數點後6位 四捨五入
				this.vector[i] = new BigDecimal(vector[i]).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();
			dif = this.vector[0];
			max1 = this.vector[1];
			min1 = this.vector[2];
			deC = this.vector[3];
			la = this.vector[4];
			oA = this.vector[5];
			sl1mx = this.vector[6];
			sl1mi = this.vector[7];
			sl2mx = this.vector[8];
			sl2mi = this.vector[9];
			eq1 = this.vector[10];
			eq2 = this.vector[11];
		}
		
		@Deprecated
		Data(String userName, ResultSet rs){//predict專用
			this.fileName = userName;
			this.result = true;	//假定
			vector = new double[12];
			data = new ArrayList<ByteBuffer>();
			try {
				while(rs.next()){
					ByteBuffer bb = ByteBuffer.allocate(32);
					bb.putLong(rs.getLong(1));	//DateTime 	0
					bb.putFloat(rs.getFloat(2));//Ax		8
					bb.putFloat(rs.getFloat(3));//Ay		12
					bb.putFloat(rs.getFloat(4));//Az		16
					bb.putFloat(rs.getFloat(5));//Gx		20
					bb.putFloat(rs.getFloat(6));//Gy		24
					bb.putFloat(rs.getFloat(7));//Gz		28
					data.add(bb);
				}
				ServerMain.log("SVM:" + String.valueOf(data.size()));
			}catch (SQLException e) {ServerMain.log(e.toString());}
			
			//只計算la, oA, maxl, minl
			float[] ac = new float[3];
			float[] gv = new float[3];
			for(int i = 0; i < 3; i++){
				ac[i] = data.get(0).getFloat(8 + i*4);	//加速度
				gv[i] = data.get(0).getFloat(20 + i*4);	//重力
			}
			la = abs(ac[0], ac[1], ac[2]);
			oA = abs(ac[0] + gv[0], ac[1] + gv[1], ac[2] + gv[2]);
			max1 = min1 = projection(ac, gv);
			for(ByteBuffer bb:data){
				for(int i = 0; i < 3; i++){
					ac[i] = bb.getFloat(8 + i*4);	//加速度
					gv[i] = bb.getFloat(20 + i*4);	//重力
				}
				double tmp_la = abs(ac[0], ac[1], ac[2]);
				double tmp_oA = abs(ac[0] + gv[0], ac[1] + gv[1], ac[2] + gv[2]);
				double tmp_gA = projection(ac, gv);
				la = (tmp_la > la)? tmp_la : la;
				oA = (tmp_oA > oA)? tmp_la : oA;
				max1 = (tmp_gA > max1)? tmp_gA : max1;
				min1 = (tmp_gA < min1)? tmp_gA : min1;
			}
			dif = deC = sl1mx = sl1mi = sl2mx = sl2mi = eq1 = eq2 = 0;
			vector[0] = dif;
			vector[1] = max1; 
			vector[2] = min1;
			vector[3] = deC;
			vector[4] = la;
			vector[5] = oA;
			vector[6] = sl1mx;
			vector[7] = sl1mi;
			vector[8] = sl2mx;
			vector[9] = sl2mi;
			vector[10] = eq1;
			vector[11] = eq2;			
		}
		
		private String getScaleString(int[] parameters, boolean test){
			String scale = (result)? "1" : "0";
			String display = "";
			for(int i = 0; i < parameters.length; i++){
				scale += String.format(" %s:%.6f", String.valueOf(i+1), vector[parameters[i]]);
				display += String.format("[%s:%.6f], ", VECTOR[parameters[i]], vector[parameters[i]]);
			}
			scale += "\n";
			if(test)
				System.out.println("Test: " + display + "\n");
			return scale;
		}
		
		private JSONObject getJSONObject() throws JSONException{
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			JsonObject obj =  factory.createObjectBuilder()
						.add("vector", factory.createObjectBuilder()
							.add("dif", vector[0])
							.add("max1", vector[1])
							.add("min1", vector[2])
							.add("deC", vector[3])
							.add("la", vector[4])
							.add("oA", vector[5])
							.add("sl1mx", vector[6])
							.add("sl1mi", vector[7])
						 	.add("sl2mx", vector[8])
						 	.add("sl2mi", vector[9])
						 	.add("eq1", vector[10])
						 	.add("eq2", vector[11]))
						.add("result", result)
						.add("fileName", fileName)
					.build();								
			return new JSONObject(obj.toString());
			
		}
		
		void saveJson(){
			JsonReader reader;
			try {
				reader = Json.createReader(new FileReader(DIR + "user.json"));
				JSONArray root = new JSONArray(reader.readArray().toString());
				root.put(getJSONObject());
				FileWriter writer = new FileWriter(DIR + "user.json");
				root.write(writer);
				writer.close();
			} catch (JSONException j) {
				j.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
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
		private double projection(float[] a, float[] b){
			return dot(a, b)/abs(b[0], b[1], b[2]);
		}
		
		//向量內積
		private float dot(float[] a, float[] b){
			return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];		
		}
		
		
	}
	
	@Deprecated
	public static void load(){
		try{
			String[] txt = new String[14];
			BufferedReader reader = new BufferedReader(new FileReader("./data/SVMz3ver10.txt"));		
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			JsonArrayBuilder root = factory.createArrayBuilder();
			for(int i = 0; i < 120; i++){
				txt = reader.readLine().split("\\s");
				root.add(factory.createObjectBuilder()
						.add("vector", factory.createObjectBuilder()
							.add("dif", Double.parseDouble(txt[0]))
							.add("max1", Double.parseDouble(txt[1]))
							.add("min1", Double.parseDouble(txt[2]))
							.add("deC", Double.parseDouble(txt[3]))
							.add("la", Double.parseDouble(txt[4]))
							.add("oA", Double.parseDouble(txt[5]))
							.add("sl1mx", Double.parseDouble(txt[6]))
							.add("sl1mi", Double.parseDouble(txt[7]))
						 	.add("sl2mx", Double.parseDouble(txt[8]))
						 	.add("sl2mi", Double.parseDouble(txt[9]))
						 	.add("eq1", Double.parseDouble(txt[10]))
						 	.add("eq2", Double.parseDouble(txt[11])))
						.add("result", ("yes".equals(txt[12]))?true : false)
						.add("fileName", txt[13]));
			}								
			BufferedWriter writer = new BufferedWriter(new FileWriter("./data/support_vector.json", false));
//			System.out.println(root.build().toString());
			writer.write(root.build().toString());
			writer.close();	
			reader.close();
		}catch(IOException i){i.printStackTrace();}		
	}	

}
