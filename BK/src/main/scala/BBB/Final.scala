import AAA.PageRank.{bronKerbosch2, fliter_cliques, statistic}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx.{Edge, EdgeDirection, EdgeTriplet, Graph, Pregel, VertexId}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import Array._
import scala.collection.{Map, mutable}
import scala.collection.mutable.Set
import scala.reflect.ClassTag

object Community {

  def main(args: Array[String]) {

    //屏蔽日志

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

    //设置运行环境

    val conf = new SparkConf().setAppName("Graph_init").setMaster("local")

    val sc = new SparkContext(conf)

    //读入数据文件

    val Edges: RDD[String] = sc.textFile("T_Edge.txt")
    //边装载
    val edges = Edges.map { line =>

      val fields = line.split(' ')

      Edge(fields(0).toLong, fields(1).toLong,0)
    }

    //自己制造节点 1-1294结点
    var c = range(1,1294)
    var tt = sc.parallelize(c);
    var vertices = tt.map{line=>
      (line.toLong,"")
    }

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

    println("结点数："+graph.vertices.count()+"边数"+graph.edges.count())

    //搜索极大团
    bronKerbosch2(R,P,X,map,ans)
    //统计极大团结果
    statistic(ans)

    /*------------------------------对图进行重构subGraph--------------------------------------*/

    //将大小为1的极大团过滤掉
    val new_vertices = fliter_cliques(ans,1)

    //根据新节点，重构子图
    val subGraph = graph.subgraph(vpred = (id, attr) => new_vertices.contains(id))

    println("新形成的节点大小"+subGraph.vertices.count()+"新形成的边大小"+subGraph.edges.count())

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
    statistic(sub_ans)

    /*-------------------------------------通过CPM算法进行社群划分-------------------------------------------------------*/
    //包含两个及以上相同节点，即进行合并
    val temp = CPM_merge(sub_ans, 2)
    val ans_2 = deal_matrix(temp, sub_ans.size)

    /*&& 用于显示结果
    val cc = ans_2.size-1
    for( i <- 0 to cc)
      {
        if(i%20 == 0)
          println()
        print(ans_2(i)+" ")
      }
    && 用于显示结果*/

    val ans_3 = deal_ans(ans_2 , sub_ans)
    var count = 0
    var standard:Double = 0
    println("最终结果：")
    ans_3.foreach(t => {
      if(t.size > 0)
        {
          //println(t)
          standard += count_Q(t,subGraph)
          count +=1
        }
    })
    println("种群大小："+count+" 模块度大小；  "+standard)


    /*-------------------------------通过LPA算法进行合并------------------------------------------
    var lpa_ans = run(subGraph,20)

    lpa_ans.vertices.foreach( t => println(t))

    val groups = lpa_ans.vertices.map(t => t._2).distinct()

    val ok = lpa_ans.vertices.collect()
    println("种群大小"+groups.count())
    println("每个种群大小")


    for(i <- groups)
    {
      var c = 0
      for(j <- ok)
      {
        if(j._2 == i)
          c += 1
      }
      println(c)
    }*/
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
    var re :Array[Int] = new Array[Int](50)
    fa.foreach( t =>
    {
      val num = t.size
      re(num) +=1
    })

    //控制台输出

    var i = 0
    re.foreach(t =>{
      if(re(i) != 0)
      println("含有 "+i+" 个节点的极大团数有 "+t+" 个")
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
      //println(i+"大小 :"+matrix(i))
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

  def deal_ans(ans:Array[Int], fa:Set[Set[VertexId]]): Array[Set[VertexId]]  ={
    var re : Array[Set[VertexId]] = new Array[Set[VertexId]](fa.size+1);

    val num = fa.size
    //初始化set集合
    for(i <- 0 to num)
      {
          re(i) = Set()
      }

    var i = 0
    fa.foreach(t=>{
      val site = ans(i)
      i += 1;
      re(site) = re(site) ++ t
    })

    return re
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

  def count_edges(R: Set[VertexId],graph:Graph[String,Int]): Int = {
    var c = 0
    for (triplet <- graph.triplets.filter(t => R.contains(t.dstId) && R.contains(t.srcId) ).collect) {
      c+=1
    }
    return c
  }

  def count_degrees(R: Set[VertexId],graph:Graph[String,Int]): Int = {
    var c = 0
    var node = graph.vertices.map( t => t._1.toInt).collect()
    var degree = graph.degrees.collect()
    for(i <- R)
    {
      c += degree(node.indexOf(i))._2
    }
    return c
  }

  def count_Q(R: Set[VertexId],graph:Graph[String,Int]): Double = {
    var c: Double = 0
    var edge: Double =graph.edges.count()

    var degree: Int = count_degrees(R,graph)

    var inner_edge: Int = count_edges(R,graph)

    //计算
    c = inner_edge/edge - Math.pow((degree/2/edge),2)

    return c
  }

}