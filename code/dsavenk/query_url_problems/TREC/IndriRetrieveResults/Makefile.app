## your application name here
INTERSECT=IntersectDocuments
INTERSECTSRC=$(INTERSECT).cpp

DUMPDOC=DumpDocTerms
DUMPDOCSRC=$(DUMPDOC).cpp

TERMBYID=TermById
TERMBYIDSRC=$(TERMBYID).cpp

IDEALTERMWEIGHT=IdealTermWeights
IDEALTERMWEIGHTSRC=$(IDEALTERMWEIGHT).cpp

RETRIEVE=RetrieveSearchResults
RETRIEVESRC=$(RETRIEVE).cpp

GETDOC=GetDocText
GETDOCSRC=$(GETDOC).cpp

## extra object files for your app here
OBJ=

prefix = /usr/local
exec_prefix = ${prefix}
libdir = ${exec_prefix}/lib
includedir = ${prefix}/include
INCPATH=-I$(includedir) -I/home/dsavenk/local/include/
LIBPATH=-L$(libdir) -L/home/dsavenk/local/lib/
CXXFLAGS=-DPACKAGE_NAME=\"querysubst\" -DPACKAGE_TARNAME=\"querysubst\" -DYYTEXT_POINTER=1 -DINDRI_STANDALONE=1 -DHAVE_LIBM=1 -DHAVE_LIBPTHREAD=1 -DHAVE_LIBZ=1 -DHAVE_NAMESPACES= -DISNAN_IN_NAMESPACE_STD= -DSTDC_HEADERS=1 -DHAVE_SYS_TYPES_H=1 -DHAVE_SYS_STAT_H=1 -DHAVE_STDLIB_H=1 -DHAVE_STRING_H=1 -DHAVE_MEMORY_H=1 -DHAVE_STRINGS_H=1 -DHAVE_INTTYPES_H=1 -DHAVE_STDINT_H=1 -DHAVE_UNISTD_H=1 -DHAVE_FSEEKO=1 -DHAVE_EXT_ATOMICITY_H=1 -DP_NEEDS_GNU_CXX_NAMESPACE=1 -DHAVE_MKSTEMP=1 -DHAVE_MKSTEMPS=1 -g -O3 -DNDEBUG=1 $(INCPATH) -pg -g3
CPPLDFLAGS = -lindri -lz -lpthread -lm 

dumpdocs:
	$(CXX) $(CXXFLAGS) $(DUMPDOCSRC) -o $(DUMPDOC) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)

intersect:
	$(CXX) $(CXXFLAGS) $(INTERSECTSRC) -o $(INTERSECT) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)

termbyid:
	$(CXX) $(CXXFLAGS) $(TERMBYIDSRC) -o $(TERMBYID) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)

ideal:
	$(CXX) $(CXXFLAGS) $(IDEALTERMWEIGHTSRC) -o $(IDEALTERMWEIGHT) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)

retrieve:	
	$(CXX) $(CXXFLAGS) $(RETRIEVESRC) -o $(RETRIEVE) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)

getdoc:	
	$(CXX) $(CXXFLAGS) $(GETDOCSRC) -o $(GETDOC) $(OBJ) $(LIBPATH) $(CPPLDFLAGS)


all:
	dumpdocs
	intersect
	termbyid
	ideal
	retrieve
	getdoc

clean:
	rm -f $(DUMPDOC)
	rm -f $(INTERSECT)
	rm -f $(TERMBYID)
	rm -f $(IDEALTERMWEIGHT)


