/* bit_ops.h */

#ifndef BIT_OPS_H_
#define BIT_OPS_H_

#include <mc_core.h> /* defines uint32_t and uint64_t */

#define BO_WORD_SIZE 32
#define BO_LOG_WORD_SIZE 5
#define BO_NULL_BIT_POS (uint32_t) ~0

/* most significant bit set or null_bit_pos if input is 0 */
inline uint32_t bo_msb(uint32_t word);

/* least significant bit set or null_bit_pos if input is 0 */
inline uint32_t bo_lsb(uint32_t word);

/* nearest bit set to 1 that is to the left of given bit or
 * BO_NULL_BIT_POS if there is no such set bit */
inline uint32_t bo_nbl(uint32_t word, uint32_t index);

/* nearest bit set to 1 that is to the right of given bit or
 * BO_NULL_BIT_POS if there is no such set bit */
inline uint32_t bo_nbr(uint32_t word, uint32_t index);

/* ceiling of the log base 2 of given integer */
inline uint32_t bo_ceil_log(uint32_t word);

/* number of significant bits = msb + 1 */
inline uint32_t bo_bits_in_32(uint32_t word);
inline uint32_t bo_bits_in_64(uint64_t word);

/* bit value at bit position in given word  */
inline bool bo_bit(uint32_t word, uint32_t index);

/* toggle bit value at bit postion in given word */
inline uint32_t bo_toggle(uint32_t word, uint32_t index);

/* binary representation, b is a pointer to a char array of size 
   BO_WORD_SIZE + 1 */
inline void bo_binary_repr(uint32_t n, char* b);

#endif /* BIT_OPS_H_ */
