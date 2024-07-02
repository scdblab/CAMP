#ifndef _MC_LEASE_H_
#define _MC_LEASE_H_

#include <stdint.h>

#define LEASE_EXP_TIME 10	/* default 10 second expiry time */


typedef uint64_t lease_token_t;

typedef enum lease_type {
	LEASE_UNSET = 0,		/* default uninitialized value. once initialized, the token should never be 0 */
	LEASE_VALUE = 1, 		/* when there is no lease and the item is valid */
	LEASE_DELETED = 2,		/* when the item has been deleted */
	LEASE_HOTMISS = 3,		/* another thread is */

	LEASE_SENTINEL,			/* must always be the last element */
} lease_type_t;

#define LEASE_START LEASE_SENTINEL + 1

void			lease_init	(void);
lease_token_t	lease_next_token	(void);



#endif // _MC_LEASE_H_
