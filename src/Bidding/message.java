package Bidding;

public class message extends Thread{
	private int localOrRemote;//��ʾ�������Ǳ��ػ������ 0Ϊ���أ�1ΪԶ��
	private String name;
	private String item;
	private int price;
	private int biddingOrRaisingBid; //0Ϊ���ģ�1Ϊ�����ġ�
	private biddingBackEnd backEnd;
	private int operationIndex;//������˳��
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

	//˯��seconds����
	public void sleepForSeconds(int milliseconds) throws InterruptedException{
		Thread.sleep(milliseconds);
	}
	
	//������غ�̨
	public void run()
	{   
		try {
			sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(biddingOrRaisingBid == 0)//����,ѹ��biddingToDoQueue
		{
			backEnd.pushBidToDoQueue(operationIndex,name,item,price);

		}
		else if(biddingOrRaisingBid == 1)//�����ģ�ѹ��raisingBidToDoQueue
		{
			backEnd.pushRaisingBidToDoQueue(operationIndex,name,item,price);
		}
	}
}
