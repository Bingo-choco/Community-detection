package AAA;
import org.apache.spark.graphx.{Edge, EdgeDirection, Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.Set

object Count1 {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("findMaximalCliques").setMaster("local")
    val sc: SparkContext = new SparkContext(conf)
    //定义顶点
    val vertexArray = Array(
      (1L, null),
      (2L, null),
      (3L, null),
      (4L, null),
      (5L, null),
      (6L, null)
    )
    //定义边
    val edgeArray = Array(
      Edge(6L, 4L, null),
      Edge(4L, 3L, null),
      Edge(4L, 5L, null),
      Edge(5L, 2L, null),
      Edge(3L, 2L, null),
      Edge(5L, 1L, null),
      Edge(2L, 1L, null)
    )

    //顶点和边转化为RDD
    val vertexRDD = sc.parallelize(vertexArray)
    val edgeRDD = sc.parallelize(edgeArray)

    //根据顶点和边创建图
    val graph = Graph(vertexRDD, edgeRDD)

    var trip = graph.triplets

    //P集合，初始值为所有的顶点
    var P = Set(1L,2L,3L,4L,5L,6L)
    //将所有的顶点添加到P集合中

    println("结果"+count_Q(P,graph))
  }

  /**
   * 搜索极大团的方法
   *
   * @param R   目前已经在团中的顶点的集合
   * @param P   可能在团中的顶点的集合
   * @param X   不被考虑的顶点的集合
   * @param map Map集合，通过顶点获取该顶点的所有邻居顶点集合
   */
  def bronKerboschl(R: Set[VertexId], P: Set[VertexId], X: Set[VertexId], map: Map[VertexId, Set[VertexId]]): Unit = {
    if (P.toList.length == 0 && X.toList.length == 0) {
      //增加条件不显示两个顶点的极大团
      println("find a maximal cilique:" + R)
    } else {
      for (v <- P) {
        var Nv: Set[VertexId] = map.get(v).get
        bronKerboschl(R + v, P.intersect(Nv), X.intersect(Nv), map)
        X += v
        P -= v
      }
    }
  }

  def count_edge(R: Set[VertexId],graph:Graph[Null,Null]): Int = {
    var c = 0
       for (triplet <- graph.triplets.filter(t => R.contains(t.dstId) && R.contains(t.srcId) ).collect) {
          c+=1
    }
    return c
  }

  def count_degrees(R: Set[VertexId],graph:Graph[Null,Null]): Int = {
    var c = 0
    var node = graph.vertices.map( t => t._1.toInt).collect()
    var degree = graph.degrees.collect()
    for(i <- R)
      {
        c += degree(node.indexOf(i))._2
      }
    return c
  }

  def count_Q(R: Set[VertexId],graph:Graph[Null,Null]): Double = {
    var c: Double = 0
    var edge: Double =graph.edges.count()

    var degree: Int = count_degrees(R,graph)

    var inner_edge: Int = count_edge(R,graph)

    //计算
    c = inner_edge/edge - Math.pow((degree/2/edge),2)

    return c
  }
}

