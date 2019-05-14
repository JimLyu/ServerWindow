package jim.server;

import java.awt.EventQueue;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import jim.server.Client.MsgSendThread;
import jstudio.fallDetector.DataSheet;

import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.ScrollPaneConstants;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.JTextField;

public class JClientsWindow {
	private JFrame frame;
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private int cWidth;
	private int cHeight;
	private Hashtable<Integer, JLabel> state = new Hashtable<Integer, JLabel>();
	private Hashtable<Integer, JLabel> ip = new Hashtable<Integer, JLabel>();
	private Hashtable<Integer, DataStreamButton> stream = new Hashtable<Integer, DataStreamButton>();
	
	public JClientsWindow(int width, int height) {
		cWidth = width;
		cHeight = height;
	}
	
	void initialize(final int index) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new JFrame("Users");
					frame.setSize(cWidth, cHeight);
					frame.getContentPane().setBackground(Color.WHITE);
					frame.setResizable(false);
					frame.setLocation((MainFrame.scWidth-cWidth)/2, (MainFrame.scHeight-cHeight)/3);
					frame.addWindowListener(new java.awt.event.WindowAdapter() {
					    @Override
					    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					       dispose();
					    }
					});			
					addTab(index);
					frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
					frame.pack();
					frame.setVisible(true);	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});	
	}
	
	
	void addTab(int index){
		if(!state.containsKey(index)){
			UsersList.User user = ServerMain.usersList.getUser(index);
			int id = user.getID();
			//User Tab
			JPanel panel = new JPanel(new BorderLayout());
			panel.setPreferredSize(new Dimension(cWidth, cHeight));
			panel.setBackground(Color.WHITE);
			tabbedPane.addTab(user.getName(), null, panel, null);
				//User Info
				JPanel panel_1 = new JPanel();
				panel_1.setPreferredSize(new Dimension(cWidth, cHeight/16));
				panel_1.setBackground(Color.WHITE);
				panel_1.setBorder(new TitledBorder(null, "使用者資訊", TitledBorder.LEFT, TitledBorder.TOP, null, null));
				panel.add(panel_1,  BorderLayout.NORTH);
				panel_1.setLayout(new GridLayout(0, 4, 0, 0));				
				JLabel label = new JLabel("名稱：" + user.getName());
				panel_1.add(label);			
				JLabel lblIp = new JLabel("IP位址：" + user.getIP());	
				ip.put(user.getID(), lblIp);
				panel_1.add(lblIp);			
				String color = ServerMain.isOnline(id)? "#00FF00>在線上" : "red>未連線";
				JLabel label_1 = new JLabel("<html>連線狀態：" + "<font color=" + color + "</font></html>");
				state.put(id, label_1);
				panel_1.add(label_1);
				JPanel panel_2 = new JPanel(new GridLayout(0,8));
				panel_2.setPreferredSize(new Dimension(cWidth, cHeight/16));
				panel_2.setBackground(Color.WHITE);
				panel_2.setBorder(new TitledBorder(null, "即時監測", TitledBorder.LEFT, TitledBorder.TOP, null, null));
				panel.add(panel_2,  BorderLayout.CENTER);
				DataStreamButton btnStream = new DataStreamButton("開始即時監測", id, panel_2, ServerMain.isOnline(id));
				stream.put(id, btnStream);
				changeState(id, ServerMain.onlineState(id));
				panel_1.add(btnStream);
				//下半部			
				JTabbedPane tabbedPane_1 = new JTabbedPane(JTabbedPane.TOP);
				tabbedPane_1.setPreferredSize(new Dimension(cWidth, cHeight*7/8));
				tabbedPane_1.setBackground(Color.WHITE);
				panel.add(tabbedPane_1, BorderLayout.SOUTH);
					//Tab Fall
					JPanel fall = new JPanel(new GridLayout(3,0));
					addFall(id, fall);
					JScrollPane spFallHistory = new JScrollPane(fall, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					tabbedPane_1.addTab("跌倒歷史", null, spFallHistory, null);
					//Tab Database
					DatabaseTable table = new DatabaseTable(id);
					JScrollPane spDatabase = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					tabbedPane_1.addTab("資料庫", null, spDatabase, null);
					//Tab Logs
//					JTextPane logs = new JTextPane();
//					logs.setBackground(SystemColor.text);
//					JScrollPane spLogs = new JScrollPane(logs, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//					tabbedPane_1.addTab("日誌", null, spLogs, null);
//					tabbedPane_1.setEnabledAt(2, false);
		}		
	}
	
	void changeState(int index, Color color){
		if(state.containsKey(index) && stream.containsKey(index)){
			String online = "";
			if(color == Color.LIGHT_GRAY){
				online = "red>未連線";
				stream.get(index).setEnabled(false);
			}else if(color == Color.GREEN){
				online = "#00FF00>在線上";
				stream.get(index).setEnabled(true);
			}else if(color == Color.ORANGE)
				online = "orange>傳輸中";
			state.get(index).setText(("<html>連線狀態：" + "<font color=" + online + "</font></html>"));
			state.get(index).repaint();
		}
	}
	
	void changeIP(int index, String address){
		if(ip.containsKey(index)){
			ip.get(index).setText("IP位址：" + address);
			ip.get(index).repaint();
		}
				
	}
	
	
	void addFall(int userID, JPanel fall){
		long start = System.currentTimeMillis();
		final String template1 = "<html><p>跌倒時間： 於 <font color=orange>";
		final String template2 = "</font> 偵測到跌倒</p><p/><p>回報結果： ";

		ResultSet result = ServerMain.sqlClient.getFall(userID);
		try {
			ArrayList<FallButton> buttons = new ArrayList();
			while(result.next()){
				long fallTime = result.getLong(2);
				long startTime = result.getLong(3);
				long endTime = result.getLong(4);
				Boolean report = result.getBoolean(5);
				String s = report? "<font color=red>發生跌倒" : "<font color=green>未發生跌倒";
				if(result.wasNull()){
					report = null;
					s = "<font color=blue>尚未回報";
				}					
				s = template1 + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fallTime)) + template2 + s + "</font></p></html>";
				buttons.add(new FallButton(cWidth, cHeight/8, s, report, ServerMain.sqlClient.getData(userID, startTime, endTime, fallTime)));
			}
			result.close();
			fall.setPreferredSize(new Dimension(cWidth, 0));
			fall.setLayout(new GridLayout(5, 1));
			for(FallButton fb : buttons)
				fall.add(fb);
			for(int i = buttons.size(); i < 5; i++)
				fall.add(new JLabel());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("add Fall complete : " + (System.currentTimeMillis()-start)/1000);
	}
	
	boolean isActive(){
		return !(frame==null);
	}
	
	
	
	void dispose(){
		frame.setVisible(false);
		frame.dispose();
		frame = null;
		tabbedPane.removeAll();
		state.clear();
		ip.clear();
	}
	
	class FallButton extends JButton{		
		private final static String FALL = "icons/slip-fall-accident.png";
		private final static String FINE = "icons/human-clipart.png";
		private final static String NULL = "icons/icon-questions.png";
		private DataSheet data;	//TODO 可能是NULL
		
		FallButton(int width, int height, String caption, Boolean report, DataSheet data){
			super(caption);
			this.data = data;
			setPreferredSize(new Dimension(width, height));
			setHorizontalAlignment(SwingConstants.LEFT);
			setFont(new Font("新細明體", Font.BOLD, 14));
			if(report == null)
				setIcon(new ImageIcon(NULL));
			else if(report)
				setIcon(new ImageIcon(FALL));
			else
				setIcon(new ImageIcon(FINE));
			
		}
	}
	
	class DatabaseTable extends JTable{
		private int index;
		final String[] columnNames = {"Time", "Ax", "Ay", "Az", "Gx", "Gy", "Gz"};
		DefaultTableModel dm = (DefaultTableModel) getModel();
		Number[] condition1 = {null, null, null, null, null, null, null};
		Number[] condition2 = {null, null, null, null, null, null, null};
		
		DatabaseTable(int index){
			super();
			this.index = index;
			setPreferredScrollableViewportSize(new Dimension(cWidth, cHeight*9/10));
			setColumnSelectionAllowed(true);
			setCellSelectionEnabled(true);
			getTableHeader().setReorderingAllowed(false);
			for(String name : columnNames)
				dm.addColumn(name);
			addListeners();			 
		}

		void addListeners(){
			for(int i = 0; i < 7; i++)
				getColumnModel().getColumn(i).setHeaderRenderer(new HeaderButton(columnNames[i], false));
			
			getTableHeader().addMouseListener(new MouseListener(){
				@Override
				public void mouseClicked(MouseEvent event) {
					if(columnAtPoint(event.getPoint()) == 0)//TODO 暫時只有時間可以篩選
						new FilterDialog(frame, columnAtPoint(event.getPoint()));
				}
				@Override
				public void mousePressed(MouseEvent event) {
			        int col = columnAtPoint(event.getPoint());
			        getColumnModel().getColumn(col).setHeaderRenderer(new HeaderButton(columnNames[col], true));
			        getTableHeader().repaint();
				}
				@Override
				public void mouseReleased(MouseEvent event) {
			        int col = columnAtPoint(event.getPoint());
			        getColumnModel().getColumn(col).setHeaderRenderer(new HeaderButton(columnNames[col], false));
			        getTableHeader().repaint();
				}
				@Override
				public void mouseEntered(MouseEvent paramMouseEvent) {
					// TODO Auto-generated method stub					
				}
				@Override
				public void mouseExited(MouseEvent paramMouseEvent) {
					// TODO Auto-generated method stub					
				}
			});
		}
		
		void reset(int column, float from, float to){
			//TODO DO NOTHING
		}//TODO 同時判斷每列的限制條件
		
		void reset(long from, long to){
			long start = System.currentTimeMillis();
			dm.setRowCount(0);
			ResultSet rs = ServerMain.sqlClient.getData(index, from, to);
			try {
				while(rs.next()){
					String[] row = new String[7];
						row[0] = String.valueOf(rs.getLong(1));
						row[1] = String.format("%.6f", rs.getFloat(2));
						row[2] = String.format("%.6f", rs.getFloat(3));
						row[3] = String.format("%.6f", rs.getFloat(4));
						row[4] = String.format("%.6f", rs.getFloat(5));
						row[5] = String.format("%.6f", rs.getFloat(6));
						row[6] = String.format("%.6f", rs.getFloat(7));
						dm.addRow(row);
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			System.out.println("reset table in " + (System.currentTimeMillis()-start)/1000 + "秒");
		}
		
		
		class HeaderButton extends JToggleButton implements TableCellRenderer{
			HeaderButton(String caption, boolean selected){super(caption, selected);}			
			@Override
			public Component getTableCellRendererComponent(JTable paramJTable, Object paramObject,
					boolean paramBoolean1, boolean paramBoolean2, int paramInt1, int paramInt2) {return this;}			
		}

		
		class FilterDialog extends JDialog {			
			int column, width, height;
			JTextField from = new JTextField();
			JTextField to = new JTextField();
			JButton enter = new JButton("確定");
			JButton cancel = new JButton("重置");
			
			FilterDialog(JFrame owner, int column){
				super(owner, columnNames[column] + " 篩選");
				this.column = column;
				width = cWidth/5;
				height = cHeight/5;
				setSize(width, height);
				setLocation((MainFrame.scWidth-width)/2, (MainFrame.scHeight-height)/2);				
				initialize();
				setVisible(true);
			}
			
			void initialize(){
				JPanel jpStart = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
				jpStart.add(new JLabel(" 從： "));
				jpStart.add(from);
				from.setPreferredSize(new Dimension(width/2, height/6));
				from.setText((condition1[column]==null)?"" : String.valueOf(condition1[column]));
				getContentPane().add(jpStart, BorderLayout.NORTH);
				
				JPanel jpEnd = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
				jpEnd.add(new JLabel(" 到： "));
				jpEnd.add(to);
				to.setPreferredSize(new Dimension(width/2, height/6));
				to.setText((condition2[column]==null)?"" : String.valueOf(condition2[column]));
				getContentPane().add(jpEnd, BorderLayout.CENTER);
				
				JPanel jpButton = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
				jpButton.add(enter);
				jpButton.add(cancel);
				getContentPane().add(jpButton, BorderLayout.SOUTH);
				
				enter.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(MouseEvent paramMouseEvent) {
						super.mouseClicked(paramMouseEvent);
						try{
							if(column == 0){
								long f = Long.parseLong(from.getText());
								long t = Long.parseLong(to.getText());
								reset(f, t);
								condition1[0] = f;
								condition2[0] = t;
								
							}else{
								float f = Float.parseFloat(from.getText());
								float t = Float.parseFloat(to.getText());
								reset(column, f, t);
								condition1[column] = f;
								condition2[column] = t;
							}
						}catch(Exception e){
							//NullPointerException - if the string is null
							//NumberFormatException - if the string does not contain a parsable number.
							condition1[column] = null;
							condition2[column] = null;
						}						
						dispose();
					}					
				});
				cancel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(MouseEvent paramMouseEvent) {
						super.mouseClicked(paramMouseEvent);
						condition1[column] = null;
						condition2[column] = null;
						dispose();
					}					
				});
				
			}
			
		} 
	}
	
	class DataStreamButton extends JToggleButton{
		private final int PORT = 42449;
		private Socket socket;
		
		private int index;
		private JPanel panel;
		private String[] column = {"Time： ", "Ax： ", "Ay： ", "Az： ", "Gx： ", "Gy： ", "Gz： ", "Ac："};
		
		DataStreamButton(String title, int index, JPanel panel, boolean online){
			super(title, false);
			this.index = index;			
			if(online)
				setEnabled(true);
			else
				setEnabled(false);
			for(int i = 0; i < 8; i++){
				JLabel label = new JLabel(column[i]);
				panel.add(label);
			}
			this.panel = panel;
			this.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ev) {
					 if(ev.getStateChange()==ItemEvent.SELECTED)
						 turnOn();
					 else if(ev.getStateChange()==ItemEvent.DESELECTED)
						 turnOff();				
				}
			});
		}
		
		Runnable stream = new Runnable(){
			@Override
			public void run() {
				try {
					String text;
					BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
					while((text = inputBuffer.readLine()) != null){
						String[] data = text.split("\\s");
						for(int i = 0; i < 8; i++)
							((JLabel)panel.getComponent(i)).setText(column[i] + data[i]);
					}					
				}catch (IOException i) {
					System.out.println(i.toString());			
				}				
				turnOff();
			}			
		};
		
		void close(){
			try {
				if(socket != null)
					socket.close();
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}

		void turnOn(){
			setSelected(true);
			setText("連接中...");
			try {
				if(ServerMain.isOnline(index)){
					ServerSocket serverSocket = new ServerSocket(PORT);
					ServerMain.clientsList.get(index).new MsgSendThread("/stream").start();
					socket = serverSocket.accept();
					if(socket.isConnected()){
						serverSocket.close();
						setText("監測中");
						new Thread(stream).start();
					}
					serverSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		void turnOff(){
			close();
			setText("即時監測");
			setSelected(false);		
		}

		@Override
		public void setEnabled(boolean b) {
			if(!b)
				turnOff();
			super.setEnabled(b);
		}
	}
}
