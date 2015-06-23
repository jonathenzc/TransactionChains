package Bidding;

public class queueElement {
	public int operIndex;
	public String seller;
	public String item;
	public int price; 
	
	public queueElement(int theOperIndex,String theSeller,String theItem,int thePrice)
	{
		operIndex = theOperIndex;
		seller = theSeller;
		item = theItem;
		price = thePrice;
	}
}
