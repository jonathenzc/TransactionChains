package Bidding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class biddingBackEnd extends Thread{
	private int localOrRemote;//��ʾ�������Ǳ��ػ������ 0Ϊ���أ�1ΪԶ��
	private Vector<Integer> bidQueue;//���Ķ���
	private int bidQueueIndex;
	private Vector<queueElement> bidToDoQueue;//�������Ķ���
	private int bidToDoQueueIndex;
	private Vector<Integer> raisingBidQueue;//�����Ķ���
	private int raisingBidQueueIndex;
	private Vector<queueElement> raisingBidToDoQueue;//���������Ķ���
	private int raisingBidToDoQueueIndex;
	
	//���ݿ����ӱ���
	private final static String driver = "com.mysql.jdbc.Driver";
	private final static String biddingUrl = "jdbc:mysql://localhost:3306/bidding";
	private final static String user = "root";
	private final static String pwd = "zc529zc952";
	
	public biddingBackEnd(int place) {
		localOrRemote = place;
		bidQueueIndex = 0;
		bidToDoQueueIndex = 0;
		raisingBidQueueIndex = 0;
		raisingBidToDoQueueIndex = 0;
		//���г�ʼ��
		bidQueue = new Vector<Integer>();
		bidToDoQueue = new Vector<queueElement>();
		raisingBidQueue = new Vector<Integer>();
		raisingBidToDoQueue = new Vector<queueElement>();
	}
	
	/*********************************���ݿ�Ĳ���**************************************/
	//����mysql����
	public static Connection buildSqlConnection(String url) throws SQLException
	{
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		Connection conn = DriverManager.getConnection(url,user,pwd);
			
//		if(!conn.isClosed())
//			System.out.println(url+" connection built");
			
		return conn;
	}
	
	//�ر�mysql����
	public static void closeSqlConnection(Connection conn) throws SQLException
	{
		conn.close();
		
		//System.out.println("connection closed");
	}	
	
	//�������ݿ����
	public void insertHelper(String sql) throws SQLException
	{
		// ����mysql����bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		
		int result = statement.executeUpdate(sql);
		
		//�ر�mysql����emailGraph
		closeSqlConnection(biddingConn);
	}
	
	//�������ݿ����
	public void updateHelper(String sql) throws SQLException
	{
		// ����mysql����bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		
		int result = statement.executeUpdate(sql);
		
		//�ر�mysql����emailGraph
		closeSqlConnection(biddingConn);
	}
	
	/***************************************�������ֺ���*******************************************/
	/***************************************���ֺ���1������****************************************/
	//ֱ�ӽ��и��²���
	public void updateBiddingPrice(String item,int price) throws SQLException
	{
		String updateSql = "update ";
		String tableName = "_item";
		//��ȡ���µı�,��ѡ����ص�item��
		if(localOrRemote == 0)//local
			tableName = "remote"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "local"+tableName;
			
		updateSql += tableName + " set HighestBid=" + price + " where Item='"+item+"' and HighestBid < "+price;
			
//		System.out.println(updateSql);		
		updateHelper(updateSql);
		System.out.println(updateSql);
	}
	
	/***************************************���ֺ���2������****************************************/
	public void insert(String name,String item, int price) throws SQLException
	{
		String insertSql = "insert into ";
		String tableName = "_item(Seller,Item,HighestBid) ";
		//��ȡ���µı�,��ѡ����ص�item��
		if(localOrRemote == 0)//local
			tableName = "remote"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "local"+tableName;
			
		insertSql += tableName + " values(";
		
		//�����sql
		insertSql += "\"" + name + "\","
				  + "\"" + item + "\","
		          + "\"" + price + "\")"; //add anotherName
		
		System.out.println(insertSql);
		
		//�������ݿ�
		insertHelper(insertSql);
	}
	
	/*********************************������غ���***************************************/
	//�������item��
	public void updateRemoteItem(String item,int price) throws SQLException
	{
		updateBiddingPrice(item,price);
	}
	
	//�������item��
	public void insertRemoteItem(String name,String item,int price) throws SQLException
	{
		insert(name,item,price);
	}
	
	/*********************************��֤ʱ����***************************************/
	public void pushBidQueue(int lineNum)
	{
		bidQueue.add(lineNum);
	}
	
	public void pushRaisingBidQueue(int lineNum)
	{
		raisingBidQueue.add(lineNum);
	}
	
	//�������������
	public void pushToDoQueueHelper(Vector<queueElement> v,int lineNum,String name,String item,int price)
	{
		//������������������
		if(v.size() == 0)
			v.add(new queueElement(lineNum,name,item,price));
		else
		{
			for(int i=0;i<v.size();i++)
			{
				if(lineNum < v.get(i).operIndex)
				{
					v.insertElementAt(new queueElement(lineNum,name,item,price), i);
					break;
				}
			}
		}		
	}
	
	public void pushBidToDoQueue(int lineNum,String name,String item,int price)
	{
		pushToDoQueueHelper(bidToDoQueue,lineNum,name,item,price);
	}
	
	public void pushRaisingBidToDoQueue(int lineNum,String name,String item,int price)
	{
		pushToDoQueueHelper(raisingBidToDoQueue,lineNum,name,item,price);
	}
	
	/*********************************ʱ���жϺ���
	 * @throws SQLException ***************************************/
	//���ʱ��׼ȷ���������item�������׼ȷ����������Ϣѹ���������У������ȴ�msg�̲߳���Ԫ��
	//insertOrUpdate��0Ϊ�������item��1Ϊ�������item��
	//vΪԤ����ʱ��ĳ�����У�vIndex�����ĵ�ǰ�±�
	//toDoVΪ�������ĳ�����У�toDoVIndex������ǰ���±�
	public void judgeOrdering(int insertOrUpdate,Vector<Integer> v,int vIndex, Vector<queueElement> toDoV,int toDoVIndex) throws SQLException
	{
		while(vIndex < v.size() && toDoVIndex < toDoV.size())
		{
			if(v.get(vIndex) == toDoV.get(toDoVIndex).operIndex)
			{
				if(insertOrUpdate == 0)
				{
//					System.out.println("update while "+toDoV.get(toDoVIndex).item+ " "+toDoV.get(toDoVIndex).price);
					updateBiddingPrice(toDoV.get(toDoVIndex).item,toDoV.get(toDoVIndex).price);
				}
				else if(insertOrUpdate == 1)
				{
//					System.out.println("insert while "+toDoV.get(toDoVIndex).item+ " "+toDoV.get(toDoVIndex).price);				
					insertRemoteItem(toDoV.get(toDoVIndex).seller,toDoV.get(toDoVIndex).item,toDoV.get(toDoVIndex).price);
				}
				
				toDoV.remove(toDoVIndex);
				v.remove(0);
			}
			else
			{
				String fileName = "output.txt";
				//��ȡ�ļ�
				File file = new File(fileName);
		        BufferedWriter writer = null;
		        try {
		        	writer = new BufferedWriter(new FileWriter(file,true));
		            writer.write("---------------------------------------------");
		            writer.newLine();
		            writer.write("Queue Top: "+v.get(vIndex) + " operation arrived: index "+toDoV.get(toDoVIndex).operIndex+
							" item "+toDoV.get(toDoVIndex).item+" price "+toDoV.get(toDoVIndex).price);
		            writer.newLine();
		            
		            writer.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            if (writer != null) {
		                try {
		                	writer.close();
		                } catch (IOException e1) {
		                }
		            }
		        }
			}
		}
	}
	
	//���߳�ִ�к���
	public void run(){
		while(true)
		{
			if(bidToDoQueue.isEmpty() && raisingBidToDoQueue.isEmpty())//�������Ϊ�վ�һֱѭ��
				continue;
			
			//��غ���ж�ordering����
			if(!bidToDoQueue.isEmpty())
			{
//				System.out.println("bidTODoQuque queuue!!!!!!!!!!!!!!!!!!");
				//��bidQueue����ʱ���ж�
				try {
					judgeOrdering(0,bidQueue,bidQueueIndex,bidToDoQueue,bidToDoQueueIndex);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(!raisingBidToDoQueue.isEmpty())
			{
				//��raisingQueue����ʱ���ж�
				try {
//					System.out.println("raising queuue!!!!!!!!!!!!!!!!!!");
					judgeOrdering(1,raisingBidQueue,raisingBidQueueIndex,raisingBidToDoQueue,raisingBidToDoQueueIndex);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
