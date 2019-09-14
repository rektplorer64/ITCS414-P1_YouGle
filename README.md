# ITCS414 - Project 1 - YouGle: Your First Search Engine
A student project. Mahidol University, Faculty of ICT: `ITCS414 – Information Storage and Retrieval`. 2019

This is a simple Implementation for a Search Engine. 

A simple indexing and retrieval system for a search engine. More specifically, it involves the Implementation a simple indexer that builds an uncompressed index over a corpus, and implement
retrieval for Boolean conjunctive queries.

###### Source Files
- **`BaseIndex.java`** → Interface for the Indexing Algorithm
- **`BasicIndex.java`** → Implementation of the BaseIndex Interface (Algorithm Implementation)
- **`Index.java`** → Container Index Runner which handle Text Files Operation (Read/Write)
- **`P1Tester.java`** → Test Case
- **`Pair.java`** → A Helper Data Structure from Dan Klein
- **`PostingList.java`** → A Data Model which contains Vocab/Word/Term ID and Posting (The place where the Vocab/Word/Term can be found like a Page/Document) ID.
- **`Query.java`** → Part of Program that responsible for Reading/Retrieval of the Vocab
- **`Index.FileUtil.java`** → Utilities for various Parts of the Program

###### Related Algorithms
- Basic
- VB
- Gamma 
- BSBI → Indexing Algorithm