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
	private int localOrRemote;//表示竞标者是本地还是异地 0为本地，1为远程
	private Vector<Integer> bidQueue;//竞拍队列
	private int bidQueueIndex;
	private Vector<queueElement> bidToDoQueue;//待处理竞拍队列
	private int bidToDoQueueIndex;
	private Vector<Integer> raisingBidQueue;//发起竞拍队列
	private int raisingBidQueueIndex;
	private Vector<queueElement> raisingBidToDoQueue;//待处理发起竞拍队列
	private int raisingBidToDoQueueIndex;
	
	//数据库连接变量
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
		//队列初始化
		bidQueue = new Vector<Integer>();
		bidToDoQueue = new Vector<queueElement>();
		raisingBidQueue = new Vector<Integer>();
		raisingBidToDoQueue = new Vector<queueElement>();
	}
	
	/*********************************数据库的操作**************************************/
	//创建mysql连接
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
	
	//关闭mysql连接
	public static void closeSqlConnection(Connection conn) throws SQLException
	{
		conn.close();
		
		//System.out.println("connection closed");
	}	
	
	//插入数据库操作
	public void insertHelper(String sql) throws SQLException
	{
		// 创建mysql连接bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		
		int result = statement.executeUpdate(sql);
		
		//关闭mysql连接emailGraph
		closeSqlConnection(biddingConn);
	}
	
	//更新数据库操作
	public void updateHelper(String sql) throws SQLException
	{
		// 创建mysql连接bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		
		int result = statement.executeUpdate(sql);
		
		//关闭mysql连接emailGraph
		closeSqlConnection(biddingConn);
	}
	
	/***************************************二个助手函数*******************************************/
	/***************************************助手函数1：更新****************************************/
	//直接进行更新操作
	public void updateBiddingPrice(String item,int price) throws SQLException
	{
		String updateSql = "update ";
		String tableName = "_item";
		//获取更新的表,得选择异地的item表
		if(localOrRemote == 0)//local
			tableName = "remote"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "local"+tableName;
			
		updateSql += tableName + " set HighestBid=" + price + " where Item='"+item+"' and HighestBid < "+price;
			
//		System.out.println(updateSql);		
		updateHelper(updateSql);
		System.out.println(updateSql);
	}
	
	/***************************************助手函数2：插入****************************************/
	public void insert(String name,String item, int price) throws SQLException
	{
		String insertSql = "insert into ";
		String tableName = "_item(Seller,Item,HighestBid) ";
		//获取更新的表,得选择异地的item表
		if(localOrRemote == 0)//local
			tableName = "remote"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "local"+tableName;
			
		insertSql += tableName + " values(";
		
		//插入的sql
		insertSql += "\"" + name + "\","
				  + "\"" + item + "\","
		          + "\"" + price + "\")"; //add anotherName
		
		System.out.println(insertSql);
		
		//插入数据库
		insertHelper(insertSql);
	}
	
	/*********************************操作异地函数***************************************/
	//更新异地item表
	public void updateRemoteItem(String item,int price) throws SQLException
	{
		updateBiddingPrice(item,price);
	}
	
	//插入异地item表
	public void insertRemoteItem(String name,String item,int price) throws SQLException
	{
		insert(name,item,price);
	}
	
	/*********************************保证时序函数***************************************/
	public void pushBidQueue(int lineNum)
	{
		bidQueue.add(lineNum);
	}
	
	public void pushRaisingBidQueue(int lineNum)
	{
		raisingBidQueue.add(lineNum);
	}
	
	//待处理队列助手
	public void pushToDoQueueHelper(Vector<queueElement> v,int lineNum,String name,String item,int price)
	{
		//待处理队列是有序队列
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
	
	/*********************************时序判断函数
	 * @throws SQLException ***************************************/
	//如果时序准确，更新异地item表；如果不准确，将这条信息压入待处理队列，继续等待msg线程插入元素
	//insertOrUpdate：0为更新异地item表，1为插入异地item表
	//v为预处理时的某个队列，vIndex是它的当前下标
	//toDoV为待处理的某个队列，toDoVIndex是它当前的下标
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
				//读取文件
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
	
	//多线程执行函数
	public void run(){
		while(true)
		{
			if(bidToDoQueue.isEmpty() && raisingBidToDoQueue.isEmpty())//待处理均为空就一直循环
				continue;
			
			//异地后端判断ordering问题
			if(!bidToDoQueue.isEmpty())
			{
//				System.out.println("bidTODoQuque queuue!!!!!!!!!!!!!!!!!!");
				//与bidQueue进行时序判断
				try {
					judgeOrdering(0,bidQueue,bidQueueIndex,bidToDoQueue,bidToDoQueueIndex);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(!raisingBidToDoQueue.isEmpty())
			{
				//与raisingQueue进行时序判断
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
