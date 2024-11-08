/*
 * twemcache - Twitter memcached.
 * Copyright (c) 2012, Twitter, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of the Twitter nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef _MC_TIME_H_
#define _MC_TIME_H_

#include <sys/time.h>
#include <time.h>

/*
 * Time relative to server start time in seconds.
 *
 * On systems where size(time_t) > sizeof(unsigned int), this gives
 * us space savings over tracking absolute unix time of type time_t
 */
typedef unsigned int rel_time_t;

void time_update(void);
rel_time_t time_now(void);
time_t time_now_abs(void);
time_t time_started(void);
rel_time_t time_reltime(time_t exptime);
void time_init(void);


/* Finer granularity clock time */
typedef struct clock_time_s
{
	union
	{
		struct timespec _posix_time;	/* For systems that support posix clock time */
		struct time_s					/* This is the member that should always be used */
		{
			time_t 	seconds;
			long	nanoseconds;
		} time;
	};
} clock_time_t;

void clocktime_now(clock_time_t *time);
uint64_t	clocktime_diff_milli(const clock_time_t *time1, const clock_time_t *time2);
int clocktime_compare(const clock_time_t *time1, const clock_time_t *time2);

#endif
