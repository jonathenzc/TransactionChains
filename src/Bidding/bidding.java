package Bidding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;

import SC.SC;

public class bidding extends Thread{
	private String bidderName;//竞标者的名字
	private int localOrRemote;//表示竞标者是本地还是异地 0为本地，1为远程
	private biddingBackEnd backEnd;//每个竞标者都有个后台线程用来执行发送给异地数据库,每个竞标者会生成消息线程唤醒异地的后端
	private Vector<String> operationV;//记录文本预处理队列
	
	//数据库连接变量
	private final static String driver = "com.mysql.jdbc.Driver";
	private final static String biddingUrl = "jdbc:mysql://localhost:3306/bidding";
	private final static String user = "root";
	private final static String pwd = "zc529zc952";
	
	public bidding(int place,biddingBackEnd theBackEnd)
	{
		localOrRemote = place;
		backEnd = theBackEnd;
		operationV = new Vector<String>();
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
	
	//返回表中最大的ID
	public int maxID(String sql) throws SQLException
	{
		int ID = 0;
		
		// 创建mysql连接bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		ResultSet rs;
		int id = 0;

		rs = statement.executeQuery(sql);
		
		if(rs.next())//如果有ID，就原ID+1
			id = rs.getInt("ID")+1;
		
		//关闭mysql连接emailGraph
		closeSqlConnection(biddingConn);
		
		return ID;
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
	
	/*********************************竞标者的三个操作助手函数**************************************/
	/*********************************助手函数1：插入**********************************************/
	//插入local还是remote的bid还是item表
	//bidOrItem: 0为bid，1为item
	public void insert(int bidOrItem,String name,String item, int price) throws SQLException
	{
		String insertSql = "insert into ";
		String tableName = "";
		String tableBidColumn = "(Bidder,Item,Price) ";
		String tableItemColumn = "(Seller,Item,HighestBid) ";
		//获取插入的表格
		if(localOrRemote == 0 && bidOrItem == 0)//local_bid
			tableName = "local_bid"+tableBidColumn;
		else if(localOrRemote == 0 && bidOrItem == 1)//local_item
			tableName = "local_item"+tableItemColumn;
		else if(localOrRemote == 1 && bidOrItem == 0)//remote_bid
			tableName = "remote_bid"+tableBidColumn;
		else if(localOrRemote == 1 && bidOrItem == 1)//remote_item
			tableName = "remote_item"+tableItemColumn;
			
		insertSql += tableName + " values(";
		
		//插入的sql
		insertSql += "\"" + name + "\","
				  + "\"" + item + "\","
		          + "\"" + price + "\")"; //add anotherName
		
		System.out.println(insertSql);
		
		//插入数据库
		insertHelper(insertSql);
	}
	
	/*********************************助手函数2：返回当前最高价*************************************/
	//查询local数据库中
	//bidOrItem: 0为bid，1为item
	public int highestPrice(int bidOrItem,String item) throws SQLException
	{
		// 创建mysql连接bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);

		//获取查询的表格
		String tableName = "";
		String priceOrBid = "";
		
		if(localOrRemote == 0 && bidOrItem == 0)//local_bid
		{
			tableName = "local_bid";
			priceOrBid = "Price";
		}
		else if(localOrRemote == 0 && bidOrItem == 1)//local_item
		{
			tableName = "local_item";
			priceOrBid = "HighestBid";
		}
		else if(localOrRemote == 1 && bidOrItem == 0)//remote_bid
		{	
			tableName = "remote_bid";
			priceOrBid = "Price";
		}
		else if(localOrRemote == 1 && bidOrItem == 1)//remote_item
		{
			tableName = "remote_item";
			priceOrBid = "HighestBid";
		}
		
		Statement statement = biddingConn.createStatement();
		
		String sql = "select "+priceOrBid+" from "+tableName+" where Item = '" + item+"'";
//		System.out.println(sql);
		
		ResultSet rs = statement.executeQuery(sql);
			
		rs.next();
		int highestBid = rs.getInt("HighestBid");
		
		//关闭mysql连接emailGraph
		closeSqlConnection(biddingConn);
		
		return highestBid;
	}
	
	/*********************************助手函数3：更新竞拍价*****************************************/
	//直接进行更新操作
	public void updateBiddingPrice(String item,int price) throws SQLException
	{
		String updateSql = "update ";
		String tableName = "_item";
		//获取更新的表
		if(localOrRemote == 0)//local
			tableName = "local"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "remote"+tableName;
			
		updateSql += tableName + " set HighestBid=" + price + " where Item='"+item+"' and HighestBid < "+price;
			
//		System.out.println(updateSql);		
		updateHelper(updateSql);
		System.out.println(updateSql);
	}
	
	/*********************************竞标者的三个操作函数**************************************/
	/*********************************操作函数1：查询竞拍价*************************************/
	public void biddingPrice(String item) throws SQLException
	{
		int highestPrice = highestPrice(1,item);
		String localStr = "";
		
		if(localOrRemote == 0)
			localStr = "Local: ";
		else if(localOrRemote == 1)
			localStr = "Remote: ";
		
		System.out.println(localStr+item+" "+highestPrice);
	}
	
	/*********************************操作函数2：竞拍 *******************************************/
	public void biddingForItem(int operIndex,String bidder,String item,int price) throws SQLException, InterruptedException
	{
		//Step1: 插入本地bid表
		insert(0,bidder,item,price);
		//Step2: 更新本地item表
		updateBiddingPrice(item,price);
		//Step3: 处理一致性，更新异地item表
		//生成一个消息线程
		Random random = new Random();
		int randomValue = Math.abs(random.nextInt())%500+500;//生成500-1000的随机数
//		
		message msg = new message(localOrRemote,0,operIndex,bidder,item,price,backEnd,randomValue);
		//下面message用于测试ordering
//		message msg = new message(localOrRemote,0,operIndex,bidder,item,price,backEnd,500*(10-operIndex));
		
//		msg.sleepForSeconds(500*(10-operIndex));//睡眠5秒模拟异地传输的延迟时间
		msg.start();//在消息run函数中插入异地后端bidToDoQueue
		//在插入待处理队列后，异地后端将要做ordering判断
	}
	
	/*********************************操作函数3：发起竞拍***************************************/
	public void raisingBidding(int operIndex,String seller,String item) throws SQLException, InterruptedException
	{
		//Step1: 插入本地item表
		insert(1,seller,item,0);
		//Step2: 处理一致性，插入异地item表
		Random random = new Random();
		int randomValue = Math.abs(random.nextInt())%500+500;//生成500-1000的随机数
		
		message msg = new message(localOrRemote,1,operIndex,seller,item,0,backEnd,randomValue);
		//下面message用于测试ordering
//		message msg = new message(localOrRemote,1,operIndex,seller,item,0,backEnd,500*(10-operIndex));
//		msg.sleepForSeconds(500);//睡眠5秒模拟异地传输的延迟时间 到时候改成随机
		msg.start();//在消息run函数中插入异地后端raisingBidToDoQueue
	}
	
	
	//多线程执行函数
	public void run(){
		/*********************************预处理操作将文本内容存入队列中***************************************/
		//获取读入文件名称
		String fileName = "Operation.txt";//本地文件叫localOperation.txt，远程文件叫remoteOpertion.txt
		int lineNum = 0;//文件行号
		
		if(localOrRemote == 0)//local
			fileName = "local"+fileName;
		else if(localOrRemote == 1)//remote
			fileName = "remote"+fileName;
		
		//读取文件
		File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = "";
            while ((tempString = reader.readLine()) != null) {
            	operationV.add(tempString);
            	
            	//判断用户操作
            	if(tempString.startsWith("1"))//竞拍
            	{
            		//异地后台压行号
            		backEnd.pushBidQueue(lineNum);
            	}
            	else if(tempString.startsWith("2"))//发起竞拍
            	{
            		//异地后台压行号
            		backEnd.pushRaisingBidQueue(lineNum);
            	}
            	
            	lineNum++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        
		/*********************************读取预处理队列***************************************/
        for(int i=0;i<operationV.size();i++)
        {
	    	//输入格式以空格划分
	    	String[] tempstr = operationV.get(i).split(" ");
	    	
	    	//判断用户操作
	    	if(tempstr[0].equals("1"))//竞拍
	    	{
	    		int price = Integer.parseInt(tempstr[3]);
	    		int latencyTime = Integer.parseInt(tempstr[4]);
	    		
	    		//等待时间模拟命令行操作的延迟时间
	    		try {
					Thread.sleep(latencyTime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		
	    		try {
					biddingForItem(i,tempstr[1],tempstr[2],price);
				} catch (SQLException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	else if(tempstr[0].equals("2"))//发起竞拍
	    	{
	    		int latencyTime = Integer.parseInt(tempstr[3]);
	    		
	    		//等待时间模拟命令行操作的延迟时间
	    		try {
					Thread.sleep(latencyTime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		
	    		try {
					raisingBidding(i,tempstr[1],tempstr[2]);
				} catch (SQLException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	else if(tempstr[0].equals("3"))//查询价格
	    	{
	    		int latencyTime = Integer.parseInt(tempstr[2]);
	    		
	    		//等待时间模拟命令行操作的延迟时间
	    		try {
					Thread.sleep(latencyTime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		
	    		try {
					biddingPrice(tempstr[1]);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }
	}
	
	public static void main(String[] args){
		//SC环判断
		SC sc = new SC();
		String file = "SC.txt";
		sc.solve(file);
		
		//local竞标者
		biddingBackEnd remoteBackEnd = new biddingBackEnd(0);	
		bidding localBidder = new bidding(0,remoteBackEnd);
		localBidder.bidderName = "bidder1";
		
		//remote竞标者
		biddingBackEnd localBackEnd = new biddingBackEnd(1);
		bidding remoteBidder = new bidding(1,localBackEnd);
		remoteBidder.bidderName = "bidder2";
		
		localBidder.start();
		remoteBackEnd.start();//监听bidToDoQueue
		
		remoteBidder.start();
		localBackEnd.start();
	}
	
}
