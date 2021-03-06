# Wikipedia Category Network Pruning

A framework to clean the Wikipedia category network.

## Getting Started

> The category network in Wikipedia is used by editors as a way to label articles and organize them in a hierarchical structure. This manually created and curated network of 1.6 million nodes in English Wikipedia generated by arranging the categories in a child-parent relation (i.e., Scientists-People, Cities-Human Settlement) allows researchers to infer valuable relations between concepts. A clean structure in this format would be a valuable resource for a variety of tools and application including automatic reasoning tools. Unfortunately, Wikipedia category network contains some "noise" since in many cases the association as subcategory does not define an is-a relation (Scientists is-a People vs. Billionaires‎ is-a Wealth). Inspired to develop a model for recommending sections to be added to the already existing Wikipedia articles, we developed a method to clean this network and to keep only the categories that have a high chance to be associated with their children by an is-a relation. The strategy is based on the concept of "pure" categories, and the algorithm uses the types of the attached articles to determine how homogenous the category is. The approach does not rely on any linguistic feature and therefore is suitable for all Wikipedia languages. In this talk, we will discuss the high-level overview of the algorithm and some of the possible applications for the generated network beyond article section recommendations.

[Video](https://www.youtube.com/watch?v=ACevHs0sMMw)

[Slides](https://upload.wikimedia.org/wikipedia/commons/c/cb/Using_Wikipedia_categories_for_research.pdf)

### Prerequisites


To run our code, you need:

- Java 8 (JDK)
- Maven
- The three input files: category network, articles-categories association, the article types

### Data requirements

The graph of the categories MUST be a [Directed Acyclic Graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph). 

We recommend removing cycles with [this](https://github.com/epfl-dlab/GraphCyclesRemoval) implementation of the paper: "A fast and effective heuristic for the feedback arc set problem".


## Build informations:

The code is distributed as a Maven project and it can be compiled with the following command:

```
mvn clean compile assembly:single
```

This will generate a runnable jar file available in the folder _target_.


### Input format:

Three files in this format:

* Category network: 

```
node1⇥parent_node1
node1⇥parent_node2
node1⇥parent_node3
```

* Articles-Categories: 

```
article_id1⇥title1⇥category1
article_id2⇥title2⇥category1
article_id3⇥title3⇥category1
```

Title is not required (useful for debug).


* Articles type 

```
article_id1⇥type
article_id2⇥type
```

**Note**: the format can customized by changing the load functions in Importer.java

### Output format:

```javascript
{"score":0.9804195804195804,"category":"Waikato_Tainui","articles":[342863,2728182,...,51201311]}
{"score":1.0,"category":"People_from_Chistopol","articles":[384682,18482502,479899]}
{"score":0.9734265734265735,"category":"1913_establishments_in_Spain","articles":[8312955,22525255,...,22648141]}
```

## Files description:

Check the comments in the source code and the javadoc for more information.

**Graph.java**

It defines the category network nodes: articles and categories. Both have the concept of virtual ID to compress the lookup table (remove gaps).

**Importer.java**

This file loads the data into memory. It can be changed accordingly with the input files.

**Main.java**

The entry point of the application. It provides some implementations of possible PurityScoreFunction (Gini coefficient, Gini impurity, etc)

**PurityScoreFunction.java**

Interface for score generators

**GraphPruner.java**

This file contains the logic of the algorithm. The constructor requires: the graph, the scoring function, the threshold (to flag the category as pure), the number of threads, and the output file.

## Data

We provide as a sample the dataset used as input and the output generated for the paper "[Structuring Wikipedia Articles with Section Recommendations](https://arxiv.org/abs/1804.05995)".

[Download here](https://doi.org/10.6084/m9.figshare.6157445)

