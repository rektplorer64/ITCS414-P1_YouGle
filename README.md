# ITCS414 - Project 1 - YouGle: Your First Search Engine
A student project. Mahidol University, Faculty of ICT: `ITCS414 – Information Storage and Retrieval`. 2019

This is a simple Implementation for a Search Engine. 

A simple indexing and retrieval system for a search engine. More specifically, it involves the Implementation a simple indexer that builds an uncompressed index over a corpus, and implement
retrieval for Boolean conjunctive queries.

Furthermore, the implementation of TF-IDF Weight with Cosine Similarity is also implemented in this project.

## 😀 Our Members
|#|Name|Surname|Nickname|Student ID|
|-|-|-|-|-|
|1|Anon|Kangpanich|Non|6088053|
|2|Krittin|Chatrinan|Tey|6088022|
|3|Tanawin|Wichit|Pooh|6088221|

## 💾 Source Files
- **`BaseIndex.java`** → Interface for the Indexing Algorithm
- **`BasicIndex.java`** → Implementation of the BaseIndex Interface (Algorithm Implementation)
- **`Index.java`** → Container Index Runner which handle Text Files Operation (Read/Write)
    - **`IndexUtil`** → Utilities which facilitate the Indexing Algorithm
    - **`FileUtil`** → Utilities for File and Binary Manipulations
- **`P1Tester.java`** → Default Test Case that is given by the instructor
- **`Pair.java`** → A Helper Data Structure from Dan Klein
- **`PostingList.java`** → A Data Model which contains Vocab/Word/Term ID and Posting (The place where the Vocab/Word/Term can be found like a Page/Document) ID.
- **`Query.java`** → Part of Program that responsible for Reading/Retrieval of the Vocab
    - **`QueryUtil`** → Utilities which facilitate the Querying Algorithm
    - **`CollectionUtil`** → Utilities which facilitate the Collection or Data Structure Manipulations such as intersection, and other manual implementations of set operations etc.
- **`RankedIndex.java`** → A part of Program that is self-contained for indexing and querying of Ranked Search Results.
    - **`BonusRunner`** → Tester for Ranked Search
    - **`DocumentVector`** → Data class that contains docId, norm, and Mapping between termId and weightScore.
    - **`RankedIndex`** → Indexer that can pre-compute all Document Vector in the corpus and write it to a file.
    - **`RankedIndexer`** → Indexer Helper that facilitate read and write of the TF-IDF weight into a file.
    - **`RankedQuery`** → Query processor that calculates Cosine similarity ranking and returns results based on given search keywords.

## 📔 Related Topics
- **Block Sorting Based Index Algorithms; BSBI** → Indexing Algorithm of Choice for this project
    - **Block of PostingList merging algorithm** 
- **TF-IDF Weight** → Weighting formulation for the similarity of query and documents
- **Cosine similarity** → Measures how similar between query and documents

