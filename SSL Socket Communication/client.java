import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/*
 * @authors : Soham Aurangabadkar, Kapil Deshpande
 */
public class client 

{
	// global variables
	static String hostName;
	static int portNumber;
	static boolean ssl;
	static String NUID;


	/*
	 * method name : validator
	 * used to validate the input
	 * @param : String[] which contains the command line input to the program
	 * @return : true if validation successful, else false
	 */
	public static boolean validator(String[] args)
	{
		if(args.length < 2 || args.length > 4)
		{
			System.out.println("Usage: client <-p port> <-s> [hostname] [NUID]");
			return false;
		}
		else if(args.length == 2)
		{
			boolean hostnameValidate = hostnameValidator(args[0]);
			boolean NEUValidate = NEUValidator(args[1]);
			if (hostnameValidate && NEUValidate)
			{
				hostName = args[0];
				NUID = args[1];
				portNumber = 27993;
				ssl = false;
				return true;
			}
			else return false;
		}
		else if(args.length == 3)
		{
			boolean hostnameValidate = hostnameValidator(args[1]);
			boolean NEUValidate = NEUValidator(args[2]);
			boolean firstValidate = firstValidator(args[0]); 
			boolean secondValidate = secondValidator(args[0]);
			if (hostnameValidate && (secondValidate || firstValidate) && NEUValidate)
			{
				hostName = args[1];
				NUID = args[2];
				if(firstValidate)
				{
					portNumber = 27994;
					ssl = true;
				}
				else
				{
					portNumber = Integer.parseInt(args[0]);
					ssl = false;
				}
				return true;
			}
			else return false;
		}
		else if(args.length == 4)
		{
			boolean firstValidate = firstValidator(args[1]); 
			boolean secondValidate = secondValidator(args[0]);
			boolean hostnameValidate = hostnameValidator(args[2]);
			boolean NEUValidate = NEUValidator(args[3]);
			if (hostnameValidate && firstValidate && secondValidate && NEUValidate)
			{
				hostName = args[2];
				portNumber = Integer.parseInt(args[0]);
				ssl = true;
				NUID = args[3];
				return true;
			}
			else return false;
		}
		else return false;

	}

	/*
	 * Method : hostNameValidator(String)
	 * @param : String hostname
	 * @return : boolean validating the correctness of input
	 */

	public static boolean hostnameValidator(String hostname)
	{
		return (hostname.equals("cs5700.ccs.neu.edu") || hostname.equals("129.10.113.83"));
	}



	/*
	 * Method : firstValidator(String)
	 * @param : String first
	 * @return : boolean validating the correctness of input
	 */
	public static boolean firstValidator(String first)
	{
		if(first.equals("-s"))
			return true;
		else
			return false;
	}


	/*
	 * Method : secondValidator(String)
	 * @param : String second
	 * @return : boolean validating the correctness of input
	 */
	public static boolean secondValidator(String second)
	{
		try
		{
			int firstInt = Integer.parseInt(second);
			return (firstInt > 0 && firstInt <= 65535);
		}
		catch(Exception e)
		{
			return false;
		}
	}

	/*
	 * Method : NEUValidator(String)
	 * @param : String NUID
	 * @return : boolean validating the correctness of input
	 */
	public static boolean NEUValidator(String NUID)
	{
		return (NUID.length() == 9);
	}

	/*
	 * Method : main(String[])
	 * @param : String[] args
	 * 
	 */
	public static void main(String[] args) 
	{
		boolean validate = validator(args);
		if(validate)
		{
			try
			{
				System.setProperty("javax.net.ssl.trustStore", "trustcentre.jks");
				System.setProperty("javax.net.ssl.trustStorePassword", "kap007");

				System.setProperty("javax.net.ssl.keyStore","publicKeyKeyStore.p12");
				System.setProperty("javax.net.ssl.keyStorePassword","kap007");
				System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");


				// declare a BufferedReader and PrintWriter object to read and write from the socket
				BufferedReader rd;
				PrintWriter writer;

				Object sslSock;
				if(ssl)
				{
					// typecast sslSock to SSLSocketFactory as ssl is true
					SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					sslSock = (SSLSocket) factory.createSocket(hostName , portNumber);	

					String[] suites = ((SSLSocket) sslSock).getEnabledCipherSuites();
					int k=0;

					for(int i=0; i<suites.length; i++)
						if(suites[i].contains("SSL"))
							k++;
					String[] set = new String[k];
					k = -1;
					for(int i=0; i<suites.length; i++)
						if(suites[i].contains("SSL"))
							set[++k]=suites[i];
					// change the enabled cipher suites for sslSock
					((SSLSocket) sslSock).setEnabledCipherSuites(set);

					// establish IO streams
					rd = new BufferedReader(new InputStreamReader(((SSLSocket) sslSock).getInputStream()));
					writer = new PrintWriter(((Socket) sslSock).getOutputStream(), true);
				}
				else
				{
					// assign a new instance of Socket to sslSock
					sslSock = new Socket(hostName, portNumber);

					// establish IO streams
					rd = new BufferedReader(new InputStreamReader(((Socket) sslSock).getInputStream()));
					writer = new PrintWriter(((Socket) sslSock).getOutputStream(), true);
				}


				writer.println("cs5700spring2014 HELLO "+ NUID +"\n");

				String array[] = null;
				String message2 = null;
				String received = rd.readLine();
				boolean notCorrupt;
				// keep communicating with server till the received message does not contain "BYE"
				while(!received.contains("BYE"))
				{

					array = received.split(" ");
					notCorrupt = (corruptCheck(array));
					if(notCorrupt)
					{
						int first = Integer.parseInt(array[2].trim());
						int second = Integer.parseInt(array[4].trim());
						char operand = array[3].charAt(0);

						int result = 0;
						switch(operand)
						{
						case '+': result = first + second;
						break;
						case '-': result = first - second;
						break;
						case '/': result = first / second;
						break;
						case '*': result = first * second;
						break;
						default: System.out.println("Corrupt data received");
						break;
						}

						// send solution back to server in below format
						message2 = "cs5700spring2014 "+result+"\n";
						writer.println(message2);
						received = rd.readLine();
					}

					else
					{
						System.out.println("Corrupt data received");
						break;
					}


				}
				array = received.split(" ");
				System.out.println(array[1]);


				// close all streams
				rd.close();
				writer.close();
				((Socket) sslSock).close();

			}
			catch(Exception ex)
			{
				System.out.println(ex.getMessage());
			}
		}
		else 
		{
			System.out.println("Usage: client <-p port> <-s> [hostname] [NUID]");
		}
	}

	/*
	 * Method : corruptCheck(String[])
	 * @param : String[] args, which is the reply from the server
	 * @return : true if args is in the expected form, else false
	 */
	public static boolean corruptCheck(String[] args)
	{
		try
		{
			return ((args[0].equals("cs5700spring2014")) &&
					(args[1].equals("STATUS")) &&
					(Integer.parseInt(args[2]) >= 1 && Integer.parseInt(args[2]) <= 1000) &&
					(args[3].equals("+") || args[3].equals("-") || args[3].equals("*") || args[3].equals("/")) &&
					(Integer.parseInt(args[4]) >= 1 && Integer.parseInt(args[4]) <= 1000));
		}
		catch(Exception e)
		{
			return false;
		}

	}


}