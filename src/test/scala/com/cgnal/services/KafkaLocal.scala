/*
 * Copyright 2016 CGnal S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cgnal.kafkaLocal

import java.io.File
import java.net.InetSocketAddress
import java.util.Properties

import com.typesafe.config.ConfigFactory
import kafka.admin.AdminUtils
import org.apache.commons.io.FileUtils
import kafka.server.{ KafkaConfig, KafkaServer }
import org.apache.log4j.LogManager
import org.apache.zookeeper.server._

/**
 * Created by cgnal on 12/09/16.
 *
 * Run an local instance of zookeeper and kafka
 */
object KafkaLocal {

  //val logger = LogManager.getLogger(this.getClass)

  var zkServer: Option[ServerCnxnFactory] = None
  var kafkaServer: Option[KafkaServer] = None

  private def startZK(): Unit = {
    if (zkServer.isEmpty) {

      val dataDirectory = System.getProperty("java.io.tmpdir")
      val dir = new File(dataDirectory, "zookeeper")
      println(dir.toString)
      if (dir.exists())
        FileUtils.deleteDirectory(dir)

      try {
        val tickTime = 5000
        val server = new ZooKeeperServer(dir.getAbsoluteFile, dir.getAbsoluteFile, tickTime)
        val factory = ServerCnxnFactory.createFactory
        factory.configure(new InetSocketAddress("0.0.0.0", 2181), 1024)
        factory.startup(server)
        println("ZOOKEEPER server up!!")
        zkServer = Some(factory)

      } catch {
        case ex: Exception => System.err.println(s"Error in zookeeper server: ${ex.printStackTrace()}")
      } finally { dir.deleteOnExit() }
    } else println("ZOOKEEPER is already up")
  }

  private def stopZK() = {
    if (zkServer.isDefined) {
      zkServer.get.shutdown()
    }
    println("ZOOKEEPER server stopped")
  }

  private def startKafka() = {
    if (kafkaServer.isEmpty) {
      val dataDirectory = System.getProperty("java.io.tmpdir")
      val dir = new File(dataDirectory, "kafka")
      if (dir.exists())
        FileUtils.deleteDirectory(dir)
      try {
        val props = new Properties()
        //props.setProperty("hostname", "localhost")
        props.setProperty("port", "9092")
        props.setProperty("broker.id", "0")
        props.setProperty("log.dir", dir.getAbsolutePath)
        props.setProperty("enable.zookeeper", "true")
        props.setProperty("zookeeper.connect", "localhost:2181")
        props.setProperty("advertised.host.name", "localhost")
        props.setProperty("connections.max.idle.ms", "9000000")

        // flush every message.
        props.setProperty("log.flush.interval", "1")

        // flush every 1ms
        props.setProperty("log.default.flush.scheduler.interval.ms", "1")

        val server: KafkaServer = new KafkaServer(new KafkaConfig(props))
        server.startup()
        println("KAFKA server on!!")

        kafkaServer = Some(server)
      } catch {
        case ex: Exception => System.err.println(s"Error in kafka server: ${ex.getMessage}")
      } finally {
        dir.deleteOnExit()
      }
    } else println("KAFKA is already up")
  }

  private def stopKafka(): Unit = {
    if (kafkaServer.isDefined) {
      kafkaServer.get.shutdown()

      println(s"KafkaServer ${kafkaServer.get.config.hostName} run state is: ${kafkaServer.get.kafkaController.isActive()} ")
    }
    println("KAFKA server stopped")
  }

  def createTopic(topic: String): Unit = {
    AdminUtils.createTopic(kafkaServer.get.zkUtils, topic, 3, 1, new Properties())
    println(s"Topic $topic created!")
  }

  def start(): Unit = {
    //startZK()
    Thread.sleep(5000)
    startKafka()
  }

  def stop(): Unit = {
    stopKafka()
    Thread.sleep(2000)
    stopZK()
  }

}

object Main extends App {
  KafkaLocal.start()
  val topic = ConfigFactory.load().getString("spark-opentsdb-exmaples.kafka.topic")
  KafkaLocal.createTopic("test-spec")

  //Thread.sleep(10000)
  //KafkaLocal.stop()

}