package Bidding;

public class message extends Thread{
	private int localOrRemote;//表示竞标者是本地还是异地 0为本地，1为远程
	private String name;
	private String item;
	private int price;
	private int biddingOrRaisingBid; //0为竞拍，1为发起竞拍。
	private biddingBackEnd backEnd;
	private int operationIndex;//操作的顺序
	private int time;
	
	public message(int place,int theChoice,int theOperIndex,String theName,String theItem,int thePrice,biddingBackEnd theBackEnd,int theTime) {
		localOrRemote = place;
		biddingOrRaisingBid = theChoice;
		name = theName;
		item = theItem;
		price = thePrice;
		backEnd = theBackEnd;
		operationIndex = theOperIndex;
		time = theTime;
	}

	//睡眠seconds秒数
	public void sleepForSeconds(int milliseconds) throws InterruptedException{
		Thread.sleep(milliseconds);
	}
	
	//唤醒异地后台
	public void run()
	{   
		try {
			sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(biddingOrRaisingBid == 0)//竞拍,压入biddingToDoQueue
		{
			backEnd.pushBidToDoQueue(operationIndex,name,item,price);

		}
		else if(biddingOrRaisingBid == 1)//发起竞拍，压入raisingBidToDoQueue
		{
			backEnd.pushRaisingBidToDoQueue(operationIndex,name,item,price);
		}
	}
}
