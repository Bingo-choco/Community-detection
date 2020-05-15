package AAA
import breeze.linalg.DenseMatrix
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import Array._
import scala.Array.ofDim
import scala.collection.{Map, mutable}
import scala.collection.mutable.Set

import scala.reflect.ClassTag
import scala.util.control.Breaks
  object PageRank {

    def main(args: Array[String]) {

      //屏蔽日志

      Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

      Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)



      //设置运行环境

      val conf = new SparkConf().setAppName("Read_text").setMaster("local")

      val sc = new SparkContext(conf)



      //读入数据文件

      val Nodes: RDD[String] = sc.textFile("Node_d.txt")
      val Edges: RDD[String] = sc.textFile("Edge_d.txt")
      //print(articles.first())

      //节点预先处理
      val vertices = Nodes.map { line =>
        val fields = line.split(' ')
        //自己明确要分割成什么样的数组
        //println("field(0)"+fields(0))
        (fields(0).toLong, fields(1))
      }

      //边装载
      val edges = Edges.map { line =>

        val fields = line.split(' ')

        Edge(fields(0).toLong, fields(1).toLong,0)
      }

      println("set大小"+vertices.count())
      val graph = Graph(vertices, edges, "").persist()

      //BK算法
      //创建一个Map集合。key是图中的所有顶点；value是一个Set集合，保存了该key的所有邻居顶点
      val map: Map[VertexId, Set[VertexId]] = graph.collectNeighborIds(EdgeDirection.Either).collect()
        .map(t => {
          var set: mutable.Set[VertexId] = Set[VertexId]()
          t._2.foreach(t=>{set+=t})
          (t._1, set)
        }).toMap

      //R集合，初始值为空
      var R = Set[VertexId]()
      //P集合，初始值为所有的顶点
      var P = Set[VertexId]()
      //将所有的顶点添加到P集合中
      vertices.collect().foreach(t=>{P+=t._1})
      //X集合，初始值为空
      var X = Set[VertexId]()

      //将找到的所有最大团存储在ans中
      var ans : Set[Set[VertexId]] = Set();

      //搜索极大团
      bronKerbosch2(R,P,X,map,ans)
      statistic(ans)

     /*----------------------------已经是二次重构图了--------------------------------------------*/

     val new_vertices = fliter_cliques(ans,10)

      //根据新节点，重构子图
      val subGraph = graph.subgraph(vpred = (id, attr) => new_vertices.contains(id))

      println("新形成的节点大小"+subGraph.vertices.count()+"新形成的边大小"+subGraph.edges.count())

      //在新形成的子图上寻找新的最大团

      //R集合，初始值为空
      var sub_R = Set[VertexId]()
      //P集合，初始值为所有的顶点
      var sub_P = Set[VertexId]()
      //将所有的顶点添加到P集合中
      new_vertices.foreach(t=>{sub_P+=t})
      //X集合，初始值为空
      var sub_X = Set[VertexId]()

      //将找到的所有最大团存储在ans中
      var sub_ans : Set[Set[VertexId]] = Set();

      val sub_map: Map[VertexId, Set[VertexId]] = graph.collectNeighborIds(EdgeDirection.Either).collect()
        .map(t => {
          var set: mutable.Set[VertexId] = Set[VertexId]()
          t._2.foreach(t=>{set+=t})
          (t._1, set)
        }).toMap
      //搜索极大团
      bronKerbosch2(sub_R,sub_P,sub_X,sub_map,sub_ans)
      println("子图结果：")
     var ttt:Set[VertexId] =Set()
      sub_ans.foreach(t => ttt= ttt++t)
      println("新形成节点总数"+ttt.size)

      var re = run(subGraph,2)

      var temp = re.vertices

      var ok = temp.collect()

      val groups = temp.map(t => t._2).distinct()

      println("每个种群大小")

      var ans_3 :Array[Set[VertexId]] = new Array[mutable.Set[VertexId]](12)

      for(i <- groups)
        {
          var c = 0
          for(j <- ok)
            {
              if(j._2 == i)
                c += 1
            }
          println(c)
        }

      /**
       * 之后就是对不同社群进行合并了
       */
    /* var need_sets =fliter_set(sub_ans, 18)
     var ans_1 = CPM_merge(need_sets,19)
     println("整合变阵：")
      ans_1.foreach(line =>println(line))
      var ans_2 = deal_matrix(ans_1,need_sets.size)
      println("合并结果：")
      for(i <- 0 to 829)
        {
          if(i % 50 == 0)
            println()
          print(ans_2(i)+" ")
        }
*/
      /**
       * 最后对归并的结果进行整合
       */

    }

    def bronKerboschl(R:Set[VertexId],P:Set[VertexId],X:Set[VertexId],map:Map[VertexId, Set[VertexId]],ans : Set[Set[VertexId]]): Unit ={
      if(P.toList.length ==0 && X.toList.length ==0){
        //增加条件不显示两个顶点的极大团
        println("find a maximal cilique:"+R)
      }else {
        for (v <- P) {
          var Nv: Set[VertexId] = map.get(v).get
          bronKerboschl(R+v, P.intersect(Nv), X.intersect(Nv), map,ans)
          X += v
          P -= v
        }
      }
    }

    def bronKerbosch2(R:Set[VertexId],P:Set[VertexId],X:Set[VertexId],map:Map[VertexId, Set[VertexId]],ans : Set[Set[VertexId]]): Unit ={
      if(P.toList.length ==0 && X.toList.length ==0){
        //增加条件不显示两个顶点的极大团
        //println("find a maximal cilique:"+R)
        copy_set(ans,R)
      }else {
        //集合头节点充当枢轴u
        var u: VertexId = (P ++ X).head
        var Nu: Set[VertexId] = map.get(u).get
        for (v <- (P &~ Nu)) {
          var Nv: Set[VertexId] = map.get(v).get
          bronKerbosch2(R+v, P.intersect(Nv), X.intersect(Nv), map, ans)
          X += v
          P -= v
        }
      }
    }

    def copy_set(fa:Set[Set[VertexId]], son:Set[VertexId]): Unit = {
      var re : Set[VertexId] = Set()
      son.foreach(t => re += t)
      fa += re
    }

    def statistic(fa:Set[Set[VertexId]]): Array[Int] = {
      //统计不同大小的极大团
      var re :Array[Int] = new Array[Int](22)
      fa.foreach( t =>
      {
        val num = t.size
        re(num) +=1
      })

      //控制台输出

      var i = 0
      re.foreach(t =>{
        println(i+" "+t)
        i+=1
      })

      return  re
    }

    def fliter_cliques(fa:Set[Set[VertexId]], k:Int): Set[VertexId] = {
      //节点数<=k的团过滤掉

      var re :Set[VertexId] = Set()

      var c =0;
      fa.foreach( t =>
      {
        val num = t.size
        if(num > k)
         {
           re = re ++ t
           c += 1
         }
      })
      //println("过滤了"+c)
      return  re
    }

    def fliter_set(fa:Set[Set[VertexId]], k:Int): Set[Set[VertexId]] = {
      //节点数<=k的团过滤掉

      var re :Set[Set[VertexId]] = Set()

      fa.foreach( t =>
      {
        val num = t.size
        if(num > k)
        {
          re += t
        }
      })
      return  re
    }

    def CPM_merge(fa:Set[Set[VertexId]], standard:Integer ):Array[Set[VertexId]] ={
      //根据CPM算法思想，得到合并矩阵
      println("集合大小"+fa.size)
      var matrix :Array[Set[VertexId]] = new Array[Set[VertexId]](fa.size)
      var i= 0
      var j =0;
      //针对社团组的双重循环
      fa.foreach(t =>
      {
        j = 0
        matrix(i) = Set()
        fa.foreach(r =>
        {
          var same = t.intersect(r).size
          if(same >= standard)
          {
            //构建新边
            matrix(i) += j
          }
          j+=1
        }
        )
        i+=1
      })
      return matrix
    }

    def dfs(matrix:Array[Set[VertexId]], ans: Array[Int], line:Int, num:Int): Unit = {
      //深搜归并社群
      for(i <- 0 to (num-1)) {
        if(matrix(line).contains(i) && ans(i) == 0)
        {
          ans(i) = ans(line)
          dfs(matrix,ans, i, num)
        }
      }
    }

    def deal_matrix(matrix:Array[Set[VertexId]], num:Int):  Array[Int]= {
      //默认re初始化均为0
      var re = new Array[Int](num);
      var count = 0;
      for(i <- 0 to (num-1))
      {
        //没有被归并过的
        if(re(i)==0)
        {
          count += 1
          re(i) = count
          dfs(matrix, re, i, num)
        }
      }

      return re;
    }

    def run[VD, ED: ClassTag](graph: Graph[VD, ED], maxSteps: Int): Graph[VertexId, ED] = {
      require(maxSteps > 0, s"Maximum of steps must be greater than 0, but got ${maxSteps}")

      val lpaGraph = graph.mapVertices { case (vid, _) => vid }
      def sendMessage(e: EdgeTriplet[VertexId, ED]): Iterator[(VertexId, Map[VertexId, Long])] = {
        Iterator((e.srcId, Map(e.dstAttr -> 1L)), (e.dstId, Map(e.srcAttr -> 1L)))
      }
      def mergeMessage(count1: Map[VertexId, Long], count2: Map[VertexId, Long])
      : Map[VertexId, Long] = {
        // Mimics the optimization of breakOut, not present in Scala 2.13, while working in 2.12
        val map = mutable.Map[VertexId, Long]()
        (count1.keySet ++ count2.keySet).foreach { i =>
          val count1Val = count1.getOrElse(i, 0L)
          val count2Val = count2.getOrElse(i, 0L)
          map.put(i, count1Val + count2Val)
        }
        map
      }
      def vertexProgram(vid: VertexId, attr: Long, message: Map[VertexId, Long]): VertexId = {
        if (message.isEmpty) attr else message.maxBy(_._2)._1
      }
      val initialMessage = Map[VertexId, Long]()
      Pregel(lpaGraph, initialMessage, maxIterations = maxSteps)(
        vprog = vertexProgram,
        sendMsg = sendMessage,
        mergeMsg = mergeMessage)
    }



  }
