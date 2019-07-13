import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

class MainThread extends Thread {

	private String controltype;
	private String vector;
	private int port_number;
	

	public MainThread(String controltype, String vector, int port) {
		this.controltype = controltype;
		this.vector = vector;
		this.port_number = port;
	}

	public MainThread(String controltype) {
		this.controltype = controltype;
	}

	public void run() {

		if (controltype.equalsIgnoreCase("r")) {
			MainRouter.readData();

		} else if (controltype.equalsIgnoreCase("w")) {

			while (true) {
				try {

					MainRouter.read();
					MainRouter.output();
					MainRouter.broadCast();

					Thread.sleep(6000);

					MainRouter.distanceAlgorithm();

					Thread.sleep(9000);

				} catch (Exception e) {
				}
			}
		} else if (controltype.equalsIgnoreCase("u")) {

			MainRouter.updateNetworkVectors(vector.split(":"), port_number);
		}
	}
}

public class MainRouter {

	public static int DisplayCount_router = 1;
	public static double[][] NetworkVectors_router;
	public static String[] Nodes_router;
	public static int[] Ports_router;
	public static int router_ID;
	public static double[] MyVector_router;
	public static String[] MyHopList_router;
	public static DatagramSocket Socket_router;
	public static String[] Neighbours_router;
	public static File File_router;
	

	public static void setParametres(int len, String[] args, int id, String parent) {

		NetworkVectors_router = new double[len][len];
		Ports_router = new int[len];
		Nodes_router = new String[len];

		for (int i = 0; i < len; i++) {
			Arrays.fill(NetworkVectors_router[i], Double.MAX_VALUE);
			NetworkVectors_router[i][i] = 0.0;
			String[] temp = args[i + 3].split(":");
			Nodes_router[i] = temp[0];
			Ports_router[i] = Integer.parseInt(temp[1]);
		}

		router_ID = id;
		MyVector_router = new double[len];
		MyHopList_router = new String[len];
		Arrays.fill(MyVector_router, Double.MAX_VALUE);
		MyVector_router[router_ID - 1] = 0.0;
		File_router = new File(parent + "/" + Nodes_router[router_ID - 1] + ".dat");
		try {
			Socket_router = new DatagramSocket(Ports_router[router_ID - 1]);
		} catch (SocketException e) {

			e.printStackTrace();
		}
		System.out.println("Router " + Nodes_router[router_ID - 1] + " is Working..!");

	}
	
	
	public static void distanceAlgorithm() {
		
		
		for (int i = 0; i < Neighbours_router.length; i++) {
			int ind = indexFinder(Neighbours_router[i]);
			for (int j = 0; j < MyVector_router.length; j++) {
				if (j == router_ID - 1) {
					continue;
				} else if (i == 0) {

					MyVector_router[j] = NetworkVectors_router[router_ID - 1][ind] + NetworkVectors_router[ind][j];
					MyHopList_router[j] = Neighbours_router[i];
				} else {

					if (MyVector_router[j] > NetworkVectors_router[router_ID - 1][ind] + NetworkVectors_router[ind][j]) {
						MyHopList_router[j] = Neighbours_router[i];
						MyVector_router[j] = NetworkVectors_router[router_ID - 1][ind] + NetworkVectors_router[ind][j];
					}

				}
			}
		}
		
		
	}


	public synchronized static void updateNetworkVectors(String[] vector, int port_number) {
		
		int index = 0;
		int ports_Length=Ports_router.length;
		while(index < ports_Length)
		{
			if (Ports_router[index] == port_number)
			{
				break;
			}
			index++;
		}
		if (index == ports_Length)
		{
			return;
		}
		for (int i = 0; i < vector.length; i++)
		{
			NetworkVectors_router[index][i] = Double.parseDouble(vector[i]);
		}
	}

	public static void broadCast() {

		try {
			for (int i = 0; i < Neighbours_router.length; i++) {
				String data = "";
				for (int j = 0; j < MyVector_router.length; j++) {
					if (Neighbours_router[i].equals(MyHopList_router[j])) {
						data = data + Double.MAX_VALUE + ":";
					} else {
						data += MyVector_router[j] + ":";
					}
				}
				DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length);
				packet.setAddress(InetAddress.getByName("localhost"));
				packet.setPort(Ports_router[indexFinder(Neighbours_router[i])]);
				Socket_router.send(packet);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void output() {
		System.out.println("> Output Number is " + DisplayCount_router++);
		System.out.println();
		String src = Nodes_router[router_ID - 1];
		for (int i = 0; i < MyVector_router.length; i++) {
			if (i != (router_ID - 1)) {
				String dest = Nodes_router[i];
				if (MyVector_router[i] == Double.MAX_VALUE) {
					System.out.println("shortest path from " + src + "to" + dest + ": " + " no route found");
				} else {
					System.out.println("shortest path from " + src + "to" + dest + " : the next hop is " + MyHopList_router[i]
							+ " and its cost is " + MyVector_router[i]);
				}
			}
		}

	}

	public static void readData() {
		boolean status = true;
		while (status) {
			try {
				String controltype = "u";
				byte[] data = new byte[1024];
				int size = data.length;
				DatagramPacket packet = new DatagramPacket(data, size);
				Socket_router.receive(packet);
				int length = packet.getLength();
				String vector = new String(packet.getData(), 0, length);
				MainThread updateThread = new MainThread(controltype, vector, packet.getPort());
				updateThread.start();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		int total = Integer.parseInt(args[2]);
		int currentNumber = Integer.parseInt(args[0]);
		String path = args[1];
		setParametres(total, args, currentNumber, path);

		MainThread readThread = new MainThread("r");
		readThread.start();

		MainThread writeThread = new MainThread("w");
		writeThread.start();

		while (true)
			;
	}

	public static void read() {
		try {
			Arrays.fill(NetworkVectors_router[router_ID - 1], Double.MAX_VALUE);
			NetworkVectors_router[router_ID - 1][router_ID - 1] = 0.0;
			BufferedReader br = new BufferedReader(new FileReader(File_router));
			int length = Integer.parseInt(br.readLine());
			Neighbours_router = new String[length];
			for (int i = 0; i < length; i++) {

				String[] temp = br.readLine().split(" ");
				int ind = indexFinder(temp[0]);
				Neighbours_router[i] = temp[0];

				if (DisplayCount_router == 1) {
					MyHopList_router[ind] = temp[0];
					MyVector_router[ind] = Double.parseDouble(temp[1]);
				} else {

				}
				NetworkVectors_router[router_ID - 1][ind] = Double.parseDouble(temp[1]);

			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int indexFinder(String temp) {
		int pos = -1;
		for (int i = 0; i < Nodes_router.length; i++) {
			if (Nodes_router[i].equals(temp)) {
				pos = i;
				break;
			}
		}
		return pos;
	}

}
