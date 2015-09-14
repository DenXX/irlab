The field of question answering has a long history of research and dates back to 60s, when first systems attempted to provide a natural language interface to databases \cite{Simmons:1965:AEQ:363707.363732}.
In 70s and 80s the development of restricted domain knowledge bases set a task for question answering frameworks to assist users in solving their problem, which lead to the development of interactive question answering systems, e.g. \cite{shortliffe1976mycin}, \cite{woods1977lunar}.

The modern era of question answering research started with the exponential growth of information available in the World Wide Web.

Most of the work on question answering have focused on the so called \textit{factoid} questions.
The answers to these questions represent simple facts, that can be expressed as a word or a short phrase, which often represents a named entity (person, organization, location), date, etc.
The other types of questions are sometimes referred to in general as \textit{non-factoid} questions.

Since 1999 the annual Text Retrieval Conference (TREC)\footnote{http://trec.nist.gov} organized a number of open domain question answering shared tasks, e.g. see \cite{dang2007overview} for a review.
This year TREC piloted a LiveQA track\footnote{http://trec-liveqa.org/}, in which the participant systems had to answer various questions coming from real users of Yahoo! Answers\footnote{http://answers.yahoo.com/} in real time.

There are a number of surveys available on developments in the question answering area \cite{Kolomiyets:2011:SQA:2046840.2047162}.

Question Answering can be classified by the type of information used to answer questions into: text/web document collection, knowledge base or text document.
There are some hybrid systems, e.g. IBM WATSON, YODAQA, OPENQA, etc.

---------------

Since the rise of the Internet and exponentially increasing volume of information available in the web, the primary data source for question answering systems were collections of text documents (domain-specific or open, including the whole web).
In the beginning of TREC competition in the late 90s most typical QA were based on information retrieval to get a set of candidate documents and named entity recognition for answer candidate selection and ranking.
The idea was to find a sentence or paragraph rich with occurances of question terms and extract entities of the expected answer type.
However, such systems required building special ontological resources to encode the relationships between different question and answer types, e.g. \cite{hovy2000question}, \cite{prager2006question}.

AskMSR system \cite{brill_askmsr} used the fact that in large collections, such as the web, the same information is expressed multiple times.
Their counting-based approach performed unexpectedly well on TREC 2001 and sparkled the interest in exploring another sources for answer candidates.

Data available in structured form, such as knowledge bases is also a useful source for answers to user questions.
Today a number of developed large-scale open domain knowledge bases (e.g. such as Freebase \cite{Bollacker:2008:FCC:1376616.1376746}, dbPedia\cite{auer2007dbpedia}, NELL \cite{carlson2010toward}) allows to use them for answer selection.
Question answering over linked data

Despite the pioneering works in question answering as a tool for natural language interface to databases,  Structured knowledge databases 

---------------

\subsection{Collection-based QA}
Review first collection algorithms.
Review AskMSR, AskMSR+ and redundancy-based techniques.


\subsection{KB-based QA}
Review QALD and work on webquestions dataset.


WebQuestions \cite{BerantCFL13:sempre}, \cite{YaoD14}, \cite{BerantL14:parasempre}, \cite{ReddyLS14}, \cite{BordesCW14:emnlp}, \cite{yao-scratch-qa-naacl2015}, \cite{bastmore:cikm:2015:aquu}, \cite{yih:ACL:2015:STAGG}





\subsection{Hybrid techniques}
Describe IBM WATSON, OPENQA, YODAQA.

Another interesting approach is based on the idea of Open Information Extraction \cite{Fader:2014:OQA:2623330.2623677}...


OpenQA is an interesting technique as it tries to convert text into OpenIE set of triples and then query it.

Describe recent work that used entity linking to Freebase.

\subsection{Reading Comprehension}
McTest dataset. Maybe this part is not relevant.
However, merging reading comprehesion to regular QA seems natural and interesting.