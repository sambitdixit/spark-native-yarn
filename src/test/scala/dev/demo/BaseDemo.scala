package dev.demo

import java.net.URLClassLoader
import java.net.URL
import java.io.File
import org.apache.spark.tez.instrument.TezInstrumentationAgent
import org.apache.spark.tez.TezConstants
import org.apache.tez.dag.api.TezConfiguration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import java.io.BufferedReader
import java.io.InputStreamReader
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.io.BytesWritable
import org.apache.hadoop.mapred.SequenceFileRecordReader
import org.apache.spark.SparkEnv
import java.nio.ByteBuffer
import org.apache.spark.tez.io.ComparableObjectWritable
import org.apache.spark.tez.io.DelegatingWritable

trait BaseDemo {

  def prepare(jobName:String, inputFiles:Array[String]) {
    val cl = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]
    val m = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL]);
	m.setAccessible(true);
	val file = new File("src/test/resources/mini/").toURI().toURL()
	m.invoke(cl, file)
    
    TezInstrumentationAgent.instrument

    System.setProperty(TezConstants.GENERATE_JAR, "true");
    val configuration = new TezConfiguration
    
    val fs = FileSystem.get(configuration);
    fs.delete(new Path(jobName + "_out"))
    inputFiles.foreach(x => fs.copyFromLocalFile(new Path(x), new Path(x)))
    
  }

  /**
   * 
   */
  def printResults(jobName: String) {
    println("=======================================")
    val conf = new TezConfiguration
    val fs = FileSystem.get(conf);
    val iter = fs.listFiles(new Path(jobName + "_out"), false);
    var counter = 0;
    var run = true
    while (iter.hasNext() && run) {
      val status = iter.next();
      if (status.isFile()) {
        if (!status.getPath().toString().endsWith("_SUCCESS")) {
          println("Sampled results from " + status.getPath() + ":");
         
          val r = new SequenceFile.Reader(conf, SequenceFile.Reader.file(status.getPath()))
          
          DelegatingWritable.setType(classOf[String])
          val key = new DelegatingWritable()
          val value = new BytesWritable
          
          val reader = new BufferedReader(new InputStreamReader(fs.open(status.getPath())));
          var line: String = null
          
          val ser = SparkEnv.get.serializer.newInstance
          while (r.next(key, value)){
            println(key.getValue)
          }

//          var x = true
//          while (x) {
//            line = reader.readLine()
//            if (line == null) {
//              x = false
//            } else {
//              println(line);
//              counter += 1
//            }
//            if (counter == 10) {
//              x = false
//              run = false
//            }
//          }
//          reader.close()
        }
      }
    }
    println("=======================================")
  }
}