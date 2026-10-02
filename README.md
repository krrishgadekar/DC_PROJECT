# Experiment 7 — MapReduce in the Distributed Computing Project

## Objective
Implement a basic MapReduce job using Apache Hadoop and use it as the
background data-processing workload for the existing distributed computing
project.

## What is implemented
- Hadoop MapReduce Word Count
- Mapper converts each word into `(word, 1)`
- Reducer adds all values belonging to the same word
- Hadoop is configured to run in local mode, so a full Hadoop cluster is not
  required for the lab demonstration
- Input data is related to the distributed-system project
- The result is written to an output directory

## Project connection
The existing `exp4` project already contains the five-node quorum election
simulation and a background worker thread. Experiment 7 is kept as a separate
MapReduce module so the election implementation is not disturbed. During the
final demonstration, the MapReduce job can be started as the computational
workload while the five-node election/partition simulation is running.

## Build
Install Java and Maven, then open a terminal in this `exp7` folder:

    mvn clean package

## Run on Windows
Use:

    run.bat

or manually:

    java -jar target/mapreduce-exp7-1.0.jar input output

The output will contain a Hadoop part file such as:

    output/part-r-00000

## Expected processing
Input:
    node leader node
    quorum leader

Mapper:
    (node,1)
    (leader,1)
    (node,1)
    (quorum,1)
    (leader,1)

Reducer:
    leader 2
    node 2
    quorum 1
