/* $Header: /var/lib/cvsroot/SapaReplan-MURI/src/edu/asu/sapa/lpsolve/matrec.java,v 1.1 2009/02/19 18:45:11 bentonj Exp $ */
/* $Log: matrec.java,v $
/* Revision 1.1  2009/02/19 18:45:11  bentonj
/* starting to write--broken
/*
/* Revision 1.2  2008/04/28 18:03:49  bentonj
/* *** empty log message ***
/*
/* Revision 1.1.6.1  2008/04/26 03:14:44  will
/* *** empty log message ***
/*
/* Revision 1.1  2006/05/05 21:59:11  bentonj
/* *** empty log message ***
/*
/* Revision 1.1  2006/02/20 06:03:53  bentonj
/* *** empty log message ***
/*
/* Revision 1.1  2005/12/29 21:37:37  minh
/* *** empty log message ***
/*
/* Revision 1.1  2005/12/29 03:32:37  bentonj
/* SapaPS for Minh to work on.
/*
/* Revision 1.1  2004/08/10 03:49:46  minh
/* First version
/*
/* Revision 1.1  2004/06/14 09:36:10  bentonj
/* added LP stuff for post-processing.  fixed a major bug in search (wrong event times associated with events on rare occasions (recharge in rover domain caused this a lot)).
/* also, separation of static predicates/functions now fully functional... as far as we can tell. :)
/*
/* Revision 1.1  2004/05/11 23:15:58  minh
/* test comments.....
/* I did nothing .. hehehe...
/*
# Revision 1.2  1996/06/06  19:47:20  hma
# added package statement
#
# Revision 1.1  1996/05/21  02:04:15  hma
# Initial revision
# */

package edu.asu.sapa.lpsolve;

public class matrec
{
  int row_nr;
  double value;
  public matrec(int r, double v) {
    row_nr = r;
    value = v;
  }
}
