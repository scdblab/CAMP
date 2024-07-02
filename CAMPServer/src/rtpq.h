/* rtpq.h */

#ifndef RTPQ_H_
#define RTPQ_H_

#include <stdio.h> /* FILE stream to print */
#include <mc_items.h>

/* Return status */
#define RTPQ_OK 0
#define RTPQ_ERROR -1

/* Data types */
typedef uint32_t rtpq_key;
typedef struct item* rtpq_value;
typedef struct { rtpq_key key; rtpq_value value; } rtpq_keyval;

/* Container types */
typedef struct rtpq rtpq;
typedef struct rtpq_location rtpq_location;
typedef struct item rtpq_node;

struct rtpq {
  uint32_t max_level;
  uint32_t** tree_levels;
  rtpq_node** leaf_level;
  rtpq_node** leaf_level_tail;
};

struct rtpq_location {
  uint32_t key;
  rtpq_node* node; 
};


/* Allocation and initialization with parameter specifying the number of bits
needed to represent the largest value that will be stored. */
int rtpq_init(rtpq* pq, uint32_t num_bits_range);

/* Memory deallocation. */
int rtpq_deinit(rtpq* pq);

/* Insert item and return location of insertion. */
rtpq_location rtpq_insert(rtpq* pq, rtpq_key key, rtpq_value value);

/* Delete the item at given location. Return status. */
int rtpq_delete(rtpq* pq, rtpq_location loc);

/* Get a key-value pair with smallest key if the container is non-empty. */
int rtpq_minimum(rtpq* pq, rtpq_keyval*);

/* Get a key-value pair with largest key if the container is non-empty. */
int rtpq_maximum(rtpq* pq, rtpq_keyval*);

/* Get a key-value pair with smallest key greater than given key. The parameter
strict = 0 for a soft inequality and = 1 for a hard inequality */
int rtpq_successor(rtpq* pq, rtpq_key true_key, rtpq_keyval*, bool strict);

/* Get a key-value pair with largest key less than given key. The parameter
strict = 0 for a soft inequality and = 1 for a hard inequality */
int rtpq_predecessor(rtpq* pq, rtpq_key true_key, rtpq_keyval*, bool strict);

/* Return whether the queue is empty or not. */
inline bool rtpq_empty(rtpq* pq);

/* Print to stream information about the container. */
/* Setting of the bits of flags yield different information:
 * bit 0 (lowest bit): list of items in each key,
 * bit 1: number of keys with a given number of items,
 * bit 2: number of keys used
 */
void rtpq_print(rtpq* pq, FILE *stream, uint32_t flags);

/* int return type represents success (RTPQ_OK) or failure (RTPQ_ERROR) */

#endif /* RTPQ_H_ */
