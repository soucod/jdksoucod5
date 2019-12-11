/*
 * @(#)MOD_munitrk.c	1.12 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*****************************************************************************/
/*
**
**	History	-
**	12/1/96		Created
**	2/13/97		Reenabled MyCmp and it fixed a pattern bug.
**	7/1/97		Increased BUFPAGE and TRESHOLD by three
**	7/17/97		Added compile time switch
**	11/10/97	Changed some preprocessor tests and flags to explicity test for flags rather
**				than assume
**	12/18/97	Cleaned up some warnings
*/
/*****************************************************************************/
#include "X_API.h"

#if USE_MOD_API == TRUE
#include "MOD_mikmod.h"
/*

Name:
MUNITRK.C

Description:
All routines dealing with the manipulation of UNITRK(tm) streams

Portability:
All systems - all compilers

*/

#define BUFPAGE  (128*3)            /* smallest unibuffer size */
#define TRESHOLD (16*3)

/* unibuffer is increased by BUFPAGE
  bytes when unipc reaches unimax-TRESHOLD */



/*
	Ok.. I'll try to explain the new internal module format.. so here it goes:


	The UNITRK(tm) Format:
	======================

	A UNITRK stream is an array of bytes representing a single track
	of a pattern. It's made up of 'repeat/length' bytes, opcodes and
	operands (sort of a assembly language):

	rrrlllll
	[REP/LEN][OPCODE][OPERAND][OPCODE][OPERAND] [REP/LEN][OPCODE][OPERAND]..
	^                                         ^ ^
	|-------ROWS 0 - 0+REP of a track---------| |-------ROWS xx - xx+REP of a track...


	The rep/len byte contains the number of bytes in the current row,
	_including_ the length byte itself (So the LENGTH byte of row 0 in the
	previous example would have a value of 5). This makes it easy to search
	through a stream for a particular row. A track is concluded by a 0-value
	length byte.

	The upper 3 bits of the rep/len byte contain the number of times -1 this
	row is repeated for this track. (so a value of 7 means this row is repeated
	8 times)

	Opcodes can range from 1 to 255 but currently only opcodes 1 to 19 are
	being used. Each opcode can have a different number of operands. You can
	find the number of operands to a particular opcode by using the opcode
	as an index into the 'unioperands' table.

*/



const UWORD unioperands[256]={
    0,              /* not used */
    1,              /* UNI_NOTE */
    1,              /* UNI_INSTRUMENT */
    1,              /* UNI_PTEFFECT0 */
    1,              /* UNI_PTEFFECT1 */
    1,              /* UNI_PTEFFECT2 */
    1,              /* UNI_PTEFFECT3 */
    1,              /* UNI_PTEFFECT4 */
    1,              /* UNI_PTEFFECT5 */
    1,              /* UNI_PTEFFECT6 */
    1,              /* UNI_PTEFFECT7 */
    1,              /* UNI_PTEFFECT8 */
    1,              /* UNI_PTEFFECT9 */
    1,              /* UNI_PTEFFECTA */
    1,              /* UNI_PTEFFECTB */
    1,              /* UNI_PTEFFECTC */
    1,              /* UNI_PTEFFECTD */
    1,              /* UNI_PTEFFECTE */
    1,              /* UNI_PTEFFECTF */
    1,				/* UNI_S3MEFFECTA */
    1,              /* UNI_S3MEFFECTD */
    1,              /* UNI_S3MEFFECTE */
    1,              /* UNI_S3MEFFECTF */
    1,              /* UNI_S3MEFFECTI */
    1,              /* UNI_S3MEFFECTQ */
    1,				/* UNI_S3MEFFECTT */
    1,				/* UNI_XMEFFECTA */
    1,				/* UNI_XMEFFECTG */
    1,				/* UNI_XMEFFECTH */
    1				/* UNI_XMEFFECTP */
};


/***************************************************************************
>>>>>>>>>>> Next are the routines for reading a UNITRK stream: <<<<<<<<<<<<<
***************************************************************************/


static UBYTE *rowstart;		/* startadress of a row */
static UBYTE *rowend;      	/* endaddress of a row (exclusive) */
static UBYTE *rowpc;       	/* current unimod(tm) programcounter */


void UniSetRow(UBYTE *t)
{
    rowstart=t;
    rowpc=rowstart;
    rowend=rowstart+(*(rowpc++)&0x1f);
}


UBYTE UniGetByte(void)
{
    return (rowpc<rowend) ? *(rowpc++) : 0;
}


void UniSkipOpcode(UBYTE op)
{
    UWORD t=unioperands[op];
    while(t--) UniGetByte();
}


UBYTE *UniFindRow(UBYTE *t,UWORD row)
/*
	Finds the address of row number 'row' in the UniMod(tm) stream 't'

	returns NULL if the row can't be found.
*/
{
    UBYTE c,l;

    while(1){

	c=*t;					/* get rep/len byte */

	if(!c) return NULL;		/* zero ? -> end of track.. */

	l=(c>>5)+1;				/* extract repeat value */

	if(l>row) break;		/* reached wanted row? -> return pointer */

	row-=l;					/* havn't reached row yet.. update row */
	t+=c&0x1f;				/* point t to the next row */
    }

    return t;
}



/***************************************************************************
>>>>>>>>>>> Next are the routines for CREATING UNITRK streams: <<<<<<<<<<<<<
***************************************************************************/


static UBYTE *unibuf;		/* pointer to the temporary unitrk buffer */
static UWORD unimax;		/* maximum number of bytes to be written to this buffer */

static UWORD unipc;			/* index in the buffer where next opcode will be written */
static UWORD unitt;        	/* holds index of the rep/len byte of a row */
static UWORD lastp;			/* holds index to the previous row (needed for compressing) */


void UniReset(void)
/*
	Resets index-pointers to create a new track.
*/
{
    unitt=0;		/* reset index to rep/len byte */
    unipc=1;		/* first opcode will be written to index 1 */
    lastp=0;		/* no previous row yet */
    unibuf[0]=0;	/* clear rep/len byte */
}


void UniWrite(UBYTE data)
/*
	Appends one byte of data to the current row of a track.
*/
{
    /* write byte to current position and update */

    unibuf[unipc++]=data;

    /* Check if we've reached the end of the buffer */

    if(unipc>(unimax-TRESHOLD))
	{
	    UBYTE *newbuf;

	    /* We've reached the end of the buffer, so expand
	       the buffer by BUFPAGE bytes */

	    newbuf=(UBYTE *)mod_realloc(unibuf,unimax+BUFPAGE);

	    /* Check if realloc succeeded */

	    if(newbuf!=NULL)
		{
		    unibuf=newbuf;
		    unimax+=BUFPAGE;
		}
	    else
		{
		
		    /* realloc failed, so decrease unipc so we won't write beyond
		       the end of the buffer.. I don't report the out-of-memory
		       here; the UniDup() will fail anyway so that's where the
		       loader sees that something went wrong */

		    unipc--;
		}
	}
}


void UniInstrument(UBYTE ins)
/*
	Appends UNI_INSTRUMENT opcode to the unitrk stream.
*/
{
    UniWrite(UNI_INSTRUMENT);
    UniWrite(ins);
}


void UniNote(UBYTE note)
/*
	Appends UNI_NOTE opcode to the unitrk stream.
*/
{
    UniWrite(UNI_NOTE);
    UniWrite(note);
}


void UniPTEffect(UBYTE eff,UBYTE dat)
/*
	Appends UNI_PTEFFECTX opcode to the unitrk stream.
*/
{
    if(eff!=0 || dat!=0){				/* don't write empty effect */
	UniWrite((UBYTE)(UNI_PTEFFECT0 + eff));
	UniWrite(dat);
    }
}


BOOL MyCmp(UBYTE *a,UBYTE *b,UWORD l)
{
    UWORD t;

    for(t=0;t<l;t++){
	if(*(a++)!=*(b++)) return 0;
    }
    return 1;
}

void UniNewline(void)
/*
	Closes the current row of a unitrk stream (updates the rep/len byte)
	and sets pointers to start a new row.
*/
{
    UWORD n,l,len;

    n=(unibuf[lastp]>>5)+1;		/* repeat of previous row */
    l=(unibuf[lastp]&0x1f);		/* length of previous row */

    len=unipc-unitt;			/* length of current row */

    /* Now, check if the previous and the current row are identical..
       when they are, just increase the repeat field of the previous row */

    if (n<8 && len==l && MyCmp(&unibuf[lastp+1], &unibuf[unitt+1], (UWORD)(len - 1))){
	unibuf[lastp]+=0x20;
	unipc=unitt+1;
    }
    else{
	/* current and previous row aren't equal.. so just update the pointers */

	unibuf[unitt] = (UBYTE)len;
	lastp=unitt;
	unitt=unipc;
	unipc++;
    }
}


UBYTE *UniDup(void)
/*
	Terminates the current unitrk stream and returns a pointer
	to a copy of the stream.
*/
{
    UBYTE *d;

    unibuf[unitt]=0;

    if((d=(UBYTE *)mod_malloc(unipc))==NULL){
	gModPlayerErrorMessage=ERROR_ALLOC_STRUCT;
	return NULL;
    }
    memcpy(d,unibuf,unipc);

    return d;
}


UWORD TrkLen(UBYTE *t)
/*
	Determines the length (in rows) of a unitrk stream 't'
*/
{
    UWORD len=0;
    UBYTE c;

    while((c=*t&0x1f) != 0){
	len+=c;
	t+=c;
    }
    len++;

    return len;
}


BOOL UniInit(void)
{
    unimax=BUFPAGE;

    if(!(unibuf=(UBYTE *)mod_malloc(unimax))){
	gModPlayerErrorMessage=ERROR_ALLOC_STRUCT;
	return 0;
    }
    return 1;
}


void UniCleanup(void)
{
    if(unibuf!=NULL) mod_free(unibuf);
    unibuf=NULL;
}

#endif	// USE_MOD_API

