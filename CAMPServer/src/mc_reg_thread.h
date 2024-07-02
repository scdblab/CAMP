/*
 * mc_reg_thread.h
 *
 *  Created on: Mar 05, 2014
 *      Author: Jason Yap
 */

#ifndef MC_REG_THREAD_H_
#define MC_REG_THREAD_H_

#include <mc_core.h>

/* Trigger registration queue */
typedef struct reg_queue_item {
	char* trigger;
	uint32_t trigger_len;
	struct reg_queue_item* next;

	char end[1];			// This MUST be the last field in this struct.
} reg_queue_item;

rstatus_t reg_enqueue(char* str, uint32_t str_len);
void reg_init(char* jdbc_server_hostname);
void reg_deinit(void);

/* Trigger map. Maintains the triggers already observed. */
bool trigger_map_contains(char* str, uint32_t str_len);
rstatus_t trigger_map_add(reg_queue_item* item);


#endif // MC_REG_THREAD_H_
