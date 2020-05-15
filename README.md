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
:facepunch: Now I generalize my function in this project  
(1)bronKerboschl              the basic form of bronkerbosch algorithm  
(2)bronkerbosch2              the bronkerbosch algorithm with pivoting  
(3)
