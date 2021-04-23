import java.util.*;
import java.io.*;

/*
* William Pattison
* wmpatti
* IT 328
*/


/*
  minimizeDFA uses the NFA2DFA class for reading NFA, string files, and
  reducing NFA to DFA. Once the conversion from NFA to DFA is finished,
  minimizeDFA will minimize the DFA resulting into a DFA that contains less states
  but accepts the same langauge as the NFA and non-reduced DFA. Any unreachable
  states will also be removed to obtain an optimal DFA.
*/



public class minimizeDFA
{

  /*adjacency matrix used to find distinguishable and non distinguishable states.*/
  private int[][] matrix;

  /*List holding the reduced DFA*/
  private ArrayList<ArrayList<Integer>> reduced_DFA = new ArrayList<ArrayList<Integer>>();

  /*Set containing the grouped states*/
  private ArrayList<Set<Integer>> reduced_states = new ArrayList<Set<Integer>>();

  /*Set containing the reduced final states of the DFA*/
  private Set<Integer> reduced_finalStates = new HashSet<Integer>();

  /*List holding the DFA (not reduced)*/
  private ArrayList<ArrayList<Integer>> DFA = new ArrayList<ArrayList<Integer>>();

  /*Set containing the non-reduced final states in DFA*/
  private Set<Integer> finalStates = new HashSet<Integer>();

  /*Set of character inputs that DFA accepts*/
  private ArrayList<String> sigma = new ArrayList<String>();

  /*List of testing strings to validate*/
  private ArrayList<String> strings = new ArrayList<String>();

  /*Name of the file that the program is reading from*/
  private String filename;


  /*Constructor takes in a dfa file input, removes any unreachable states, and creates
  an adjacency matrix of every state.*/
  minimizeDFA(String DFA_file)
  {
    this.filename = DFA_file;
    readFile(DFA_file);
    System.out.println("\nParsing results of "+this.filename+" on strings attached in "+this.filename);
    validateInputs(DFA,finalStates);
    removeUnreachables();
    matrix = new int[this.DFA.size()][this.DFA.size()];
  }


  /*Returns true if two states are distinguishable*/
  private boolean distinguishable(int s1, int s2)
  {
    boolean different = false;
    int val1, val2;
    for(int i = 0; i<sigma.size(); i++)
    {
      val1 = DFA.get(s1).get(i);
      val2 = DFA.get(s2).get(i);
      if(matrix[val1][val2] == 1 || matrix[val2][val1] == 1)
      {different = true; break;}
    }
    return different;
  }


  /*DFA reduction algorithm. Finds distinguishable and non-distinguishable states,
  groups alike states, then creates the reduced table.*/
  public void reduce()
  {
    for(int i = 0; i<matrix.length; i++)
      for(int j = i+1; j<matrix.length; j++)
        if(finalStates.contains(i) != finalStates.contains(j))
          matrix[i][j] = 1;
    while(true)
    {
      boolean changed = false;
      for(int i = 0; i<matrix.length; i++)
      {
        for(int j = i+1; j<matrix.length; j++)
        {
          if(matrix[i][j] != 1)
          {
            if(distinguishable(i,j))
            {
              matrix[i][j] = 1;
              changed = true;
            }
          }
        }
      }
      if(!changed) break;
    }
    groupStates();
    createReducedTable();
  }



  /*Create groupings of the non-distinguishable states*/
  public void groupStates()
  {
    Set<Integer> elements;
    ArrayList<Integer> visited = new ArrayList<Integer>();
    boolean isFinal;
    for(int i = 0; i<matrix.length; i++)
    {
      isFinal = false;
      if(visited.contains(i))
        continue;
      elements = new HashSet<Integer>();
      for(int j = i+1; j<matrix.length; j++)
      {
        if(matrix[i][j] == 0)
        {
          if(finalStates.contains(j))
            isFinal = true;
          elements.add(j);
          visited.add(j);
        }
      }
      if(finalStates.contains(i))
        isFinal = true;
      elements.add(i);
      visited.add(i);
      reduced_states.add(elements);
      if(isFinal)
        reduced_finalStates.add(reduced_states.size()-1);
    }
  }


  /*Return the grouped index of a state from the DFA. Returns -1
  on error.*/
  private int newIndex(int state)
  {
    int counter = 0;
    for(Set<Integer> s : reduced_states)
    {
      for(int el : s)
        if(state == el)
          return counter;
      counter++;
    }
    return -1;
  }


  /*Takes the first entry in each group and finds the group index of each
  alphabet input from the DFA that has not been reduced.*/
  public void createReducedTable()
  {
    int newVal;
    ArrayList<Integer> inputs;
    for(Set<Integer> s : reduced_states)
    {
      inputs = new ArrayList<Integer>();
      Integer[] arr = new Integer[s.size()];
      arr = s.toArray(arr);
      for(int i = 0; i<sigma.size(); i++)
      {
          newVal = newIndex(DFA.get(arr[0]).get(i));
          inputs.add(newVal);
      }
      reduced_DFA.add(inputs);
    }
  }


  /*Remove extra states that are not found in the DFS.*/
  private void removeUnreachables()
  {
    Set<Integer> reachables = findReachables();
    Set<Integer> hashed_finalStates= new HashSet<Integer>();
    ArrayList<ArrayList<Integer>> temp_DFA = new ArrayList<ArrayList<Integer>>();
    HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
    int shift = 0;
    for(int i = 0; i<DFA.size(); i++)
    {
      if(reachables.contains(i))
      {
        temp_DFA.add(DFA.get(i));
        map.put(i,i-shift);
      }
      else
        shift++;
    }
    for(ArrayList<Integer> element : temp_DFA)
      for(int i = 0; i<element.size(); i++)
        element.set(i,map.get(element.get(i)));

    for(int element : finalStates)
      if(reachables.contains(element))
        hashed_finalStates.add(map.get(element));
    finalStates = hashed_finalStates;
    DFA = temp_DFA;
  }


  /*Perform iterative DFS to find reachable states in DFA.*/
  private Set<Integer> findReachables()
  {
    Set<Integer> reachables = new HashSet<Integer>();
    Stack<Integer> s = new Stack<Integer>();
    int currentState= 0, nextState;
    s.push(currentState);
    reachables.add(0);
    while(!s.empty())
    {
      currentState = s.pop();
      for(int i = 0; i<sigma.size(); i++)
      {
        nextState = DFA.get(currentState).get(i);
        if(!reachables.contains(nextState))
        {
          s.push(nextState);
          reachables.add(nextState);
        }
      }
    }
    return reachables;
  }


  /*Validate a string input using the reduced DFA*/
 private void validateInputs(ArrayList<ArrayList<Integer>> DFA, Set<Integer> finalStates)
 {
   int currentState, index, counter = 0, yes = 0, no = 0;
   for(String input : strings)
   {
     currentState = 0;
     for(int i = 0; i<input.length(); i++)
     {
       index = sigma.indexOf(Character.toString(input.charAt(i)));
       if(index != -1)
         currentState = DFA.get(currentState).get(index);
       else
       {
         currentState = -1;
         break;
       }
     }
   if(finalStates.contains(currentState))
   {
     System.out.print("Yes ");
     yes++;
   }
   else
   {
     System.out.print("No  ");
     no++;
   }
   if(++counter % 15 == 0)
     System.out.println();
   }
   System.out.println("Yes: "+yes+" No: "+no);
 }


  /*Print the results of the newly reduced DFA*/
  public void results()
  {
    System.out.println("\nMinimized DFA from "+this.filename+":");
    System.out.print("\nSigma:      ");
    for(String alphabet : sigma)
      System.out.print(alphabet+"      ");
    System.out.println();
    for(int i = 0; i<=sigma.size(); i++)
    System.out.print("-------");
    System.out.println();
    for(int i = 0; i<reduced_DFA.size(); i++)
    {
      System.out.print("    "+i+":      ");
      for(int j = 0; j<reduced_DFA.get(i).size(); j++)
        System.out.print(reduced_DFA.get(i).get(j)+"      ");
      System.out.println();
    }
    for(int i = 0; i<=sigma.size(); i++)
      System.out.print("-------");
    System.out.println("\n0: Initial state");
    for(int i : reduced_finalStates)
      System.out.print(i+" ");
    System.out.println(": Accepting state(s)\n");
    System.out.println("\nParsing results of minimized "+this.filename+" on same set of strings:");
    validateInputs(reduced_DFA,reduced_finalStates);
  }


  /*Read in any .dfa file and store contents into program.*/
  private void readFile(String dfa_file)
  {
    try{
      BufferedReader br = new BufferedReader(new FileReader(dfa_file));
      ArrayList<Integer> transitions;
      int numStates = Integer.parseInt(br.readLine());
      String temp = br.readLine().replaceAll(" +"," ");
      StringTokenizer st = new StringTokenizer(temp," ");
      StringTokenizer st2;
      st.nextToken();
      while(st.hasMoreTokens())
        sigma.add(st.nextToken());
      br.readLine();
      for(int i = 0; i<numStates; i++)
      {
        temp = br.readLine().replaceAll(" +"," ");
        st = new StringTokenizer(temp," ");
        st.nextToken();
        transitions = new ArrayList<Integer>();
        while(st.hasMoreTokens())
          transitions.add(Integer.parseInt(st.nextToken()));
        DFA.add(transitions);
      }
      br.readLine();
      br.readLine();
      st = new StringTokenizer(br.readLine()," ");
      st2 = new StringTokenizer(st.nextToken(),",:");
      while(st2.hasMoreTokens())
        finalStates.add(Integer.parseInt(st2.nextToken()));
      temp = br.readLine();
      while(temp!=null)
      {
        strings.add(temp);
        temp = br.readLine();
      }
      br.close();
    } catch(IOException e){
      System.out.println("Unable to open "+dfa_file);
      System.exit(1);
    }
  }


  /*Run the code by taking in command line input*/
  public static void main(String[] args)
  {
    if(args.length != 1)
      System.out.println("Use: java minimizeDFA <DFA file>");
    minimizeDFA test = new minimizeDFA(args[0]);
    test.reduce();
    test.results();
  }
}
