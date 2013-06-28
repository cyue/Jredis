package test;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import test.mail.MailSenderInfo;
import test.mail.SimpleMailSender;

public class test {

	/**
	 * @param args
	 */
	public static String HOST = "localhost"
	public static int PORT = 6379;
	public static long RECONN_TIMEOUT_MS = 20000;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		sendMail("admin@YOURMAIL", "test subj", "test cont");
//		try {
//			writeToRedis();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Jedis monitor = new Jedis("localhost", 6379);
		List<String> master = monitor.sentinel("get-master-addr-by-name", "mymaster");
		System.out.println(master.get(0) + ":" + master.get(1));
		List<String> rely = monitor.sentinel("is-master-down-by-addr", "master_host", 6379);
		System.out.println(rely.get(0) + " " + rely.get(1));

	}
	
	public static void writeToRedis() throws InterruptedException{
		Jedis monitor = new Jedis("localhost", 6379);
		JedisPool pool = new JedisPool(new JedisPoolConfig(), HOST, PORT);
		Jedis  redis = pool.getResource();
		redis.flushDB();
		for (int i=0; i < 1000; ++i) {
			try{
				redis.set(Long.toString(i), Long.toString(i));
				System.out.println(redis.get(Long.toString(i)));
				Thread.sleep(1000);
			}catch( JedisConnectionException e) {
				Thread.sleep(20000);
				List<String> master = monitor.sentinel("get-master-addr-by-name", "mymaster");
				String host = master.get(0);
				String port = master.get(1);
				if (host.equals(HOST) && port.equals(Long.toString(PORT))) {
					pool.returnBrokenResource(redis);
					redis = pool.getResource();
				}else{
					sendMail("admin@youmail", "master down; switch completed!", HOST + ":" + PORT + " down");
					pool.returnResource(redis);
					pool.destroy();
					pool = new JedisPool(new JedisPoolConfig(), host, Integer.parseInt(port));
					redis = pool.getResource();	
					HOST=host;
					PORT=Integer.parseInt(port);
				}

			}
		}
	}
	
	public static void connExceptionHandler() {
		
	}
	
	public static void sendMail(String addr, String subj, String cont) {
		MailSenderInfo mailInfo = new MailSenderInfo();  
		mailInfo.setMailServerHost("mailserver.host")
		mailInfo.setMailServerPort("25");
		mailInfo.setValidate(false);
		mailInfo.setFromAddress("admin@redis-server");
		mailInfo.setToAddress(addr);
		mailInfo.setSubject(subj);
		mailInfo.setContent(cont);
		
		SimpleMailSender sms = new SimpleMailSender();
		sms.sendTextMail(mailInfo);
	}
}
	
