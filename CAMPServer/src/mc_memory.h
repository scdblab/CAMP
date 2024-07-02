/*
 * mc_memory.h
 *
 *  Created on: Nov 7, 2013
 *      Author: jyap
 */

#ifndef MC_MEMORY_H_
#define MC_MEMORY_H_

#include <mc_items.h>

struct mem_instance {
	uint64_t allocated_bytes;
	uint64_t max_bytes;
	pthread_mutex_t lock;
};

void mem_init(struct mem_instance* inst, size_t max_cachesize);
void mem_deinit(struct mem_instance* inst);

struct item* mem_alloc(struct mem_instance* inst, size_t size);
void mem_free(struct mem_instance* inst, struct item* it);

#endif /* MC_MEMORY_H_ */
