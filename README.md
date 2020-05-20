:bulb: The latest and final version of this algorithm, its route is BK\src\main\scala\BBB\Final.scala  
:scissors: I'm so sorry that I donn't have sufficient time to sort up my code.  
I'll gradually update it in the next few days after my first Commit.:stuck_out_tongue_closed_eyes:  

# Community-detection
use [spark+scala] to implement CPM(Cluster Percolation Method):smiley:   
As we konw, the CPM algorithm has three steps on the whole, but I have made a little alteration for the dataset and my project.  
My algorithm process is shown below.   
  
1.Find all kliques of size k(or larger than k) in the graph,  
As a result, Bron-kerbosch algorithm is an efficient choice for this step.  
ps: There are two versions of BK that are most commonly used: (1)the basic form; (2)with pivoting.   
For my experience, the second one has a better performance :smirk:  
  
2.Fliter noisy nodes to make the final evaluation(use the modularity) valuable, so I create a new subgraph based on the initial graph.  
ps:the noisy nodes are refer to the ones who has few connections with others (such as: the isolated points)  

3.Statistic whether these two maximal cliques are 'related'(if they contain the same nodes more than n),  
If they are 'related', they could be merged together.

:confused:ps:  
(1)the author of the paper use the matrix to finally merge all the 'related' maximal cliques together, I use the set to store which
maximal cliques are 'related' with the current one. However, this alteratin doesn't make any difference essentially.  
(2)n is just a variable, you can adjust it with your result. :relaxed:   
 
4.Use the ans of step 3 to merge  the 'related' maximal cliques into one community.  

5.Use the modularity to evaluate the outcome.  

'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''  
The first update after I firstly wrote Readme: 5/20/2020 14:26  
:zap:About dataset  
:thought_balloon: The dataset I finally used to test my algorithm, is also the one showed on the console.  
:speech_balloon:Its description is below:    
This dataset consists of 1293 terrorist attacks each assigned one of 6 labels indicating the type of the attack. Each attack is   described by a 0/1-valued vector of attributes whose entries indicate the absence/presence of a feature. There are a total of 106   distinct features. The files in the dataset can be used to create two distinct graphs.     
Although the dataset has its real labels essentially, you can refer that its labels is aimed at the classification which apply  
0/1 vectors of attributes on the algorithm.  
:maple_leaf:As a result, the two common methods for evaluation in community detection: (1) rand index (2) modularity  
We could only use the latter, because labels are unavailabel. 
  
:mushroom:ps:this dataset I only use file named 'terrorist_attack_loc.edges' which contains all the edges that we should take into consideration   and changed it into the format, eg: 23 200 this means that the '23' is related to the '200'.  
As for the nodes, it only need to create a range (1,1293) which is no need to read the node file.  
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''' 
:facepunch: Now I generalize my function in this project  
(1)bronKerboschl              the basic form of bronkerbosch algorithm  
(2)bronkerbosch2              the bronkerbosch algorithm with pivoting  
(3)
