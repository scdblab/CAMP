/*
 * mc_index.h
 *
 *  Created on: Nov 7, 2013
 *      Author: jyap
 */

#ifndef MC_INDEX_H_
#define MC_INDEX_H_

#include <mc_core.h>
#include <rtpq.h>
#include "implicit_heap.h"

#define USE_RTPQ		// Comment out this line to use default LRU
//#define USE_HEAP
typedef uint64_t cost_t;	/* Cost type */

// Forward declaration so that this file can know what pq_node_type is.
// TODO: very messy. fix this circular dependency.
struct implicit_node_t;
typedef struct implicit_node_t implicit_node;
typedef implicit_node pq_node_type;

struct implicit_heap_t;
typedef struct implicit_heap_t implicit_heap;
typedef implicit_heap pq_type;


/* Per-item metadata needed by the index technique */
struct index_lru_metadata {
	TAILQ_ENTRY(item) i_tqe;      /* link in lru q or free q */
	cost_t cost;
};

typedef struct rtpq_index_metadata {
	uint32_t cost;
	uint32_t size;
	rtpq_location location;
	rtpq_node* prev;
	rtpq_node* next;
} rtpq_index_metadata;

typedef struct heap_index_metadata {
	uint32_t cost;
	uint32_t size;
	struct item** prev;
	struct item* next;
	uint32_t priority;
	uint64_t inflated_priority;
} heap_index_metadata;


/* Index structure. */
struct index_lru {
	struct item_tqh lruq;
};

struct index_rtpq {
	uint32_t range;
	uint32_t minimum;
	uint32_t precision;
	uint32_t min_priority[2];
	/* the priority queue */
	rtpq queue;
};

struct index_heap {
	uint32_t range;
	uint64_t minimum;
	uint32_t precision;
	uint32_t min_priority[2];
	/* the components of the priority queue */
	pq_type* queue;
	struct lruq {
		struct item* head;
		struct item** tail;
		pq_node_type* node;
	} * lruqs;
};



#ifdef USE_RTPQ
typedef struct index_rtpq index_struct;
typedef struct rtpq_index_metadata index_metadata;
#elif defined(USE_HEAP)
typedef struct index_heap index_struct;
typedef struct heap_index_metadata index_metadata;
#else
typedef struct index_lru index_struct;
typedef struct index_lru_metadata index_metadata;
#endif

//typedef struct rtpq_index_metadata index_metadata;
//typedef struct index_rtpq index_struct;

/* Initialize index structure. Reads settings for initialization parameters */
int index_init(index_struct* index);

/* Destruct index structure. Cleans up any memory allocated for this structure */
int index_deinit(index_struct* index);

/* Inserts an item into the index */
int index_insert(index_struct* index, struct item* it);

/* Removes an item from the index */
int index_delete(index_struct* index, struct item* it);

/* Given an item, get's the next item with the next highest priority */
struct item* index_get_next(index_struct* index, struct item* prev_item);

/* Gets the item with the lowest priority */
struct item* index_get_lowest(index_struct* index);

/* Mark an item as recently accessed */
int index_touch(struct item* it);

int index_set_cost(struct item* it, cost_t cost);

/* Run all unit tests for this index */
void indextest_run_all(void);


#endif /* MC_INDEX_H_ */
