package AAA;
import org.apache.spark.graphx.{Edge, EdgeDirection, Graph, VertexId}
import Array._
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.Set
import scala.util.control.Breaks

object restore {
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

    var ans : Set[Set[VertexId]] = Set();

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
    bronKerboschl(R,P,X,map,ans)

    //根据公共节点数对图进行合并


    printf("最大团数量为"+ans.size)

    //矩阵两两聚合
    var there = merge(ans)
    there.foreach(t => println(t))

    //进行CPM派系过滤算法
    val ans_1 = CPM_merge(there,2);

    val ans_2 = deal_matrix(ans_1,there.size)

    for(i <- 0 to 2)
      print(ans_2(i)+"  ")

   // var temp =Array{(),(),(),()}

    println("聚合矩阵的结果："+ans_1.size)
    for( i <- 0 to 2)
      {
        for( j <- 0 to 2)
          print(ans_1(i)(j))
        println()
      }

  }

  /**
   * 搜索极大团的方法
   * @param R 目前已经在团中的顶点的集合
   * @param P 可能在团中的顶点的集合
   * @param X 不被考虑的顶点的集合
   * @param map Map集合，通过顶点获取该顶点的所有邻居顶点集合
   */
  def bronKerboschl(R:Set[VertexId],P:Set[VertexId],X:Set[VertexId],map:Map[VertexId, Set[VertexId]],ans : Set[Set[VertexId]]):Unit ={
    if(P.toList.length ==0 && X.toList.length ==0){
      //增加条件不显示两个顶点的极大团
      println("find a maximal cilique:"+R)
      copy_set(ans,R)
    }else {
      for (v <- P) {
        var Nv: Set[VertexId] = map.get(v).get
        bronKerboschl(R+v, P.intersect(Nv), X.intersect(Nv), map, ans)
        X += v
        P -= v
      }
    }
  }

  def merge(fa: Set[Set[VertexId]]): Set[Set[VertexId]] = {

    var re : Set[Set[VertexId]] = Set();
    //双层循环
    val loop = new Breaks;
    while(fa.size>0)
      {
        var m = fa.head
        fa -= m
        loop.breakable {
          for (k <- fa) {
            //可以变稠密，产生新集合
            if (become_dense(m, k)) {
              fa -= k
              m = m++(k)
              loop.break
            }
          }
        }
        //加入新合并簇
        re += m
      }
    return re
  }

  //检测两个团是否可以变稠密
  def become_dense(a:Set[VertexId],b:Set[VertexId]):Boolean ={
    val num1 = a.size
    var num2 =b.size
    var same = a.intersect(b).size
    if(same!= 0 && num1 <= num2+ same)
      {return true}
    else
      {return false}
  }

  def copy_set(fa:Set[Set[VertexId]], son:Set[VertexId]): Unit = {
    var re : Set[VertexId] = Set()
    son.foreach(t => re += t)
    fa += re
  }

  def CPM_merge(fa:Set[Set[VertexId]], standard:Integer ):Array[Array[Int]] ={
    //根据CPM算法思想，得到合并矩阵
    println("集合大小"+fa.size)
    var matrix = ofDim[Int](fa.size,fa.size)
    var i= 0
    var j =0;
    //针对社团组的双重循环
    fa.foreach(t =>
    {
      j = 0
      fa.foreach(r =>
          {
              val same = t.intersect(r).size
              if(same >= standard)
                {matrix(i)(j) = 1}
              else
                {matrix(i)(j) = 0}
              j+=1
          }
      )
      i+=1
    })
    return matrix
  }

  def dfs(matrix:Array[Array[Int]], ans: Array[Int], line:Int, num:Int): Unit = {
       //深搜归并社群
    for(i <- 0 to (num-1)) {
       if(matrix(line)(i)==1 && ans(i) == 0)
         {
           ans(i) = ans(line)
           dfs(matrix,ans, i, num)
         }
    }
  }

  def deal_matrix(matrix:Array[Array[Int]], num:Int):  Array[Int]= {
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

}
