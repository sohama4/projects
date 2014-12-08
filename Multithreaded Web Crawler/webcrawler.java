import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.PrintWriter;

import java.net.InetAddress;

import java.net.Socket;

import java.util.HashSet;

import java.util.concurrent.LinkedBlockingQueue;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;

import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;





public class webcrawler implements Runnable

{

	/*

	 * global variables

	 */

	static int flagCounter = 0;

	public static LinkedBlockingQueue<String> queue=new LinkedBlockingQueue<String>();

	static String csrfToken;

	static String sessionId;

	static HashSet<String> visited = new HashSet<String>();

	String str;

	

	/*

	 * constructor

	 * @param: String object str

	 * Assignes str to the global variable str

	 */

	public webcrawler(String str)

	{

		this.str=str;

	}

	

	public webcrawler()

	{



	}

	/*

	 * method: main

	 * @param: String[] args

	 * Starting point of webcrawler, calls login with command-line arguments

	 */

	public static void main(String args[]) 

	{

		try

		{

			if(args.length != 2)

				throw new Exception("Usage: java webcrawler [username] [password]");

			webcrawler w = new webcrawler();

			w.login(args[0], args[1]);

		}

		catch(Exception e)

		{

			System.out.println(e.getMessage());

		}

	}



	/*

	 * method: login

	 * @param: String username, String password

	 * logs in to fakebook using parameters and starts the crawl procedure

	 */

	public void login(String username, String password) 

	{

		String host = "cs5700.ccs.neu.edu";

		int portNumber = 80;

		try 

		{

			

			Socket echoSocket = new Socket(InetAddress.getByName(host), portNumber);

			PrintWriter pw = new PrintWriter(echoSocket.getOutputStream());

			

			//GET HTTP request for the login form of the website

			pw.print("GET " + "/accounts/login/" + " HTTP/1.0\r\n" + 

					"Host: " + host +"\r\n\r\n"); 

			pw.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));

			String t;

			                





			//Hold the received cookies from the page

			String[] array = new String[2];

			int i=0;

			while((t = br.readLine()) != null) 

			{

				if(i == 2)

					break;



				if(t.contains("Set-Cookie:"))

				{

					array[i] = t;

					i++;

				}

			}

			br.close();

			pw.close();

			echoSocket.close();



			String csrfArray[] = array[0].split(" ");

			String sessionIdArray[] = array[1].split(" ");



			

			csrfToken = csrfArray[1];

			sessionId = sessionIdArray[1];
			




			//POST HTTP request for log-in using stored cookie

			String httpHeader="POST /accounts/login/ HTTP/1.0\r\n"

					+"Host: cs5700.ccs.neu.edu\r\n"

					+"Content-Length: 109\r\n"

					+"Content-Type: application/x-www-form-urlencoded\r\n"

					+"Cookie: "+csrfToken+" "+sessionId+"\r\n";



			echoSocket = new Socket(InetAddress.getByName(host), portNumber);

			pw = new PrintWriter(echoSocket.getOutputStream());

			br = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));











			pw.print(httpHeader);

			pw.println("");

			pw.print("csrfmiddlewaretoken="+csrfToken.substring(10, csrfToken.length() - 1)+"&username=" + username 

					+ "&password="+ password + "&next=%2Ffakebook%2F\r\n");



			pw.flush();



			while ((t = br.readLine()) != null) 

			{

				//handle incorrect username-password combination
				

				if(t.contains("200"))

				{

					pw.close();

					br.close();

					echoSocket.close();

					throw new Exception("Incorrect username and password");



				}

				if(t.contains("500"))

				{

					pw.close();

                                        br.close();

                                        echoSocket.close();

					throw new Exception("500: Internal Server Error");

				}

				//store new sessionid

				if(t.contains("sessionid"))

				{

					array[0] = t;

				}

				//store redirect location

				if(t.contains("Location:"))

				{

					array[1] = t;

					break;

				}



			}



			String locationArray[] = array[1].split(" ");

			String location = locationArray[1];

			pw.close();

			br.close();

			echoSocket.close();



			sessionIdArray = array[0].split(" ");

			sessionId = sessionIdArray[1];

			sessionId = sessionId.substring(0, sessionId.length() - 1);



			echoSocket = new Socket(InetAddress.getByName(host), portNumber);

			pw = new PrintWriter(echoSocket.getOutputStream());

			br = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));



			// GET HTTP request for homepage of the logged-in user

			httpHeader="GET "+location+" HTTP/1.0\r\n"

					+"Host: cs5700.ccs.neu.edu\r\n"

					+"Cookie: "+csrfToken+" "+sessionId+"\r\n\r\n";

			pw.print(httpHeader);

			pw.flush();





			StringBuilder sb = new StringBuilder();



			while((t = br.readLine()) != null)

			{



				if(t.contains("<a href=\""))

					sb.append(t);



			}



			

			String html = sb.toString();



			// parse HTML string to locate URLs in the <a> tag

			Document doc = Jsoup.parse(html);

			Elements links = doc.select("a[href]");

			

			/*

			 *  parse HTML string to search for 64 bit secret flags stored as:

			 *  <h2 class='secret_flag' style="color:red">FLAG: 64-characters-of-random-alphanumerics</h2>

			 */

			Elements secretFlags = doc.getElementsByClass("secret_flag");

			

			//store the URLs in global variable queue

			for(Element link : links)

			{

				if(link.toString().contains("fakebook"))

					queue.add(link.attr("href"));

			}



			if(!secretFlags.isEmpty())

			{

				flagCounter++;

				for(Element flag : secretFlags)

					System.out.println(flag.text().substring(6));

			}





			breadthFirstSearch();



			pw.close();

			br.close();

			echoSocket.close();





		} 

		catch (Exception e) 

		{
			System.out.println(e.getMessage());

		} 

	}



	/*

	 * method: breadthFirstSearch

	 * @param: none

	 * spawns Thread instances to crawl all the URLs in queue

	 */

	void breadthFirstSearch()

	{



		Thread t[] = new Thread[queue.size()];

		int i = 0;

		int j=0;

		while(!queue.isEmpty() && j<25)

		{

			String str1 = queue.remove();

			t[i]=new Thread(new webcrawler(str1));

			t[i].start();

			i++;

			j++;

		}

		for (int k = 0; k < i; k++) 

		{

			try 

			{

				t[k].join();

			} 

			catch (Exception e) 

			{



				System.out.println(e.getMessage());

			}



		}

		/*

		 * use emptiness of queue and number of flags counted as 

		 * terminating condition

		 */

		if(!queue.isEmpty() && flagCounter != 5)

			breadthFirstSearch();



	}

	@Override

	public void run()

	{

		try

		{

			boolean error;

			String t;

			StringBuilder sb;



			Socket echoSocket1=null;

			

			//check if visited contains str before crawling

			if(!visited.contains(str))

			{
			<a href="../fileview/Default.aspx?~/experiments/4html">HTML</a>

				sb = new StringBuilder();

				echoSocket1 = new Socket(InetAddress.getByName("cs5700.ccs.neu.edu"), 80);

				PrintWriter pw = new PrintWriter(echoSocket1.getOutputStream());

				BufferedReader br = new BufferedReader(new InputStreamReader(echoSocket1.getInputStream()));



				//GET HTTP request for str

				String httpHeader= new String("GET "+str+" HTTP/1.0\r\n"

						+"Host: cs5700.ccs.neu.edu\r\n"

						+"Cookie: "+csrfToken+" "+sessionId+"\r\n\r\n");

				pw.print(httpHeader);

				pw.flush();



				error = false;



				// parse HTTP response

				while((t = br.readLine()) != null)

				{

					if(t.contains("500"))

						error = true;



					if(t.contains("<a href=\""))

						sb.append(t);



				}

				//no error, implies that the URL is successfully visited

				if(error == false)

				{

					visited.add(str);

				}

				//error, implies that the URL could not be visited, so enqueue it again

				else

				{

					queue.add(str);	

				}

				String html=sb.toString();

				

				// parse HTML string to locate URLs in the <a> tag

				Document doc = Jsoup.parse(html);

				Elements links = doc.select("a[href]");

				

				/*

				 *  parse HTML string to search for 64 bit secret flags stored as:

				 *  <h2 class='secret_flag' style="color:red">FLAG: 64-characters-of-random-alphanumerics</h2>

				 */

				Elements secretFlags = doc.getElementsByClass("secret_flag");

				for(Element link : links)

				{

					if(link.toString().contains("fakebook"))

						queue.add(link.attr("href"));

				}



				if(!secretFlags.isEmpty())

				{

					flagCounter++;

					for(Element flag : secretFlags)

						System.out.println(flag.text().substring(6));

				}

				pw.close();

				br.close();

				echoSocket1.close();

			}



		}

		catch(Exception e)

		{

			System.out.println(e.getMessage());

		}

	}





}
