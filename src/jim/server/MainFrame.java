package jim.server;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import jim.server.JClientsWindow.DatabaseTable.FilterDialog;

class MainFrame extends JFrame {

//===================================================================================//
//	成員
//===================================================================================//
	
	private final String LOGS_DIR = "Logs";	
	private Container cp;
	public static int scWidth, scHeight;
	public int mfWidth, mfHeight;
	private JMenuBar menuBar;
	private JPanel jpFunction, jpClients, jpLogs, jpBottom;
	
	JListClients jListClients;
	JTextLogs jTextLogs;
	JTextChat jTextChat;
	JClientsWindow jClientsWindow;

	MainFrame(String arg0) throws HeadlessException {
		super(arg0);
		initialize();
	}
	
//===================================================================================//
//	建立元件及排版
//===================================================================================//
	
	private void initialize(){
		cp = getContentPane();
		scWidth  = (int)getToolkit().getScreenSize().getWidth();	//取得各長寬參數
		scHeight = (int)getToolkit().getScreenSize().getHeight();	
		mfWidth  = scWidth *3/4;
		mfHeight = scHeight *3/4 ;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);				//設定關閉鍵
		setSize(mfWidth, mfHeight);									//設定大小
		setLocation((scWidth-mfWidth)/2, (scHeight-mfHeight)/2);	//設定視窗預設位置
		setResizable(false);										//固定大小
		//setMainMenuBar();		//建立功能選單
		setJPanel();			//設定容器
		setVisible(true);
		jClientsWindow = new JClientsWindow(mfWidth, mfHeight);
	}
	
	private void setJPanel() {	//排版
		/*功能區*/
		jpFunction = new JPanel(new GridLayout(1,ServerMain.MAX_CLIENT,5,5));	//格狀：1列3欄
		jpFunction.setPreferredSize(new Dimension(0,mfHeight/7));
		jpFunction.setBackground(Color.WHITE);
		jpFunction.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY, 1, true), "功能區", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
		for(int i=0; i < ServerMain.MAX_CLIENT; i++){
			JButton button = new JButton();
			button.setVisible(false);
			if(i == 0)
				button.setEnabled(false);
			button.setFont(new Font(null, Font.BOLD, 14));
			jpFunction.add(button);
		}
		
		/*客戶端狀態區*/
		jpClients = new JPanel(new FlowLayout(FlowLayout.LEFT));//Flow：靠左
		jpClients.setPreferredSize(new Dimension(mfWidth*3/13,100));
		jpClients.setBackground(Color.WHITE);
		jpClients.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY, 1, true), "使用者", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
		jpClients.add(jListClients = new JListClients());
		
		/*日誌區*/
		jpLogs = new JPanel(new GridBagLayout());	//格狀： 2列1欄(199:1)
		jpLogs.setPreferredSize(new Dimension(mfWidth*10/13 - 5, 100));
		jpLogs.setBackground(Color.WHITE);
		jpLogs.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY, 1, true), "日誌&指令", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
		jpLogs.add(new JScrollPane(jTextLogs = new JTextLogs(), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
					new GridBagConstraints(0,0,1,1, 1, 0.995, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(5,5,5,5),0,0));
		jpLogs.add(jTextChat = new JTextChat(),
					new GridBagConstraints(0,1,1,1, 1, 0.005, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(5,5,5,5),0,0));
		
		/*底列區*/
		jpBottom = new JPanel();
		cp.setLayout(new BorderLayout());
		cp.add(jpFunction, BorderLayout.NORTH);
		cp.add(jpClients, BorderLayout.WEST);
		cp.add(jpLogs, BorderLayout.EAST);
		cp.add(jpBottom, BorderLayout.SOUTH);		
		
	}
	
	private void setMainMenuBar() { 
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(new MainMenu("伺服器", 'S', 0));		//名稱，快速鍵，位置		
	}
	
	
//===================================================================================//
//	package 方法
//===================================================================================//
	void setFunction(final int index, String name){
		JButton button = (JButton) jpFunction.getComponent(index);
		if(!button.isVisible()){
			button.setText(name);
			button.setVisible(true);
			button.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent event) {
					super.mouseClicked(event);
					if(jClientsWindow.isActive())
						jClientsWindow.addTab(index);
					else
						jClientsWindow.initialize(index);
				}
			});
		}
	}
	
	void clearChat(){
		jTextChat.setText("");
	}
	
	static void plot(String name, ArrayList<Long> x, ArrayList<Float> y){
		new Chart(name, x, y);
	}
	
	static void plot(String name, long time, ArrayList<Long> x, ArrayList<Float> y, int timeScale){
		new Chart(name, time, x, y, timeScale);
	}
	
	static void plot(String name, ArrayList<Long> x, ArrayList<Float> y, int width, int height){
		new Chart(name, x, y, width, height);
	}
	
	static void plot(String name, ArrayList<Float> y){//setByIndex
		new Chart(name, y);
	}
	
	void plot(String name, ArrayList<Float> y, int scale){
		new Chart(name, y, scale);
	}
	
	
	
//===================================================================================//
//	內層類別
//===================================================================================//
	
	class MainMenu extends JMenu{		//功能選單
		
		private int index;
		
		public MainMenu(String label, char mnemonic, int index){
			super(label);
			setMnemonic(mnemonic);
			this.index = index;
			switch(index){
				case 0:
					setMenu_Server();
					break;
				case 1:
					setMenu_Edit();
					break;
				default:
					//doNothing				
			}		
		}
		
		private void setMenu_Server() {
			// TODO Auto-generated method stub
			
		}

		
		private void setMenu_Edit() {
			// TODO Auto-generated method stub
			
		}

		
	}
	
	class JListClients extends JList<String>{	//用戶端列表
		
		private final int maxRow = ServerMain.MAX_CLIENT;		
		String[] listItem = new String[maxRow];
		Color[] color = new Color[maxRow];
		
		public JListClients(){
			super();
			setVisibleRowCount(maxRow);
			setLayoutOrientation(JList.VERTICAL);
			for(int i = 0; i < maxRow; i++)
				color[i] = Color.LIGHT_GRAY;
		}
		
		public void set(int index, String client){
			listItem[index] = client;
		}
		
		public void update(){
			setListData(listItem);
			this.setCellRenderer(new ColorRender(color));
		}
		
		public void setColor(int index, Color c){
			if(index < maxRow){
				color[index] = c;
				jClientsWindow.changeState(index, c);
			}
		}
		
		public void setColor(int index){//自動設定成亮灰色
			if(index < maxRow){
				color[index] = Color.LIGHT_GRAY;
				jClientsWindow.changeState(index, Color.LIGHT_GRAY);
			}
										
		}
		
		public void setAdmin(boolean online){
			if(online){
				setColor(0, Color.RED);
				jClientsWindow.changeState(0, Color.RED);
			}else{
				setColor(0, Color.LIGHT_GRAY);
				jClientsWindow.changeState(0, Color.LIGHT_GRAY);
			}
				
			
		}
		
	}
	
	class ColorRender extends JLabel implements ListCellRenderer<String>{

		Color[] color;		
		ColorRender(Color[] color){
			setOpaque(true);
			this.color = color;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {
			DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
			JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);			
			renderer.setForeground(color[index]);
			renderer.setFont(new Font(null, Font.BOLD, 20));
			return renderer;
		}
		
	}
	

	class JTextLogs extends JTextPane{	//日誌區
		
		public JTextLogs(){
			super();
			setProperties();
		}

		private void setProperties() {
//			setTabSize(4);		//設定Tab為4字元
//			setLineWrap(true);	//設定文字過長時換行
			setEditable(false);
			setForeground(Color.blue);
			setFont(new Font(getFont().getFontName(), getFont().getStyle(), 20));
		}
		
		public void println(String str){
			SimpleAttributeSet attrSet = new SimpleAttributeSet();   
			StyleConstants.setForeground(attrSet, Color.BLACK);  
	        try {					
				getDocument().insertString(getDocument().getLength(), str + "\n", attrSet);
			} catch (BadLocationException e) {e.printStackTrace();}
		}
		
		public void println(String str, Color color){//不自動換行
			SimpleAttributeSet attrSet = new SimpleAttributeSet();   
	        StyleConstants.setForeground(attrSet, color);  
	        try {
				getDocument().insertString(getDocument().getLength(), str, attrSet);
			} catch (BadLocationException e) {e.printStackTrace();}
		}		
		
		public void saveLogs(){		
			try{
				String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
				File logs = new File(LOGS_DIR + "/" + date + ".log");
				if(!logs.exists()){					//如果日誌不存在，建立資料夾及檔案
					logs.getParentFile().mkdirs();
					logs.createNewFile();
				}
				FileWriter fw = new FileWriter(logs, true);
				write(fw);
				fw.close();
				System.out.println("日誌儲存成功");
			}catch(IOException e){
				System.out.println("日誌儲存失敗");
			}			
		}
		
		
	}

	class JTextChat extends JTextField{	//指令區
		
		public JTextChat(){
			super();
			addActionListener(ServerMain.chatListener);
			
		}
		
		
	}
	
	class ClientWindow extends JFrame {	//Client 視窗
		 private int cWidth, cHeight;
		 private JTabbedPane tpFrame;
		 private int userCount = ServerMain.usersList.getSize();	//目前顯示的使用者數
		 
		 ClientWindow(){
			 super("Users");
			 cWidth = mfWidth * 5/6;
			 cHeight = mfHeight;
			 setSize(cWidth, cHeight);
			 setResizable(false);
			 setLocationRelativeTo(this);	//設定視窗預設位置
			 setVisible(true);
		 }
		 
		 void create(){
			 tpFrame = new JTabbedPane();
			 for(int i = 0; i < userCount; i++){
				 JPanel tab = new JPanel(new GridLayout(2,1,5,5));
					 //使用者資訊
					 JPanel info = new JPanel(new GridLayout(1,3,5,5));
					 info.setPreferredSize(new Dimension(0, cHeight/8));
					 info.setBackground(Color.WHITE);
					 info.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY, 1, true), "使用者資訊", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
					 info.add(new JLabel(ServerMain.usersList.getUserName(i)));
					 info.add(new JLabel(ServerMain.usersList.getUUID(i).toString()));
					 info.add(new JLabel(ServerMain.usersList.getLatestHostIP(i)));
					 //使用者資料	2Tab
					 JTabbedPane user = new JTabbedPane();
					 	//跌倒歷史
					 	
				 tab.add(info);
				 tpFrame.addTab(ServerMain.usersList.getUserName(i), tab);
			 }
			 setContentPane(tpFrame);
			 
		 }
		 
		 void load(int index){
			 
		 }
		
		
	}
	
	static class Chart extends JFrame{	//圖表(圖形盡量要可微)
		private JLayeredPane lp;
		private int width, height, canvasWidth, canvasHeight, mx, my;
		private double scaleX, scaleY;//scaleX：canvas寬中有幾毫秒； scaleY：canvas長中有幾 m/s2
		private Canvas canvas;
		private long maxTime, minTime;
		private float maxData, minData;
		private long fallTime = 0l;
		
		private final int MRX = 30;	//margin ratio x = 1/30
		private final int MRY = 20;	//margin ratio Y = 1/20
		private final int MAXPOINT = 10000;	//最大資料長度
		private int cooX = 20;//坐標軸的線數量
		private int cooY = 9;
		
		Chart(String title, ArrayList<Long> x, ArrayList<Float> y){//主用
			super(title);
			this.width = scWidth * 3/5;
			this.height = scHeight * 3/5;
			initialize();
			if(x.size() > 1 && y.size() > 1)
				createCanvas(x, y, 0);
			else
				dispose();
		}
		
		Chart(String title, long time, ArrayList<Long> x, ArrayList<Float> y, int scale){//主用
			super(title);
			this.width = scWidth * 3/5;
			this.height = scHeight * 3/5;
			fallTime = time;
			initialize();
			if(x.size() > 1 && y.size() > 1)
				createCanvas(x, y, scale);
			else{
				System.out.println("Fail, data = null");
				dispose();
			}				
		}
		
		Chart(String title, ArrayList<Float> y){
			super(title);
			this.width = scWidth * 3/5;
			this.height = scHeight * 3/5;
			initialize();
			if(y.size() >1)
				createCanvas(y,0);
			else
				dispose();
		}
		
		Chart(String title, ArrayList<Float> y, int scale){//scale:每格包含多少單位x
			super(title);
			this.width = scWidth * 3/5;
			this.height = scHeight * 3/5;
			initialize();
			if(y.size() >1)
				createCanvas(y, scale);
			else
				dispose();
		}
		
		Chart(String title, ArrayList<Long> x, ArrayList<Float> y, int width, int height){
			super(title);
			this.width = width;
			this.height = height;
			initialize();
			if(x.size() > 1 && y.size() > 1)
				createCanvas(x, y, 0);
			else
				dispose();
		}
		
		Chart(String title, ArrayList<Float> y, int width, int height){
			super(title);
			this.width = width;
			this.height = height;
			initialize();
			if(y.size() >1)
				createCanvas(y, 0);
			else
				dispose();
		}
		
		private void initialize(){
			//計算邊距
			mx = width / MRX;		
			my = height / MRY;
			
			//初始化視窗
//			lp = getLayeredPane();
			lp = new JLayeredPane();
			lp.setLayout(new GridBagLayout());
			setSize(width, height);
			setResizable(false);
			setLocationRelativeTo(this);	//設定視窗預設位置
			setVisible(true);
			
			//計算canvas長寬
			canvasWidth = getLayeredPane().getWidth() - 2*mx;
			canvasHeight = getLayeredPane().getHeight() - 2*my;		
		}
		
		private void createCanvas(final float[] data, final int dataSize, final int timeScale) {//必須保證 x 遞增
			//波形及座標軸
			canvas = new Canvas(){
				@Override
				public void paint(Graphics g){
					super.paint(g);
					int fall = (int)((int)fallTime*canvasWidth/scaleX);
					g.setColor(Color.RED);
					g.drawLine(fall, 0, fall, canvasHeight);
					double gridx = (timeScale==0)? (double)canvasWidth/(cooX+1) : (int)(canvasWidth/(scaleX/timeScale));
					for(int j = 1; j*gridx < canvasWidth; j++){//畫x座標軸
						int x = (int)(gridx*j);
						g.setColor(Color.GRAY);
						g.drawLine(x, 0, x, canvasHeight);
					}
					int mid = canvasHeight/2;
					g.setColor(Color.BLACK);
					g.drawLine(0, mid, canvasWidth, mid);
					int y = 0;
					int gridy = canvasHeight/(int)(scaleY/9.81);
					for(int j = 1; (y = (int)(gridy*j))< canvasHeight/2 ; j++){//畫y座標軸												
						g.setColor(Color.GRAY);
						g.drawLine(0, mid + y, canvasWidth, mid + y);
						g.drawLine(0, mid - y, canvasWidth, mid - y);
					}
					for(int i = 0; i < data.length-1; i++){//畫圖							
						int y1 = Double.valueOf((canvasHeight/2 - data[i]*(canvasHeight/scaleY))).intValue();
						int y2 = Double.valueOf(canvasHeight/2 - data[i+1]*(canvasHeight/scaleY)).intValue();
//						System.out.println(data[i] + " " + data[i+1] + " " + y1 + " " + y2);
						g.setColor(Color.BLUE);
						if(y2 < 0){
							System.out.println("data[" + i + "] < 0");
							return;
						}else							
							g.drawLine(i, y1, i+1, y2);
					}
				}
			};
			canvas.setBackground(Color.white);
			lp.add(canvas, 
					new GridBagConstraints(0,0,1, 1, 1, 1, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(my,mx,my,mx),0,0));					
			//建立底層			
			lp.add(new Canvas(){
				@Override
				public void paint(Graphics g){
					super.paint(g);
					if(timeScale == 0){
						for(int i = 0; i < cooX + 2; i++){
							Double s = i*scaleX/(cooX+1);	
							int ox =  (-3) * ((s==0)?1 : (int)Math.ceil(Math.log10(s)));//算出有多少字 置中
							int oy = 0 - my/2 + 3;	//y offset					
							String timeString = String.valueOf(s.intValue());
							g.drawString(timeString, mx + i*(canvasWidth/(cooX+1)) + ox, getHeight() + oy);
						}
					}else{//指定座標時						
						Double s = 0d;
						for(int i = 0; s < scaleX ; i++){//x軸
							s = i*(double)timeScale;	
							int ox =  (-3) * ((s==0)?1 : (int)Math.ceil(Math.log10(s)));//算出有多少字 置中
							int oy = 0 - my/2 + 3;	//y offset					
							String timeString = String.valueOf(s.intValue());
							g.drawString(timeString, mx + i*(int)(canvasWidth/(scaleX/timeScale)) + ox, getHeight() + oy);
						}
						int mid = canvasHeight/2 + my + 4;
						g.setColor(Color.BLACK);
						g.drawString("0", mx/2, mid);
						int y = 0;
						int gridy = canvasHeight/(int)(scaleY/9.81);
						for(int j = 1; (y = (int)(gridy*j))< canvasHeight/2 ; j++){//y軸
							String dataString = String.format("%1.1f", j*9.81);
							g.setColor(Color.GRAY);
							g.drawString("-" + dataString, mx/6, mid + y);
							g.drawString(dataString, mx/4, mid - y);
						}
					}
					
				}
			},new GridBagConstraints(0,0,1, 1, 1, 1, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
			lp.moveToFront(canvas);
			setContentPane(lp);
			setVisible(true);
		}

		
		private void createCanvas(ArrayList<Long> x, ArrayList<Float> y, int timeScale) {
			createCanvas(setByTime(x, y), y.size(), timeScale);
		}
		
		private void createCanvas(ArrayList<Float> y, int timeScale) {
			createCanvas(setByIndex(y), y.size(), timeScale);
		}
		
		float interpolation(long ax, long bx, float ay, float by, double position){
//			System.out.println(ax + " " + bx+ " " + ay+ " " + by+ " " + position + " " +(ay + (by - ay)*(position-ax)/(bx-ax)));
			return (float) (ay + (by - ay)*(position-ax)/(bx-ax));
		}
		
		float[] setByTime(ArrayList<Long> x, ArrayList<Float> y){
			//尋找最大值及最小值，計算scale
			maxTime = minTime = x.get(0);
			maxData = minData = y.get(0);
			for(int i = 1; i < Math.min(x.size(), y.size()); i++){
				if(x.get(i) > maxTime)
					maxTime = x.get(i);
				if(x.get(i) < minTime)
					minTime = x.get(i);
				if(y.get(i) > maxData)
					maxData = y.get(i);
				if(y.get(i) < minData)
					minData = y.get(i);
			}
			scaleY = Math.min(2*(maxData - minData)*MRY/(MRY-2), canvasHeight);	//微調 限制最大單位差為高的一半
			scaleX = maxTime - minTime;
//			scaleX = Math.min(maxTime - minTime, x.size());
//			System.out.println("SCALEX = " + scaleX);
			
			float[] data = new float[canvasWidth];//每個X座標上的Data					
			double u = scaleX / (double)canvasWidth;	//每單位像素代表多少單位data
			data[0] = y.get(0);//第一個值
			int j = 0;
			double position;
			for(int i = 1; i < canvasWidth; i++){
				for(; j < Math.min(x.size(), y.size())-1; j++){
					position = x.get(0) + i*u;
					if((x.get(j) <= position) && (x.get(j+1) >= position)){
						data[i] = interpolation(x.get(j), x.get(j+1), y.get(j), y.get(j+1), position);
//						data[i] = Math.max(y.get(j), y.get(j+1));//解析度 > canvasWidth 時才可用
						break;
					}else if(x.get(j) > x.get(j+1)){ //x非遞增
						ServerMain.log("Plot [ " + getTitle() + " ] ： X未遞增，自動以索引排列" );
						return setByIndex(y);
					}
				}
			}
			return data;
		}
		
		float[] setByIndex(ArrayList<Float> y){
			ArrayList<Long> x = new ArrayList<Long>();			
			for(int i = 0; i < y.size(); i++)
				x.add((long)i);
			return setByTime(x, y);
		}
	}
}
