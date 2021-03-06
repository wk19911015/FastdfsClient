package net.mikesu.fastdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration.ConfigurationException;

public class MultiThreadUploadTest {
	
	private String prefix = "D:\\imgs\\";
	private String[] imgs = new String[]{"xqx.jpg","mn.jpg","js.jpg","xsk.jpg","bb.jpg","ha.jpg","qc.jpg"/*,"aqy.f4v"*/};
	
	private AtomicInteger index = new AtomicInteger();
	private FastdfsClient client;
	private int threadCount;
	private int time;
	private long interval;
	private List<Thread> uploadThreads = new ArrayList<Thread>();
	
	private AtomicLong totalCost = new AtomicLong();
	private AtomicLong totalUploadCount = new AtomicLong();
	private CountDownLatch latch;
	
	private byte[] readFile(File file) throws IOException{
		byte[] buf = new byte[(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(buf);
		fis.close();
		return buf;
	}
	
	public void start(){
		int c = 0;
		latch = new CountDownLatch(threadCount);
		while(c<threadCount){
			UploadThread thread = new UploadThread(time, interval);
			thread.start();
			uploadThreads.add(thread);
			c++;
		}
	}
	
	public void waitForFinish(){
		try {
			latch.await();
		} catch (InterruptedException e) {
			
		}
	}
	
	public void stop(){
		for(Thread th:uploadThreads){
			th.interrupt();
		}
		client.close();
	}
	
	private class UploadThread extends Thread{
		private int time;
		private long interval;
		
		public UploadThread(int time,long interval){
			this.time = time;
			this.interval = interval;
		}
		@Override
		public void run() {
			int t = 0;
			while(t<time){
				try {
					String f = imgs[index.incrementAndGet()%imgs.length];
					File file = new File(prefix+f);
					if(file.exists()){
						byte[] bs = readFile(file);
						long start = System.currentTimeMillis();
						String upload = client.upload(file);
						HashMap<String, String> meta = new HashMap<String,String>();
						meta.put("haha", "hahashduiw");
						meta.put("file", f);
						client.setMeta(upload, meta);
//						client.upload(file, f, "group1");
//						String upload = client.upload(bs, file.getName(),"group2");
						System.out.println(upload+" source:"+f);
						long end = System.currentTimeMillis();
						totalCost.addAndGet(end-start);
						totalUploadCount.incrementAndGet();
						Thread.sleep(interval);
					}
				} catch (InterruptedException e) {
					break;
				}catch(Exception e){
					e.printStackTrace();
					break;
				}
				t++;
			}
			latch.countDown();
		}
	}
	
	public static void main(String[] args) throws ConfigurationException {
		FastdfsClient fastdfsClient = FastdfsClientFactory.getFastdfsClient("FastdfsClient.properties");
		MultiThreadUploadTest test = new MultiThreadUploadTest();
		test.time = 10;
		test.interval = 100;
		test.threadCount = 1;
		test.client = fastdfsClient;
		test.start();
		test.waitForFinish();
		System.out.println("totalCount:"+test.totalUploadCount.get()+" totalCost:"+test.totalCost.get());
	}
	

}
