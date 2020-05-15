package AAA;
import org.apache.spark.graphx.{Edge, EdgeDirection, Graph, VertexId}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.Set

object COUNT {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("findMaximalCliques").setMaster("local")
    val sc: SparkContext = new SparkContext(conf)
    //定义顶点
    val vertexArray = Array(
      (1L,null),
      (2L,null),
      (3L,null),
      (4L,null),
      (5L,null),
      (6L,null)
    )
    //定义边
    val edgeArray = Array(
      Edge(6L, 4L,null),
      Edge(4L, 3L,null),
      Edge(4L, 5L,null),
      Edge(5L, 2L,null),
      Edge(3L, 2L,null),
      Edge(5L, 1L,null),
      Edge(2L, 1L,null)
    )

    //顶点和边转化为RDD
    val vertexRDD = sc.parallelize(vertexArray)
    val edgeRDD  = sc.parallelize(edgeArray)

    //根据顶点和边创建图
    val graph= Graph(vertexRDD,edgeRDD)

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
    vertexRDD.collect().foreach(t=>{P+=t._1})
    //X集合，初始值为空
    var X = Set[VertexId]()

    //搜索极大团
    bronKerbosch3(R,P,X,map)
  }

  /**
   * 搜索极大团的方法
   * @param R 目前已经在团中的顶点的集合
   * @param P 可能在团中的顶点的集合
   * @param X 不被考虑的顶点的集合
   * @param map Map集合，通过顶点获取该顶点的所有邻居顶点集合
   */
  def bronKerbosch3(R:Set[VertexId],P:Set[VertexId],X:Set[VertexId],map:Map[VertexId, Set[VertexId]]): Unit ={
    if(P.toList.length ==0 && X.toList.length ==0){
      //增加条件不显示两个顶点的极大团
      println("find a maximal cilique:"+R)
    }else {
      //集合头节点充当枢轴u
      var u: VertexId = (P ++ X).head
      var Nu: Set[VertexId] = map.get(u).get
      for (v <- (P &~ Nu)) {
        var Nv: Set[VertexId] = map.get(v).get
        bronKerbosch3(R+v, P.intersect(Nv), X.intersect(Nv), map)
        X += v
        P -= v
      }
    }
  }

}
