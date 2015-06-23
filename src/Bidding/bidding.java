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
	private String bidderName;//�����ߵ�����
	private int localOrRemote;//��ʾ�������Ǳ��ػ������ 0Ϊ���أ�1ΪԶ��
	private biddingBackEnd backEnd;//ÿ�������߶��и���̨�߳�����ִ�з��͸�������ݿ�,ÿ�������߻�������Ϣ�̻߳�����صĺ��
	private Vector<String> operationV;//��¼�ı�Ԥ�������
	
	//���ݿ����ӱ���
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
	
	//���ر�������ID
	public int maxID(String sql) throws SQLException
	{
		int ID = 0;
		
		// ����mysql����bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);
		
		Statement statement = biddingConn.createStatement();
		ResultSet rs;
		int id = 0;

		rs = statement.executeQuery(sql);
		
		if(rs.next())//�����ID����ԭID+1
			id = rs.getInt("ID")+1;
		
		//�ر�mysql����emailGraph
		closeSqlConnection(biddingConn);
		
		return ID;
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
	
	/*********************************�����ߵ������������ֺ���**************************************/
	/*********************************���ֺ���1������**********************************************/
	//����local����remote��bid����item��
	//bidOrItem: 0Ϊbid��1Ϊitem
	public void insert(int bidOrItem,String name,String item, int price) throws SQLException
	{
		String insertSql = "insert into ";
		String tableName = "";
		String tableBidColumn = "(Bidder,Item,Price) ";
		String tableItemColumn = "(Seller,Item,HighestBid) ";
		//��ȡ����ı��
		if(localOrRemote == 0 && bidOrItem == 0)//local_bid
			tableName = "local_bid"+tableBidColumn;
		else if(localOrRemote == 0 && bidOrItem == 1)//local_item
			tableName = "local_item"+tableItemColumn;
		else if(localOrRemote == 1 && bidOrItem == 0)//remote_bid
			tableName = "remote_bid"+tableBidColumn;
		else if(localOrRemote == 1 && bidOrItem == 1)//remote_item
			tableName = "remote_item"+tableItemColumn;
			
		insertSql += tableName + " values(";
		
		//�����sql
		insertSql += "\"" + name + "\","
				  + "\"" + item + "\","
		          + "\"" + price + "\")"; //add anotherName
		
		System.out.println(insertSql);
		
		//�������ݿ�
		insertHelper(insertSql);
	}
	
	/*********************************���ֺ���2�����ص�ǰ��߼�*************************************/
	//��ѯlocal���ݿ���
	//bidOrItem: 0Ϊbid��1Ϊitem
	public int highestPrice(int bidOrItem,String item) throws SQLException
	{
		// ����mysql����bidding
		Connection biddingConn = buildSqlConnection(biddingUrl);

		//��ȡ��ѯ�ı��
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
		
		//�ر�mysql����emailGraph
		closeSqlConnection(biddingConn);
		
		return highestBid;
	}
	
	/*********************************���ֺ���3�����¾��ļ�*****************************************/
	//ֱ�ӽ��и��²���
	public void updateBiddingPrice(String item,int price) throws SQLException
	{
		String updateSql = "update ";
		String tableName = "_item";
		//��ȡ���µı�
		if(localOrRemote == 0)//local
			tableName = "local"+tableName;
		else if(localOrRemote == 1)//remote
			tableName = "remote"+tableName;
			
		updateSql += tableName + " set HighestBid=" + price + " where Item='"+item+"' and HighestBid < "+price;
			
//		System.out.println(updateSql);		
		updateHelper(updateSql);
		System.out.println(updateSql);
	}
	
	/*********************************�����ߵ�������������**************************************/
	/*********************************��������1����ѯ���ļ�*************************************/
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
	
	/*********************************��������2������ *******************************************/
	public void biddingForItem(int operIndex,String bidder,String item,int price) throws SQLException, InterruptedException
	{
		//Step1: ���뱾��bid��
		insert(0,bidder,item,price);
		//Step2: ���±���item��
		updateBiddingPrice(item,price);
		//Step3: ����һ���ԣ��������item��
		//����һ����Ϣ�߳�
		Random random = new Random();
		int randomValue = Math.abs(random.nextInt())%500+500;//����500-1000�������
//		
		message msg = new message(localOrRemote,0,operIndex,bidder,item,price,backEnd,randomValue);
		//����message���ڲ���ordering
//		message msg = new message(localOrRemote,0,operIndex,bidder,item,price,backEnd,500*(10-operIndex));
		
//		msg.sleepForSeconds(500*(10-operIndex));//˯��5��ģ����ش�����ӳ�ʱ��
		msg.start();//����Ϣrun�����в�����غ��bidToDoQueue
		//�ڲ����������к���غ�˽�Ҫ��ordering�ж�
	}
	
	/*********************************��������3��������***************************************/
	public void raisingBidding(int operIndex,String seller,String item) throws SQLException, InterruptedException
	{
		//Step1: ���뱾��item��
		insert(1,seller,item,0);
		//Step2: ����һ���ԣ��������item��
		Random random = new Random();
		int randomValue = Math.abs(random.nextInt())%500+500;//����500-1000�������
		
		message msg = new message(localOrRemote,1,operIndex,seller,item,0,backEnd,randomValue);
		//����message���ڲ���ordering
//		message msg = new message(localOrRemote,1,operIndex,seller,item,0,backEnd,500*(10-operIndex));
//		msg.sleepForSeconds(500);//˯��5��ģ����ش�����ӳ�ʱ�� ��ʱ��ĳ����
		msg.start();//����Ϣrun�����в�����غ��raisingBidToDoQueue
	}
	
	
	//���߳�ִ�к���
	public void run(){
		/*********************************Ԥ����������ı����ݴ��������***************************************/
		//��ȡ�����ļ�����
		String fileName = "Operation.txt";//�����ļ���localOperation.txt��Զ���ļ���remoteOpertion.txt
		int lineNum = 0;//�ļ��к�
		
		if(localOrRemote == 0)//local
			fileName = "local"+fileName;
		else if(localOrRemote == 1)//remote
			fileName = "remote"+fileName;
		
		//��ȡ�ļ�
		File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = "";
            while ((tempString = reader.readLine()) != null) {
            	operationV.add(tempString);
            	
            	//�ж��û�����
            	if(tempString.startsWith("1"))//����
            	{
            		//��غ�̨ѹ�к�
            		backEnd.pushBidQueue(lineNum);
            	}
            	else if(tempString.startsWith("2"))//������
            	{
            		//��غ�̨ѹ�к�
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
        
		/*********************************��ȡԤ�������***************************************/
        for(int i=0;i<operationV.size();i++)
        {
	    	//�����ʽ�Կո񻮷�
	    	String[] tempstr = operationV.get(i).split(" ");
	    	
	    	//�ж��û�����
	    	if(tempstr[0].equals("1"))//����
	    	{
	    		int price = Integer.parseInt(tempstr[3]);
	    		int latencyTime = Integer.parseInt(tempstr[4]);
	    		
	    		//�ȴ�ʱ��ģ�������в������ӳ�ʱ��
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
	    	else if(tempstr[0].equals("2"))//������
	    	{
	    		int latencyTime = Integer.parseInt(tempstr[3]);
	    		
	    		//�ȴ�ʱ��ģ�������в������ӳ�ʱ��
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
	    	else if(tempstr[0].equals("3"))//��ѯ�۸�
	    	{
	    		int latencyTime = Integer.parseInt(tempstr[2]);
	    		
	    		//�ȴ�ʱ��ģ�������в������ӳ�ʱ��
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
		//SC���ж�
		SC sc = new SC();
		String file = "SC.txt";
		sc.solve(file);
		
		//local������
		biddingBackEnd remoteBackEnd = new biddingBackEnd(0);	
		bidding localBidder = new bidding(0,remoteBackEnd);
		localBidder.bidderName = "bidder1";
		
		//remote������
		biddingBackEnd localBackEnd = new biddingBackEnd(1);
		bidding remoteBidder = new bidding(1,localBackEnd);
		remoteBidder.bidderName = "bidder2";
		
		localBidder.start();
		remoteBackEnd.start();//����bidToDoQueue
		
		remoteBidder.start();
		localBackEnd.start();
	}
	
}
