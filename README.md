# CAMP
This repository contain an implementation of the CAMP algorithm.

Authors:  Shahram Ghandeharizadeh, Sandy Irani, Jenny Lam, and Jason Yap.

## Citations

Shahram Ghandeharizadeh, Sandy Irani, Jenny Lam, and Jason Yap. 2014. CAMP: a cost adaptive multi-queue eviction policy for key-value stores. In Proceedings of the 15th International Middleware Conference (Middleware '14). Association for Computing Machinery, New York, NY, USA, 289–300. https://doi.org/10.1145/2663165.2663317

```
@inproceedings{10.1145/2663165.2663317,
author = {Ghandeharizadeh, Shahram and Irani, Sandy and Lam, Jenny and Yap, Jason},
title = {{CAMP: A Cost Adaptive Multi-Queue Eviction Policy for Key-Value Stores}},
year = {2014},
isbn = {9781450327855},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
url = {https://doi.org/10.1145/2663165.2663317},
doi = {10.1145/2663165.2663317},
abstract = {Cost Adaptive Multi-queue eviction Policy (CAMP) is an algorithm for a general purpose key-value store (KVS) that manages key-value pairs computed by applications with different access patterns, key-value sizes, and varying costs for each key-value pair. CAMP is an approximation of the Greedy Dual Size (GDS) algorithm in that its eviction policy is as effective as GDS. At the same time, its implementation is as efficient at LRU. Similar to an implementation of LRU using queues, it adapts to changing workload patterns based on the history of requests for different key-value pairs. It is superior to LRU because it considers both the size and cost of key-value pairs to maximize the utility of the available memory across competing applications. We compare CAMP with both LRU and an alternative that requires human intervention to partition memory into pools and assign grouping of key-value pairs to different pools. The results demonstrate CAMP is as fast as LRU while outperforming both LRU and the pooled alternative. We also present results from an implementation of CAMP using Twitter's version of memcached.},
booktitle = {Proceedings of the 15th International Middleware Conference},
pages = {289–300},
numpages = {12},
keywords = {performance, key-value stores, cache replacement},
location = {Bordeaux, France},
series = {Middleware '14}
}
```
