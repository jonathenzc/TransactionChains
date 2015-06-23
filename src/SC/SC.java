package SC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class SC {

	static int n;//节点个数
	static int m = 0;//边的条数
	static Vector<Vector<Integer>> adj = new Vector<Vector<Integer>>();//记录邻接矩阵,-1表示无边,0表示S边,1表示C边
	static Vector<Integer> deg = new Vector<Integer>();//记录点的度数
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String fileName = "/Users/tss/Desktop/csdi/SC";
		read(fileName);
//		print();
		getSC();
	}
	
	public void solve(String file)
	{
		read(file);
		getSC();
	}

	private static void getSC() {
		Vector<Vector<Boolean>> visited = new Vector<Vector<Boolean>>();//用来记录边是否被访问过
		for (int i = 0;i<n;i++)
		{
			Vector<Boolean> tempVisited = new Vector<Boolean>();
			for (int j = 0;j<n;j++)
				tempVisited.add(false);
			visited.add(tempVisited);
		}//初始化边访问表
		while (true)
		{
//			print();
			if (m==0)
			{
				System.out.println("该图不存在SC环！");
				return;
			}
			if (clear()) continue;//清除图中度数为1的点,清理到不存在度数为1的点为止
			for (int i = 0;i<n;i++)
				if (deg.get(i)!=0)
				{
					Vector<Integer> stack = new Vector<Integer>();
					stack.add(i);
					int now = i;
					int start = 0;//记录stack当中的环的起点
					while (true)
					{
						boolean jump = false;
						for (int j = 0;j<stack.size()-1;j++)
							if (now==stack.get(j))
							{
								start = j+1;
								jump = true;
								break;
							}
						if (jump) break;
						for (int j = 0;j<n;j++)
							if (adj.get(now).get(j)!=-1 && visited.get(now).get(j)==false)//now和i之间有边且未访问过
							{
								stack.add(j);
								visited.get(now).set(j, true);
								visited.get(j).set(now, true);
								now = j;
								break;
							}
					}
//					for (int j = 0;j<stack.size();j++)
//						System.out.println(stack.get(j));
					boolean existS = false;//记录环是否存在S边
					boolean existC = false;//记录环是否存在C边
					for (int j = start;j<stack.size();j++)
					{
						int tempFrom = stack.get(j);
						int tempTo;
						if (j==stack.size()-1)
						{
							tempTo = stack.get(start);
						}
						else tempTo = stack.get(j+1);
						if (adj.get(tempFrom).get(tempTo)==0 && !existS)//从tempFrom到tempTo是S边且之前不存在S边
						{
							if (existC)
							{
								System.out.println("存在SC环!");
								return;
							}
							existS = true;
						}
						else if (adj.get(tempFrom).get(tempTo)==1 && !existC)//从tempFrom到tempTo是C边且之前不存在C边
						{
							if (existS)
							{
								System.out.println("存在SC环!");
								return;
							}
							existC = true;
						}
					}//检验是否存在SC环，如果是直接return，否则继续
					Vector<Integer> circle = new Vector<Integer>();
					for (int j = start;j<stack.size();j++)
						circle.add(stack.get(j));
					removeCircle(circle);
					break;
					//TODO:找到环的起点，判断这个环是否是SC环，如果是返回，如果不是，去掉该环
				}
		}
	}

	private static void removeCircle(Vector<Integer> circle) {//在原图中去掉circle环
		for (int i = 0;i<circle.size();i++)
		{
			int tempFrom = circle.get(i);
			int tempTo;
			if (i==circle.size()-1)
				tempTo = circle.get(0);
			else tempTo = circle.get(i+1);
			adj.get(tempFrom).set(tempTo, -1);
			adj.get(tempTo).set(tempFrom, -1);
			deg.set(tempFrom, deg.get(tempFrom)-1);
			deg.set(tempTo, deg.get(tempTo)-1);
			m--;
		}
	}

	private static boolean clear() {//清除图中度数为1的点
		boolean flag = false;
		for (int i = 0;i<n;i++)
		{
			if (deg.get(i)==1)
			{
				flag = true;
				removeEdge(i);
			}
		}
		return flag;
	}

	private static void removeEdge(int x) {//清除节点x的边
		for (int i = 0;i<n;i++)
		{
			if (adj.get(x).get(i)!=-1)
			{
				adj.get(x).set(i, -1);
				adj.get(i).set(x, -1);
				deg.set(x,0);
				deg.set(i, deg.get(i)-1);
				m--;
				return;
			}
		}
	}

	private static void print() {//输出邻接矩阵
		for (int i = 0;i<n;i++)
		{
			for (int j = 0;j<n;j++)
				System.out.print(adj.get(i).get(j)+" ");
			System.out.println();
		}
		System.out.println("m = "+m);
	}

	private static void read(String fileName) {//读文件，生成度数表和邻接矩阵
		File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = reader.readLine();
            n = Integer.parseInt(tempString);
            init();
            while ((tempString = reader.readLine()) != null) {
            	m++;
            	String[] tempstr = tempString.split(" ");
            	if (tempstr.length>2)
            	{
            		int tempFrom = Integer.parseInt(tempstr[0]);
            		int tempTo = Integer.parseInt(tempstr[1]);
            		String temp = tempstr[2];
            		int type;
            		if (temp.equals("S"))
            			type = 0;
            		else type = 1;
            		adj.get(tempFrom).set(tempTo, type);
            		adj.get(tempTo).set(tempFrom, type);
            		deg.set(tempTo, deg.get(tempTo)+1);
            		deg.set(tempFrom, deg.get(tempFrom)+1);
            	}
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
	}

	private static void init() {//初始化邻接表以及度数表
		for (int i = 0;i<n;i++)
		{
			Vector<Integer> temp = new Vector<Integer>();
			for (int j = 0;j<n;j++)
				temp.add(-1);
			adj.add(temp);
		}
		for (int i = 0;i<n;i++)
			deg.add(0);
	}

}
