/*
 * mc_sqltrig.h
 *
 *  Created on: Apr 10, 2013
 *      Author: Jason Yap
 */

#ifndef MC_SQLTRIG_H_
#define MC_SQLTRIG_H_

#include <mc_core.h>


typedef uint8_t 	trig_keylen_t;
typedef uint32_t	trig_listlen_t;

typedef char*		trig_cursor_t;

#define TRIGGER_BUFFER_SIZE 	1024 * 1024		// Size of each scratch buffer for each thread


/***
 * Each registration contains a trigger to be registered and its corresponding internal token.
 */
typedef struct trig_entry_s
{
	char					*text;
	uint32_t				text_len;
	char					*internal_token;
	uint32_t				internal_token_len;
	uint32_t				template_id;
	struct trig_entry_s		*next;
	char					end[1];
} trig_entry_t;

#define TRIG_ENTRY_SIZE   offsetof(trig_entry_t, end)


/***
 * Collection of trig entries.
 */
typedef struct trig_coll_s
{
	trig_entry_t	*registration_list;
	uint32_t		num_registrations;
	char			*key;
	uint32_t		key_len;
	char			end[1];
} trig_coll_t;

#define TRIG_COLL_SIZE   offsetof(trig_coll_t, end)


typedef enum trig_status_e {
	TRIG_OK,
	TRIG_QUEUED,
	TRIG_ERROR,
	TRIG_NOTFOUND
} trig_status_t;



trig_entry_t* 	trig_allocRegistrationEntry(
		char* trigger_text, uint32_t text_len,
		char* internal_token, uint32_t internal_token_len
		);

trig_coll_t*	trig_allocRegistrationColl(
		char* key, uint32_t key_len
		);

trig_status_t 	trig_checkDuplicateRegistration(char* trigger, trig_listlen_t trigger_len);


trig_status_t	trig_check_keylist(
		char* it_list, trig_listlen_t it_list_len,
		char* new_key, trig_keylen_t new_key_len );

trig_status_t	trig_keylist_addkey(
		char* dest, 		trig_listlen_t *dest_listlen,	trig_listlen_t dest_maxsize,
		char* old_keylist, 	trig_listlen_t old_keylistlen,
		char* new_key, 		trig_keylen_t new_keylen);

trig_listlen_t	trig_new_keylist_size(char* key, trig_keylen_t key_len);

trig_status_t	trig_keylist_merge_lists(
		char *dest_keylist, 	trig_listlen_t *dest_keylistlen, trig_listlen_t dest_maxlen,
		char *input_keylist, 	trig_listlen_t input_keylistlen);

trig_status_t	trig_keylist_next(
    	char *keylist, 		trig_listlen_t keylistlen,
    	trig_cursor_t *delete_cursor,
    	char **key, 		size_t *nkey);

#endif /* MC_SQLTRIG_H_ */
