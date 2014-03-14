package splitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncFileSplitter {
	ArrayList<Long> start = new ArrayList<Long>();
	ArrayList<Long> end = new ArrayList<Long>();
	AsynchronousFileChannel fileChannel ;
	long chunkSize;
	long offset=0;
	byte[] lf= System.getProperty("line.separator").getBytes();
	
	
	public AsyncFileSplitter(long nChunkSize) {
		chunkSize = nChunkSize;
			}

	public void map() {
		File file = new File(
				"/home/bsendir1/workspacemarla/materials_dbv2-04052013.json");
		
		try {
			RandomAccessFile raf;
			raf = new RandomAccessFile(file, "rw");
			System.out.println(file.length());
			raf.setLength(file.length());
			long t0 = System.currentTimeMillis();
			System.out.println(raf.length());
		
			while (offset < raf.length()) {
				if ((offset + chunkSize) > raf.length()) {
					chunkSize = raf.length() - offset;
				}
				long diff=findNextLine(raf);
				start.add(offset);
				end.add(chunkSize+diff);
				offset = offset+ chunkSize+ diff;
			}
			raf.close();
			long t1 = System.currentTimeMillis();
			System.out.println("Mapping Took: " + (t1 - t0) + "ms");
			System.out.println("ITEMS "+start.size() + " "+ end.size()+" ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private long findNextLine(RandomAccessFile raf) throws IOException{
		raf.seek(offset+chunkSize);
		long old=raf.getFilePointer();
		System.out.println("Pre:" + raf.getFilePointer());
		raf.readLine();
		System.out.println("Post:"+raf.getFilePointer());
		long newp = raf.getFilePointer();
		return newp - old;
	}

	public void split() {
		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService e = Executors.newFixedThreadPool(cores);
		System.out.println("Executing with " + cores + " threads.");
		long t0 = System.currentTimeMillis();
		try {
			fileChannel = AsynchronousFileChannel.open(Paths.get("/home/bsendir1/workspacemarla/materials_dbv2-04052013.json"));
			System.out.println("-------------------------->" + fileChannel.size());
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		for (int i = 0; i < start.size(); i++) {
			e.execute(new SplitWorker(start.get(i),end.get(i), i));
			
		}
		e.shutdown();
		try {
			e.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		long t1 = System.currentTimeMillis();
		System.out.println("Splitting: " + (t1 - t0) + "ms");
	}
	
	
	public class SplitWorker implements Runnable {
		int split_num;
		Long st;
		Long en;
		
		public SplitWorker(Long nstart,Long nend, int i) {
			st=nstart;
			en=nend;
			split_num = i;
		}
		public void run() {
			
			System.out.println(split_num+ "th task started");

			FileChannel wChannel;
			try {
				wChannel = new FileOutputStream(new File("/home/bsendir1/workspacemarla/FileSplitter_new/splits/temp"
						+ split_num)).getChannel();
			       
				ByteBuffer buf = ByteBuffer.allocateDirect((int)((long) en));
				System.out.println("test   " + (int)((long) en));
				Future<Integer> x=fileChannel.read(buf,st);
				while(!x.isDone()){}
				System.out.println("CAPACITY "+buf.capacity());
				buf.flip();
				wChannel.write(buf);
				// maps.get(i).load().asReadOnlyBuffer()
				wChannel.close();
				// capacity gets map size
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}